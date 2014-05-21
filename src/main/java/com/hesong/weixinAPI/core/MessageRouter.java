package com.hesong.weixinAPI.core;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.hesong.sugarCRM.SugarCRMCaller;
import com.hesong.weixinAPI.context.ContextPreloader;
import com.hesong.weixinAPI.job.CheckSessionAvailableJob;
import com.hesong.weixinAPI.model.StaffSessionInfo;
import com.hesong.weixinAPI.tools.API;
import com.hesong.weixinAPI.tools.WeChatHttpsUtil;

public class MessageRouter implements Runnable {

    private static Logger log = Logger.getLogger(MessageRouter.class);
    
    private static String USER_INFO_URL = "https://api.weixin.qq.com/cgi-bin/user/info?access_token=ACCESS_TOKEN&openid=OPENID&lang=zh_CN";

    private BlockingQueue<Map<String, String>> messageQueue;
    private BlockingQueue<JSONObject> messageToSendQueue;
    
    public static Map<String, StaffSessionInfo> activeStaffMap = new HashMap<String, StaffSessionInfo>();
    public static Queue<String> waitingList = new LinkedList<String>();

    @Override
    public void run() {

        while (true) {
            try {
                Map<String, String> message = getMessageQueue().take();
                log.info("Received message: " + message.toString());
                switch (message.get(API.MESSAGE_TYPE_TAG)) {
                case API.TEXT_MESSAGE:
                    textMessage(message);
                    break;
                case API.IMAGE_MESSAGE:
                    imageMessage(message);
                    break;
                case API.VOICE_MESSAGE:
                    voiceMessage(message);
                    break;
                case API.EVENT_MESSAGE:
                    String event = message.get(API.MESSAGE_EVENT_TAG);
                    if (event.equals("subscribe")) {
                        subscribe(message);
                    } else if (event.equalsIgnoreCase("click")) {
                        switch (message.get(API.MESSAGE_EVENT_KEY_TAG)) {
                        case "CLIENT_INFO":  // 获取用户详情
                            clientInfo(message);
                            break;
                        case "CHECK_OUT":    // 坐席登出
                            checkout(message);
                            break;
                        case "STAFF_SERVICE":  // 人工服务
                            staffService(message);
                            break;
                        case "END_SESSION":    // 结束对话
                            endSession(message);
                            break;
                        default:
                            break;
                        }
                    }
                    break;
                default:
                    break;
                }
                
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
    
    private void textMessage(Map<String, String> message) {
        String to_account = message.get(API.MESSAGE_TO_TAG);
        String from_openid = message.get(API.MESSAGE_FROM_TAG);
        if (to_account.equals(ContextPreloader.clientAccount.getAccount())) {
            // Message from client
            if (CheckSessionAvailableJob.clientMap.containsKey(from_openid)) {
                StaffSessionInfo s = CheckSessionAvailableJob.sessionMap.get(from_openid);
                if (s != null && s.isBusy()) {
                    String sToken = ContextPreloader.Account_Map.get(s.getAccount()).getToken();
                    String content = String.format("%s: %s", s.getClient_name(), message.get(API.MESSAGE_CONTENT_TAG));
                    sendMessage(s.getOpenid(), sToken, content, API.TEXT_MESSAGE);
                    s.setLastReceived(new Date());
                }
                return;
            }
        } else {
            // Message from staff
            StaffSessionInfo s = activeStaffMap.get(from_openid);
            if (s != null && s.isBusy()) {
                String cToken = ContextPreloader.Account_Map.get(s.getClient_account()).getToken();
                String content = String.format("客服%s: %s", s.getStaffid(), message.get(API.MESSAGE_CONTENT_TAG));
                sendMessage(s.getClient_openid(), cToken, content, API.TEXT_MESSAGE);
                s.setLastReceived(new Date());
                return;
            }
        }
        log.info("This is not an available session.");
    }
    
    private void imageMessage(Map<String, String> message) {
        String img_account = message.get(API.MESSAGE_TO_TAG);
        String img_Token = ContextPreloader.Account_Map.get(img_account).getToken();
        String img_openid = message.get(API.MESSAGE_FROM_TAG);
        String img_toToken = "";
        String img_to_openid = "";
        if (img_account.equals(ContextPreloader.clientAccount.getAccount())) {
            // Message from client
            if (CheckSessionAvailableJob.clientMap.containsKey(img_openid)) {
                StaffSessionInfo s = CheckSessionAvailableJob.sessionMap.get(img_openid);
                img_toToken = ContextPreloader.Account_Map.get(s.getAccount()).getToken();
                img_to_openid = s.getOpenid();
                s.setLastReceived(new Date());
            }
        } else {
            // Message from staff
            StaffSessionInfo s = activeStaffMap.get(img_openid);
            if (s != null && s.isBusy()) {
                img_toToken = ContextPreloader.Account_Map.get(s.getClient_account()).getToken();
                img_to_openid = s.getClient_openid();
                s.setLastReceived(new Date());
            }
        }
        String img_id = getMediaId(message.get(API.MESSAGE_MEDIA_ID_TAG), img_Token, img_toToken, "image", "image/*");
        if (img_id.equals("error")) {
            sendMessage(img_openid, img_Token, "系统消息:发送图片失败,请发送小于120KB的图片", API.TEXT_MESSAGE);
            return;
        }
        sendMessage(img_to_openid, img_toToken, img_id, API.IMAGE_MESSAGE);
    }
    
    private void voiceMessage(Map<String, String> message) {
        String voice_account = message.get(API.MESSAGE_TO_TAG);
        String voice_openid = message.get(API.MESSAGE_FROM_TAG);
        String voice_Token = ContextPreloader.Account_Map.get(voice_account).getToken();
        String voice_toToken = "";
        String voice_to_openid = "";
        if (voice_account.equals(ContextPreloader.clientAccount.getAccount())) {
            // Message from client
            if (CheckSessionAvailableJob.clientMap.containsKey(voice_openid)) {
                StaffSessionInfo s = CheckSessionAvailableJob.sessionMap.get(voice_openid);
                voice_toToken = ContextPreloader.Account_Map.get(s.getAccount()).getToken();
                voice_to_openid = s.getOpenid();
                s.setLastReceived(new Date());
            }
        } else {
            // Message from staff
            StaffSessionInfo s = activeStaffMap.get(voice_openid);
            if (s != null && s.isBusy()) {
                voice_toToken = ContextPreloader.Account_Map.get(s.getClient_account()).getToken();
                voice_to_openid = s.getClient_openid();
                s.setLastReceived(new Date());
            }
        }
        String voice_id = getMediaId(message.get(API.MESSAGE_MEDIA_ID_TAG), voice_Token, voice_toToken, "voice", "audio/amr");
        if (voice_id.equals("error")) {
            sendMessage(voice_openid, voice_Token, "系统消息:发送语音消息失败", API.TEXT_MESSAGE);
            return;
        }
        sendMessage(voice_to_openid, voice_toToken, voice_id, API.VOICE_MESSAGE);
    }
    
    private void subscribe(Map<String, String> message) throws InterruptedException {
        String account = message.get(API.MESSAGE_TO_TAG);
        if (account.equals(ContextPreloader.serviceAccount.getAccount())) {
            String toToken = ContextPreloader.Account_Map.get(account).getToken();
            String openid =  message.get(API.MESSAGE_FROM_TAG);
            String content = "欢迎使用和声云，<a href=\"https://open.weixin.qq.com/connect/oauth2/authorize?appid=wx735e58e85eb3614a&redirect_uri=http://www.clouduc.cn/crm/mobile/auth/index.php&response_type=code&scope=snsapi_base&state=123#wechat_redirect\">点击绑定</a>获得CRM账户[微笑]";
            sendMessage(openid, toToken, content, API.TEXT_MESSAGE);
        } else {
            String toToken = ContextPreloader.Account_Map.get(account).getToken();
            String openid = message.get(API.MESSAGE_FROM_TAG);
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
            
            // Welcom news
            JSONObject news = new JSONObject();
            news.put("msgtype", "news");
            news.put("touser", openid);
            JSONArray articles = new JSONArray();
            
            JSONObject article_1 = new JSONObject();
            article_1.put("title", "和声云端服务");
            article_1.put("url", "http://hesong.net/");
            article_1.put("picurl", "http://hesong.net/images/banner2.jpg");
            
            JSONObject article_2 = new JSONObject();
            article_2.put("title", "活动于指尖");
            article_2.put("url", "http://hesong.net/");
            article_2.put("picurl", "http://hesong.net/images/tu4.jpg");
            
            articles.add(article_1);
            articles.add(article_2);
            
            JSONObject container = new JSONObject();
            container.put("articles", articles);
            
            news.put("news", container);
            news.put("access_token", toToken);
            
            getMessageToSendQueue().put(news);
        }
    }
    
    private void clientInfo(Map<String, String> message) {
        String openid = message.get(API.MESSAGE_FROM_TAG);
        String toToken = ContextPreloader.Account_Map.get(
                message.get(API.MESSAGE_TO_TAG)).getToken();
        StaffSessionInfo s = activeStaffMap.get(openid);
        if (s != null) {
            String url = "<a href=\"http://www.clouduc.cn/crm/mobile/weixin/prospectsDetail.php?openid="
                    + s.getClient_openid() + "\" >点击查看用户信息</a>";
            sendMessage(openid, toToken, url, API.TEXT_MESSAGE);
        }
    }
    
    private void checkout(Map<String, String> message) {
        StaffSessionInfo s = activeStaffMap.get(message.get(API.MESSAGE_FROM_TAG));
        if (s != null) {
            String content = "登出成功";
            String sToken = ContextPreloader.Account_Map.get(s.getAccount()).getToken();
            sendMessage(s.getOpenid(), sToken, content, API.TEXT_MESSAGE);
            activeStaffMap.remove(message.get(API.MESSAGE_FROM_TAG));
            log.info("Staff id="+message.get(API.MESSAGE_FROM_TAG)+" checked out.");
        }
    }
    
    private void staffService(Map<String, String> message) {
        String client_openid = message.get(API.MESSAGE_FROM_TAG);
        String client_account = message.get(API.MESSAGE_TO_TAG);
        String cToken = ContextPreloader.Account_Map.get(client_account).getToken();
        
        String query = USER_INFO_URL.replace("ACCESS_TOKEN", cToken).replace("OPENID", client_openid);
        JSONObject user_info = WeChatHttpsUtil.httpsRequest(query, "GET", null);
        String client_name = "";
        if (user_info.containsKey("errcode")) {
            log.error("Get client info failed: "+user_info.toString());
            client_name = "匿名客户";
        } else {
            client_name = user_info.getString("nickname");
        }
        
        if (CheckSessionAvailableJob.clientMap.containsKey(client_openid)) {
            sendMessage(client_openid, cToken, "您已经接通人工服务!", API.TEXT_MESSAGE);
            return;
        }
        String content = "正在为您接通人工服务,请稍等...";
        if (activeStaffMap.size() == 0) {
            content = "暂时没有空闲客服,请稍后再试!";
        }
        sendMessage(client_openid, cToken, content, API.TEXT_MESSAGE);
        // Broadcast client request to all staffs
        for (StaffSessionInfo s : activeStaffMap.values()) {
            if (!s.isBusy()) {
                String url = String.format("客户:%s,寻求人工对话服务,<a href=\"http://www.clouduc.cn/wx/staff/takeSession?clientid=%s&account=%s&clientname=%s&staffid=%s\">点击抢单接入会话</a>", client_name, client_openid,client_account, client_name, s.getOpenid());
                String sToken = ContextPreloader.Account_Map.get(s.getAccount()).getToken();
                sendMessage(s.getOpenid(), sToken, url, API.TEXT_MESSAGE);
            }
        }
    }
    
    private void endSession(Map<String, String> message) {
        StaffSessionInfo s = activeStaffMap.get(message.get(API.MESSAGE_FROM_TAG));
        if (s != null) {
            s.setBusy(false);
            s.setClient_account("");
            s.setClient_name("");
            s.setClient_openid("");
            String content = "系统:会话已结束";
            String sToken = ContextPreloader.Account_Map.get(s.getAccount()).getToken();
            // To staff
            sendMessage(s.getOpenid(), sToken, content, API.TEXT_MESSAGE);
            
            // To client
            String cToken = ContextPreloader.Account_Map.get(s.getClient_account()).getToken();
            sendMessage(s.getClient_openid(), cToken, content, API.TEXT_MESSAGE);
            log.info("Session ended.");
            
            CheckSessionAvailableJob.clientMap.remove(s.getClient_openid());
            CheckSessionAvailableJob.sessionMap.remove(s.getClient_openid());
        }
    }
    
    private void sendMessage(String openid, String token, String text, String type) {
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
            getMessageToSendQueue().put(message);
        } catch (InterruptedException e) {
            log.error("Put message to queue error: "+e.toString());
        }
    }
    
    
    public MessageRouter(BlockingQueue<Map<String, String>> messageQueue,
            BlockingQueue<JSONObject> messageToSendQueue) {
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

    public BlockingQueue<JSONObject> getMessageToSendQueue() {
        return messageToSendQueue;
    }

    public void setMessageToSendQueue(
            BlockingQueue<JSONObject> messageToSendQueue) {
        this.messageToSendQueue = messageToSendQueue;
    }
}
