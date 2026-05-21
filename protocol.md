# WebSocket 聊天室通信协议文档

## 一、协议概述

- **传输协议**: `ws://host:port/context/chatroom`
- **消息格式**: JSON（UTF-8 编码文本帧）
- **认证方式**: JWT Token，通过 WebSocket 连接 URL 的 query 参数传递
- **连接模式**: 单长连接，不随会话切换断开重连

## 二、连接建立

### 2.1 握手请求

```
ws://host:port/context/chatroom?token=<JWT_TOKEN>
```

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| token | String | 是 | JWT 认证令牌 |

> 注意：连接时不携带 `roomId`。房间成员关系通过后续的 `SWITCH_ROOM` 消息管理。

### 2.2 连接成功

服务端验证 Token 后接受连接，自动推送：
1. 离线消息（私信，Redis 缓存，7天过期）
2. 无房间历史（首次连接未加入任何房间）

### 2.3 连接失败

| 情况 | 服务端行为 |
|---|---|
| 缺少 token | 直接关闭连接 |
| token 无效/过期 | 直接关闭连接 |
| 用户名为空 | 直接关闭连接 |

## 三、消息格式

### 3.1 消息体结构

```json
{
    "msgid": "消息唯一标识 (UUID)",
    "type": "消息类型 (见下表)",
    "roomId": "房间ID",
    "msgSender": "发送者用户名",
    "msgReceiver": "私聊目标用户名",
    "msgInfo": "消息内容",
    "msgDate": "发送时间 (ISO Date)",
    "msgDateStr": "格式化时间字符串",
    "userList": ["在线用户列表"],
    "ack": true
}
```

### 3.2 消息类型

| type | 方向 | 说明 | 必需字段 |
|---|---|---|---|
| `CHAT` | 客户端→服务端 | 发送房间聊天消息 | `roomId` (从 session 获取) |
| `PRIVATE` | 客户端→服务端 | 发送私聊消息 | `msgReceiver` |
| `SYSTEM` | 服务端→客户端 | 系统通知（用户进出房间、在线列表更新） | `msgInfo`, `userList` |
| `PING` | 服务端→客户端 | 心跳请求（每5秒） | `msgid` |
| `PONG` | 客户端→服务端 | 心跳响应 | `msgid` |
| `ACK` | 客户端→服务端 | 应用层确认收到 | `msgid` |
| `SWITCH_ROOM` | 客户端→服务端 | 切换房间（不断开连接） | `roomId` |
| `LOAD_PRIVATE_HISTORY` | 客户端→服务端 | 加载私聊历史记录 | `msgReceiver` |

## 四、消息流

### 4.1 房间聊天

**客户端发送:**
```json
{
    "msgid": "msg_1234567890_abc",
    "type": "CHAT",
    "msgInfo": "大家好",
    "ack": true
}
```

**服务端处理:**
1. 自动填充 `msgSender`（从 JWT 获取）、`msgDate`、`roomId`（从 session 获取）
2. 存入 Redis（`chat:room:{roomId}`，7天过期）
3. 广播给该房间所有在线成员（包括发送者自己）

**房间成员收到:**
```json
{
    "msgid": "msg_1234567890_abc",
    "type": "CHAT",
    "roomId": "5",
    "msgSender": "张三",
    "msgInfo": "大家好",
    "msgDate": "2026-05-21T08:00:00.000+0000",
    "msgDateStr": "2026-05-21 08:00:00"
}
```

### 4.2 私聊

**客户端发送:**
```json
{
    "msgid": "msg_1234567890_def",
    "type": "PRIVATE",
    "msgReceiver": "李四",
    "msgInfo": "你好",
    "ack": true
}
```

**服务端处理:**
1. 自动填充 `msgSender`、`msgDate`
2. 存入 Redis（`chat:private:张三:李四`，7天过期）
3. 查找目标用户在线 session：
   - 在线：直接发送
   - 离线：存入离线消息队列
4. 回显给发送者（发送者可见自己的消息）

**目标用户收到（在线）:**
```json
{
    "msgid": "msg_1234567890_def",
    "type": "PRIVATE",
    "msgReceiver": "李四",
    "msgSender": "张三",
    "msgInfo": "你好",
    "msgDate": "2026-05-21T08:00:00.000+0000",
    "msgDateStr": "2026-05-21 08:00:00"
}
```

### 4.3 切换房间

**客户端发送:**
```json
{
    "type": "SWITCH_ROOM",
    "roomId": "3"
}
```

**服务端处理:**
1. 从旧房间移除该 session，广播 "XXX left the room"
2. 加入新房间，广播 "XXX entered the room"
3. 推送新房间最近 50 条历史消息
4. 更新在线用户列表

### 4.4 加载私聊历史

**客户端发送:**
```json
{
    "type": "LOAD_PRIVATE_HISTORY",
    "msgReceiver": "李四"
}
```

**服务端处理:**
从 Redis 拉取最近 50 条私聊历史消息，逐条推送给客户端。

### 4.5 心跳机制

**服务端 → 客户端（每 5 秒）:**
```json
{
    "type": "PING",
    "msgid": "uuid-random"
}
```

**客户端 → 服务端（收到 PING 后立即回复）:**
```json
{
    "type": "PONG",
    "msgid": "uuid-random"
}
```

**超时规则:** 超过 30 秒未响应 PONG，服务端标记为离线并关闭连接。

### 4.6 应用层 ACK

客户端发送 `ack: true` 的消息后，接收方需在收到后立即回复确认：

```json
{
    "type": "ACK",
    "msgid": "msg_1234567890_abc"
}
```

客户端维护 `receivedMsgIds` 去重集，已处理的 `msgid` 不再重复渲染。

## 五、系统通知

### 5.1 用户进入房间

```json
{
    "type": "SYSTEM",
    "msgSender": "system",
    "msgInfo": "张三 entered the room",
    "userList": ["张三", "李四", "王五"],
    "msgDate": "..."
}
```

### 5.2 用户离开房间

```json
{
    "type": "SYSTEM",
    "msgSender": "system",
    "msgInfo": "张三 left the room",
    "userList": ["李四", "王五"],
    "msgDate": "..."
}
```

## 六、客户端自动重连

| 参数 | 值 |
|---|---|
| 最大重连次数 | 10 |
| 退避策略 | 指数退避，`min(1000 * 2^n, 30000)` 毫秒 |
| 重连触发 | `ws.onclose` 事件 |

## 七、REST API 补充

### 7.1 获取房间列表

```
GET /room/list
```

响应:
```json
[
    {
        "id": 5,
        "roomName": "张三的房间",
        "creatorId": 2,
        "createTime": 1779376970000
    }
]
```

### 7.2 创建房间

```
POST /room/create
Headers: Authorization: Bearer <JWT_TOKEN>
Body: roomName=xxx
```

响应:
```json
{"success": true, "roomId": "6"}
```

### 7.3 删除房间

```
POST /room/delete
Headers: Authorization: Bearer <JWT_TOKEN>
Body: roomId=xxx
```

响应:
```json
{"success": true, "msg": "删除成功"}
```

### 7.4 用户登录

```
POST /login
Body: userName=xxx&userPwd=xxx
```

响应:
```json
{
    "success": true,
    "token": "eyJhbG...",
    "username": "张三",
    "userId": 2
}
```

## 八、Redis 数据结构

| Key 模式 | 类型 | 用途 | TTL |
|---|---|---|---|
| `chat:offline:{username}` | List | 离线私信队列 | 7天 |
| `chat:room:{roomId}` | List | 房间历史消息（最多500条） | 7天 |
| `chat:private:{user1}:{user2}` | List | 私聊历史消息（最多500条） | 7天 |
| `chat:msgids:{roomId}` | Set | 房间消息ID去重 | 24小时 |

私聊 key 中 `{user1}` 和 `{user2}` 按字母序排列，确保双向一致性。
