package com.hesong.weChatAdapter.controller;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.async.DeferredResult;

import com.hesong.smartbus.client.net.Client.SendDataError;
import com.hesong.weChatAdapter.model.ChatMessage;

@Controller
@RequestMapping("/chat")
public class ChatController {
    private static Logger log = Logger.getLogger(ChatController.class);
    
    private static int count = 0;
    
    private final Map<String, Map<String, DeferredResult<ChatMessage>>> chatRoomMap = new ConcurrentHashMap<String, Map<String, DeferredResult<ChatMessage>>>();
    private final Map<String, DeferredResult<ChatMessage>> chatRequests = new ConcurrentHashMap<String, DeferredResult<ChatMessage>>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    @RequestMapping(method = RequestMethod.GET)
    public String show(HttpSession session) {
        log.info("Enter room.");
        ++count;
        String name = "Guest0"+count;
        session.setAttribute("sender", "room1");
        ChatMessage msg = new ChatMessage();
        msg.setRoomId("room1");
        msg.setSender("系统");
        msg.setDate(sdf.format(new Date()));
        msg.setContent(name + "已加入");
        //通知所有用户有人进入聊天室
        processMessage(msg);
        return "chatDiv";
    }
    
    @RequestMapping(value = "/enterRoom", method = RequestMethod.POST)
    public String login(@RequestParam String name, HttpSession session){
        log.info("Enter room.");
        session.setAttribute("sender", name);
        ChatMessage msg = new ChatMessage();
        msg.setSender("系统");
        msg.setDate(sdf.format(new Date()));
        msg.setContent(name + "已加入");
        //通知所有用户有人进入聊天室
        processMessage(msg);
        return "room";
    }
    
    @RequestMapping(value = "/getMessages", method = RequestMethod.GET)
    @ResponseBody
    public DeferredResult<ChatMessage> getMessages(HttpSession session){
        //取出当前登录用户
        final String user = "room1";//(String)session.getAttribute("sender");
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
                    // TODO 自动生成的方法存根
                    chatRequests.remove(user);
                }
            });
            chatRequests.put(user, dr);
            return dr;
        }
    }
    
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/sendMessage", method = RequestMethod.POST)
    @ResponseBody
    public String sendMessage(HttpSession session, @RequestParam("message") String message){
        log.info("set msg: "+message);
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> messageMap;
        try {
            messageMap = mapper.readValue(message, Map.class);

            String content = messageMap.get("content");
            log.info("content: "+content);

            ChatMessage msg = new ChatMessage();
            msg.setContent(content);
            msg.setDate(sdf.format(new Date()));
            //msg.setSender("test");
            msg.setSender((String)session.getAttribute("sender"));
            //发布消息给所有用户
            processMessage(msg);
            return "success";
            
        } catch (IOException e) {
            log.info("Json mapper exception: "+e.toString());
            return "failed";
        }

    }
    
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/sendMessageQuest", method = RequestMethod.POST)
    public @ResponseBody String sendMessage(HttpServletRequest request){
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> messageMap;
        try {
            messageMap = mapper.readValue(request.getInputStream(), Map.class);
            String content = messageMap.get("content");
            log.info("content: "+content);

            ChatMessage msg = new ChatMessage();
            msg.setContent(content);
            msg.setDate(sdf.format(new Date()));
            //msg.setSender("test");
            msg.setSender("room1");
            //发布消息给所有用户
            processMessage(msg);
            return "success";
            
        } catch (IOException e) {
            log.info("Json mapper exception: "+e.toString());
            return "failed";
        }
    }
    
    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    @ResponseBody
    public String logout(HttpSession session){
        log.info("logout");
        ChatMessage msg = new ChatMessage();
        String user = (String)session.getAttribute("sender");
        msg.setContent("已离开");
        msg.setDate(sdf.format(new Date()));
        msg.setSender(user);
        chatRequests.remove(user);
        //通知所有用户有人离开聊天室
        processMessage(msg);
        return "Success";
    }
    
    private void processMessage(ChatMessage msg){
        Set<String> keys = chatRequests.keySet();
        for(String key : keys){
            chatRequests.get(key).setResult(msg);
        }
    }
}
