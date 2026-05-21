<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<!DOCTYPE html>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
  <title>聊天室</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body { font-family: 'Microsoft YaHei', Arial, sans-serif; background: #f5f5f5; height: 100vh; overflow: hidden; }
    .container { display: flex; height: 100vh; max-width: 1200px; margin: 0 auto; background: #fff; }
    .sidebar { width: 300px; border-right: 1px solid #e0e0e0; display: flex; flex-direction: column; }
    .sidebar-header { padding: 15px; border-bottom: 1px solid #e0e0e0; background: #fafafa; }
    .sidebar-header h3 { font-size: 16px; color: #333; }
    .conv-list { flex: 1; overflow-y: auto; }
    .conv-item { display: flex; align-items: center; padding: 12px 15px; cursor: pointer; border-bottom: 1px solid #f0f0f0; transition: background 0.2s; position: relative; }
    .conv-item:hover, .conv-item.active { background: #e3f2fd; }
    .conv-item .delete-btn { display: none; position: absolute; right: 10px; top: 50%; transform: translateY(-50%); background: #f44336; color: #fff; border: none; border-radius: 50%; width: 20px; height: 20px; font-size: 12px; line-height: 20px; text-align: center; cursor: pointer; }
    .conv-item:hover .delete-btn { display: block; }
    .conv-avatar { width: 40px; height: 40px; border-radius: 50%; background: #4CAF50; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 14px; margin-right: 10px; flex-shrink: 0; }
    .conv-avatar.group { background: #2196F3; }
    .conv-avatar.private { background: #9C27B0; }
    .conv-info { flex: 1; min-width: 0; }
    .conv-name { font-size: 14px; color: #333; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .conv-last { font-size: 12px; color: #999; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-top: 2px; }
    .conv-meta { text-align: right; flex-shrink: 0; margin-left: 8px; }
    .conv-time { font-size: 11px; color: #bbb; }
    .chat-area { flex: 1; display: flex; flex-direction: column; }
    .chat-header { padding: 15px; border-bottom: 1px solid #e0e0e0; background: #fafafa; display: flex; align-items: center; justify-content: space-between; }
    .chat-header-title { font-size: 16px; color: #333; }
    .chat-header-actions { display: flex; gap: 10px; }
    .chat-header-actions button { padding: 5px 12px; border: none; border-radius: 4px; cursor: pointer; font-size: 13px; }
    .btn-primary { background: #2196F3; color: #fff; }
    .btn-danger { background: #f44336; color: #fff; }
    .messages { flex: 1; overflow-y: auto; padding: 15px; background: #f9f9f9; }
    .msg-row { display: flex; margin-bottom: 15px; }
    .msg-row.self { justify-content: flex-end; }
    .msg-bubble { max-width: 60%; padding: 10px 14px; border-radius: 12px; font-size: 14px; line-height: 1.5; word-wrap: break-word; }
    .msg-row .msg-bubble { background: #fff; border: 1px solid #e0e0e0; }
    .msg-row.self .msg-bubble { background: #dcf8c6; border: 1px solid #c5e1a5; }
    .msg-sender { font-size: 12px; color: #999; margin-bottom: 3px; }
    .msg-time { font-size: 11px; color: #bbb; text-align: right; margin-top: 4px; }
    .input-area { padding: 10px 15px; border-top: 1px solid #e0e0e0; background: #fff; }
    .input-row { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
    .input-row input[type="text"] { flex: 1; padding: 10px 14px; border: 1px solid #ddd; border-radius: 20px; outline: none; font-size: 14px; }
    .input-row button { padding: 10px 20px; border: none; border-radius: 20px; cursor: pointer; font-size: 14px; }
    .btn-send { background: #4CAF50; color: #fff; }
    .system-msg { text-align: center; color: #999; font-size: 12px; margin: 10px 0; }
    .online-users { padding: 10px 15px; border-top: 1px solid #e0e0e0; background: #fafafa; }
    .online-users h4 { font-size: 13px; color: #666; margin-bottom: 8px; }
    .user-tag { display: inline-block; padding: 3px 8px; background: #e3f2fd; color: #1976d2; border-radius: 10px; font-size: 12px; margin: 2px; cursor: pointer; }
    .modal { display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); justify-content: center; align-items: center; z-index: 100; }
    .modal.show { display: flex; }
    .modal-content { background: #fff; padding: 20px; border-radius: 8px; width: 400px; }
    .modal-content h3 { margin-bottom: 15px; }
    .modal-content input { width: 100%; padding: 10px; margin-bottom: 10px; border: 1px solid #ddd; border-radius: 4px; }
    .modal-content button { padding: 10px 20px; margin-right: 10px; }
    .hidden { display: none !important; }
    .creator-info { font-size: 12px; color: #999; margin-left: 8px; }
  </style>
</head>
<body>
  <div class="container">
    <div class="sidebar">
      <div class="sidebar-header">
        <h3>会话列表</h3>
      </div>
      <div class="conv-list" id="convList"></div>
      <div class="online-users">
        <h4>在线用户</h4>
        <div id="onlineUserList"></div>
      </div>
    </div>
    <div class="chat-area">
      <div class="chat-header">
        <div class="chat-header-title" id="chatTitle">选择一个会话</div>
        <div style="display:flex;align-items:center;gap:10px;">
          <span style="font-size:14px;color:#666;">当前用户: <b id="currentUsername">--</b></span>
          <div class="chat-header-actions">
            <button class="btn-primary" onclick="showCreateRoom()">创建房间</button>
            <button class="btn-danger" id="deleteRoomBtn" style="display:none;" onclick="deleteCurrentRoom()">删除房间</button>
            <button class="btn-danger" onclick="logout()">退出</button>
          </div>
        </div>
      </div>
      <div class="messages" id="messages"></div>
      <div class="input-area">
        <div class="input-row">
          <input type="text" id="msgInput" placeholder="输入消息..." onkeydown="if(event.keyCode===13) sendMsg();" />
          <button class="btn-send" onclick="sendMsg()">发送</button>
        </div>
      </div>
    </div>
  </div>

  <div class="modal" id="createRoomModal">
    <div class="modal-content">
      <h3>创建房间</h3>
      <input type="text" id="roomName" placeholder="房间名称" />
      <button class="btn-primary" onclick="createRoom()">创建</button>
      <button onclick="hideCreateRoom()">取消</button>
    </div>
  </div>

  <script src="${pageContext.request.contextPath}/js/jquery-1.4.3.js"></script>
  <script>
    var ws;
    var username = localStorage.getItem('username');
    var token = localStorage.getItem('jwtToken');
    var userId = localStorage.getItem('userId');
    var currentRoom = null;
    var currentChatType = 'room'; // 'room' or 'private'
    var currentPrivateTarget = null;
    var reconnectAttempts = 0;
    var MAX_RECONNECT = 10;
    var receivedMsgIds = {};
    var unreadCounts = {};
    var roomListData = [];
    var creatorIdMap = {};

    if (!token) {
      window.location.href = '${pageContext.request.contextPath}/';
    }

    $(function() {
      $('#currentUsername').text(username || '未知');
      loadPrivateConversations();
      // Load rooms first, then connect WebSocket
      // This ensures creatorIdMap is populated before switchRoom() is called
      loadRoomsAndConnect();
    });

    function genMsgId() {
      return 'msg_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    }

    function connect() {
      // Connect without roomId - room membership is managed via SWITCH_ROOM messages
      var wsUrl = 'ws://' + window.location.host + '${pageContext.request.contextPath}/chatroom?token=' + encodeURIComponent(token);
      ws = new WebSocket(wsUrl);

      ws.onopen = function() {
        console.log('WebSocket connected');
        reconnectAttempts = 0;
        // CreatorIdMap is guaranteed to be populated since loadRoomsAndConnect()
        // loads rooms before calling connect()
        if (roomListData.length > 0 && currentChatType === 'room' && !currentRoom) {
          switchRoom(roomListData[0].id, roomListData[0].roomName);
        }
      };

      ws.onmessage = function(e) {
        handleMessage(e.data);
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

        if (msg.type === 'SYSTEM') {
          $('#messages').append('<div class="system-msg">' + (msg.msgInfo || '') + '</div>');
          if (msg.userList) {
            updateOnlineUsers(msg.userList);
          }
        } else if (msg.type === 'PRIVATE') {
          handlePrivateMessage(msg);
        } else {
          var isSelf = msg.msgSender === username;
          var html = '<div class="msg-row ' + (isSelf ? 'self' : '') + '">';
          html += '<div class="msg-bubble">';
          if (!isSelf) {
            html += '<div class="msg-sender">' + (msg.msgSender || '') + '</div>';
          }
          html += '<div>' + (msg.msgInfo || '') + '</div>';
          html += '<div class="msg-time">' + (msg.msgDateStr || '') + '</div>';
          html += '</div></div>';
          $('#messages').append(html);
          $('#messages').scrollTop($('#messages')[0].scrollHeight);
        }
      } catch(e) {
        $('#messages').append('<div class="system-msg">' + data + '</div>');
      }
    }

    function handlePrivateMessage(msg) {
      var otherUser = msg.msgSender === username ? msg.msgReceiver : msg.msgSender;
      if (currentChatType === 'private' && currentPrivateTarget === otherUser) {
        var isSelf = msg.msgSender === username;
        var displayName = isSelf ? '我' : msg.msgSender;
        var html = '<div class="msg-row ' + (isSelf ? 'self' : '') + '">';
        html += '<div class="msg-bubble">';
        html += '<div class="msg-sender">' + displayName + '</div>';
        html += '<div>' + (msg.msgInfo || '') + '</div>';
        html += '<div class="msg-time">' + (msg.msgDateStr || '') + '</div>';
        html += '</div></div>';
        $('#messages').append(html);
        $('#messages').scrollTop($('#messages')[0].scrollHeight);
      } else {
        if (!unreadCounts[otherUser]) {
          unreadCounts[otherUser] = 0;
          addPrivateConversation(otherUser);
        }
        unreadCounts[otherUser]++;
        updateConvList();
      }
    }

    function sendMsg() {
      var input = $('#msgInput');
      var text = input.val().trim();

      if (!text || !ws || ws.readyState !== WebSocket.OPEN) return;

      if (currentChatType === 'private') {
        if (!currentPrivateTarget) return;
        var msg = {
          msgid: genMsgId(),
          type: 'PRIVATE',
          msgReceiver: currentPrivateTarget,
          msgInfo: text,
          ack: true
        };
        ws.send(JSON.stringify(msg));
        input.val('');
      } else {
        var msg = {
          msgid: genMsgId(),
          type: 'CHAT',
          roomId: currentRoom,
          msgInfo: text,
          ack: true
        };
        ws.send(JSON.stringify(msg));
        input.val('');
      }
    }

    function updateOnlineUsers(userList) {
      var html = '';
      for (var i = 0; i < userList.length; i++) {
        if (userList[i] !== username) {
          html += '<span class="user-tag" onclick="startPrivateChat(\'' + userList[i] + '\')">' + userList[i] + '</span>';
        }
      }
      $('#onlineUserList').html(html);
    }

    function startPrivateChat(target) {
      if (target === username) return;
      addPrivateConversation(target);
      selectPrivateChat(target);
    }

    function addPrivateConversation(target) {
      var convs = getPrivateConversations();
      if (convs.indexOf(target) === -1) {
        convs.push(target);
        localStorage.setItem('privateConvs_' + username, JSON.stringify(convs));
        updateConvList();
      }
    }

    function removePrivateConversation(target) {
      var convs = getPrivateConversations();
      var idx = convs.indexOf(target);
      if (idx !== -1) {
        convs.splice(idx, 1);
        localStorage.setItem('privateConvs_' + username, JSON.stringify(convs));
        updateConvList();
      }
      if (currentChatType === 'private' && currentPrivateTarget === target) {
        // Switch back to first room
        if (roomListData.length > 0) {
          enterRoom(roomListData[0].id, roomListData[0].roomName);
        }
      }
    }

    function getPrivateConversations() {
      try {
        var data = localStorage.getItem('privateConvs_' + username);
        return data ? JSON.parse(data) : [];
      } catch (e) {
        return [];
      }
    }

    function loadPrivateConversations() {
      updateConvList();
    }

    function selectPrivateChat(target) {
      currentChatType = 'private';
      currentPrivateTarget = target;
      $('#chatTitle').text('私聊: ' + target);
      $('#messages').html('');
      $('#deleteRoomBtn').hide();
      receivedMsgIds = {};

      // Request private history via WebSocket without reconnecting
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({type:'LOAD_PRIVATE_HISTORY', msgReceiver: target}));
      }

      if (unreadCounts[target]) {
        delete unreadCounts[target];
      }
      updateConvList();
    }

    function loadRoomsAndConnect() {
      $.get('${pageContext.request.contextPath}/room/list', function(data) {
        var rooms = typeof data === 'string' ? JSON.parse(data) : data;
        roomListData = rooms;
        for (var i = 0; i < rooms.length; i++) {
          creatorIdMap[rooms[i].id] = rooms[i].creatorId;
        }
        updateConvList();
        // Now connect WebSocket - creatorIdMap is ready
        connect();
      });
    }

    function updateConvList() {
      var html = '';
      // Rooms
      for (var i = 0; i < roomListData.length; i++) {
        var r = roomListData[i];
        var isActive = currentChatType === 'room' && currentRoom == r.id;
        html += '<div class="conv-item ' + (isActive ? 'active' : '') + '" onclick="enterRoom(\'' + r.id + '\', \'' + r.roomName + '\')">';
        html += '<div class="conv-avatar group">群</div>';
        html += '<div class="conv-info">';
        html += '<div class="conv-name">' + r.roomName + '</div>';
        html += '</div></div>';
      }
      // Private conversations
      var privs = getPrivateConversations();
      for (var j = 0; j < privs.length; j++) {
        var p = privs[j];
        var isActive = currentChatType === 'private' && currentPrivateTarget === p;
        var unread = unreadCounts[p] || 0;
        html += '<div class="conv-item ' + (isActive ? 'active' : '') + '">';
        html += '<div class="conv-avatar private" onclick="selectPrivateChat(\'' + p + '\')">私</div>';
        html += '<div class="conv-info" onclick="selectPrivateChat(\'' + p + '\')">';
        html += '<div class="conv-name">' + p + '</div>';
        html += '</div>';
        html += '<button class="delete-btn" onclick="event.stopPropagation(); removePrivateConversation(\'' + p + '\')">&times;</button>';
        html += '</div>';
      }
      $('#convList').html(html);
    }

    function switchRoom(roomId, roomName) {
      currentChatType = 'room';
      currentRoom = roomId;
      currentPrivateTarget = null;
      $('#chatTitle').text(roomName || '房间 ' + roomId);
      $('#messages').html('');
      receivedMsgIds = {};

      $('#deleteRoomBtn').show();

      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify({type:'SWITCH_ROOM', roomId: roomId}));
      }
      updateConvList();
    }

    function enterRoom(roomId, roomName) {
      switchRoom(roomId, roomName);
    }

    function deleteCurrentRoom() {
      if (currentChatType !== 'room') return;
      if (!confirm('确定要删除该房间吗？')) return;
      $.ajax({
        url: '${pageContext.request.contextPath}/room/delete',
        type: 'POST',
        headers: { 'Authorization': 'Bearer ' + token },
        data: { roomId: currentRoom },
        success: function(data) {
          var res = typeof data === 'string' ? JSON.parse(data) : data;
          if (res.success) {
            loadRooms();
          } else {
            alert(res.msg || '删除失败');
          }
        }
      });
    }

    function showCreateRoom() {
      $('#createRoomModal').addClass('show');
    }

    function hideCreateRoom() {
      $('#createRoomModal').removeClass('show');
    }

    function createRoom() {
      var name = $('#roomName').val().trim();
      if (!name) return;
      $.ajax({
        url: '${pageContext.request.contextPath}/room/create',
        type: 'POST',
        headers: { 'Authorization': 'Bearer ' + token },
        data: { roomName: name },
        success: function(data) {
          var res = typeof data === 'string' ? JSON.parse(data) : data;
          if (res.success) {
            hideCreateRoom();
            loadRooms();
          } else {
            alert(res.msg || '创建失败');
          }
        }
      });
    }

    function logout() {
      localStorage.removeItem('jwtToken');
      localStorage.removeItem('username');
      localStorage.removeItem('userId');
      if (ws) ws.close();
      window.location.href = '${pageContext.request.contextPath}/';
    }
  </script>
</body>
</html>
