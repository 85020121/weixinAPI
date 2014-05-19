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
    private static final int MAX_HANDLER_NUM = 10;
    private static ExecutorService pool = Executors
            .newFixedThreadPool(THREAD_POOL_SIZE);
    
    private static Logger log = Logger.getLogger(MessageExecutor.class);
    
    public static BlockingQueue<Map<String, String>> messageQueue = new LinkedBlockingDeque<Map<String,String>>();

    private static BlockingQueue<Map<String, String>> messageToSendQueue = new LinkedBlockingDeque<Map<String,String>>();
    public static BlockingQueue<JSONObject> groupMessagesQueue = new LinkedBlockingDeque<JSONObject>();
    public static BlockingQueue<JSONObject> activeMessagesQueue = new LinkedBlockingDeque<JSONObject>();
    
    public static void execute(){
        for (int i = 0; i < MAX_HANDLER_NUM; i++) {
            MessageRouter router = new MessageRouter(messageQueue, messageToSendQueue);
            pool.execute(router);
            MessageSender sender = new MessageSender(messageToSendQueue);
            pool.execute(sender);
            GroupMessagesSender gourpSender = new GroupMessagesSender(groupMessagesQueue);
            ActiveMessagesSender activeSender = new ActiveMessagesSender(activeMessagesQueue);
            pool.execute(gourpSender);
            pool.execute(activeSender);
        }
        log.info("MessageExecutor executed.");
    }
}
