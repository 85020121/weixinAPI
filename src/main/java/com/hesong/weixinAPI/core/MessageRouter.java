package com.hesong.weixinAPI.core;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.hesong.sugarCRM.SugarCRMCaller;
import com.hesong.weixinAPI.context.ContextPreloader;
import com.hesong.weixinAPI.model.StaffSessionInfo;
import com.hesong.weixinAPI.tools.API;
import com.hesong.weixinAPI.tools.WeChatHttpsUtil;

public class MessageRouter implements Runnable {

    private static Logger log = Logger.getLogger(MessageRouter.class);
    
    private static String USER_INFO_URL = "https://api.weixin.qq.com/cgi-bin/user/info?access_token=ACCESS_TOKEN&openid=OPENID&lang=zh_CN";

    private BlockingQueue<Map<String, String>> messageQueue;
    private BlockingQueue<Map<String, String>> messageToSendQueue;
    
    public static Map<String, StaffSessionInfo> activeStaffMap = new HashMap<String, StaffSessionInfo>();
    
    public MessageRouter(BlockingQueue<Map<String, String>> messageQueue,
            BlockingQueue<Map<String, String>> messageToSendQueue) {
        super();
        this.messageQueue = messageQueue;
        this.messageToSendQueue = messageToSendQueue;
    }

    public BlockingQueue<Map<String, String>> getMessageQueue() {
        return messageQueue;
    }

    public void setMessageQueue(BlockingQueue<Map<String, String>> messageQueue) {
        this.messageQueue = messageQueue;
    }

    public BlockingQueue<Map<String, String>> getMessageToSendQueue() {
        return messageToSendQueue;
    }

    public void setMessageToSendQueue(
            BlockingQueue<Map<String, String>> messageToSendQueue) {
        this.messageToSendQueue = messageToSendQueue;
    }

    @Override
    public void run() {

        while (true) {

            try {
                Map<String, String> message = getMessageQueue().take();
                JSONObject jo = new JSONObject();
                String toToken;
                if (message.get(API.MESSAGE_TO_TAG).equals(ContextPreloader.clientAccount.getAccount())) {
                    // received message from client side
                    ContextPreloader.clientAccount.setToUser(message.get(API.MESSAGE_FROM_TAG));
                    jo.put("touser", ContextPreloader.serviceAccount.getToUser());
                    toToken = ContextPreloader.Account_Map.get(ContextPreloader.serviceAccount.getAccount()).getToken();
                    //messageToSend.put("access_token", ContextPreloader.Account_Map.get(ContextPreloader.serviceAccount.getAccount()).getToken());
                } else {
                    //ContextPreloader.clientAccount.setToUser(message.get(API.MESSAGE_FROM_TAG));
                    jo.put("touser", ContextPreloader.clientAccount.getToUser());
                    toToken = ContextPreloader.Account_Map.get(ContextPreloader.clientAccount.getAccount()).getToken();
                    //messageToSend.put("access_token", ContextPreloader.Account_Map.get(ContextPreloader.clientAccount.getAccount()).getToken());
                }
                
                switch (message.get(API.MESSAGE_TYPE_TAG)) {
                case API.TEXT_MESSAGE:
                    jo.put("msgtype", API.TEXT_MESSAGE);
                    JSONObject text = new JSONObject();
                    text.put("content", message.get(API.MESSAGE_CONTENT_TAG));
                    jo.put(API.TEXT_MESSAGE, text);
                    break;
                case API.IMAGE_MESSAGE:
                    String img_account = message.get(API.MESSAGE_TO_TAG);
                    String img_Token = ContextPreloader.Account_Map.get(img_account).getToken();
                    String img_id = getMediaId(message.get(API.MESSAGE_MEDIA_ID_TAG), img_Token, toToken, "image", "image/*");
                    if (img_id.equals("error")) {
                        toToken = img_Token;
                        jo.put("touser", message.get(API.MESSAGE_FROM_TAG));
                        jo.put("msgtype", API.TEXT_MESSAGE);
                        JSONObject img_error = new JSONObject();
                        img_error.put("content", "系统消息:发送图片失败,请发送小于120KB的图片");
                        jo.put(API.TEXT_MESSAGE, img_error);
                        break;
                    }
                    jo.put("msgtype", API.IMAGE_MESSAGE);
                    JSONObject image = new JSONObject();
                    image.put("media_id", img_id);
                    jo.put(API.IMAGE_MESSAGE, image);
                    break;
                case API.VOICE_MESSAGE:
                    String voice_account = message.get(API.MESSAGE_TO_TAG);
                    String voice_Token = ContextPreloader.Account_Map.get(voice_account).getToken();
                    String voice_id = getMediaId(message.get(API.MESSAGE_MEDIA_ID_TAG), voice_Token, toToken, "voice", "audio/amr");
                    if (voice_id.equals("error")) {
                        toToken = voice_Token;
                        jo.put("touser", message.get(API.MESSAGE_FROM_TAG));
                        jo.put("msgtype", API.TEXT_MESSAGE);
                        JSONObject voice_error = new JSONObject();
                        voice_error.put("content", "系统消息:发送语音消息失败");
                        jo.put(API.TEXT_MESSAGE, voice_error);
                        break;
                    }
                    jo.put("msgtype", API.VOICE_MESSAGE);
                    JSONObject voice = new JSONObject();
                    voice.put("media_id", voice_id);
                    jo.put(API.VOICE_MESSAGE, voice);
                    break;
                case API.EVENT_MESSAGE:
                    
                    String event = message.get(API.MESSAGE_EVENT_TAG);
                    if (event.equals("subscribe")) {
                        if (message.get(API.MESSAGE_TO_TAG).equals(ContextPreloader.serviceAccount.getAccount())) {
                            toToken = ContextPreloader.Account_Map.get(ContextPreloader.serviceAccount.getAccount()).getToken();
                            jo.put("msgtype", API.TEXT_MESSAGE);
                            jo.put("touser", ContextPreloader.serviceAccount.getToUser());
                            JSONObject welcome = new JSONObject();
                            welcome.put("content", "欢迎使用和声云，<a href=\"https://open.weixin.qq.com/connect/oauth2/authorize?appid=wx735e58e85eb3614a&redirect_uri=http://www.clouduc.cn/crm/mobile/auth/index.php&response_type=code&scope=snsapi_base&state=123#wechat_redirect\">点击绑定</a>获得CRM账户[微笑]");
                            jo.put(API.TEXT_MESSAGE, welcome);
                        } else {
                            toToken = ContextPreloader.Account_Map.get(ContextPreloader.clientAccount.getAccount()).getToken();
                            String openid = ContextPreloader.clientAccount.getToUser();
                            String url = USER_INFO_URL.replace("ACCESS_TOKEN", toToken).replace("OPENID", openid);
                            JSONObject user_info = WeChatHttpsUtil.httpsRequest(url, "GET", null);
                            if (user_info.containsKey("errcode")) {
                                log.error("Get client info failed: "+user_info.toString());
                            } else {
                                log.info("Client info: " + user_info.toString());
                                SugarCRMCaller crmCaller = new SugarCRMCaller();
                                String session = crmCaller.login("admin",
                                        "p@ssw0rd");
                                if (!crmCaller.isOpenidBinded(session, openid)) {
                                    String insert_recall = crmCaller
                                            .insertToCRM_Prospects(session,
                                                    user_info.toString());
                                    log.info("insert_recall: " + insert_recall);
                                }
                            }
                            jo.put("msgtype", API.TEXT_MESSAGE);
                            jo.put("touser", openid);
                            JSONObject welcome = new JSONObject();
                            welcome.put("content", "欢迎使用和声云[微笑]");
                            jo.put(API.TEXT_MESSAGE, welcome);
                        }
                    } else if (event.equalsIgnoreCase("click")) {
                        String event_key = message.get(API.MESSAGE_EVENT_KEY_TAG);
                        if (event_key.equals("CLIENT_INFO")) {
                            toToken = ContextPreloader.Account_Map.get(ContextPreloader.serviceAccount.getAccount()).getToken();
                            jo.put("msgtype", API.TEXT_MESSAGE);
                            jo.put("touser", ContextPreloader.serviceAccount.getToUser());
                            JSONObject user_info = new JSONObject();
                            String url = "<a href=\"http://www.clouduc.cn/crm/mobile/weixin/prospectsDetail.php?openid="+ContextPreloader.clientAccount.getToUser() + "\" >点击查看用户信息</a>";
                            user_info.put("content", url);
                            jo.put(API.TEXT_MESSAGE, user_info);
                        } 
                        // Check in
                        else if (event_key.equals("CHECK_IN")) {
                            String account = message.get(API.MESSAGE_TO_TAG);
                            String openid = message.get(API.MESSAGE_FROM_TAG);
                            StaffSessionInfo s = new StaffSessionInfo(account, openid);
                            activeStaffMap.put(openid, s);
                            log.info("Add staff to list: "+s.toString());
                        } 
                        // Check out
                        else if (event_key.equals("CHECK_OUT")) {
                            activeStaffMap.remove(message.get(API.MESSAGE_FROM_TAG));
                            log.info("Remove staff id="+message.get(API.MESSAGE_FROM_TAG)+" from list.");
                        } 
                        // Staff Service
                        else if (event_key.equals("STAFF_SERVICE")) {
                            for (StaffSessionInfo s : activeStaffMap.values()) {
                                JSONObject request = new JSONObject();
                                request.put("msgtype", API.TEXT_MESSAGE);
                                jo.put("touser", s.getOpenid());
                                JSONObject content = new JSONObject();
                                String url = "";
                                content.put("content", url);
                                String sToken = ContextPreloader.Account_Map.get(s.getAccount()).getToken();
                            }
                        } 
                        else{
                            return;
                        }
                    } else {
                        return;
                    }
                    
                    break;
                default:
                    break;
                }
                
                Map<String, String> messageToSend = new HashMap<String,String>();
                messageToSend.put("access_token", toToken);
                log.info("Message: " + jo.toString());
                
                messageToSend.put("message", jo.toString());
                getMessageToSendQueue().put(messageToSend);
                
            } catch (Exception e) {
                log.error("Exception: " + e.toString());
                 e.printStackTrace();
            }
        }

    }
    
    public String getMediaId(String origMediaId, String origToken, String token, String media_type, String content_type){
        String pull_url = API.PULLING_MEDIA_URL.replace("ACCESS_TOKEN", origToken) + origMediaId;
        InputStream input = WeChatHttpsUtil.httpGetInputStream(pull_url, content_type);
        String post_url = API.UPLOAD_IMAGE_REQUEST_URL.replace("ACCESS_TOKEN", token) + media_type;
        JSONObject ret;
        if (media_type.equals(API.IMAGE_MESSAGE)) {
            ret = WeChatHttpsUtil.httpPostFile(post_url, input, UUID.randomUUID().toString()+".jpg");
        } else {
            ret = WeChatHttpsUtil.httpPostFile(post_url, input, UUID.randomUUID().toString()+".amr");
        }
        log.info("Upload return: "+ret.toString());
        if (ret.containsKey("media_id")) {
            return ret.getString("media_id");
        } 
        log.error("Transport media message failed: "+ret.toString());
        return "error";
            
    }
}
