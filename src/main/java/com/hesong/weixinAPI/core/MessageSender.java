package com.hesong.weixinAPI.core;

import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import com.hesong.weixinAPI.tools.WeChatHttpsUtil;

import net.sf.json.JSONObject;

public class MessageSender implements Runnable {
    private static Logger log = Logger.getLogger(MessageSender.class);
    private static String SEND_MESSAGE_REQUEST_URL = "https://api.weixin.qq.com/cgi-bin/message/custom/send?access_token=";

    private BlockingQueue<Map<String, String>> messageQueue;
    
    public MessageSender(BlockingQueue<Map<String, String>> messageQueue) {
        super();
        this.messageQueue = messageQueue;
    }

    public BlockingQueue<Map<String, String>> getResponseQueue() {
        return messageQueue;
    }

    public void setResponseQueue(BlockingQueue<Map<String, String>> messageQueue) {
        this.messageQueue = messageQueue;
    }


    @Override
    public void run() {
        while(true){
            try {
                Map<String, String> message = messageQueue.take();
                log.info("Message to send: "+message.toString());
                String access_token = (String)message.get("access_token");
                String messageToSend = message.get("message");
                String request = SEND_MESSAGE_REQUEST_URL + access_token;
                JSONObject ret = WeChatHttpsUtil.httpsRequest(request, "POST", messageToSend);
                log.info("Send message ret: "+ret.toString());;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
    }

}
