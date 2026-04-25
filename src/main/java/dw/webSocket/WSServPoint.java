package dw.webSocket;

import com.alibaba.fastjson.JSONObject;
import dw.pojo.Msg;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint(value="/chatroom")
public class WSServPoint {
    static Set<Session> set = new CopyOnWriteArraySet<>();
    static  List<String>userList=new ArrayList<>();
    Map<String,String>map;
    private Msg ms;
    @OnOpen
    public void onOpen(Session session) throws UnsupportedEncodingException {
        System.out.println("连接建立成功！！！");
        String msg=session.getQueryString();
        msg= URLDecoder.decode(msg,"utf-8");
        map=new HashMap<String,String>();
        if(msg.contains("&")){
            String[] str=msg.split("\\&");
            for(String s:str){
                String[] strs=s.split("=");
                map.put(strs[0],strs[1]);
            }
        }
        else{
            String[] strs=msg.split("=");
            map.put(strs[0],strs[1]);
        }
        userList.add(map.get("loginName"));
        System.out.println("map:"+map);
        ms=new Msg();
        ms.setType("s");
        ms.setMsgSender("system");
        ms.setMsgDate(new Date());
        ms.setUserList(userList);
        ms.setMsgInfo(map.get("loginName")+"已上线");
        set.add(session);
        broadcast(set, JSONObject.toJSONString(ms));
    }


    @OnClose
    public void onClose(Session session) {
        userList.remove(map.get("loginName"));
        ms=new Msg();
        ms.setType("s");
        ms.setMsgSender("system");
        ms.setMsgDate(new Date());
        ms.setUserList(userList);
        ms.setMsgInfo(map.get("loginName")+"已下线");
        set.remove(session);
        broadcast(set, JSONObject.toJSONString(ms));
        System.out.println("连接已关闭！！！");
    }


    @OnMessage
    public void onMessage(String message,Session session) throws IOException, InterruptedException {
        System.out.println("信息已接受！！！"+message);
        ms=new Msg();
        ms.setType("p");
        ms.setMsgSender(map.get("loginName"));
        ms.setMsgDate(new Date());
        ms.setMsgInfo(message);
        broadcast(set,JSONObject.toJSONString(ms));
    }


    @OnError
    public void onError(Throwable t) {
        System.out.println("系统异常！！！");
        t.printStackTrace();
    }
    public void broadcast(Set<Session>set, String message){
        for(Session s:set){
            try {
                s.getBasicRemote().sendText(message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
