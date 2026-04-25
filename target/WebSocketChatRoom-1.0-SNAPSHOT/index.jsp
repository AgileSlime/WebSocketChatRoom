<%--
  Created by IntelliJ IDEA.
  User: YuQi
  Date: 2026/4/20
  Time: 22:54
  To change this template use File | Settings | File Templates.
--%>
<%@ page language="java" contentType="text/html; charset=utf-8"
         pageEncoding="utf-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
  <title>Login登录</title>
</head>
<body>
<div>
  <fieldset>
    <legend>登录窗口</legend>
    <form action="${pageContext.request.contextPath}/login"  method="post">
      <table>
        <tr><td>账号：</td><td><input type="text" name="userName" size="20"/></td></tr>
        <tr><td>密码：</td><td><input type="password" name="userPwd" size="20"/></td></tr>
        <tr><td colspan="2"><input type="submit"  value="提交"/></td></tr>
      </table>
    </form>
  </fieldset>
</div>
</body>
</html>
