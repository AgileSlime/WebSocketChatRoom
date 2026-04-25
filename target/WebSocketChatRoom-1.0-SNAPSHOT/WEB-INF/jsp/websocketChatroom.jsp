<%--
  Created by IntelliJ IDEA.
  User: 梁霁轩
  Date: 2026/4/20
  Time: 23:14
  To change this template use File | Settings | File Templates.
--%>
<%@ page language="java" contentType="text/html; charset=utf-8"
         pageEncoding="utf-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
  <title>聊天窗口</title>
  <script type="text/javascript" src="js/jquery-1.4.3.js"></script>
  <script type="text/javascript">
  var ws;
  var ws_url="ws://" + window.location.host + "${pageContext.request.contextPath}/chatroom";

  $(function(){
    ws_connect();
    $("#send").click(function(){
      ws_sendMsg();
      $("#msg").val("");
    });
  });
  function ws_connect(){
    var LoginUser="${sessionScope.loginName}";
    if ('WebSocket' in window) {
      ws = new WebSocket(ws_url+"?loginName="+LoginUser);
    } else if ('MozWebSocket' in window) {
      ws = new MozWebSocket(ws_url);
    } else {
      console.log('Error: WebSocket is not supported by this browser.');
      return;
    }

    ws.onopen = function () {
      console.log('Info: WebSocket connection opened.');

    };

    ws.onclose = function () {
      // document.getElementById('chat').onkeydown = null;
      console.log('Info: WebSocket closed.');
    };

    ws.onmessage = function (message) {
      //console.log("@@@"+message.data);
      var receiveMsg=message.data;
      var obj=JSON.parse(receiveMsg);
      if (obj.type==="s"){
        $("#record").append("<div>"+obj.msgDateStr+""+obj.msgInfo+"</div>");
        var userHtml="";
        var userList=obj.userList;
        for (var i=0;i<userList.length;i++){
            userHtml=userHtml+userList[i]+"<br/><br/>";
        }
        $("#userList").html(userHtml);
      }
      else if (obj.type==="p"){
        $("#record").append("<div>"+obj.msgSender+":&nbsp;"+obj.msgDateStr+"</div><div>"+obj.msgInfo+"</div>");
      }
    };

  };
  function ws_sendMsg(){
    var msg=$("#msg").val();
    ws.send(msg);
  };
  function ws_sendImg(){
  };
  </script>
</head>
<body>
********************聊天窗口******************************
<table style="border: 1px solid #00F;">
  <tbody>
  <tr>
    <td colspan="2" align="center">
      <h3>welcome [${sessionScope.loginName}] to use this system!</h3>
    </td>
  </tr>
  <tr>
    <td width="500px" height="300px" style="border: 1px solid #00F; vertical-align: top;" id="content"
        name="content">
      <div style="background-color:white;">
        <table id="tbRecord">
          <tbody id="record" style="display:block;height:300px; width:500px; overflow:auto;"/>
        </table>
      </div>
    </td>
    <td width="100px" style="border:1px solid #00F; vertical-align:top;">
      <div style="overflow:auto;">
        <table id="tbuserList">
          <tbody id="userList" style="display:block; height:300px;overflow:auto;"/>
        </table>
      </div>
    </td>
  </tr>
  </tbody>
  <tfoot>
  <tr>
    <td colspan="2" align="center">
      <input id="msg" name="msg" style="width:100%;" placeholder="信息输入"/>
    </td>
  </tr>
  <tr>
    <td colspan="2" align="center">
      <button style="margin:0 30px 0 30px" id="send" name="send">send</button>
      <input type="file" id="img" style="width:200px; height:30px"/>
      <button id="uploadImg" name="uploadImg">uploadImg</button>
      <button style="margin:0 30px 0 30px" id="disconnect" name="disconnect">Disconnect</button>
    </td>
  </tr>
  </tfoot>
</table>
</body>
</html>