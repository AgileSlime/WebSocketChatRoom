package dw.dao;

import dw.pojo.Msg;
import dw.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MessageDAO {

    public void saveMessage(Msg msg) {
        String sql = "INSERT INTO message (room_id, msg_sender, msg_receiver, type, msg_info, create_time) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, msg.getRoomId());
            stmt.setString(2, msg.getMsgSender());
            stmt.setString(3, msg.getMsgReceiver());
            stmt.setString(4, msg.getType());
            stmt.setString(5, msg.getMsgInfo());
            stmt.setTimestamp(6, new Timestamp(msg.getMsgDate().getTime()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, stmt, null);
        }
    }

    public List<Msg> getRoomMessages(String roomId, int limit) {
        String sql = "SELECT * FROM message WHERE room_id = ? AND create_time > DATE_SUB(NOW(), INTERVAL 7 DAY) ORDER BY create_time ASC LIMIT ?";
        List<Msg> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, roomId);
            stmt.setInt(2, limit);
            rs = stmt.executeQuery();
            while (rs.next()) {
                Msg msg = new Msg();
                msg.setRoomId(rs.getString("room_id"));
                msg.setMsgSender(rs.getString("msg_sender"));
                msg.setMsgReceiver(rs.getString("msg_receiver"));
                msg.setType(rs.getString("type"));
                msg.setMsgInfo(rs.getString("msg_info"));
                msg.setMsgDate(rs.getTimestamp("create_time"));
                list.add(msg);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, stmt, rs);
        }
        return list;
    }

    public List<Msg> getPrivateMessages(String sender, String receiver, int limit) {
        String sql = "SELECT * FROM message WHERE ((msg_sender = ? AND msg_receiver = ?) OR (msg_sender = ? AND msg_receiver = ?)) " +
                     "AND create_time > DATE_SUB(NOW(), INTERVAL 7 DAY) AND type = 'PRIVATE' ORDER BY create_time ASC LIMIT ?";
        List<Msg> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, sender);
            stmt.setString(2, receiver);
            stmt.setString(3, receiver);
            stmt.setString(4, sender);
            stmt.setInt(5, limit);
            rs = stmt.executeQuery();
            while (rs.next()) {
                Msg msg = new Msg();
                msg.setRoomId(rs.getString("room_id"));
                msg.setMsgSender(rs.getString("msg_sender"));
                msg.setMsgReceiver(rs.getString("msg_receiver"));
                msg.setType(rs.getString("type"));
                msg.setMsgInfo(rs.getString("msg_info"));
                msg.setMsgDate(rs.getTimestamp("create_time"));
                list.add(msg);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, stmt, rs);
        }
        return list;
    }

    public void cleanOldMessages() {
        String sql = "DELETE FROM message WHERE create_time < DATE_SUB(NOW(), INTERVAL 7 DAY)";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, stmt, null);
        }
    }
}
