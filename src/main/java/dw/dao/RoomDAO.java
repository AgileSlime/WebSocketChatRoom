package dw.dao;

import dw.pojo.Room;
import dw.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoomDAO {

    public boolean createRoom(Room room) {
        String sql = "INSERT INTO room (room_name, creator_id) VALUES (?, ?)";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBUtil.getConnection();
            stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, room.getRoomName());
            stmt.setInt(2, room.getCreatorId());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                room.setId(rs.getInt(1));
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtil.close(conn, stmt, null);
        }
    }

    public List<Room> listRooms() {
        String sql = "SELECT * FROM room ORDER BY create_time DESC";
        List<Room> rooms = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();
            while (rs.next()) {
                Room room = new Room();
                room.setId(rs.getInt("id"));
                room.setRoomName(rs.getString("room_name"));
                room.setCreatorId(rs.getInt("creator_id"));
                room.setCreateTime(rs.getTimestamp("create_time"));
                rooms.add(room);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, stmt, rs);
        }
        return rooms;
    }

    public Room findById(int id) {
        String sql = "SELECT * FROM room WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            rs = stmt.executeQuery();
            if (rs.next()) {
                Room room = new Room();
                room.setId(rs.getInt("id"));
                room.setRoomName(rs.getString("room_name"));
                room.setCreatorId(rs.getInt("creator_id"));
                room.setCreateTime(rs.getTimestamp("create_time"));
                return room;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, stmt, rs);
        }
        return null;
    }

    public boolean deleteRoom(int id) {
        String sql = "DELETE FROM room WHERE id = ?";
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBUtil.getConnection();
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            DBUtil.close(conn, stmt, null);
        }
    }
}
