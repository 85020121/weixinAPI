package com.hesong.weChatAdapter.controller;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.async.DeferredResult;

import com.hesong.jsonrpc.WeChatMethodSet;
import com.hesong.smartbus.client.PackInfo;
import com.hesong.weChatAdapter.context.ContextPreloader;
import com.hesong.weChatAdapter.model.ChatMessage;
import com.hesong.weChatAdapter.runner.JsonrpcHandlerRunner;
import com.hesong.weChatAdapter.runner.SmartbusExecutor;
import com.hesong.weChatAdapter.tools.API;
import com.hesong.weChatAdapter.tools.WeChatHttpsUtil;

@Controller
@RequestMapping("/client")
public class ClientController {

    private static Logger log = Logger.getLogger(ClientController.class);


    //private final Map<String, Map<String, DeferredResult<ChatMessage>>> chatRoomMap = new ConcurrentHashMap<String, Map<String, DeferredResult<ChatMessage>>>();
    private final Map<String, DeferredResult<ChatMessage>> chatRequests = new ConcurrentHashMap<String, DeferredResult<ChatMessage>>();
    private final SimpleDateFormat sdf = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");

    @ResponseBody
    @RequestMapping(value = "/{account}/login", method = RequestMethod.GET)
    public String login(@PathVariable String account, HttpSession session) {

        // JSONRPC PARAMS
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
            BlockingQueue<String> ackRet = new LinkedBlockingDeque<String>();
            JsonrpcHandlerRunner.ackRetQueue.put(id, ackRet);
            SmartbusExecutor.responseQueue.put(pack);
            String ret = ackRet.poll(5, TimeUnit.SECONDS);
            ackRet = null;
            JsonrpcHandlerRunner.ackRetQueue.remove(id);
            if(ret==null){
                log.info("NO ACK RETURN.");
                return "Failed";
            }
            log.info("ACK RETURN: "+ret);
            return ret;
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "failed";
        }

    }
    
    @RequestMapping(value = "/{account}/logout", method = RequestMethod.GET)
    public String logout(@PathVariable String account, HttpSession session) {

        // JSONRPC PARAMS
        Map<String, Object> paramsList = new HashMap<String, Object>();
        paramsList.put("imtype", "weixin");
        paramsList.put("account", null);

        JSONObject user = new JSONObject();
        user.put("user", account);
        user.put("usertype", API.MOCK_CLIENT);

        paramsList.put("user", user);
        String id = UUID.randomUUID().toString();
        JSONObject jsonrpc = WeChatMethodSet.createJsonrpcRequest(
                "imsm.ClientLoggedOut", id, paramsList);
        PackInfo pack = new PackInfo((byte) ContextPreloader.destUnitId,
                (byte) ContextPreloader.destClientId,
                (byte) ContextPreloader.srcUnitId,
                (byte) ContextPreloader.srctClientId, jsonrpc.toString());
        try {
            SmartbusExecutor.responseQueue.put(pack);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "failed";
        }
        return "success";

    }
    
    @RequestMapping(value = "/{account}/chatroom", method = RequestMethod.GET)
    public
    String login(@PathVariable String account, HttpSession session, HttpServletResponse response) {
        try {
            response.addCookie(new Cookie("MOCK_CLIENT_ID", account));
            session.setAttribute("clientId", account);
            ChatMessage msg = new ChatMessage();
            msg.setRoomId("room1");
            msg.setSender("系统");
            msg.setDate(sdf.format(new Date()));
            msg.setContent(account + "已加入");
        } catch (Exception e) {
            log.info("Json mapper exception: " + e.toString());
            return "failed";
        }
        return "chatRoom";
    }
    
    @RequestMapping(value = "/getMessages", method = RequestMethod.GET)
    @ResponseBody
    public DeferredResult<ChatMessage> getMessages(HttpSession session){
        // 取出当前登录用户
        final String clientId = (String)session.getAttribute("clientId");
        // 创建DeferredResult<Message>
        DeferredResult<ChatMessage> dr = new DeferredResult<ChatMessage>(10000);
        // 若用户不存在则直接返回，否则将其放入用户请求列表中然后返回
        if(null == clientId){
            return dr;
        }else{
            // 当DeferredResult对客户端响应后将其从列表中移除
            dr.onCompletion(new Runnable() {
                @Override
                public void run() {
//                    Map<String, DeferredResult<ChatMessage>> tmp = chatRoomMap.get(clientId);
//                    if (tmp != null) {
//                        tmp.remove(clientId);
//                    }
                    chatRequests.remove(clientId);
                }
            });
//            Map<String, DeferredResult<ChatMessage>> tmp = chatRoomMap.get(clientId);
//            log.info("ROOOOOOOOM map:"+ chatRoomMap.toString());
//            if (tmp==null) {
//                log.info("Create new room for client: "+clientId);
//                tmp = new ConcurrentHashMap<String, DeferredResult<ChatMessage>>();
//                tmp.put(clientId, dr);
//                chatRoomMap.put(clientId, tmp);
//            }else{
//                tmp.put(clientId, dr);
//            }
            chatRequests.put(clientId, dr);
            return dr;
        }
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/{account}/sendMessageRequest", method = RequestMethod.POST)
    public @ResponseBody JSONObject sendMessage(@PathVariable String account, HttpServletRequest request, HttpSession session){
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> messageMap;
        try {
            messageMap = mapper.readValue(request.getInputStream(), Map.class);
            String content = messageMap.get("content");
            log.info("content: "+content);

            ChatMessage msg = new ChatMessage();
            String msgtype = messageMap.get("msgtype");
            msg.setContent(content);
            msg.setDate(sdf.format(new Date()));
            msg.setSender(messageMap.get("sender"));
            msg.setRoomId(messageMap.get("roomId"));
            msg.setMsgtype(msgtype);
            // 发布消息给所有用户
            processMessage(msg, account);

            return WeChatHttpsUtil.getErrorMsg(0, "OK");
            
        } catch (Exception e) {
            log.info("Json mapper exception: "+e.toString());
            return WeChatHttpsUtil.getErrorMsg(7999, e.toString());
        }
    }
    
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/{account}/msg", method = RequestMethod.POST)
    public @ResponseBody String sendMessageWithSmartbus(@PathVariable String account, HttpServletRequest request, HttpSession session){
        log.info("ClientId: "+session.getAttribute("clientId"));
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> messageMap;
        try {
            messageMap = mapper.readValue(request.getInputStream(), Map.class);
            String content = messageMap.get("content");
            log.info("content: "+content);

            ChatMessage msg = new ChatMessage();
            String msgtype = messageMap.get("msgtype");
            msg.setContent(content);
            msg.setDate(sdf.format(new Date()));
            msg.setSender(messageMap.get("sender"));
            msg.setRoomId(messageMap.get("roomId"));
            // msg.setRoomId(messageMap.get("roomId"));
            msg.setMsgtype(msgtype);
            // 发布消息给所有用户
            processMessage(msg, account);
            
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
            paramsList.put("room_id", messageMap.get("roomId"));
            paramsList.put("msgtype", msgtype);
            
            JSONObject sendmsg = new JSONObject();
            sendmsg.put("msgtype", msgtype);
            sendmsg.put("room_id", messageMap.get("roomId"));
            JSONObject msgcontent = new JSONObject();
            msgcontent.put("content", content);
            sendmsg.put(msgtype, msgcontent);
            paramsList.put("msgcontent", sendmsg);
            
            jsonrpc.put("params", paramsList);
            // JSONRPC END
            
            PackInfo pack = new PackInfo((byte)ContextPreloader.destUnitId, (byte)ContextPreloader.destClientId, (byte)ContextPreloader.srcUnitId, (byte)ContextPreloader.srctClientId, jsonrpc.toString());  
            try {
                SmartbusExecutor.responseQueue.put(pack);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return "Success";
            
        } catch (IOException e) {
            log.info("Json mapper exception: "+e.toString());
            return  "Failed";
        }
    }
    
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/{account}/invite", method = RequestMethod.POST)
    public @ResponseBody
    JSONObject invite(@PathVariable String account, HttpServletRequest request) {
        log.info("invite have been called.");
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, String> inviteProps = mapper.readValue(request.getInputStream(), Map.class);

            ChatMessage msg = new ChatMessage();
            msg.setMsgtype("invitation");
            msg.setRoomId(inviteProps.get("roomId"));
            processMessage(msg, account);

        } catch (IOException e) {
            log.info("Json mapper exception: " + e.toString());
            e.printStackTrace();
            return WeChatHttpsUtil.getErrorMsg(8002, "Invitation request exception: "+e.toString());
        }
        return WeChatHttpsUtil.getErrorMsg(0, "Invitation received.");
    }
    
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/{account}/ackInvitation", method = RequestMethod.POST)
    public @ResponseBody
    String ackInvitation(@PathVariable String account, HttpServletRequest request) {
        log.info("ackInvitation have been called.");
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, String> inviteProps = mapper.readValue(request.getInputStream(), Map.class);
            log.info("ACK info: "+inviteProps.toString());
            Map<String, Object> paramsList = new HashMap<String, Object>();
            paramsList.put("imtype", "weixin");
            paramsList.put("account", account);

            JSONObject user = new JSONObject();
            user.put("user", account);
            user.put("usertype", API.MOCK_CLIENT);

            paramsList.put("user", user);
            paramsList.put("room_id", inviteProps.get("roomId"));
            paramsList.put("agreed", inviteProps.get("agreed").equalsIgnoreCase("true")?true:false);
            String id = UUID.randomUUID().toString();
            JSONObject jsonrpc = WeChatMethodSet.createJsonrpcRequest("imsm.AckInviteEnterRoom", id, paramsList);
            PackInfo pack = new PackInfo((byte)ContextPreloader.destUnitId, (byte)ContextPreloader.destClientId, (byte)ContextPreloader.srcUnitId, (byte)ContextPreloader.srctClientId, jsonrpc.toString());  
            try {
                SmartbusExecutor.responseQueue.put(pack);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (IOException e) {
            log.info("Json mapper exception: " + e.toString());
            e.printStackTrace();
            return "Failed.";
        }
        return "Success.";
    }
    
    private void processMessage(ChatMessage msg, String clientId){
//        Map<String, DeferredResult<ChatMessage>> tmp = chatRoomMap.get(clientId);
//        if (tmp == null) {
//            return;
//        } else {
//            Set<String> keys = tmp.keySet();
//            for (String key : keys) {
//                tmp.get(key).setResult(msg);
//            }
//        }
//        Set<String> keys = chatRequests.keySet();
//        for(String key : keys){
//            chatRequests.get(key).setResult(msg);
//        }
        DeferredResult<ChatMessage> tmp = chatRequests.get(clientId);
        if (tmp == null) {
            return;
        } else {
            tmp.setResult(msg);
        }
    }

}
