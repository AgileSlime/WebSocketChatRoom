package dw.servlet;

import dw.dao.UserDAO;
import dw.pojo.User;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "RegisterServlet", value = "/register")
public class RegisterServlet extends HttpServlet {
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("utf-8");
        response.setContentType("application/json;charset=utf-8");
        PrintWriter out = response.getWriter();

        String username = request.getParameter("userName");
        String password = request.getParameter("userPwd");

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            out.print("{\"success\":false,\"msg\":\"用户名或密码不能为空\"}");
            return;
        }

        if (userDAO.findByUsername(username) != null) {
            out.print("{\"success\":false,\"msg\":\"用户名已存在\"}");
            return;
        }

        String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
        User user = new User(username, hashed);
        boolean success = userDAO.register(user);

        if (success) {
            out.print("{\"success\":true,\"msg\":\"注册成功\"}");
        } else {
            out.print("{\"success\":false,\"msg\":\"注册失败\"}");
        }
    }
}
