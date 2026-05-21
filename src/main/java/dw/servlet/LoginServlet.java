package dw.servlet;

import dw.dao.UserDAO;
import dw.pojo.User;
import dw.util.JWTUtil;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "LoginServlet", value = "/login")
public class LoginServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("utf-8");
        response.setContentType("application/json;charset=utf-8");
        PrintWriter out = response.getWriter();

        String username = request.getParameter("userName");
        String password = request.getParameter("userPwd");

        User user = userDAO.findByUsername(username);
        if (user == null) {
            out.print("{\"success\":false,\"msg\":\"用户不存在\"}");
            return;
        }

        if (!BCrypt.checkpw(password, user.getPassword())) {
            out.print("{\"success\":false,\"msg\":\"密码错误\"}");
            return;
        }

        String token = JWTUtil.generateToken(user.getUsername(), user.getId());
        out.print("{\"success\":true,\"token\":\"" + token + "\",\"username\":\"" + user.getUsername() + "\",\"userId\":" + user.getId() + "}");
    }
}
