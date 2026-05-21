<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<!DOCTYPE html>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
  <title>注册</title>
  <style>
    body { font-family: Arial, sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; background: #f5f5f5; }
    .box { background: #fff; padding: 40px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); width: 320px; }
    h2 { text-align: center; margin-bottom: 20px; color: #333; }
    input { width: 100%; padding: 10px; margin-bottom: 15px; border: 1px solid #ddd; border-radius: 4px; box-sizing: border-box; }
    button { width: 100%; padding: 10px; background: #4CAF50; color: #fff; border: none; border-radius: 4px; cursor: pointer; font-size: 16px; }
    button:hover { background: #45a049; }
    .link { text-align: center; margin-top: 15px; }
    .link a { color: #4CAF50; text-decoration: none; }
    .msg { text-align: center; margin-top: 10px; font-size: 14px; }
  </style>
</head>
<body>
  <div class="box">
    <h2>用户注册</h2>
    <input type="text" id="userName" placeholder="用户名" />
    <input type="password" id="userPwd" placeholder="密码" />
    <input type="password" id="userPwd2" placeholder="确认密码" />
    <button onclick="register()">注册</button>
    <div class="msg" id="msg"></div>
    <div class="link"><a href="${pageContext.request.contextPath}/">已有账号？去登录</a></div>
  </div>
  <script>
    function register() {
      var userName = document.getElementById('userName').value.trim();
      var userPwd = document.getElementById('userPwd').value;
      var userPwd2 = document.getElementById('userPwd2').value;
      var msg = document.getElementById('msg');
      if (!userName || !userPwd) { msg.innerText = '用户名和密码不能为空'; msg.style.color = 'red'; return; }
      if (userPwd !== userPwd2) { msg.innerText = '两次密码不一致'; msg.style.color = 'red'; return; }
      var xhr = new XMLHttpRequest();
      xhr.open('POST', '${pageContext.request.contextPath}/register', true);
      xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
      xhr.onreadystatechange = function() {
        if (xhr.readyState === 4 && xhr.status === 200) {
          var res = JSON.parse(xhr.responseText);
          msg.innerText = res.msg;
          msg.style.color = res.success ? 'green' : 'red';
          if (res.success) {
            setTimeout(function() { window.location.href = '${pageContext.request.contextPath}/'; }, 1000);
          }
        }
      };
      xhr.send('userName=' + encodeURIComponent(userName) + '&userPwd=' + encodeURIComponent(userPwd));
    }
  </script>
</body>
</html>
