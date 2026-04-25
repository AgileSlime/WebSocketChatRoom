package dw.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import javax.websocket.Session;
import java.io.IOException;

@WebServlet(name = "LoginServlet", value = "/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("utf-8");
        response.setContentType("text/html;charset=utf-8");
        String userName = request.getParameter("userName");
        String userPwd = request.getParameter("userPwd");
        System.out.println(userName+"||"+userPwd);
        HttpSession session = request.getSession();
        session.setAttribute("loginName",userName);
        System.out.println("sessionID为："+session.getId());
        request.getRequestDispatcher("/WEB-INF/jsp/websocketChatroom.jsp").forward(request, response);
    }
}
