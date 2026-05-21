package dw.servlet;

import com.alibaba.fastjson.JSON;
import dw.dao.RoomDAO;
import dw.pojo.Room;
import dw.util.JWTUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet(name = "RoomServlet", value = "/room/*")
public class RoomServlet extends HttpServlet {
    private final RoomDAO roomDAO = new RoomDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("utf-8");
        response.setContentType("application/json;charset=utf-8");
        PrintWriter out = response.getWriter();

        String path = request.getPathInfo();
        if ("/list".equals(path)) {
            List<Room> rooms = roomDAO.listRooms();
            out.print(JSON.toJSONString(rooms));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("utf-8");
        response.setContentType("application/json;charset=utf-8");
        PrintWriter out = response.getWriter();

        String path = request.getPathInfo();
        if ("/create".equals(path)) {
            String roomName = request.getParameter("roomName");
            int creatorId = getCreatorId(request);
            if (roomName == null || roomName.isEmpty()) {
                out.print("{\"success\":false,\"msg\":\"房间名不能为空\"}");
                return;
            }
            Room room = new Room();
            room.setRoomName(roomName);
            room.setCreatorId(creatorId);
            boolean success = roomDAO.createRoom(room);
            if (success) {
                out.print("{\"success\":true,\"roomId\":\"" + room.getId() + "\"}");
            } else {
                out.print("{\"success\":false,\"msg\":\"创建失败\"}");
            }
        } else if ("/delete".equals(path)) {
            String roomIdStr = request.getParameter("roomId");
            int creatorId = getCreatorId(request);
            if (roomIdStr == null || roomIdStr.isEmpty()) {
                out.print("{\"success\":false,\"msg\":\"房间ID不能为空\"}");
                return;
            }
            int roomId = Integer.parseInt(roomIdStr);
            Room room = roomDAO.findById(roomId);
            if (room == null) {
                out.print("{\"success\":false,\"msg\":\"房间不存在\"}");
                return;
            }
            if (room.getCreatorId() != creatorId) {
                out.print("{\"success\":false,\"msg\":\"只有创建者可以删除房间\"}");
                return;
            }
            boolean success = roomDAO.deleteRoom(roomId);
            if (success) {
                out.print("{\"success\":true,\"msg\":\"删除成功\"}");
            } else {
                out.print("{\"success\":false,\"msg\":\"删除失败\"}");
            }
        }
    }

    private int getCreatorId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return 0;
        }
        token = token.substring(7);
        if (!JWTUtil.validateToken(token)) {
            return 0;
        }
        return JWTUtil.getUserIdFromToken(token);
    }
}
