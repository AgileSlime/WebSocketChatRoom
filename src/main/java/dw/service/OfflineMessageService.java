package dw.service;

import dw.util.RedisUtil;
import redis.clients.jedis.Jedis;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

public class OfflineMessageService {

    public void saveOfflineMessage(String username, String message) {
        try (Jedis jedis = RedisUtil.getJedis()) {
            String key = "chat:offline:" + username;
            // Ensure key is a list, delete if wrong type
            String type = jedis.type(key);
            if (!"list".equals(type) && !"none".equals(type)) {
                jedis.del(key);
            }
            jedis.lpush(key, message);
            jedis.expire(key, 7 * 24 * 60 * 60); // 7 days
        }
    }

    public List<String> getOfflineMessages(String username) {
        try (Jedis jedis = RedisUtil.getJedis()) {
            String key = "chat:offline:" + username;
            String type = jedis.type(key);
            if (!"list".equals(type)) {
                if (!"none".equals(type)) {
                    jedis.del(key);
                }
                return new ArrayList<>();
            }
            List<String> msgs = jedis.lrange(key, 0, -1);
            return msgs != null ? msgs : new ArrayList<>();
        }
    }

    public void addMsgId(String roomId, String msgId) {
        try (Jedis jedis = RedisUtil.getJedis()) {
            String key = "chat:msgids:" + roomId;
            String type = jedis.type(key);
            if (!"set".equals(type) && !"none".equals(type)) {
                jedis.del(key);
            }
            jedis.sadd(key, msgId);
            jedis.expire(key, 24 * 60 * 60); // 24 hours
        }
    }

    public boolean hasMsgId(String roomId, String msgId) {
        try (Jedis jedis = RedisUtil.getJedis()) {
            String key = "chat:msgids:" + roomId;
            String type = jedis.type(key);
            if (!"set".equals(type)) {
                return false;
            }
            return jedis.sismember(key, msgId);
        }
    }

    // Room message history (Redis-only, 7-day TTL)
    public void saveRoomMessage(String roomId, String message) {
        try (Jedis jedis = RedisUtil.getJedis()) {
            String key = "chat:room:" + roomId;
            String type = jedis.type(key);
            if (!"list".equals(type) && !"none".equals(type)) {
                jedis.del(key);
            }
            jedis.lpush(key, message);
            jedis.expire(key, 7 * 24 * 60 * 60);
            jedis.ltrim(key, 0, 499);
        }
    }

    public List<String> getRoomMessages(String roomId, int limit) {
        try (Jedis jedis = RedisUtil.getJedis()) {
            String key = "chat:room:" + roomId;
            String type = jedis.type(key);
            if (!"list".equals(type)) {
                if (!"none".equals(type)) {
                    jedis.del(key);
                }
                return new ArrayList<>();
            }
            List<String> msgs = jedis.lrange(key, 0, limit - 1);
            if (msgs == null) return new ArrayList<>();
            Collections.reverse(msgs);
            return msgs;
        }
    }

    // Private chat history (Redis-only, 7-day TTL)
    public void savePrivateMessage(String sender, String receiver, String message) {
        try (Jedis jedis = RedisUtil.getJedis()) {
            String key = privateKey(sender, receiver);
            String type = jedis.type(key);
            if (!"list".equals(type) && !"none".equals(type)) {
                jedis.del(key);
            }
            jedis.lpush(key, message);
            jedis.expire(key, 7 * 24 * 60 * 60);
            jedis.ltrim(key, 0, 499);
        }
    }

    public List<String> getPrivateMessages(String user1, String user2, int limit) {
        try (Jedis jedis = RedisUtil.getJedis()) {
            String key = privateKey(user1, user2);
            String type = jedis.type(key);
            if (!"list".equals(type)) {
                if (!"none".equals(type)) {
                    jedis.del(key);
                }
                return new ArrayList<>();
            }
            List<String> msgs = jedis.lrange(key, 0, limit - 1);
            if (msgs == null) return new ArrayList<>();
            Collections.reverse(msgs);
            return msgs;
        }
    }

    private String privateKey(String a, String b) {
        if (a.compareTo(b) < 0) {
            return "chat:private:" + a + ":" + b;
        } else {
            return "chat:private:" + b + ":" + a;
        }
    }
}
