package com.hesong.weixinAPI.core;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;


public class MessageExecutor {
    private static final int THREAD_POOL_SIZE = 10;
    private static final int MAX_HANDLER_NUM = 6;
    private static ExecutorService pool = Executors
            .newFixedThreadPool(THREAD_POOL_SIZE);
    
    private static Logger log = Logger.getLogger(MessageExecutor.class);
    
    public static BlockingQueue<Map<String, String>> messageQueue = new LinkedBlockingDeque<Map<String,String>>();

    private static BlockingQueue<JSONObject> messageToSendQueue = new LinkedBlockingDeque<JSONObject>();
    public static BlockingQueue<JSONObject> groupMessagesQueue = new LinkedBlockingDeque<JSONObject>();
    public static BlockingQueue<JSONObject> activeMessagesQueue = new LinkedBlockingDeque<JSONObject>();
    private static BlockingQueue<JSONObject> suaRequestToExecuteQueue = new LinkedBlockingDeque<JSONObject>();
    
    public static void execute(){
        for (int i = 0; i < MAX_HANDLER_NUM; i++) {
            MessageRouter router = new MessageRouter(messageQueue, messageToSendQueue, suaRequestToExecuteQueue);
            pool.execute(router);
            MessageSender sender = new MessageSender(messageToSendQueue);
            pool.execute(sender);
            SUAExecutor executor = new SUAExecutor(suaRequestToExecuteQueue);
            pool.execute(executor);
            
            GroupMessagesSender gourpSender = new GroupMessagesSender(groupMessagesQueue);
            ActiveMessagesSender activeSender = new ActiveMessagesSender(activeMessagesQueue);
            pool.execute(gourpSender);
            pool.execute(activeSender);
            
        }
        log.info("MessageExecutor executed.");
    }
    
    public static void sendMessage(String openid, String token, String text, String type) {
        JSONObject message = new JSONObject();
        message.put("msgtype", type);
        message.put("touser", openid);
        JSONObject content = new JSONObject();
        if (type.equals("text")) {
            content.put("content", text);
        } else if (type.equals("image")) {
            content.put("media_id", text);
        } else if (type.equals("voice")) {
            content.put("media_id", text);
        }
        message.put(type, content);
        message.put("access_token", token);
        try {
            messageToSendQueue.put(message);
        } catch (InterruptedException e) {
            log.error("Put message to queue error: "+e.toString());
        }
    }
}
