package com.hesong.ws;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.hesong.jsonrpc.WeChatMethodSet;
import com.hesong.smartbus.client.PackInfo;
import com.hesong.weChatAdapter.context.ContextPreloader;
import com.hesong.weChatAdapter.runner.SmartbusExecutor;
import com.hesong.weChatAdapter.tools.API;


@ServerEndpoint (value = "/websocket/wechatMessage")
public class WeChatMessageAnnotation {
    
    private static Logger log = Logger.getLogger(WeChatMessageAnnotation.class);
    
    private static String WORKING_NUM = "working_num";
    
    private static final AtomicInteger connectionIds = new AtomicInteger();
    public static final Map<String, Session> session_map = new ConcurrentHashMap<String, Session>();
    
    private Session session;
    
    public WeChatMessageAnnotation() {
        log.info("New connection, count: " + connectionIds.getAndIncrement());
    }
    
    @OnOpen
    public void start(Session session) {
        this.session = session;
        Map<String, List<String>> pathParams = session.getRequestParameterMap();
        if (pathParams.containsKey(WORKING_NUM)) {
            String account = pathParams.get(WORKING_NUM).get(0);
            session_map.put(account, session);
            log.info("New websocket session added for working_num: " + pathParams.get(WORKING_NUM));
            
            Map<String, Object> paramsList = new HashMap<String, Object>();
            paramsList.put("imtype", "weixin");
            paramsList.put("account", null);

            JSONObject user = new JSONObject();
            user.put("user", account);
            user.put("usertype", API.MOCK_CLIENT);

            paramsList.put("user", user);

            String id = UUID.randomUUID().toString();
            
            JSONObject jsonrpc = WeChatMethodSet.createJsonrpcRequest(
                    "imsm.ClientLoggedIn", id, paramsList);
            PackInfo pack = new PackInfo((byte) ContextPreloader.destUnitId,
                    (byte) ContextPreloader.destClientId,
                    (byte) ContextPreloader.srcUnitId,
                    (byte) ContextPreloader.srctClientId, jsonrpc.toString());
            
            try {
                SmartbusExecutor.responseQueue.put(pack);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            log.debug(session_map.toString());
        } else {
            log.warn("Now wroking_num param.");
        }
    }
    
    @OnClose
    public void end() {
        Map<String, List<String>> pathParams = this.session.getRequestParameterMap();
        log.info(pathParams.toString());
        if (pathParams.containsKey(WORKING_NUM)) {
            session_map.remove(pathParams.get(WORKING_NUM).get(0));
            log.info("Websocket session has been removed for working_num: " + pathParams.get(WORKING_NUM));
            log.debug(session_map.toString());
        }
    }
    
    @OnMessage
    public void incoming(String message) throws Exception {
        try {
            
            JSONObject msg = JSONObject.fromObject(message);
            String action = msg.getString("action");
            if (action.equalsIgnoreCase("sendMessage")) {
                sendMessageToSmartbus(msg.getJSONObject("params"), msg.getString("id"));
            } else if (action.equalsIgnoreCase("ack_invite")) {
                ackInvitation(msg.getJSONObject("params"), msg.getString("id"));
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Exception: "+e.toString());
        }
    }
    
    @OnError
    public void onError(Throwable t) throws Throwable {
        log.error("@OnError: " + t.toString());
    }
    
    public static void sendMessage(String account, String message) {
        try {
            session_map.get(account).getBasicRemote().sendText(message);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    private static void sendMessageToSmartbus(JSONObject msg, String id) {
        String ack;
        String account = msg.getString("working_num");
        try {
            // JSONRPC PARAMS
            JSONObject jsonrpc = new JSONObject();
            jsonrpc.put("jsonrpc", "2.0");
            jsonrpc.put("method", "imsm.ImMessageReceived");
            jsonrpc.put("id", UUID.randomUUID().toString());
            Map<String, Object> paramsList = new HashMap<String, Object>();
            paramsList.put("imtype", "weixin");
            paramsList.put("account", null);

            JSONObject user = new JSONObject();
            user.put("user", account);
            user.put("usertype", API.MOCK_CLIENT);

            paramsList.put("user", user);
            paramsList.put("room_id", msg.getString("room_id"));
            paramsList.put("msgtype", msg.getString("msgtype"));

            JSONObject sendmsg = new JSONObject();
            sendmsg.put("msgtype", msg.getString("msgtype"));
            sendmsg.put("room_id", msg.getString("room_id"));
            JSONObject msgcontent = new JSONObject();
            msgcontent.put("content", msg.getString("content"));
            sendmsg.put(msg.getString("msgtype"), msgcontent);
            paramsList.put("msgcontent", sendmsg);

            jsonrpc.put("params", paramsList);
            // JSONRPC END

            PackInfo pack = new PackInfo((byte) ContextPreloader.destUnitId,
                    (byte) ContextPreloader.destClientId,
                    (byte) ContextPreloader.srcUnitId,
                    (byte) ContextPreloader.srctClientId, jsonrpc.toString());

            SmartbusExecutor.responseQueue.put(pack);
            
            JSONObject ret = new JSONObject();
            ret.put("id", id);
            ret.put("errcode", 0);
            ret.put("errmsg", "ok");
            ack = ret.toString();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // e.printStackTrace();
            log.error("Exception: " + e.toString());
            JSONObject ret = new JSONObject();
            ret.put("id", id);
            ret.put("errcode", 1);
            ret.put("errmsg", e.toString());
            ack = ret.toString();
        }
        
        if (session_map.containsKey(account)) {
            try {
                session_map.get(account).getBasicRemote().sendText(ack);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    private static void ackInvitation(JSONObject message, String id) {
        String ack;
        String account = message.getString("working_num");
        try {
            Map<String, Object> paramsList = new HashMap<String, Object>();
            paramsList.put("imtype", "weixin");

            JSONObject user = new JSONObject();
            user.put("user", account);
            user.put("usertype", API.MOCK_CLIENT);

            paramsList.put("user", user);
            paramsList.put("room_id", message.getString("room_id"));
            paramsList.put("agreed", message.getBoolean("agreed"));
            JSONObject jsonrpc = WeChatMethodSet.createJsonrpcRequest(
                    "imsm.AckInviteEnterRoom", UUID.randomUUID().toString(),
                    paramsList);
            PackInfo pack = new PackInfo((byte) ContextPreloader.destUnitId,
                    (byte) ContextPreloader.destClientId,
                    (byte) ContextPreloader.srcUnitId,
                    (byte) ContextPreloader.srctClientId, jsonrpc.toString());

            SmartbusExecutor.responseQueue.put(pack);
            JSONObject ret = new JSONObject();
            ret.put("id", id);
            ret.put("errcode", 0);
            ret.put("errmsg", "ok");
            ack = ret.toString();
        } catch (InterruptedException e) {
            log.error("Response BlockingQueue exception: " + e.toString());
            JSONObject ret = new JSONObject();
            ret.put("id", id);
            ret.put("errcode", 1);
            ret.put("errmsg", e.toString());
            ack = ret.toString();
        }
        
        if (session_map.containsKey(account)) {
            try {
                session_map.get(account).getBasicRemote().sendText(ack);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
