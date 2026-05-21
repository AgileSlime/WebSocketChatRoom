package dw.webSocket;

import com.alibaba.fastjson.JSONObject;
import dw.pojo.Msg;
import dw.pojo.UserSession;
import dw.service.OfflineMessageService;
import dw.util.JWTUtil;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ServerEndpoint(value = "/chatroom")
public class WSServPoint {

    private static final ConcurrentHashMap<Session, UserSession> userSessions = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Session> usernameToSession = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Set<Session>> rooms = new ConcurrentHashMap<>();
    private static final OfflineMessageService offlineService = new OfflineMessageService();
    private static final ScheduledExecutorService heartbeater = Executors.newScheduledThreadPool(2);
    private static volatile boolean initialized = false;

    private static final long HEARTBEAT_TIMEOUT = 30000; // 30 seconds
    private static final long PING_INTERVAL = 10000;     // 10 seconds

    public WSServPoint() {
        if (!initialized) {
            synchronized (WSServPoint.class) {
                if (!initialized) {
                    // heartbeat check: remove users who haven't responded for 30s
                    heartbeater.scheduleAtFixedRate(() -> {
                        long now = System.currentTimeMillis();
                        userSessions.entrySet().removeIf(entry -> {
                            if (now - entry.getValue().getLastHeartbeat() > HEARTBEAT_TIMEOUT) {
                                Session s = entry.getKey();
                                UserSession us = userSessions.get(s);
                                System.out.println("[Heartbeat] Removing inactive user: " + (us != null ? us.getUsername() : "unknown"));
                                try { s.close(); } catch (Exception ignored) {}
                                return true;
                            }
                            return false;
                        });
                    }, 10, 10, TimeUnit.SECONDS);

                    // ping all users every 10 seconds
                    heartbeater.scheduleAtFixedRate(() -> {
                        for (Session s : userSessions.keySet()) {
                            if (s.isOpen()) {
                                try {
                                    s.getBasicRemote().sendText("{\"type\":\"PING\",\"msgid\":\"" + UUID.randomUUID().toString() + "\"}");
                                } catch (Exception ignored) {}
                            }
                        }
                    }, PING_INTERVAL, PING_INTERVAL, TimeUnit.SECONDS);
                    initialized = true;
                }
            }
        }
    }

    @OnOpen
    public void onOpen(Session session) throws UnsupportedEncodingException {
        String query = session.getQueryString();
        if (query == null) query = "";
        query = URLDecoder.decode(query, "utf-8");
        Map<String, String> map = parseQuery(query);

        String token = map.get("token");
        String roomId = map.get("roomId");

        if (token == null || !JWTUtil.validateToken(token)) {
            try { session.close(); } catch (Exception ignored) {}
            return;
        }

        String username = JWTUtil.getUsernameFromToken(token);
        if (username == null) {
            try { session.close(); } catch (Exception ignored) {}
            return;
        }

        // Close old session if same user reconnects
        Session oldSession = usernameToSession.put(username, session);
        if (oldSession != null && oldSession != session) {
            if (oldSession.isOpen()) {
                try { oldSession.close(); } catch (Exception ignored) {}
            }
            UserSession oldUs = userSessions.remove(oldSession);
            if (oldUs != null && roomId != null) {
                Set<Session> room = rooms.get(roomId);
                if (room != null) room.remove(oldSession);
            }
        }

        UserSession us = new UserSession(session, username);
        us.setRoomId(roomId);
        userSessions.put(session, us);

        if (roomId != null) {
            rooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        }

        // Pull offline messages
        try {
            List<String> offlineMsgs = offlineService.getOfflineMessages(username);
            for (String msgJson : offlineMsgs) {
                try {
                    if (session.isOpen()) {
                        session.getBasicRemote().sendText(msgJson);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("[Redis] Failed to get offline messages for " + username + ": " + e.getMessage());
        }

        // Load recent 50 room messages from Redis (only if roomId provided)
        if (roomId != null) {
            try {
                List<String> roomHistory = offlineService.getRoomMessages(roomId, 50);
                for (String msgJson : roomHistory) {
                    try {
                        if (session.isOpen()) {
                            session.getBasicRemote().sendText(msgJson);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                System.err.println("[Redis] Failed to get room history for " + roomId + ": " + e.getMessage());
            }
        }

        // Only broadcast join if a room is provided and this is a new session
        if (roomId != null && oldSession == null) {
            Msg msg = new Msg();
            msg.setType("SYSTEM");
            msg.setMsgSender("system");
            msg.setMsgDate(new Date());
            msg.setUserList(getUserList(roomId));
            msg.setMsgInfo(username + " entered the room");
            broadcastToRoom(roomId, JSONObject.toJSONString(msg));
            System.out.println("[OnOpen] User joined: " + username + " | Room: " + roomId);
        }
    }

    @OnClose
    public void onClose(Session session) {
        UserSession us = userSessions.remove(session);
        if (us != null) {
            String roomId = us.getRoomId();
            Session current = usernameToSession.get(us.getUsername());
            // Only broadcast if this is not a reconnect (i.e., the session being closed is the current one)
            if (current == session) {
                usernameToSession.remove(us.getUsername());
            }
            if (roomId != null) {
                Set<Session> room = rooms.get(roomId);
                if (room != null) {
                    room.remove(session);
                }

                // Only broadcast if this is a real disconnect (not a reconnect)
                if (current == session) {
                    Msg msg = new Msg();
                    msg.setType("SYSTEM");
                    msg.setMsgSender("system");
                    msg.setMsgDate(new Date());
                    msg.setUserList(getUserList(roomId));
                    msg.setMsgInfo(us.getUsername() + " left the room");
                    broadcastToRoom(roomId, JSONObject.toJSONString(msg));
                    System.out.println("[OnClose] User left: " + us.getUsername() + " | Room: " + roomId);
                }
            }
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
        Msg msg = JSONObject.parseObject(message, Msg.class);
        UserSession us = userSessions.get(session);
        if (us == null) return;

        if ("PONG".equals(msg.getType())) {
            us.setLastHeartbeat(System.currentTimeMillis());
            return;
        }

        if ("SWITCH_ROOM".equals(msg.getType())) {
            String oldRoomId = us.getRoomId();
            String newRoomId = msg.getRoomId();
            // Leave old room
            if (oldRoomId != null) {
                Set<Session> oldRoom = rooms.get(oldRoomId);
                if (oldRoom != null) {
                    oldRoom.remove(session);
                    Msg leaveMsg = new Msg();
                    leaveMsg.setType("SYSTEM");
                    leaveMsg.setMsgSender("system");
                    leaveMsg.setMsgDate(new Date());
                    leaveMsg.setUserList(getUserList(oldRoomId));
                    leaveMsg.setMsgInfo(us.getUsername() + " left the room");
                    broadcastToRoom(oldRoomId, JSONObject.toJSONString(leaveMsg));
                }
            }
            // Join new room
            us.setRoomId(newRoomId);
            if (newRoomId != null) {
                rooms.computeIfAbsent(newRoomId, k -> ConcurrentHashMap.newKeySet()).add(session);
                Msg joinMsg = new Msg();
                joinMsg.setType("SYSTEM");
                joinMsg.setMsgSender("system");
                joinMsg.setMsgDate(new Date());
                joinMsg.setUserList(getUserList(newRoomId));
                joinMsg.setMsgInfo(us.getUsername() + " entered the room");
                broadcastToRoom(newRoomId, JSONObject.toJSONString(joinMsg));
                // Load room history
                try {
                    List<String> history = offlineService.getRoomMessages(newRoomId, 50);
                    for (String msgJson : history) {
                        if (session.isOpen()) {
                            session.getBasicRemote().sendText(msgJson);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[Redis] Failed to get room history for " + newRoomId + ": " + e.getMessage());
                }
            }
            return;
        }

        if ("LOAD_PRIVATE_HISTORY".equals(msg.getType())) {
            String otherUser = msg.getMsgReceiver();
            if (otherUser != null) {
                try {
                    List<String> history = offlineService.getPrivateMessages(us.getUsername(), otherUser, 50);
                    for (String msgJson : history) {
                        if (session.isOpen()) {
                            session.getBasicRemote().sendText(msgJson);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[Redis] Failed to get private history for " + otherUser + ": " + e.getMessage());
                }
            }
            return;
        }

        msg.setMsgSender(us.getUsername());
        msg.setMsgDate(new Date());
        if (msg.getMsgid() == null) {
            msg.setMsgid(UUID.randomUUID().toString());
        }
        if ("CHAT".equals(msg.getType()) || msg.getType() == null) {
            msg.setRoomId(us.getRoomId());
            try {
                offlineService.saveRoomMessage(us.getRoomId(), JSONObject.toJSONString(msg));
            } catch (Exception e) {
                System.err.println("[Redis] Failed to save room message: " + e.getMessage());
            }
            broadcastToRoom(us.getRoomId(), JSONObject.toJSONString(msg));
        } else if ("PRIVATE".equals(msg.getType())) {
            try {
                offlineService.savePrivateMessage(us.getUsername(), msg.getMsgReceiver(), JSONObject.toJSONString(msg));
            } catch (Exception e) {
                System.err.println("[Redis] Failed to save private message: " + e.getMessage());
            }
            sendPrivate(msg, us.getUsername());
        }
    }

    @OnError
    public void onError(Session session, Throwable t) {
        t.printStackTrace();
    }

    private void sendPrivate(Msg msg, String senderUsername) {
        String target = msg.getMsgReceiver();
        boolean found = false;
        for (UserSession us : userSessions.values()) {
            if (us.getUsername().equals(target)) {
                try {
                    us.getSession().getBasicRemote().sendText(JSONObject.toJSONString(msg));
                    found = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        // If target not online, save to Redis offline queue
        if (!found) {
            try {
                offlineService.saveOfflineMessage(target, JSONObject.toJSONString(msg));
            } catch (Exception e) {
                System.err.println("[Redis] Failed to save offline message: " + e.getMessage());
            }
        }
        // Echo back to sender so they see their own message
        for (UserSession us : userSessions.values()) {
            if (us.getUsername().equals(senderUsername)) {
                try {
                    us.getSession().getBasicRemote().sendText(JSONObject.toJSONString(msg));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    private List<String> getUserList(String roomId) {
        Set<String> seen = new LinkedHashSet<>();
        Set<Session> room = rooms.get(roomId);
        if (room != null) {
            for (Session s : room) {
                UserSession us = userSessions.get(s);
                if (us != null) {
                    seen.add(us.getUsername());
                }
            }
        }
        return new ArrayList<>(seen);
    }

    private void broadcastToRoom(String roomId, String message) {
        Set<Session> room = rooms.get(roomId);
        if (room == null) return;
        for (Session s : room) {
            if (s.isOpen()) {
                try {
                    s.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                map.put(kv[0], kv[1]);
            }
        }
        return map;
    }
}
