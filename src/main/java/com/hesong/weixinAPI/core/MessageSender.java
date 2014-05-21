package com.hesong.weixinAPI.core;

import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import com.hesong.weixinAPI.tools.WeChatHttpsUtil;

import net.sf.json.JSONObject;

public class MessageSender implements Runnable {
    private static Logger log = Logger.getLogger(MessageSender.class);
    private static String SEND_MESSAGE_REQUEST_URL = "https://api.weixin.qq.com/cgi-bin/message/custom/send?access_token=";

    private BlockingQueue<JSONObject> messageQueue;
    
    public MessageSender(BlockingQueue<JSONObject> messageQueue) {
        super();
        this.messageQueue = messageQueue;
    }

    public BlockingQueue<JSONObject> getResponseQueue() {
        return messageQueue;
    }

    public void setResponseQueue(BlockingQueue<JSONObject> messageQueue) {
        this.messageQueue = messageQueue;
    }


    @Override
    public void run() {
        while(true){
            try {
                JSONObject message = messageQueue.take();
                log.info("Message to send: "+message.toString());
                String access_token = message.getString("access_token");
                message.remove("access_token");
                String request = SEND_MESSAGE_REQUEST_URL + access_token;
                JSONObject ret = WeChatHttpsUtil.httpsRequest(request, "POST", message.toString());
                log.info("Send message ret: "+ret.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
    }

}
