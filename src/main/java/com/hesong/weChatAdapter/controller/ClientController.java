package com.hesong.weChatAdapter.controller;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.async.DeferredResult;

import com.hesong.smartbus.client.PackInfo;
import com.hesong.weChatAdapter.context.ContextPreloader;
import com.hesong.weChatAdapter.model.ChatMessage;
import com.hesong.weChatAdapter.runner.SmartbusExecutor;
import com.hesong.weChatAdapter.tools.API;
import com.hesong.weChatAdapter.tools.WeChatHttpsUtil;

@Controller
@RequestMapping("/client")
public class ClientController {

    private static Logger log = Logger.getLogger(ClientController.class);

    private static int count = 0;

    //private final Map<String, Map<String, DeferredResult<ChatMessage>>> chatRoomMap = new ConcurrentHashMap<String, Map<String, DeferredResult<ChatMessage>>>();
    private final Map<String, DeferredResult<ChatMessage>> chatRequests = new ConcurrentHashMap<String, DeferredResult<ChatMessage>>();
    private final SimpleDateFormat sdf = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/{account}/login", method = RequestMethod.POST)
    public
    String login(@PathVariable String account,
            @RequestParam("login") String login, HttpSession session) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> loginProps;
        try {
            loginProps = mapper.readValue(login, Map.class);

            String password = loginProps.get("password");
            log.info("Account: " + account + " Password: " + password);
            ++count;
            session.setAttribute("sender", account);
            ChatMessage msg = new ChatMessage();
            msg.setRoomId("room1");
            msg.setSender("系统");
            msg.setDate(sdf.format(new Date()));
            msg.setContent(account + "已加入");
        } catch (IOException e) {
            log.info("Json mapper exception: " + e.toString());
            return "failed";
        }
        return "chatDiv";
    }
    
    @RequestMapping(value = "/{account}", method = RequestMethod.GET)
    public
    String login(@PathVariable String account, HttpSession session) {
        try {

            log.info("In chat room1");
            ++count;
            session.setAttribute("sender", account);
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
        //取出当前登录用户
        final String user = (String)session.getAttribute("sender");
        //创建DeferredResult<Message>
        DeferredResult<ChatMessage> dr = new DeferredResult<ChatMessage>(5000);
        //若用户不存在则直接返回，否则将其放入用户请求列表中然后返回
        if(null == user){
            return dr;
        }else{
            //当DeferredResult对客户端响应后将其从列表中移除
            dr.onCompletion(new Runnable() {
                @Override
                public void run() {
//                    Map<String, DeferredResult<ChatMessage>> tmp = chatRoomMap.get("room1");
//                    if (tmp != null) {
//                        tmp.remove(user);
//                    }
                    chatRequests.remove(user);
                }
            });
//            Map<String, DeferredResult<ChatMessage>> tmp = chatRoomMap.get("room1");
//            if (tmp==null) {
//                tmp = new ConcurrentHashMap<String, DeferredResult<ChatMessage>>();
//                tmp.put(user, dr);
//                chatRoomMap.put("room1", tmp);
//            }else{
//                tmp.put(user, dr);
//            }
            chatRequests.put(user, dr);
            return dr;
        }
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/sendMessageRequest", method = RequestMethod.POST)
    public @ResponseBody String sendMessage(HttpServletRequest request, HttpSession session){
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
            msg.setSender("Bowen");
            msg.setRoomId(messageMap.get("roomId"));
            //msg.setRoomId(messageMap.get("roomId"));
            msg.setMsgtype(msgtype);
            //发布消息给所有用户
            processMessage(msg);

            return "success";
            
        } catch (IOException e) {
            log.info("Json mapper exception: "+e.toString());
            return "failed";
        }
    }
    
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/msg", method = RequestMethod.POST)
    public @ResponseBody String sendMessageWithSmartbus(HttpServletRequest request, HttpSession session){
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
            //msg.setRoomId(messageMap.get("roomId"));
            msg.setMsgtype(msgtype);
            //发布消息给所有用户
            processMessage(msg);
            
            // JSONRPC PARAMS
            JSONObject jsonrpc = new JSONObject();
            jsonrpc.put("jsonrpc", "2.0");
            jsonrpc.put("method", "imsm.ImMessageReceived");
            jsonrpc.put("id", UUID.randomUUID().toString());
            Map<String, Object> paramsList = new HashMap<String, Object>();
            paramsList.put("imtype", "weixin");
            paramsList.put("account", null);

            JSONObject user = new JSONObject();
            user.put("user", "Employer");
            user.put("usertype", API.MOCK_CLIENT);

            paramsList.put("user", user);
            paramsList.put("room", messageMap.get("roomId"));
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
            return "success";
            
        } catch (IOException e) {
            log.info("Json mapper exception: "+e.toString());
            return "failed";
        }
    }
    
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/{account}/invite", method = RequestMethod.POST)
    public @ResponseBody
    JSONObject invite(@PathVariable String account, HttpServletRequest request) {
        log.info("invite have been called.");
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> inviteProps;
        try {
            inviteProps = mapper.readValue(request.getInputStream(), Map.class);

            log.info("Invite: " + account + " Password: "
                    + inviteProps.toString());

        } catch (IOException e) {
            log.info("Json mapper exception: " + e.toString());
            e.printStackTrace();
            return WeChatHttpsUtil.getErrorMsg(8002, "Invitation request exception: "+e.toString());
        }
        return WeChatHttpsUtil.getErrorMsg(0, "Invitation accepted.");
    }
    
    private void processMessage(ChatMessage msg){
//        Map<String, DeferredResult<ChatMessage>> tmp = chatRoomMap.get(msg
//                .getRoomId());
//        if (tmp == null) {
//            return;
//        } else {
//            Set<String> keys = tmp.keySet();
//            for (String key : keys) {
//                tmp.get(key).setResult(msg);
//            }
//        }
        Set<String> keys = chatRequests.keySet();
        for(String key : keys){
            chatRequests.get(key).setResult(msg);
        }
    }

}
