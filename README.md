# WebSocketChatRoom

基于 JSR 356 WebSocket 的多人实时聊天室，支持多房间隔离、JWT 认证、私聊、离线消息缓存。

## 一、技术栈

| 层次 | 技术 |
|---|---|
| 后端 | Servlet 3.0 + WebSocket (JSR 356) + JDBC |
| 前端 | JSP + jQuery + Vanilla JS |
| 认证 | JWT (jjwt) + BCrypt 密码加密 |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis (Jedis) — 离线消息/聊天历史 |
| 服务器 | Apache Tomcat 9.0 |
| JDK | Java 1.8 |

## 二、核心功能

1. **多房间聊天** — 创建/加入/删除房间，消息按房间隔离，房间创建者有删除权限
2. **私聊** — 从在线用户列表或左侧会话列表发起一对一私聊，离线消息自动缓存
3. **会话管理** — 统一会话列表（群聊 + 私聊），支持删除私聊会话（历史记录保留 7 天）
4. **JWT 认证** — 注册/登录发放 Token，WebSocket 连接携带 Token 验证身份
5. **离线消息** — 用户上线自动拉取离线私信和房间历史（Redis 缓存，7 天过期）
6. **心跳保活** — 服务端每 5 秒 PING，30 秒超时自动清理离线连接
7. **自动重连** — 客户端断线后指数退避重连（最多 10 次）
8. **应用层 ACK** — 消息确认、去重、防重复渲染

## 三、快速启动

### 1. 前置条件

- JDK 1.8 + Apache Tomcat 9.0
- MySQL 8.0（创建数据库并导入 `chatroom.sql`）
- Redis（默认 localhost:6379）

### 2. 配置数据库

修改 `src/main/java/dw/util/DBUtil.java` 中的数据库连接信息：

```java
String url = "jdbc:mysql://localhost:3306/chatroom?useSSL=false&characterEncoding=utf8";
String username = "root";
String password = "你的密码";
```

### 3. 部署运行

1. 使用 IntelliJ IDEA 导入项目
2. 配置 Tomcat 运行环境
3. 启动后访问 `http://localhost:8080/WebSocketChatRoom_war/`

### 4. WebSocket 连接

```javascript
var ws = new WebSocket('ws://localhost:8080/WebSocketChatRoom_war/chatroom?token=' + token);
```

## 四、项目结构

```
├── src/main/java/dw/
│   ├── pojo/          # 实体类 (User, Room, Msg, UserSession, RoomMember)
│   ├── dao/           # 数据访问层 (UserDAO, RoomDAO, MessageDAO)
│   ├── servlet/       # Servlet (Login, Register, Room, Chat, SSE)
│   ├── webSocket/     # WebSocket 端点 (WSServPoint)
│   ├── service/       # 业务服务 (OfflineMessageService)
│   └── util/          # 工具类 (DBUtil, JWTUtil, RedisUtil)
├── web/
│   ├── index.jsp      # 登录页
│   ├── register.jsp   # 注册页
│   └── WEB-INF/jsp/   # 聊天页面 (websocketChatroom.jsp, privateChat.jsp)
├── chatroom.sql       # 数据库建表脚本
├── protocol.md        # WebSocket 通信协议文档
└── stress_test_report.md  # 压力测试报告
```

## 五、数据库表

| 表名 | 说明 |
|---|---|
| `user` | 用户表 (id, username, password(BCrypt), create_time) |
| `room` | 房间表 (id, room_name, creator_id, create_time) |
| `room_member` | 房间成员关联表 (id, room_id, user_id, join_time) |

## 六、REST API

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/login` | 用户登录，返回 JWT Token |
| POST | `/register` | 用户注册 |
| GET | `/room/list` | 获取房间列表 |
| POST | `/room/create` | 创建房间 (需 JWT) |
| POST | `/room/delete` | 删除房间 (仅创建者可操作) |
| GET | `/chat` | 进入群聊页面 |

## 七、WebSocket 协议

完整协议文档见 [protocol.md](protocol.md)

| 消息类型 | 方向 | 说明 |
|---|---|---|
| CHAT | C→S | 发送房间消息 |
| PRIVATE | C→S | 发送私聊消息 |
| SWITCH_ROOM | C→S | 切换房间（不断开连接） |
| LOAD_PRIVATE_HISTORY | C→S | 加载私聊历史 |
| SYSTEM | S→C | 系统通知（用户进出/在线列表） |
| PING | S→C | 心跳（每 5 秒） |
| PONG | C→S | 心跳响应 |
| ACK | C→S | 应用层确认 |

## 八、压力测试

详见 [stress_test_report.md](stress_test_report.md)

| 指标 | WebSocket | SSE |
|---|---|---|
| 1000 并发成功率 | 100% | 18.6% |
| 每连接内存 | 0.60 MB | 1.00 MB |
| 消息延迟 | < 50 ms | 200-500 ms |
| 双向通信 | 支持 | 不支持 |
