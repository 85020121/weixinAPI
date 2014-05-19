package com.hesong.weixinAPI.core;

import java.util.concurrent.BlockingQueue;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.log4j.Logger;

import com.hesong.weixinAPI.tools.WeChatHttpsUtil;

public class ActiveMessagesSender implements Runnable {
    private static Logger log = Logger.getLogger(MessageSender.class);
    private static String SEND_MESSAGE_REQUEST_URL = "https://api.weixin.qq.com/cgi-bin/message/custom/send?access_token=";
    private static String GET_FOLLOWERS_OPENID_REQUEST_URL = "https://api.weixin.qq.com/cgi-bin/user/get?access_token=";

    private BlockingQueue<JSONObject> activeMessagesQueue;

    public ActiveMessagesSender(BlockingQueue<JSONObject> activeMessagesQueue) {
        super();
        this.activeMessagesQueue = activeMessagesQueue;
    }

    public BlockingQueue<JSONObject> getActiveMessagesQueue() {
        return activeMessagesQueue;
    }

    public void setActiveMessagesQueue(
            BlockingQueue<JSONObject> activeMessagesQueue) {
        this.activeMessagesQueue = activeMessagesQueue;
    }

    @Override
    public void run() {
        while (true) {
            try {
                JSONObject message = activeMessagesQueue.take();
                log.info("Message: " + message.toString());
                String access_token = message.getString("access_token");
                message.remove("access_token");
                String request_url = GET_FOLLOWERS_OPENID_REQUEST_URL
                        + access_token;
                JSONObject jo = WeChatHttpsUtil.httpsRequest(request_url,
                        "GET", null);
                int total = jo.getInt("total");
                int counter = jo.getInt("count");
                JSONObject data = jo.getJSONObject("data");
                JSONArray openIDs = data.getJSONArray("openid");
                for (Object openID : openIDs) {
                    message.put("touser", openID);
                    sendMessage(message.toString(), access_token);
                }
                if (counter < total) {
                    sendToNext(jo.getString("next_openid"), access_token,
                            message, counter);
                }

            } catch (Exception e) {
                log.error("Send active messages error: " + e.toString());
                e.printStackTrace();
            }
        }

    }

    private void sendMessage(String msg, String token) {
        String request = SEND_MESSAGE_REQUEST_URL + token;
        JSONObject jo = WeChatHttpsUtil.httpsRequest(request, "POST", msg);
        log.info("sendMessage ret: " + jo.toString());
    }

    private void sendToNext(String next_openid, String token,
            JSONObject message, int counter) {
        String request_url = GET_FOLLOWERS_OPENID_REQUEST_URL + token
                + "&next_openid=" + next_openid;
        JSONObject jo = WeChatHttpsUtil.httpsRequest(request_url, "GET", null);
        JSONObject data = jo.getJSONObject("data");
        JSONArray openIDs = data.getJSONArray("openid");
        for (Object openID : openIDs) {
            message.put("touser", openID);
            sendMessage(message.toString(), token);
        }
        counter += jo.getInt("count");
        if (counter < jo.getInt("total")) {
            sendToNext(jo.getString("next_openid"), token, message, counter);
        }
    }
}
