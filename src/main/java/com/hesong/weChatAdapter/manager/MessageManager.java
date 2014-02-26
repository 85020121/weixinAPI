package com.hesong.weChatAdapter.manager;

import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;
import net.sf.json.JSONObject;

import com.hesong.weChatAdapter.message.response.RespTextMessage;
import com.hesong.weChatAdapter.model.AccessToken;
import com.hesong.weChatAdapter.tools.API;
import com.hesong.weChatAdapter.tools.WeChatHttpsUtil;
import com.hesong.weChatAdapter.tools.WeChatXMLParser;

public class MessageManager {
    private static Logger log = Logger.getLogger(MessageManager.class);

    private static String SEND_MESSAGE_REQUEST_URL = "https://api.weixin.qq.com/cgi-bin/message/custom/send?access_token=ACCESS_TOKEN";

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
            
            RespTextMessage msg = new RespTextMessage();
            msg.setContent("Response Test");
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

    public static String processEventMessage(Map<String, String> message){
        log.info("Response event message");
        try {
            log.info("From = "+message.get(API.MESSAGE_FROM_TAG));
            log.info("To = "+message.get(API.MESSAGE_TO_TAG));
            log.info("Message type = "+message.get(API.MESSAGE_TYPE_TAG));
            log.info("Create time = "+message.get(API.MESSAGE_CREATE_TIME_TAG));
            log.info("Event = "+message.get(API.MESSAGE_EVENT_TAG));
            
            RespTextMessage msg = new RespTextMessage();
            if (message.get(API.MESSAGE_EVENT_TAG).equals(API.SUBSCRIBE_EVENT)) {
                msg.setContent("Welcome!");
            } else if (message.get(API.MESSAGE_EVENT_TAG).equals(API.UNSUBSCRIBE_EVENT)) {
                // TODO: handle unsubscribe event
                return "";
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

    public static boolean sendMessage(Object msg) {
        String jsonMsg = JSONObject.fromObject(msg).toString();

        AccessToken token = WeChatHttpsUtil.getAccessToken(API.APPID,
                API.APP_SECRET);
        String request = SEND_MESSAGE_REQUEST_URL.replace("ACCESS_TOKEN",
                token.getToken());

        JSONObject jo = WeChatHttpsUtil.httpsRequest(request, "POST", jsonMsg);

        if (jo != null) {
            if (0 != jo.getInt("errcode")) {
                log.error("Get token failed, errorcode:{"
                        + jo.getInt("errcode") + "} errormsg:{"
                        + jo.getString("errmsg") + "}");
                return true;
            } else {
                log.info(jo.toString());
            }

        }
        return false;
    }
}
