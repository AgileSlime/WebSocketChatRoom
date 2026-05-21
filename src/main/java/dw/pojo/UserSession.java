package dw.pojo;

import javax.websocket.Session;

public class UserSession {
    private Session session;
    private String username;
    private String roomId;
    private long lastHeartbeat;
    private boolean authenticated;

    public UserSession(Session session, String username) {
        this.session = session;
        this.username = username;
        this.lastHeartbeat = System.currentTimeMillis();
        this.authenticated = true;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public long getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void setLastHeartbeat(long lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }
}
