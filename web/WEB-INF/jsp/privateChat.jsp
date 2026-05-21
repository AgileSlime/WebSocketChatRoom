<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<!DOCTYPE html>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
  <title>私聊</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body { font-family: 'Microsoft YaHei', Arial, sans-serif; background: #f5f5f5; height: 100vh; overflow: hidden; }
    .container { display: flex; height: 100vh; max-width: 1200px; margin: 0 auto; background: #fff; }
    .sidebar { width: 300px; border-right: 1px solid #e0e0e0; display: flex; flex-direction: column; }
    .sidebar-header { padding: 15px; border-bottom: 1px solid #e0e0e0; background: #fafafa; }
    .sidebar-header h3 { font-size: 16px; color: #333; }
    .conv-list { flex: 1; overflow-y: auto; }
    .conv-item { display: flex; align-items: center; padding: 12px 15px; cursor: pointer; border-bottom: 1px solid #f0f0f0; transition: background 0.2s; }
    .conv-item:hover, .conv-item.active { background: #e3f2fd; }
    .conv-avatar { width: 40px; height: 40px; border-radius: 50%; background: #9C27B0; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 14px; margin-right: 10px; flex-shrink: 0; }
    .conv-info { flex: 1; min-width: 0; }
    .conv-name { font-size: 14px; color: #333; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .chat-area { flex: 1; display: flex; flex-direction: column; }
    .chat-header { padding: 15px; border-bottom: 1px solid #e0e0e0; background: #fafafa; display: flex; align-items: center; justify-content: space-between; }
    .chat-header-title { font-size: 16px; color: #333; }
    .messages { flex: 1; overflow-y: auto; padding: 15px; background: #f9f9f9; }
    .msg-row { display: flex; margin-bottom: 15px; }
    .msg-row.self { justify-content: flex-end; }
    .msg-bubble { max-width: 60%; padding: 10px 14px; border-radius: 12px; font-size: 14px; line-height: 1.5; word-wrap: break-word; }
    .msg-row .msg-bubble { background: #fff; border: 1px solid #e0e0e0; }
    .msg-row.self .msg-bubble { background: #dcf8c6; border: 1px solid #c5e1a5; }
    .msg-sender { font-size: 12px; color: #999; margin-bottom: 3px; }
    .msg-time { font-size: 11px; color: #bbb; text-align: right; margin-top: 4px; }
    .input-area { padding: 10px 15px; border-top: 1px solid #e0e0e0; background: #fff; display: flex; align-items: center; gap: 10px; }
    .input-area input[type="text"] { flex: 1; padding: 10px 14px; border: 1px solid #ddd; border-radius: 20px; outline: none; font-size: 14px; }
    .input-area button { padding: 10px 20px; border: none; border-radius: 20px; cursor: pointer; font-size: 14px; }
    .btn-send { background: #9C27B0; color: #fff; }
    .btn-back { background: #2196F3; color: #fff; }
    .system-msg { text-align: center; color: #999; font-size: 12px; margin: 10px 0; }
    .user-info { font-size: 14px; color: #666; }
  </style>
</head>
<body>
  <div class="container">
    <div class="sidebar">
      <div class="sidebar-header">
        <h3>私聊会话</h3>
      </div>
      <div class="conv-list" id="convList"></div>
    </div>
    <div class="chat-area">
      <div class="chat-header">
        <div class="chat-header-title" id="chatTitle">选择一个用户</div>
        <div style="display:flex;align-items:center;gap:10px;">
          <span class="user-info">当前用户: <b id="currentUsername">--</b></span>
          <button class="btn-back" onclick="goBack()">返回聊天室</button>
        </div>
      </div>
      <div class="messages" id="messages"></div>
      <div class="input-area">
        <div style="display:flex;align-items:center;gap:10px;">
          <input type="text" id="msgInput" placeholder="输入消息..." onkeydown="if(event.keyCode===13) sendMsg();" style="flex:1;padding:10px 14px;border:1px solid #ddd;border-radius:20px;outline:none;font-size:14px;" />
<button class="btn-send" onclick="sendMsg()">发送</button>
        </div>
      </div>
    </div>
  </div>

  <script src="${pageContext.request.contextPath}/js/jquery-1.4.3.js"></script>
  <script>
    var ws;
    var username = localStorage.getItem('username');
    var token = localStorage.getItem('jwtToken');
    var currentTarget = null;
    var reconnectAttempts = 0;
    var MAX_RECONNECT = 10;
    var receivedMsgIds = {};

    if (!token) {
      window.location.href = '${pageContext.request.contextPath}/';
    }

    $(function() {
      $('#currentUsername').text(username || '未知');
      loadConversations();
      connect();
    });

    function genMsgId() {
      return 'msg_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    }

    function connect() {
      var wsUrl = 'ws://' + window.location.host + '${pageContext.request.contextPath}/chatroom?token=' + encodeURIComponent(token) + '&roomId=private_' + username;
      ws = new WebSocket(wsUrl);

      ws.onopen = function() {
        console.log('Private WebSocket connected');
        reconnectAttempts = 0;
      };

      ws.onmessage = function(e) {
        if (typeof e.data === 'string') {
          handleMessage(e.data);
        }
      };

      ws.onclose = function() {
        console.log('WebSocket closed, reconnecting...');
        if (reconnectAttempts < MAX_RECONNECT) {
          var delay = Math.min(1000 * Math.pow(2, reconnectAttempts), 30000);
          setTimeout(connect, delay);
          reconnectAttempts++;
        }
      };

      ws.onerror = function(e) {
        console.error('WebSocket error', e);
      };
    }

    function handleMessage(data) {
      try {
        var msg = JSON.parse(data);
        if (msg.type === 'PING') {
          if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({type:'PONG',msgid:msg.msgid}));
          }
          return;
        }
        if (msg.type === 'PONG') {
          return;
        }
        if (msg.msgid && receivedMsgIds[msg.msgid]) {
          return;
        }
        if (msg.msgid) {
          receivedMsgIds[msg.msgid] = true;
          if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({type:'ACK',msgid:msg.msgid}));
          }
        }

        if (msg.type === 'PRIVATE') {
          renderMessage(msg);
        } else if (msg.type === 'SYSTEM') {
          $('#messages').append('<div class="system-msg">' + (msg.msgInfo || '') + '</div>');
        }
      } catch(e) {
        $('#messages').append('<div class="system-msg">' + data + '</div>');
      }
    }

    function formatMsgDate(date, dateStr) {
      if (dateStr) return dateStr;
      if (!date) return '';
      var d = new Date(date);
      var Y = d.getFullYear();
      var M = ('0' + (d.getMonth() + 1)).slice(-2);
      var D = ('0' + d.getDate()).slice(-2);
      var h = ('0' + d.getHours()).slice(-2);
      var m = ('0' + d.getMinutes()).slice(-2);
      var s = ('0' + d.getSeconds()).slice(-2);
      return Y + '-' + M + '-' + D + ' ' + h + ':' + m + ':' + s;
    }

    function renderMessage(msg) {
      var isSelf = msg.msgSender === username;
      var displayName = isSelf ? ('发给: ' + msg.msgReceiver) : ('来自: ' + msg.msgSender);
      var html = '<div class="msg-row ' + (isSelf ? 'self' : '') + '">';
      html += '<div class="msg-bubble">';
      html += '<div class="msg-sender">' + displayName + '</div>';
      html += '<div>' + (msg.msgInfo || '') + '</div>';
      html += '<div class="msg-time">' + formatMsgDate(msg.msgDate, msg.msgDateStr) + '</div>';
      html += '</div></div>';
      $('#messages').append(html);
      $('#messages').scrollTop($('#messages')[0].scrollHeight);
    }

    function sendMsg() {
      var input = $('#msgInput');
      var text = input.val().trim();

      if (!text || !ws || ws.readyState !== WebSocket.OPEN) return;
      if (!currentTarget) {
        alert('请先选择一个用户');
        return;
      }
      var msg = {
        msgid: genMsgId(),
        type: 'PRIVATE',
        msgReceiver: currentTarget,
        msgInfo: text,
        ack: true
      };
      ws.send(JSON.stringify(msg));
      input.val('');
      msg.msgSender = username;
      msg.msgDate = new Date();
      renderMessage(msg);
    }

    function getUrlParam(name) {
      var reg = new RegExp('(^|&)' + name + '=([^&]*)(&|$)', 'i');
      var r = window.location.search.substr(1).match(reg);
      if (r != null) return decodeURIComponent(r[2]);
      return null;
    }

    function loadConversations() {
      var targets = JSON.parse(localStorage.getItem('privateTargets') || '[]');
      // Check URL target param
      var urlTarget = getUrlParam('target');
      if (urlTarget && targets.indexOf(urlTarget) === -1) {
        targets.push(urlTarget);
        localStorage.setItem('privateTargets', JSON.stringify(targets));
      }
      renderConvList(targets);
      if (urlTarget) {
        selectTarget(urlTarget);
      }
    }

    function renderConvList(targets) {
      var html = '<div class="conv-item" style="padding:8px 15px;">';
      html += '<input type="text" id="newTarget" placeholder="输入用户名..." style="flex:1;padding:6px 10px;border:1px solid #ddd;border-radius:4px;font-size:13px;" onkeydown="if(event.keyCode===13)addTarget();" />';
      html += '<button onclick="addTarget()" style="margin-left:8px;padding:6px 12px;border:none;border-radius:4px;background:#4CAF50;color:#fff;cursor:pointer;font-size:13px;">添加</button>';
      html += '</div>';
      for (var i = 0; i < targets.length; i++) {
        html += '<div class="conv-item" onclick="selectTarget(\'' + targets[i] + '\')">';
        html += '<div class="conv-avatar">私</div>';
        html += '<div class="conv-info"><div class="conv-name">' + targets[i] + '</div></div>';
        html += '</div>';
      }
      $('#convList').html(html);
    }

    function addTarget() {
      var input = $('#newTarget');
      var name = input.val().trim();
      if (!name) return;
      var targets = JSON.parse(localStorage.getItem('privateTargets') || '[]');
      if (targets.indexOf(name) === -1) {
        targets.push(name);
        localStorage.setItem('privateTargets', JSON.stringify(targets));
      }
      renderConvList(targets);
      selectTarget(name);
    }

    function selectTarget(target) {
      currentTarget = target;
      $('#chatTitle').text('私聊: ' + target);
      $('#messages').html('');
      receivedMsgIds = {};
    }

    function goBack() {
      window.location.href = '${pageContext.request.contextPath}/chat';
    }
  </script>
</body>
</html>
