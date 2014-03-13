package com.hesong.weChatAdapter.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import net.sf.json.JSONObject;

import com.hesong.smartbus.client.PackInfo;
import com.hesong.smartbus.client.net.Client.SendDataError;
import com.hesong.smartbus.client.net.JniWrapper;
import com.hesong.weChatAdapter.context.ContextPreloader;
import com.hesong.weChatAdapter.runner.SmartbusExecutor;
import com.hesong.weChatAdapter.tools.API;
import com.hesong.weChatAdapter.tools.WeChatHttpsUtil;

public class MessageManager {
    private static Logger log = Logger.getLogger(MessageManager.class);
    
    private static String ACCESS_TOKEN_TAG = "ACCESS_TOKEN";
    private static String OPENID_TAG = "OPENID";

    private static String GET = "GET";
    private static String POST = "POST";
    
    private static String SEND_MESSAGE_REQUEST_URL = "https://api.weixin.qq.com/cgi-bin/message/custom/send?access_token=";
    private static String MANAGE_MENU_REQUEST_URL = "https://api.weixin.qq.com/cgi-bin/menu/ACTION?access_token=";
    private static String GET_FOLLOWERS_OPENID_REQUEST_URL = "https://api.weixin.qq.com/cgi-bin/user/get?access_token=";
    private static String GET_FOLLOWERS_FROM_REQUEST_URL = "https://api.weixin.qq.com/cgi-bin/user/get?access_token=ACCESS_TOKEN&next_openid=NEXT_OPENID";
    private static String GET_CLIENT_INFO_REQUEST_URL = "https://api.weixin.qq.com/cgi-bin/user/info?access_token=ACCESS_TOKEN&openid=OPENID&lang=zh_CN";

    public static String getResponseMessage(Map<String, String> message) {
        log.info("MsgType = " + message.get(API.MESSAGE_TYPE_TAG));
        if (message.get(API.MESSAGE_TYPE_TAG) != null) {
            return messageRouter(message);
        }
        log.info("MsgType is null");
        return "";
    }

    public static String messageRouter(Map<String, String> message) {
        JSONObject jo = new JSONObject();
        log.info("Message received: "+message.toString());
        jo.put("jsonrpc", "2.0");
        jo.put("method", "imsm.ImMessageReceived");
        jo.put("id", UUID.randomUUID().toString());
        Map<String, Object> paramsList = new HashMap<String, Object>();
        paramsList.put("imtype", "weixin");
        paramsList.put("account", message.get(API.MESSAGE_TO_TAG));

        JSONObject user = new JSONObject();
        user.put("user", message.get(API.MESSAGE_FROM_TAG));
        user.put("usertype", API.REAL_WEIXIN_CLIENT);

        paramsList.put("user", user);
        paramsList.put("room", null);
        paramsList.put("msgcontent", message);

        switch (message.get(API.MESSAGE_TYPE_TAG)) {
        case API.TEXT_MESSAGE:
            paramsList.put("msgtype", API.TEXT_MESSAGE);
            break;
        case API.IMAGE_MESSAGE:
            paramsList.put("msgtype", API.IMAGE_MESSAGE);
            break;
        case API.EVENT_MESSAGE:
            if (message.get(API.MESSAGE_EVENT_KEY_TAG) != null) {
                paramsList.put(
                        "msgtype",
                        "event.CLICK."
                                + message.get(API.MESSAGE_EVENT_KEY_TAG));
            } else {
                paramsList.put("msgtype",
                        "event." + message.get(API.MESSAGE_EVENT_TAG));
            }
            break;
        default:
            break;
        }
//        JSONObject tmp = new JSONObject();
//        tmp.put("content", message.get("Content"));
//        WeChatHttpsUtil.httpPostRequest("http://localhost:8080/weChatAdapter/chat/sendMessageQuest", tmp.toString(), 2000);
        jo.put("params", paramsList);
        PackInfo pack = new PackInfo((byte)ContextPreloader.destUnitId, (byte)ContextPreloader.destClientId, (byte)ContextPreloader.srcUnitId, (byte)ContextPreloader.srctClientId, jo.toString());
        try {
            SmartbusExecutor.responseQueue.put(pack);
        } catch (InterruptedException e) {
            log.error("Puc packinfo into response queue failed: " + e.toString());
        }

//        smartbusSendMessage(jo.toString());
        return "";
    }

    public static JSONObject sendMessage(String msg, String token) {
        String request = SEND_MESSAGE_REQUEST_URL + token;
        log.info("sendMessage: " + msg);
        JSONObject jo = WeChatHttpsUtil.httpsRequest(request, POST, msg);
        log.info("Send message ret: " + jo.toString());
        return jo;
    }
    
    public static JSONObject getClientInfo(String accessToken, String openid){
        String request = (GET_CLIENT_INFO_REQUEST_URL).replace(ACCESS_TOKEN_TAG, accessToken).replace(OPENID_TAG, openid);
        JSONObject jo = WeChatHttpsUtil.httpsRequest(request, GET, null);
        log.info("Client info: "+jo.toString());
        return jo;
    }

    public static JSONObject manageMenu(String accessToken, String action,
            String jsonMenu) {
        String request = (MANAGE_MENU_REQUEST_URL+accessToken).replace("ACTION", action);
    
        log.info("Menu request: " + request);
        JSONObject jObject;
        if (action.equals("create")) {
            jObject = WeChatHttpsUtil.httpsRequest(request, POST,
                    jsonMenu);
        } else {
            jObject = WeChatHttpsUtil.httpsRequest(request, GET, null);
        }
        log.info("Result: " + jObject.toString());
        return jObject;
    }

    public static JSONObject getFollowersList(String access_token) {
        String request = GET_FOLLOWERS_OPENID_REQUEST_URL + access_token;
        JSONObject jo = WeChatHttpsUtil.httpsRequest(request, GET, null);
        log.info("Result: " + jo.toString());
        // ObjectMapper objectMapper = new ObjectMapper();
        // FollowersList followers;
        // try {
        // followers = objectMapper.readValue(jo.toString(),
        // FollowersList.class);
        // } catch (IOException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        return jo;
    }

    public static String getFollowersFrom(String access_token, String openid) {
        String request = GET_FOLLOWERS_FROM_REQUEST_URL.replace("ACCESS_TOKEN",
                access_token).replace("NEXT_OPENID", openid);
        JSONObject jo = WeChatHttpsUtil.httpsRequest(request, GET, null);
        log.info("Result: " + jo.toString());
        // ObjectMapper objectMapper = new ObjectMapper();
        // FollowersList followers;
        // try {
        // followers = objectMapper.readValue(jo.toString(),
        // FollowersList.class);
        // followers.getCount();
        // } catch (IOException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        return jo.toString();
    }

//    private static void smartbusSendMessage(String msg) {
//        try {
//            JniWrapper.CLIENT.sendText((byte) 0, (byte) 2, ContextPreloader.destUnitId, ContextPreloader.destClientId, 11, msg);
//        } catch (SendDataError e) {
//            log.error("Smartbus send message error: " + e.toString());
//        }
//    }
    
}
