package com.hesong.weChatAdapter.manager;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import net.sf.json.JSONObject;

import com.hesong.smartbus.client.net.JniWrapper;
import com.hesong.weChatAdapter.message.response.RespTextMessage;
import com.hesong.weChatAdapter.model.AccessToken;
import com.hesong.weChatAdapter.tools.API;
import com.hesong.weChatAdapter.tools.WeChatHttpsUtil;
import com.hesong.weChatAdapter.tools.WeChatXMLParser;

public class MessageManager {
    private static Logger log = Logger.getLogger(MessageManager.class);

    private static String SEND_MESSAGE_REQUEST_URL = "https://api.weixin.qq.com/cgi-bin/message/custom/send?access_token=";
    private static String CREATE_MENU_REQUEST_URL = "https://api.weixin.qq.com/cgi-bin/menu/create?access_token=";
    private static String GET_MENU_REQUEST_URL = "https://api.weixin.qq.com/cgi-bin/menu/get?access_token=";
    private static String DELETE_MENU_REQUEST_URL = "https://api.weixin.qq.com/cgi-bin/menu/delete?access_token=";
    private static String GET_FOLLOWERS_OPENID_REQUEST_URL = "https://api.weixin.qq.com/cgi-bin/user/get?access_token=";
    private static String GET_FOLLOWERS_FROM_REQUEST_URL = "https://api.weixin.qq.com/cgi-bin/user/get?access_token=ACCESS_TOKEN&next_openid=NEXT_OPENID";
    
    public static String getResponseMessage(Map<String, String> message) {
        log.info("MsgType = "+message.get(API.MESSAGE_TYPE_TAG));
        if (message.get(API.MESSAGE_TYPE_TAG) != null) {
            return messageRouter(message);
        }
        log.info("MsgType is null");
        return "";
    }

    public static String messageRouter(Map<String, String> message) {
        switch (message.get(API.MESSAGE_TYPE_TAG)) {
        case API.TEXT_MESSAGE:
            return processTextMessage(message);
        case API.IMAGE_MESSAGE:
            return processImageMessage(message);
        case API.EVENT_MESSAGE:
            return processEventMessage(message);
        default:
            break;
        }
        return "";
    }
    
    public static String processTextMessage(Map<String, String> message){
        log.info("Response text message");
        try {
            log.info("From = "+message.get(API.MESSAGE_FROM_TAG));
            log.info("To = "+message.get(API.MESSAGE_TO_TAG));
            log.info("Message type = "+message.get(API.MESSAGE_TYPE_TAG));
            log.info("Create time = "+message.get(API.MESSAGE_CREATE_TIME_TAG));
            log.info("Content = "+message.get(API.MESSAGE_CONTENT_TAG));
            log.info("Message ID = "+message.get(API.MESSAGE_ID_TAG));
            
            JSONObject jo = new JSONObject();
            jo.put("jsonrpc", "2.0");
            jo.put("method", "imsm.ImMessageReceived");
            jo.put("id", UUID.randomUUID().toString());
            Map<String,Object> list = new HashMap<String,Object>();
            list.put("imtype", "weixin");
            list.put("account", message.get(API.MESSAGE_TO_TAG));
            
            JSONObject user = new JSONObject();
            user.put("user", message.get(API.MESSAGE_FROM_TAG));
            user.put("usertype", 2);
            
            list.put("user", user);
            list.put("room", null);
            list.put("msgtype", "text");
            list.put("msgcontent", message);
            
            jo.put("params", list);
            log.info("JSON MESSAGE: "+jo.toString());
            
            
            JniWrapper.instances.get((byte)33).sendText((byte)0, (byte)2, 0, 14, 11, jo.toString());
//            RespTextMessage msg = new RespTextMessage();
//            
//            msg.setContent("Response Test");
//            msg.setCreateTime(new Date().getTime());
//            msg.setFromUserName(message.get(API.MESSAGE_TO_TAG));
//            msg.setMsgType(API.TEXT_MESSAGE);
//            msg.setToUserName(message.get(API.MESSAGE_FROM_TAG));
//            
//            WeChatXMLParser.xstream.alias("xml", msg.getClass());
//            String respXml = WeChatXMLParser.xstream.toXML(msg);
            
//            log.info("respXml: "+respXml);
            return "";
            
        } catch (Exception e) {
            e.printStackTrace();
            log.info("Exception: "+e.toString());
        }
        return "";
    }
    
    public static String processImageMessage(Map<String, String> message){
        log.info("Response image message");
        try {
            log.info("From = "+message.get(API.MESSAGE_FROM_TAG));
            log.info("To = "+message.get(API.MESSAGE_TO_TAG));
            log.info("Message type = "+message.get(API.MESSAGE_TYPE_TAG));
            log.info("Create time = "+message.get(API.MESSAGE_CREATE_TIME_TAG));
            log.info("PicUrl = "+message.get(API.MESSAGE_PIC_URL_TAG));
            log.info("MediaId = "+message.get(API.MESSAGE_MEDIA_ID_tAG));
            log.info("Message ID = "+message.get(API.MESSAGE_ID_TAG));
            
            return "";
            
        } catch (Exception e) {
            e.printStackTrace();
            log.info("Exception: "+e.toString());
        }
        return "";
    }

    public static String processEventMessage(Map<String, String> message){
        log.info("Response event message");
        try {
            log.info("From = "+message.get(API.MESSAGE_FROM_TAG));
            log.info("To = "+message.get(API.MESSAGE_TO_TAG));
            log.info("Message type = "+message.get(API.MESSAGE_TYPE_TAG));
            log.info("Create time = "+message.get(API.MESSAGE_CREATE_TIME_TAG));
            log.info("Event = "+message.get(API.MESSAGE_EVENT_TAG));
            
            RespTextMessage msg = new RespTextMessage();
            switch (message.get(API.MESSAGE_EVENT_TAG)) {
            case API.SUBSCRIBE_EVENT:
                msg.setContent("Welcome!");
                break;
            case API.UNSUBSCRIBE_EVENT:
                return "";
            case API.CLICK_EVENT:
                log.info("Event key is: "+message.get(API.MESSAGE_EVENT_KEY_TAG));
                msg.setContent("Event key is: "+message.get(API.MESSAGE_EVENT_KEY_TAG));
            default:
                break;
            }

            msg.setCreateTime(new Date().getTime());
            msg.setFromUserName(message.get(API.MESSAGE_TO_TAG));
            msg.setMsgType(API.TEXT_MESSAGE);
            msg.setToUserName(message.get(API.MESSAGE_FROM_TAG));
            
            WeChatXMLParser.xstream.alias("xml", msg.getClass());
            String respXml = WeChatXMLParser.xstream.toXML(msg);
            log.info("respXml: "+respXml);
            return respXml;
            
        } catch (Exception e) {
            e.printStackTrace();
            log.info("Exception: "+e.toString());
        }
        return "";
    }

    public static JSONObject sendMessage(String msg) {
       // String jsonMsg = JSONObject.fromObject(msg).toString();
        
        String request = SEND_MESSAGE_REQUEST_URL+API.ACCESS_TOKEN;
        log.info("sendMessage: "+msg);
        JSONObject jo = WeChatHttpsUtil.httpsRequest(request, "POST", msg);
        log.info("Send message ret: "+jo.toString());
        return jo;

    }

    public static void createMenu(String appID, String appSecret, String jsonMenu){
        String request = getRequestUrl(appID, appSecret, CREATE_MENU_REQUEST_URL);;
        
        log.info("Menu string: "+jsonMenu);
        JSONObject jObject = WeChatHttpsUtil.httpsRequest(request, "POST", jsonMenu);
        if (jObject != null) {
            if (jObject.getInt("errcode")!= 0) {
                log.error("Create menu failed, errorcode:{"
                        + jObject.getInt("errcode") + "} errormsg:{"
                        + jObject.getString("errmsg") + "}");
            }
        }
        log.info("Result: "+jObject.toString());
    }
    
    public static String getMenu(String appID, String appSecret){
        String request = getRequestUrl(appID, appSecret, GET_MENU_REQUEST_URL);
        JSONObject jo = WeChatHttpsUtil.httpsRequest(request, "GET", null);
        log.info("Result: "+jo.toString());
        return jo.toString();
    }
    
    public static String deleteMenu(String appID, String appSecret){
        String request = getRequestUrl(appID, appSecret, DELETE_MENU_REQUEST_URL);
        JSONObject jo = WeChatHttpsUtil.httpsRequest(request, "GET", null);
        log.info("Result: "+jo.toString());
        return jo.toString();
    }
    
    public static String getFollowersList(String access_token) {
        String request = GET_FOLLOWERS_OPENID_REQUEST_URL + access_token;
        JSONObject jo = WeChatHttpsUtil.httpsRequest(request, "GET", null);
        log.info("Result: " + jo.toString());
//        ObjectMapper objectMapper = new ObjectMapper();
//        FollowersList followers;
//        try {
//            followers = objectMapper.readValue(jo.toString(),
//                    FollowersList.class);
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
        return jo.toString();
    }

    public static String getFollowersFrom(String access_token, String openid){
        String request = GET_FOLLOWERS_FROM_REQUEST_URL.replace("ACCESS_TOKEN", access_token).replace("NEXT_OPENID", openid);
        JSONObject jo = WeChatHttpsUtil.httpsRequest(request, "GET", null);
        log.info("Result: "+jo.toString());
//        ObjectMapper objectMapper = new ObjectMapper();
//        FollowersList followers;
//        try {
//            followers = objectMapper.readValue(jo.toString(), FollowersList.class);
//            followers.getCount();
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
        return jo.toString();
    }
    
//    public static String makeClient(byte unitId, byte clientId, String host, short port){
//        Client.initialize(unitId);
//
//        Client client = new Client(clientId, (long) 11, host, port,
//                "WeChat client");
//        client.setCallbacks(new WeChatCallback());
//
//        log.info("Connect...");
//
//        try {
//            client.connect();
//            Thread.sleep(2000);
//            client.sendText((byte) 0, (byte) 211, 28, 25, 11,
//                    "{\"method\":\"Echo\",\"params\":[\"Hello world\"]}");
//            return "success";
//        } catch (ConnectError | InterruptedException | SendDataError e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//            return "failed";
//        }
//    }
    
    private static String getRequestUrl(String appID, String appSecret, String url){
        AccessToken token = WeChatHttpsUtil.getAccessToken(appID, appSecret);
        return url+token.getToken();
    }
}
