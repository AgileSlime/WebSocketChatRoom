package dw.servlet;

import com.alibaba.fastjson.JSON;
import dw.util.JWTUtil;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.*;

@WebServlet(name = "SseChatServlet", value = "/sse/chat", asyncSupported = true)
public class SseChatServlet extends HttpServlet {

    private static final ConcurrentHashMap<String, List<SseClient>> roomClients = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    static {
        scheduler.scheduleAtFixedRate(() -> {
            broadcastToRoom("", ":heartbeat\n\n");
        }, 10, 10, TimeUnit.SECONDS);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = req.getParameter("token");
        String roomId = req.getParameter("roomId");

        if (token == null || !JWTUtil.validateToken(token)) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }
        if (roomId == null || roomId.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing roomId");
            return;
        }

        String username = JWTUtil.getUsernameFromToken(token);

        resp.setContentType("text/event-stream");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("Connection", "keep-alive");

        AsyncContext asyncContext = req.startAsync();
        asyncContext.setTimeout(0);

        PrintWriter writer = resp.getWriter();
        SseClient client = new SseClient(username, roomId, writer);

        roomClients.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>()).add(client);

        broadcastToRoom(roomId, formatSseEvent("system", "{\"msg\":\"" + username + " joined\"}"));

        asyncContext.addListener(new javax.servlet.AsyncListener() {
            @Override
            public void onComplete(javax.servlet.AsyncEvent event) throws IOException { removeClient(roomId, client); }
            @Override
            public void onTimeout(javax.servlet.AsyncEvent event) throws IOException { removeClient(roomId, client); }
            @Override
            public void onError(javax.servlet.AsyncEvent event) throws IOException { removeClient(roomId, client); }
            @Override
            public void onStartAsync(javax.servlet.AsyncEvent event) throws IOException {}
        });
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String token = req.getParameter("token");
        String roomId = req.getParameter("roomId");
        String content = req.getParameter("msgInfo");

        if (token == null || !JWTUtil.validateToken(token)) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
            return;
        }
        if (roomId == null || roomId.isEmpty() || content == null || content.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad request");
            return;
        }

        String username = JWTUtil.getUsernameFromToken(token);
        String msgJson = JSON.toJSONString(new SseMessage("chat", content, roomId, username));
        broadcastToRoom(roomId, formatSseEvent("chat", msgJson));
    }

    private static void broadcastToRoom(String roomId, String sseData) {
        List<SseClient> clients = roomClients.get(roomId);
        if (clients == null) return;
        for (SseClient client : clients) {
            try {
                synchronized (client.writer) {
                    client.writer.write(sseData);
                    client.writer.flush();
                }
            } catch (Exception e) {
                // Client disconnected
            }
        }
    }

    private static String formatSseEvent(String event, String data) {
        return "event: " + event + "\ndata: " + data + "\n\n";
    }

    private static void removeClient(String roomId, SseClient client) {
        List<SseClient> clients = roomClients.get(roomId);
        if (clients != null) {
            clients.remove(client);
        }
    }

    private static class SseClient {
        String username;
        String roomId;
        PrintWriter writer;

        SseClient(String username, String roomId, PrintWriter writer) {
            this.username = username;
            this.roomId = roomId;
            this.writer = writer;
        }
    }

    private static class SseMessage {
        String type;
        String content;
        String roomId;
        String sender;

        SseMessage(String type, String content, String roomId, String sender) {
            this.type = type;
            this.content = content;
            this.roomId = roomId;
            this.sender = sender;
        }
    }
}
