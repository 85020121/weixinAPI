package com.hesong.weixinAPI.core;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.log4j.Logger;

import redis.clients.jedis.Jedis;

import com.hesong.sugarCRM.HttpClientUtil;
import com.hesong.weixinAPI.context.AppContext;
import com.hesong.weixinAPI.context.ContextPreloader;
import com.hesong.weixinAPI.controller.WebchatController;
import com.hesong.weixinAPI.job.CheckEndSessionJob;
import com.hesong.weixinAPI.job.CheckLeavingMessageJob;
import com.hesong.weixinAPI.job.CheckSessionAvailableJob;
import com.hesong.weixinAPI.job.CheckWeiboSessionAvailableJob;
import com.hesong.weixinAPI.model.LeavingMessageClient;
import com.hesong.weixinAPI.model.Staff;
import com.hesong.weixinAPI.model.StaffSessionInfo;
import com.hesong.weixinAPI.model.WaitingClient;
import com.hesong.weixinAPI.mq.MQEvent;
import com.hesong.weixinAPI.mq.MQManager;
import com.hesong.weixinAPI.mq.events.MQClientSubscribeEvent;
import com.hesong.weixinAPI.mq.events.MQStaffSubscribeEvent;
import com.hesong.weixinAPI.mq.events.MQStaffUnsubscribeEvent;
import com.hesong.weixinAPI.tools.API;
import com.hesong.weixinAPI.tools.WeChatHttpsUtil;

public class MessageRouter implements Runnable {

    private static Logger log = Logger.getLogger(MessageRouter.class);

    private static final String SKILL_PREFIX = "SKILL_SERVICE_";
    
    private static String USER_INFO_URL = "https://api.weixin.qq.com/cgi-bin/user/info?access_token=ACCESS_TOKEN&openid=OPENID&lang=zh_CN";
    private static String QRCODE_URL = "https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=";

    
    private BlockingQueue<Map<String, String>> messageQueue;
    private BlockingQueue<JSONObject> messageToSendQueue;
    public static BlockingQueue<JSONObject> suaRequestToExecuteQueue;
    
    /**
     * 租户的坐席表
     * <tenantUn, <staff_uuid, staff>>
     */
    public static Map<String, Map<String, Staff>> mulClientStaffMap = new ConcurrentHashMap<String, Map<String, Staff>>();
    public static Map<String, StaffSessionInfo> activeStaffMap = new ConcurrentHashMap<String, StaffSessionInfo>();   // <Staff openid, StaffSessionInfo>
    
    /**
     * 已发起人工服务请求并处于等待状态的客户排队列表
     * <tenantUn, <skill_category, queue>>
     */
    public static Map<String, Map<String, Queue<WaitingClient>>> waitingList = new ConcurrentHashMap<String, Map<String,Queue<WaitingClient>>>();
    
    /**
     * 排队中的客户openid集合
     */
    public static Set<String> waitingListIDs= new HashSet<String>();
    public static Map<String, JSONObject> staffIdList = new HashMap<String, JSONObject>();
//    public static Map<String, Queue<JSONObject>> leavedMessageMap = new HashMap<String, Queue<JSONObject>>();  // <tenantUn, Queue>
    
    @Override
    public void run() {

        while (true) {
            try {
                Map<String, String> message = getMessageQueue().take();
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
                case API.VIDEO_MESSAGE:
                    videoMessage(message);
                    break;
                case API.EVENT_MESSAGE:
                    String event = message.get(API.MESSAGE_EVENT_TAG);
                    if (event.equals("subscribe")) {
                        subscribe(message);
                    } else if (event.equals("unsubscribe")) {
                        unsubscribe(message);
                    }else if (event.equals("SCAN")) {
                        scan(message);
                    } else if (event.equalsIgnoreCase("click")) {
                        if (message.containsKey(API.TENANTUN)) {
                            String tenantUn = message.get(API.TENANTUN);
                            Jedis jedis = ContextPreloader.jedisPool.getResource();
                            if (jedis.hexists(API.REDIS_CLIENT_EVENT_LIST, tenantUn)) {
                                String events = jedis.hget(API.REDIS_CLIENT_EVENT_LIST, tenantUn);
                                JSONArray ja = JSONArray.fromObject(events);
                                String eventKey = message.get(API.MESSAGE_EVENT_KEY_TAG);
                                if (ja.contains(eventKey)) {
                                    String reply = jedis.hget(API.REDIS_CLIENT_EVENT_IVR + tenantUn, eventKey);
                                    log.info("Reply: " + reply);
                                    JSONObject jo = JSONObject.fromObject(reply);
                                    jo.put("touser", message.get(API.MESSAGE_FROM_TAG));
                                    jo.put("access_token", getAccessToken(message.get(API.MESSAGE_TO_TAG), jedis));
                                    getMessageToSendQueue().put(jo);
                                    ContextPreloader.jedisPool.returnResource(jedis);
                                    break;
                                }
                            }
                            ContextPreloader.jedisPool.returnResource(jedis);
                        }
                        
                        if (message.get(API.MESSAGE_EVENT_KEY_TAG).contains(SKILL_PREFIX)) {
                            String skill = message.get(API.MESSAGE_EVENT_KEY_TAG).replace(SKILL_PREFIX, "");
                            staffService(message, skill);
                            break;
                        }
                        switch (message.get(API.MESSAGE_EVENT_KEY_TAG)) {
                        case "CLIENT_INFO":  // 获取用户详情
                            clientInfo(message);
                            break;
                        case "CHECK_IN":    // 坐席登入
                            checkin(message);
                            break;
                        case "CHECK_OUT":    // 坐席登出
                            checkout(message);
                            break;
                        case "CLIENT_END_SESSION":    // 客户结束对话
                            clientEndSession(message);
                            break;
                        case "STAFF_END_SESSION":    // 客服结束对话
                            staffEndSession(message);
                            break;
                        case "TAKE_CLIENT":
                            takeClient(message);  // 抢接
                            break;
//                        case "GET_MESSAGE":
//                            getMessage(message);  // 获取留言
//                            break;
                        case "CHAT_HISTORY":
                            getChatHistory(message);  // 获取留言
                            break;
                        case "CHECK_CLIENT_NUM":  // 查看等待客户数量
                            checkClientNum(message);
                            break;
                        case "GET_ORCODE":
                            getQRCode(message);
                            break;
                        case "LEAVE_MESSAGE":
                            leaveMessage(message);
                            break;
                        case "EXPRESS_RETURN_1":
                            expressMessage(message, "您好！请问有什么可以帮助您[微笑]?");
                            break;
                        case "EXPRESS_RETURN_2":
                            expressMessage(message, "感谢您对我们的支持！如有疑问请及时与我们沟通[微笑]?");
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
        String extension = "";
        if (media_type.equals(API.IMAGE_MESSAGE)) {
            extension = ".jpg";
        } else if (media_type.equals(API.VOICE_MESSAGE)) {
            extension = ".amr";
        } else if (media_type.equals(API.VIDEO_MESSAGE)) {
            extension = ".mp4";
        }
        JSONObject ret = WeChatHttpsUtil.httpPostFile(post_url, input, UUID.randomUUID().toString() + extension);
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
        String tenantUn = message.get("tenantUn");
        
        // Leaving message
        if (null != tenantUn && CheckLeavingMessageJob.leavingMessageClientList.containsKey(tenantUn) && CheckLeavingMessageJob.leavingMessageClientList.get(tenantUn).containsKey(from_openid)) {
            recordLeaveMessage(message);
            return;
        }

        if (!ContextPreloader.staffAccountList.contains(to_account)) {
            // Message from client
            if (CheckSessionAvailableJob.sessionMap.containsKey(tenantUn)
                    && CheckSessionAvailableJob.sessionMap.get(tenantUn).containsKey(from_openid)) {
                StaffSessionInfo s = CheckSessionAvailableJob.sessionMap.get(tenantUn).get(from_openid);
                if (s != null && s.isBusy()) {
                    Jedis jedis = ContextPreloader.jedisPool.getResource();
                    //String sToken = ContextPreloader.Account_Map.get(s.getAccount()).getToken();
                    String sToken = jedis.hget(API.REDIS_WEIXIN_ACCESS_TOKEN_KEY, s.getAccount());
                    ContextPreloader.jedisPool.returnResource(jedis);
                    
                    String content = String.format("%s: %s", s.getClient_name(), message.get(API.MESSAGE_CONTENT_TAG));
                    sendMessage(s.getOpenid(), sToken, content, API.TEXT_MESSAGE);
                    s.setLastReceived(new Date());
                    recordMessage(s, message, API.TEXT_MESSAGE, "wx", true);
                    
                    // Remove session from CheckEndSessionJob.endSessionMap
                    if (CheckEndSessionJob.endSessionMap.containsKey(tenantUn)
                            && CheckEndSessionJob.endSessionMap.get(tenantUn).containsKey(from_openid)) {
                        CheckEndSessionJob.endSessionMap.get(tenantUn).remove(from_openid);
                    }
                    
                    // Send to web page
                    if (s.isWebStaff()) {
                        sendWebMessage("text", message.get(API.MESSAGE_CONTENT_TAG), s.getOpenid(), s.getClient_name(), s.getStaff_uuid());
                    }

                }
                
                return;
            }
        } else {
            // Message from staff
            
            StaffSessionInfo s = activeStaffMap.get(from_openid);

            if (s != null && s.isBusy()) {
                String content = String.format("客服%s: %s", s.getStaffid(), message.get(API.MESSAGE_CONTENT_TAG));

                if (s.getClient_type().equalsIgnoreCase("wb")) {
                    JSONObject weibo_request = new JSONObject();
                    weibo_request.put("user_id", s.getClient_openid());
                    weibo_request.put("tenantUn", s.getTenantUn());
                    weibo_request.put("msgtype", API.TEXT_MESSAGE);
                    weibo_request.put("content", content);
                    
                    JSONObject weibo_ret = WeChatHttpsUtil.httpPostRequest(API.WEIBO_SEND_MESSAGE_URL, weibo_request.toString(), 0);
                    log.info("Send weibo message return: "+ weibo_ret.toString());
                    s.setLastReceived(new Date());
                    recordMessage(s, message, API.TEXT_MESSAGE, "wb", false);
                    return;
                }
                
                Jedis jedis = ContextPreloader.jedisPool.getResource();
//                String cToken = ContextPreloader.Account_Map.get(s.getClient_account()).getToken();
                String cToken = jedis.hget(API.REDIS_WEIXIN_ACCESS_TOKEN_KEY, s.getClient_account());
                ContextPreloader.jedisPool.returnResource(jedis);
                
                sendMessage(s.getClient_openid(), cToken, content, API.TEXT_MESSAGE);
                s.setLastReceived(new Date());
                String message_source = s.getClient_type();
                recordMessage(s, message, API.TEXT_MESSAGE, message_source, false);
                
                // Send to web page
                if (s.isWebStaff()) {
                    sendWebMessage("text", message.get(API.MESSAGE_CONTENT_TAG), s.getOpenid(), s.getStaff_uuid(), s.getStaff_uuid());

                }
                
                return;
            }
        }
        
        if (null != tenantUn) {
            Jedis jedis = ContextPreloader.jedisPool.getResource();
            if (jedis.hexists(API.REDIS_CLIENT_KEYWORDS_REGEX, tenantUn)) {
                String regex = jedis.hget(API.REDIS_CLIENT_KEYWORDS_REGEX, tenantUn);
                log.info("Regex: " + regex);
                String text = message.get(API.MESSAGE_CONTENT_TAG);
                String keyword = getMatchedWord(regex, text);
                if (null != keyword) {
                    String reply = jedis.hget(API.REDIS_CLIENT_TEXT_IVR + tenantUn, keyword);
                    try {
                        JSONObject jo = JSONObject.fromObject(reply);
                        jo.put("touser", message.get(API.MESSAGE_FROM_TAG));
                        jo.put("access_token", getAccessToken(message.get(API.MESSAGE_TO_TAG), jedis));
                        MessageExecutor.messageToSendQueue.put(jo);
                    } catch (Exception e) {
                        log.error("Send reply message failed: " + e.toString());
                        e.printStackTrace();
                    }
                }
            }
            ContextPreloader.jedisPool.returnResource(jedis);
        }
        log.info("This is not an available session.");
    }
    
    private void imageMessage(Map<String, String> message) {
        Jedis jedis = ContextPreloader.jedisPool.getResource();
        
        String img_account = message.get(API.MESSAGE_TO_TAG);
        String img_Token = jedis.hget(API.REDIS_WEIXIN_ACCESS_TOKEN_KEY, img_account); // ContextPreloader.Account_Map.get(img_account).getToken();
        String img_openid = message.get(API.MESSAGE_FROM_TAG);
        String img_toToken = "";
        String img_to_openid = "";
        if (!ContextPreloader.staffAccountList.contains(img_account)) {
            String tenantUn = message.get("tenantUn");
            // Message from client
            if (CheckSessionAvailableJob.sessionMap.containsKey(tenantUn)
                    && CheckSessionAvailableJob.sessionMap.get(tenantUn).containsKey(img_openid)) {
                StaffSessionInfo s = CheckSessionAvailableJob.sessionMap.get(tenantUn).get(img_openid);
                img_toToken = jedis.hget(API.REDIS_WEIXIN_ACCESS_TOKEN_KEY, s.getAccount());  // ContextPreloader.Account_Map.get(s.getAccount()).getToken();
                img_to_openid = s.getOpenid();
                s.setLastReceived(new Date());
            }
        } else {
            // Message from staff
            StaffSessionInfo s = activeStaffMap.get(img_openid);
            if (s != null && s.isBusy()) {
                img_toToken = jedis.hget(API.REDIS_WEIXIN_ACCESS_TOKEN_KEY, s.getClient_account()); //ContextPreloader.Account_Map.get(s.getClient_account()).getToken();
                img_to_openid = s.getClient_openid();
                s.setLastReceived(new Date());
            }
        }
        
        ContextPreloader.jedisPool.returnResource(jedis);
        
        String img_id = getMediaId(message.get(API.MESSAGE_MEDIA_ID_TAG), img_Token, img_toToken, "image", API.CONTENT_TYPE_IMAGE);
        if (img_id.equals("error")) {
            sendMessage(img_openid, img_Token, "系统消息:发送图片失败,请发送小于120KB的图片", API.TEXT_MESSAGE);
            return;
        }
        sendMessage(img_to_openid, img_toToken, img_id, API.IMAGE_MESSAGE);
    }
    
    private void voiceMessage(Map<String, String> message) {
        Jedis jedis = ContextPreloader.jedisPool.getResource();
        
        String voice_account = message.get(API.MESSAGE_TO_TAG);
        String voice_openid = message.get(API.MESSAGE_FROM_TAG);
        String voice_Token = jedis.hget(API.REDIS_WEIXIN_ACCESS_TOKEN_KEY, voice_account); //ContextPreloader.Account_Map.get(voice_account).getToken();
        String voice_toToken = "";
        String voice_to_openid = "";
        if (!ContextPreloader.staffAccountList.contains(voice_account)) {
            String tenantUn = message.get("tenantUn");
            // Message from client
            if (CheckSessionAvailableJob.sessionMap.containsKey(tenantUn)
                    && CheckSessionAvailableJob.sessionMap.get(tenantUn).containsKey(voice_openid)) {
                StaffSessionInfo s = CheckSessionAvailableJob.sessionMap.get(tenantUn).get(voice_openid);
                voice_toToken = jedis.hget(API.REDIS_WEIXIN_ACCESS_TOKEN_KEY, s.getAccount()); //ContextPreloader.Account_Map.get(s.getAccount()).getToken();
                voice_to_openid = s.getOpenid();
                s.setLastReceived(new Date());
            }
        } else {
            // Message from staff
            StaffSessionInfo s = activeStaffMap.get(voice_openid);
            if (s != null && s.isBusy()) {
                voice_toToken = jedis.hget(API.REDIS_WEIXIN_ACCESS_TOKEN_KEY, s.getClient_account()); //ContextPreloader.Account_Map.get(s.getClient_account()).getToken();
                voice_to_openid = s.getClient_openid();
                s.setLastReceived(new Date());
            }
        }
        
        ContextPreloader.jedisPool.returnResource(jedis);
        
        String voice_id = getMediaId(message.get(API.MESSAGE_MEDIA_ID_TAG), voice_Token, voice_toToken, "voice", API.CONTENT_TYPE_VOICE);
        if (voice_id.equals("error")) {
            sendMessage(voice_openid, voice_Token, "系统消息:发送语音消息失败", API.TEXT_MESSAGE);
            return;
        }
        sendMessage(voice_to_openid, voice_toToken, voice_id, API.VOICE_MESSAGE);
    }
    
    private void videoMessage(Map<String, String> message) {
        Jedis jedis = ContextPreloader.jedisPool.getResource();
        
        String video_account = message.get(API.MESSAGE_TO_TAG);
        String video_openid = message.get(API.MESSAGE_FROM_TAG);
        String video_Token = jedis.hget(API.REDIS_WEIXIN_ACCESS_TOKEN_KEY, video_account); //ContextPreloader.Account_Map.get(video_account).getToken();
        String video_toToken = "";
        String video_to_openid = "";
        if (!ContextPreloader.staffAccountList.contains(video_account)) {
            String tenantUn = message.get("tenantUn");
            // Message from client
            if (CheckSessionAvailableJob.sessionMap.containsKey(tenantUn)
                    && CheckSessionAvailableJob.sessionMap.get(tenantUn).containsKey(video_openid)) {
                StaffSessionInfo s = CheckSessionAvailableJob.sessionMap.get(tenantUn).get(video_openid);
                video_toToken = jedis.hget(API.REDIS_WEIXIN_ACCESS_TOKEN_KEY, s.getAccount()); //ContextPreloader.Account_Map.get(s.getAccount()).getToken();
                video_to_openid = s.getOpenid();
                s.setLastReceived(new Date());
            }
        } else {
            // Message from staff
            StaffSessionInfo s = activeStaffMap.get(video_openid);
            if (s != null && s.isBusy()) {
                video_toToken = jedis.hget(API.REDIS_WEIXIN_ACCESS_TOKEN_KEY, s.getClient_account()); //ContextPreloader.Account_Map.get(s.getClient_account()).getToken();
                video_to_openid = s.getClient_openid();
                s.setLastReceived(new Date());
            }
        }
        
        ContextPreloader.jedisPool.returnResource(jedis);
        
        String voice_id = getMediaId(message.get(API.MESSAGE_MEDIA_ID_TAG), video_Token, video_toToken, "video", API.CONTENT_TYPE_VIDEO);
        if (voice_id.equals("error")) {
            sendMessage(video_openid, video_Token, "系统消息:发送视频消息失败", API.TEXT_MESSAGE);
            return;
        }
        sendMessage(video_to_openid, video_toToken, voice_id, API.VIDEO_MESSAGE);
    }
    
    private void subscribe(Map<String, String> message) throws InterruptedException {
        String account = message.get(API.MESSAGE_TO_TAG);
        String openid =  message.get(API.MESSAGE_FROM_TAG);
        String toToken = getAccessToken(account);
        String url = USER_INFO_URL.replace("ACCESS_TOKEN", toToken).replace("OPENID", openid);
        JSONObject user_info = WeChatHttpsUtil.httpsRequest(url, "GET", null);
        if (user_info.containsKey("errcode")) {
            log.error("Get client info failed: "+user_info.toString());
            return;
        }
        
        log.debug("Staff info: " + user_info.toString());

        if (ContextPreloader.staffAccountList.contains(account)) {
            // Staff subscribe
            if (message.containsKey(API.MESSAGE_EVENT_KEY_TAG)) {
                String qrscene = message.get(API.MESSAGE_EVENT_KEY_TAG);
                String scene_id = qrscene.replace("qrscene_", "");
                log.debug("qrscene is: " + qrscene + " id = " + scene_id);
                try {
                    JSONObject params = new JSONObject();
                    params.put("channelid",
                            ContextPreloader.channelMap.get(account));
                    params.put("userinfo", user_info.toString());
                    params.put("tenantid", scene_id);
                    params.put("weixin_public_id", account);

                    MQEvent event = new MQStaffSubscribeEvent(params.toString());
                    MQManager manager = (MQManager) AppContext
                            .getApplicationContext().getBean("MQManager");
                    manager.publishTopicEvent(event);

                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("CRM registe failed: " + e.toString());

                    // TODO Send error message to client
                }

            }
        } else {
            String tenantUn = message.get("tenantUn");
            
            if (account.equals(ContextPreloader.HESONG_ACCOUNT)) {
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
                article_2.put("title", "指尖上的和声");
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
            
            
            if (message.containsKey("tenantUn")) {
                log.info("New client subscribe for tenantUn: " + tenantUn);
                Jedis jedis = ContextPreloader.jedisPool.getResource();
                String ivr_tag = API.REDIS_CLIENT_EVENT_IVR + tenantUn;
                if (jedis.hexists(ivr_tag, "subscribe")) {
                    String reply = jedis.hget(ivr_tag, "subscribe");
                    log.info("Subscribe reply: " + reply);
                    JSONObject jo = JSONObject.fromObject(reply);
                    jo.put("touser", message.get(API.MESSAGE_FROM_TAG));
                    jo.put("access_token", getAccessToken(account, jedis));
                    getMessageToSendQueue().put(jo);
                }
                ContextPreloader.jedisPool.returnResource(jedis);
            }
            
            // Insert client info to SugerCRM
            // TODO
            if (null == tenantUn) {
                tenantUn = "null";
            }
            log.info("Client info: " + user_info.toString());
            user_info.put("tenant_code", tenantUn);
            user_info.put("source", "wx");

            MQEvent event = new MQClientSubscribeEvent(user_info.toString());
            MQManager manager = (MQManager) AppContext.getApplicationContext()
                    .getBean("MQManager");
            manager.publishTopicEvent(event);

//                SugarCRMCaller crmCaller = new SugarCRMCaller();
//                
//                if (!crmCaller.check_oauth(SUAExecutor.session)) {
//                    SUAExecutor.session = crmCaller.login("admin",
//                            "p@ssw0rd");
//                }
//                String session = SUAExecutor.session;
//                
//                if (!crmCaller.isOpenidBinded(session, openid)) {
//                    String insert_recall = crmCaller
//                            .insertToCRM_Prospects(session,
//                                    user_info);
//                    log.info("insert_recall: " + insert_recall);
//                }
            
        }
    }
    
    private void unsubscribe(Map<String, String> message){
        String account = message.get(API.MESSAGE_TO_TAG);
        if (ContextPreloader.staffAccountList.contains(account)) {
            String openid = message.get(API.MESSAGE_FROM_TAG);
//            String r = HttpClientUtil.httpGet(API.SUA_DEL_STAFF_URL + openid);
//            JSONObject rj = JSONObject.fromObject(r);
//            log.info("Staff unsubscribe return: " + rj.toString());
            
            JSONObject params = new JSONObject();
            params.put("openid", openid);
            MQEvent event = new MQStaffUnsubscribeEvent(params.toString());
            MQManager manager = (MQManager)AppContext.getApplicationContext().getBean("MQManager");
            manager.publishTopicEvent(event);
        }
        // TODO remove active session
        
    }
    
    private void scan(Map<String, String> message) {
        String openid = message.get(API.MESSAGE_FROM_TAG);
        String toToken = getAccessToken(message.get(API.MESSAGE_TO_TAG)); //ContextPreloader.Account_Map.get(
//                message.get(API.MESSAGE_TO_TAG)).getToken();
        String content = "亲,您已经关注了该公众号!";
        sendMessage(openid, toToken, content, API.TEXT_MESSAGE);
    }
    
    private void clientInfo(Map<String, String> message) {
        String openid = message.get(API.MESSAGE_FROM_TAG);
        StaffSessionInfo s = activeStaffMap.get(openid);
        String account = message.get(API.MESSAGE_TO_TAG);
        //AccessToken ac = ContextPreloader.Account_Map.get(account);
        JSONObject accountInfo = getAccountInfo(account, API.REDIS_STAFF_ACCOUNT_INFO_KEY);
        if (s != null && s.isBusy()) {
            String url = String.format(API.CLIENT_INFO_URL, s.getClient_openid(), ContextPreloader.channelMap.get(account));
            try {
                String text = String.format("<a href=\"https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_base&state=123#wechat_redirect\">请查看</a>", accountInfo.getString("appid"), URLEncoder.encode(url, "utf8"));
                sendMessage(openid, getAccessToken(account), text, API.TEXT_MESSAGE);
            } catch (UnsupportedEncodingException e) {
                log.error("URLEncoder error: " + e.toString() + ", URL = " + url);
                e.printStackTrace();
            }
        } else {
            String text = "目前没有和客户建立连接，无法查看详情.";
            sendMessage(openid, getAccessToken(account), text, API.TEXT_MESSAGE);
        }
    }
    
    private void getChatHistory(Map<String, String> message) {
        String openid = message.get(API.MESSAGE_FROM_TAG);
        StaffSessionInfo s = activeStaffMap.get(openid);
        String account = message.get(API.MESSAGE_TO_TAG);
//        AccessToken ac = ContextPreloader.Account_Map.get(account);
        JSONObject accountInfo = getAccountInfo(account, API.REDIS_STAFF_ACCOUNT_INFO_KEY);
        String url = null;
        if (s != null && s.isBusy()) {
            url = String
                    .format(API.CHAT_HISTORY_URL,
                            s.getClient_openid(), openid,
                            ContextPreloader.channelMap.get(account));
        } else {
            url = String
                    .format(API.ALL_CHAT_HISTORY_URL,
                            openid, ContextPreloader.channelMap.get(account));
        }
        try {
            String text = String
                    .format("<a href=\"https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_base&state=123#wechat_redirect\">请点击查看历史记录</a>",
                            accountInfo.getString("appid"), URLEncoder.encode(url, "utf8"));
            sendMessage(openid, getAccessToken(account), text, API.TEXT_MESSAGE);
        } catch (UnsupportedEncodingException e) {
            log.error("URLEncoder error: " + e.toString() + ", URL = " + url);
            e.printStackTrace();
        }
    }
    
    private void checkin(Map<String, String> message) {
        String openid = message.get(API.MESSAGE_FROM_TAG);
        String account = message.get(API.MESSAGE_TO_TAG);
        String url = API.STAFF_OPENID_CHECKIN_URL + openid;
        String token = getAccessToken(account); //ContextPreloader.Account_Map.get(account).getToken();
        try {
            JSONObject staff_info = JSONObject.fromObject(HttpClientUtil.httpGet(url));
            log.info("Checkin SUA return: " + staff_info.toString());
            if (staff_info.getBoolean("success")) {
                if (staff_info.getInt("tenantstatus") != 1) {
                    log.warn("Staff no autho");
                    String text = "欢迎使用和声云客服系统，您申请成为【商家】客服，正在审核中，请耐心等待...";
                    sendMessage(openid, token, text, API.TEXT_MESSAGE);
                    return;
                }
                
                String wx_account = staff_info.getString("wx_account");
                String tenantUn = staff_info.getString("tenantUn");
                String staff_uuid = staff_info.getString("staff_id");
                
                Map<String, Staff> staff_map = null;
                
                if (mulClientStaffMap.containsKey(tenantUn)) {
                    staff_map = mulClientStaffMap.get(tenantUn);
                    if (staff_map.containsKey(staff_uuid)) {
                        log.info("Staff already checked in, staff_uuid: " + staff_uuid);
                        String text = "系统消息:您已经签入了,无需再次签入.";
                        sendMessage(openid, token, text, API.TEXT_MESSAGE);
                        return;
                    }
                } else {
                    staff_map = new HashMap<String, Staff>();
                    mulClientStaffMap.put(tenantUn, staff_map);
                }
                // Create staff
                String staff_working_num = staff_info.getString("staff_number");
                String staff_name = staff_info.getString("staff_name");
                
                JSONArray channel_list = staff_info.getJSONArray("channels");
                WebchatController.channelList.put(staff_uuid, channel_list);
                List<StaffSessionInfo> sessionChannelList = new ArrayList<StaffSessionInfo>();
                String text = String.format("系统提示:签到成功,你的工号是%s.", staff_working_num);

                for (int i = 0; i < channel_list.size(); i++) {
                    JSONObject channel = channel_list.getJSONObject(i);
                    String staff_account = channel.getString("account");
                    String staff_openid = channel.getString("openid");
                    StaffSessionInfo s = new StaffSessionInfo(tenantUn, staff_account, staff_openid, staff_working_num, staff_name, staff_uuid);
                    MessageRouter.activeStaffMap.put(staff_openid, s);
                    sessionChannelList.add(s);
                    JSONObject staff_account_id = new JSONObject();
                    staff_account_id.put("wx_account", staff_account);
                    staff_account_id.put("staffid", staff_uuid);
                    staff_account_id.put("tenantUn", tenantUn);
                    staffIdList.put(staff_openid, staff_account_id);
                    
                    String stoken = getAccessToken(staff_account);
                    MessageRouter.sendMessage(staff_openid, stoken, text, API.TEXT_MESSAGE);
                }
                
                List<String> skills = new ArrayList<String>();
                if (staff_info.containsKey("skills")) {
                    JSONArray skl = staff_info.getJSONArray("skills");
                    for (int i = 0; i < skl.size(); i++) {
                        skills.add(skl.getJSONObject(i).getString("code"));
                    }
                }
                
                Staff staff = new Staff(staff_uuid, staff_name, tenantUn, wx_account, staff_working_num, sessionChannelList, skills);
                staff_map.put(staff_uuid, staff);
                log.info("Staff checked in: " + staff.toString());
            } else {
                log.error("Staff checkin failed: " + staff_info.getString("msg"));
                sendMessage(openid, token, "系统消息:签入失败,请联系管理员!", API.TEXT_MESSAGE);
            }
        } catch (Exception e) {
            log.error("Staff checkin failed: " + e.toString());
            sendMessage(openid, token, "系统消息:签入失败,请联系管理员!", API.TEXT_MESSAGE);
        }
    }
    
    private void checkout(Map<String, String> message) {
        String openid = message.get(API.MESSAGE_FROM_TAG);
        JSONObject staff_account_id = staffIdList.get(openid);
        if (null != staff_account_id) {
            log.info("staff_account_id: "+staff_account_id.toString());
            String tenantUn = staff_account_id.getString("tenantUn"); 
            String staff_id = staff_account_id.getString("staffid"); 
            Map<String, Staff> staff_map = mulClientStaffMap.get(tenantUn);
            log.info("staff_map: "+staff_map.toString());
            Staff staff = staff_map.get(staff_id);
            log.info("Staff: "+staff.toString());
            List<StaffSessionInfo> sessionList = staff.getSessionChannelList();
            
            Jedis jedis = ContextPreloader.jedisPool.getResource();
            
            for (StaffSessionInfo s : sessionList) {
                if (s.isBusy()) {
                    s.setBusy(false);
                    
                    if (s.getClient_type().equalsIgnoreCase("wx")) {
                        // Remaind client that staff is leaving.
                        String token = jedis.hget(API.REDIS_WEIXIN_ACCESS_TOKEN_KEY, s.getClient_account()); //ContextPreloader.Account_Map.get(s.getClient_account()).getToken();
                        String text = "对不起，客服MM有急事下线了,会话已结束。您可以使用留言功能,客服MM将会在第一时间给您回复[微笑]";
                        sendMessage(s.getClient_openid(), token, text, "text");

                        if (CheckSessionAvailableJob.sessionMap.containsKey(tenantUn)) {
                            CheckSessionAvailableJob.sessionMap.get(tenantUn).remove(s.getClient_openid());
                        }
                    }
                    
                    if (CheckWeiboSessionAvailableJob.weiboSessionMap.containsKey(tenantUn) && 
                            CheckWeiboSessionAvailableJob.weiboSessionMap.get(tenantUn).containsKey(s.getClient_openid())) {
                        CheckWeiboSessionAvailableJob.weiboSessionMap.get(tenantUn).remove(s.getClient_openid());
                    }
                    
                    s.setEndTime(API.TIME_FORMAT.format(new Date()));
                    MessageRouter.recordSession(s, 0);
                }
                String token = jedis.hget(API.REDIS_WEIXIN_ACCESS_TOKEN_KEY, s.getAccount()); // ContextPreloader.Account_Map.get(s.getAccount()).getToken();
                sendMessage(s.getOpenid(), token, "系统消息:您已成功签出!", API.TEXT_MESSAGE);
                activeStaffMap.remove(s.getOpenid());
                
            }
            jedis.hdel(API.REDIS_WEIXIN_WEBCHAT_SESSIONID, staff_id);
            ContextPreloader.jedisPool.returnResource(jedis);
            
            // TODO send checkout message to web
            
            staff_map.remove(staff_id);
            log.info("staff_map after remove: "+staff_map.toString());
            log.info("mulClientStaffMap after remove: "+mulClientStaffMap.get(tenantUn).toString());
            staffIdList.remove(openid);
            if (staff_map.isEmpty()) {
                mulClientStaffMap.remove(tenantUn);
            }
            WebchatController.channelList.remove(staff_id);
        }
    }
    
    private void staffService(Map<String, String> message, String skill_category) {
        String client_openid = message.get(API.MESSAGE_FROM_TAG);
        String client_account = message.get(API.MESSAGE_TO_TAG);
        String tenantUn = message.get("tenantUn");
        String cToken = getAccessToken(client_account); // ContextPreloader.Account_Map.get(client_account).getToken();
        
        JSONObject crmRequest = new JSONObject();
        crmRequest.put("method", "convertProspectsToLeads");
        crmRequest.put("session", "");
        crmRequest.put("prospectsOpenid", client_openid);
        try {
            getSuaRequestToExecuteQueue().put(crmRequest);
        } catch (InterruptedException e) {
            log.error("Put convertProspectsToLeads request to queue failed: " + e.toString());
            e.printStackTrace();
        }
        
        if (CheckSessionAvailableJob.sessionMap.containsKey(tenantUn)
                && CheckSessionAvailableJob.sessionMap.get(tenantUn).containsKey(client_openid)) {
            sendMessage(client_openid, cToken, "您已经接通人工服务!", API.TEXT_MESSAGE);
            return;
        }
        
        if (null == tenantUn || !mulClientStaffMap.containsKey(tenantUn) || mulClientStaffMap.get(tenantUn).isEmpty()) {
            String content = "暂时没有空闲客服,请稍后再试!您可以使用留言功能，客服MM将第一时间给您回复[微笑]";
            sendMessage(client_openid, cToken, content, API.TEXT_MESSAGE);
            return;
        }
        
        if (waitingListIDs.contains(client_openid)) {
            String content = "您已经发出人工服务请求,我们将尽快为您接通,请耐心等候!";
            sendMessage(client_openid, cToken, content, API.TEXT_MESSAGE);
            return;
        }
        
        // Get client nickname
        String query = USER_INFO_URL.replace("ACCESS_TOKEN", cToken).replace("OPENID", client_openid);
        JSONObject user_info = WeChatHttpsUtil.httpsRequest(query, "GET", null);
        String client_name = "";
        if (user_info.containsKey("errcode")) {
            log.error("Get client info failed: "+user_info.toString());
            client_name = "匿名客户";
        } else {
            client_name = user_info.getString("nickname");
        }
        
        WaitingClient c = new WaitingClient(tenantUn, client_openid, client_account, client_name, new Date().getTime());
        if (!waitingList.containsKey(tenantUn)) {
            Queue<WaitingClient> queue = new LinkedList<WaitingClient>();
            queue.offer(c);
            Map<String, Queue<WaitingClient>> m = new ConcurrentHashMap<String, Queue<WaitingClient>>();
            m.put(skill_category, queue);
            waitingList.put(tenantUn, m);
        } else if (!waitingList.get(tenantUn).containsKey(skill_category)) {
            Queue<WaitingClient> queue = new LinkedList<WaitingClient>();
            queue.offer(c);
            waitingList.get(tenantUn).put(skill_category, queue);
        } else {
            waitingList.get(tenantUn).get(skill_category).offer(c);
        }
        
        // Broadcast client request to all staffs
        Jedis jedis = ContextPreloader.jedisPool.getResource();
        
        boolean isAllBusy = true;
        for (Staff staff : mulClientStaffMap.get(tenantUn).values()) {
            if (staff.getSkills().contains(skill_category)) {
                for (StaffSessionInfo s : staff.getSessionChannelList()) {
                    if (!s.isBusy()) {
                        isAllBusy = false;
                        int num = waitingList.size();
                        String text = String.format("有%d个客户寻求人工服务,请点击抢接按钮接通会话.", num);
                        String sToken = jedis.hget(API.REDIS_WEIXIN_ACCESS_TOKEN_KEY, s.getAccount()); //ContextPreloader.Account_Map.get(s.getAccount()).getToken();
                        sendMessage(s.getOpenid(), sToken, text, API.TEXT_MESSAGE);
                        
                        if (s.isWebStaff()) {
                            sendWebMessage("staffService", String.format("有%d个客户寻求人工服务,点击确认可接通会话.", num),
                                    s.getOpenid(), "", s.getStaff_uuid());
                        }
                        
                        break; // Only one message for each staff
                    }
                }
            }
        }
        ContextPreloader.jedisPool.returnResource(jedis);
        
        if (isAllBusy) {
            String text = "暂时没有空闲客服,请稍后再试!您可以使用留言功能，客服MM将第一时间给您回复[微笑]";
            sendMessage(client_openid, cToken, text, API.TEXT_MESSAGE);
            return;
        } else {
            waitingListIDs.add(client_openid);
            String content = "正在为您接通人工服务,请稍等...";
            sendMessage(client_openid, cToken, content, API.TEXT_MESSAGE);
        }
        
    }
    
    private void clientEndSession(Map<String, String> message) {
        
        String cOpenid = message.get(API.MESSAGE_FROM_TAG);
        String tenantUn = message.get("tenantUn");
        
        StaffSessionInfo s = null;
        if (CheckSessionAvailableJob.sessionMap.containsKey(tenantUn) 
                &&CheckSessionAvailableJob.sessionMap.get(tenantUn).containsKey(cOpenid)) {
            s = CheckSessionAvailableJob.sessionMap.get(tenantUn).get(cOpenid);
        } else {
            String token = getAccessToken(message.get(API.MESSAGE_TO_TAG));
            String content = "系统消息:您并没有接通会话.";
            sendMessage(cOpenid, token, content, API.TEXT_MESSAGE);
            return;
        }

      
        String content = "系统消息:客户已退出,会话已结束";

        Jedis jedis = ContextPreloader.jedisPool.getResource();
        String cToken = jedis.hget(API.REDIS_WEIXIN_ACCESS_TOKEN_KEY,
                s.getClient_account()); // ContextPreloader.Account_Map.get(s.getClient_account()).getToken();
        String sToken = jedis.hget(API.REDIS_WEIXIN_ACCESS_TOKEN_KEY,
                s.getAccount()); // ContextPreloader.Account_Map.get(s.getAccount()).getToken();
        ContextPreloader.jedisPool.returnResource(jedis);

        // To staff
        sendMessage(s.getOpenid(), sToken, content, API.TEXT_MESSAGE);

        // To client
        content = "系统消息:会话已结束";
        sendMessage(s.getClient_openid(), cToken, content, API.TEXT_MESSAGE);
        
        log.info("Session ended.");
        CheckSessionAvailableJob.sessionMap.get(tenantUn).remove(cOpenid);

        s.setBusy(false);
        s.setClient_account("");
        s.setClient_name("");
        s.setClient_openid("");
    }
    
    private void staffEndSession(Map<String, String> message) {
        
        String sOpenid = message.get(API.MESSAGE_FROM_TAG);
        
        StaffSessionInfo s = null;
        if (activeStaffMap.containsKey(sOpenid)
                && activeStaffMap.get(sOpenid).isBusy()) {
            s = activeStaffMap.get(sOpenid);
        } else {
            String token = getAccessToken(message.get(API.MESSAGE_TO_TAG));
            String content = "系统消息:您并没有接通会话.";
            sendMessage(sOpenid, token, content, API.TEXT_MESSAGE);
            return;
        }

        if (CheckEndSessionJob.endSessionMap.containsKey(s.getTenantUn())
                && CheckEndSessionJob.endSessionMap.get(s.getTenantUn()).containsKey(s.getClient_openid())) {
            String content = "系统消息:请不要重复发出结束会话提示.";
            String token = getAccessToken(message.get(API.MESSAGE_TO_TAG));
            sendMessage(sOpenid, token, content, API.TEXT_MESSAGE);
            return;
        }
        
        String content = "系统消息:您已向客户发出结束会话提示,30秒内客户如果没有任何回复,该会话将自动结束.";

        Jedis jedis = ContextPreloader.jedisPool.getResource();
        String cToken = jedis.hget(API.REDIS_WEIXIN_ACCESS_TOKEN_KEY,
                s.getClient_account()); // ContextPreloader.Account_Map.get(s.getClient_account()).getToken();
        String sToken = jedis.hget(API.REDIS_WEIXIN_ACCESS_TOKEN_KEY,
                s.getAccount()); // ContextPreloader.Account_Map.get(s.getAccount()).getToken();
        ContextPreloader.jedisPool.returnResource(jedis);

        // To staff
        sendMessage(sOpenid, sToken, content, API.TEXT_MESSAGE);

        // To client
        content = "系统消息:30秒内如果您未作任何回复,该会话将自动结束.";
        sendMessage(s.getClient_openid(), cToken, content, API.TEXT_MESSAGE);
        
        Map<String, StaffSessionInfo> session_map = null;
        if (!CheckEndSessionJob.endSessionMap.containsKey(s.getTenantUn())) {
            session_map = new ConcurrentHashMap<String, StaffSessionInfo>();
            CheckEndSessionJob.endSessionMap.put(s.getTenantUn(), session_map);
        } else {
            session_map = CheckEndSessionJob.endSessionMap.get(s.getTenantUn());
        }
        s.setLastReceived(new Date());
        session_map.put(s.getClient_openid(), s);
    }
    
    private synchronized void takeClient(Map<String, String> message) {
        String staff_openid = message.get(API.MESSAGE_FROM_TAG);
        String sToken = getAccessToken(message.get(API.MESSAGE_TO_TAG)); //ContextPreloader.Account_Map.get(message.get(API.MESSAGE_TO_TAG)).getToken();
        
        StaffSessionInfo session = activeStaffMap.get(staff_openid);
        if (null == session) {
            String text = "系统消息：您还没有签到，无法使用此功能!";
            log.error("Get session returns null, staff openid: " + staff_openid);
            sendMessage(staff_openid, sToken, text, "text");
            return;
        }
        
        if (session.isBusy()) {
            String text = "系统消息：您正在和客户通话,无法实施该操作.";
            sendMessage(staff_openid, sToken, text, "text");
            return;
        }
        
        String tenentUn = session.getTenantUn();
        if (!mulClientStaffMap.containsKey(tenentUn) || !mulClientStaffMap.get(tenentUn).containsKey(session.getStaff_uuid())) {
            String text = "系统消息：您还没有签到，无法使用此功能!";
            log.error("Get staff returns null, staff openid: " + staff_openid);
            sendMessage(staff_openid, sToken, text, "text");
            return;
        }
        
        if (!waitingList.containsKey(tenentUn)) {
            String text = "系统消息：请求已被其他客服抢接或没有客户发起人工请求.";
            sendMessage(staff_openid, sToken, text, "text");
            return;
        }
        
        Staff staff = mulClientStaffMap.get(tenentUn).get(session.getStaff_uuid());
        Map<String, Queue<WaitingClient>> c_map = waitingList.get(tenentUn);
        WaitingClient client = null;
        for (String skill : staff.getSkills()) {
            if (c_map.containsKey(skill) && !c_map.get(skill).isEmpty()) {
                client = c_map.get(skill).poll();
                break;
            }
        }
        if (null == client) {
            String text = "系统消息：请求已被其他坐席抢接或没有客户发起人工请求.";
            sendMessage(staff_openid, sToken, text, "text");
            return;
        }
        
        Map<String, StaffSessionInfo> client_session = CheckSessionAvailableJob.sessionMap.get(session.getTenantUn());
        if (null == client_session) {
            client_session = new ConcurrentHashMap<String, StaffSessionInfo>();
            CheckSessionAvailableJob.sessionMap.put(tenentUn, client_session);
        }
        
        if (!client_session.containsKey(client.getOpenid())) {
            // Remove client openid from waiting list
            waitingListIDs.remove(client.getOpenid());

            session.setBusy(true);
            session.setLastReceived(new Date());
            session.setClient_openid(client.getOpenid());
            session.setClient_account(client.getAccount());
            session.setClient_name(client.getName());
            session.setClient_type("wx");
            session.setSession(UUID.randomUUID().toString());
            session.setBeginTime(API.TIME_FORMAT.format(new Date()));
            client_session.put(client.getOpenid(), session);
            
            // To staff
            String text = String.format("系统消息：您已经和客户\"%s\"建立通话.", client.getName());
            sendMessage(staff_openid, sToken, text, API.TEXT_MESSAGE);
            // To web staff
            if (session.isWebStaff()) {
                sendWebMessage("sysMessage", text, session.getOpenid(), "", session.getStaff_uuid());
            }
            // To client
            text = String.format("系统消息：会话已建立,客服%s将为您服务[微笑]", session.getStaffid());
            sendMessage(client.getOpenid(), getAccessToken(client.getAccount()), text, API.TEXT_MESSAGE);

        } else {
            String text = "系统消息：系统错误，请联系管理员.";
            sendMessage(staff_openid, sToken, text, "text");
        }
        
    }
    
//    private synchronized void getMessage(Map<String, String> message) {
//        String openid = message.get(API.MESSAGE_FROM_TAG);
//        String account = message.get(API.MESSAGE_TO_TAG);
//        String token = getAccessToken(account); // ContextPreloader.Account_Map.get(account).getToken();
//        
//        StaffSessionInfo s = activeStaffMap.get(openid);
//        String tenantUn = s.getTenantUn();
//        
//        JSONObject leavedMessage = null;
//        if (leavedMessageMap.containsKey(tenantUn)) {
//            leavedMessage = leavedMessageMap.get(tenantUn).poll();
//        }
//
//        if (null != leavedMessage) {
//            log.info("Leaved message: " + leavedMessage.toString());
//            
//            JSONObject accountInfo = getAccountInfo(account, API.REDIS_STAFF_ACCOUNT_INFO_KEY);
//            String appid = accountInfo.getString("appid"); //ContextPreloader.Account_Map.get(account).getAppid();
//            String channel = ContextPreloader.channelMap.get(account);
//            
//            String source = leavedMessage.getString("source");
//            s.setClient_openid(leavedMessage.getString("messager_id"));
//            s.setClient_name(leavedMessage.getString("messager_name"));
//            s.setSession(leavedMessage.getString("message_group_id"));
//            s.setBeginTime(API.TIME_FORMAT.format(new Date()));
//            s.setClient_type(source);
//            
//            if (source.equalsIgnoreCase("wx")) {
//                source = "微信";
//            } else if (source.equalsIgnoreCase("wb")) {
//                source = "微博";
//            } else if (source.equalsIgnoreCase("tb")) {
//                String text = leavedMessage.getString("content");
//                text = text.replace("$%7Bappid%7D", appid);
//                text = text.replace("%24%7Bchannel%7D", channel);
//                sendMessage(openid, token, text, API.TEXT_MESSAGE);
//                return;
//            }
//            String url = String.format("http://www.clouduc.cn/crm/mobile/replymessage/index.php?message_group_id=%s&channel=%s", leavedMessage.getString("message_group_id"), channel);
//            try {
//                String text = String.format("读取到一条来自%s的留言,<a href=\"https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_base&state=123#wechat_redirect\">请查看</a>", source, appid, URLEncoder.encode(url, "utf8"));
//                sendMessage(openid, token, text, API.TEXT_MESSAGE);
//                
//                recordSession(s, 1);
//            } catch (UnsupportedEncodingException e) {
//                log.error("URLEncoder error: " + e.toString() + ", URL = " + url);
//                e.printStackTrace();
//            }
//            
//        } else {
//            String text = "留言已被其他客服提取，或目前没有留言需要处理。";
//            sendMessage(openid, token, text, API.TEXT_MESSAGE);
//        }
//        
//    }
    
    private synchronized void checkClientNum(Map<String, String> message) {
        String staff_openid = message.get(API.MESSAGE_FROM_TAG);
        String sToken = getAccessToken(message.get(API.MESSAGE_TO_TAG)); //ContextPreloader.Account_Map.get(message.get(API.MESSAGE_TO_TAG)).getToken(); 
        String content = String.format("有%d个客户正在等待人工服务", waitingList.size());
        sendMessage(staff_openid, sToken, content, API.TEXT_MESSAGE);
    }
    
    private void getQRCode(Map<String, String> message) {
        String openid = message.get(API.MESSAGE_FROM_TAG);
        String account = message.get(API.MESSAGE_TO_TAG);
        String token = getAccessToken(account); //ContextPreloader.Account_Map.get(account).getToken();
        
        String url = String.format("%s?openid=%s", API.GET_QRCODE_TICKETS_URL, openid);
        String qrcodeList = HttpClientUtil.httpGet(url);
        JSONObject qrcode_ret = (JSONObject) JSONSerializer.toJSON(qrcodeList);
        log.info("Get QRCode requst return: " + qrcode_ret.toString());
        boolean isSuccessed = qrcode_ret.getBoolean("success");
        if (!isSuccessed) {
            log.error("Get QRCode list failed: " + qrcode_ret.toString());
            sendMessage(openid, token, "获取二维码失败,请确定您的账户已经通过审核.", "text");
            return;
        }
        
        JSONArray qrcode_list = qrcode_ret.getJSONArray("tokens");
        JSONArray articles = new JSONArray();
        JSONObject first_article = new JSONObject();
        first_article.put("picurl", "http://hesong.net/images/logo.png");
        first_article.put("title", "和声云客服");
        articles.add(first_article);
        String title = "客服通道";
        for (int i = 1; i <= qrcode_list.size(); i++) {
            JSONObject article = new JSONObject();
            article.put("title", title + i);
            String qrUrl = QRCODE_URL + qrcode_list.getString(i-1);
            article.put("picurl", qrUrl);
            article.put("url", qrUrl);
            articles.add(article);
        }
        
        JSONObject news = new JSONObject();
        news.put("articles", articles);
        
        JSONObject qr_message = new JSONObject();
        qr_message.put("touser", openid);
        qr_message.put("msgtype", "news");
        qr_message.put("news", news);
        qr_message.put("access_token", token);
        try {
            getMessageToSendQueue().put(qr_message);
        } catch (InterruptedException e) {
            log.error("Put message to queue error: "+e.toString());
        }
    }
    
    private void leaveMessage(Map<String, String> message) {
        try {
            String openid = message.get(API.MESSAGE_FROM_TAG);
            String account = message.get(API.MESSAGE_TO_TAG);
            String token = getAccessToken(message.get(API.MESSAGE_TO_TAG)); //ContextPreloader.Account_Map.get(
//                    message.get(API.MESSAGE_TO_TAG)).getToken();
            String tenantUn = message.get("tenantUn");
            
            JSONObject crmRequest = new JSONObject();
            crmRequest.put("method", "convertProspectsToLeads");
            crmRequest.put("session", "");
            crmRequest.put("prospectsOpenid", openid);
            try {
                getSuaRequestToExecuteQueue().put(crmRequest);
            } catch (InterruptedException e) {
                log.error("Put convertProspectsToLeads request to queue failed: " + e.toString());
                e.printStackTrace();
            }
            
            if (CheckLeavingMessageJob.leavingMessageClientList
                    .containsKey(tenantUn) && CheckLeavingMessageJob.leavingMessageClientList.get(tenantUn).containsKey(openid)) {
                String content = "您正在使用留言功能,赶紧发送您的留言吧!";
                sendMessage(openid, token, content, API.TEXT_MESSAGE);
                return;
            }
            
            if (CheckSessionAvailableJob.sessionMap.containsKey(tenantUn)
                    && CheckSessionAvailableJob.sessionMap.get(tenantUn).containsKey(openid)) {
                String content = "您正在和客服对话中，无法使用留言功能[微笑]";
                sendMessage(openid, token, content, API.TEXT_MESSAGE);
                return;
            }
            String content = "欢迎使用在线留言服务,客服MM将会在第一时间处理您的留言。请您在五分钟内完成留言,发送END(大小写不限)结束留言,否则系统将在五分钟后自动结束您的留言请求!";
            sendMessage(openid, token, content, API.TEXT_MESSAGE);

            String url = USER_INFO_URL.replace("ACCESS_TOKEN", token).replace(
                    "OPENID", openid);
            JSONObject user_info = WeChatHttpsUtil.httpsRequest(url, "GET",
                    null);
            if (user_info.containsKey("errcode")) {
                log.error("Get client info failed: " + user_info.toString());
            } else {
                String client_name = user_info.getString("nickname");
                String headimgurl = user_info.getString("headimgurl");
                headimgurl = headimgurl.substring(0,headimgurl.length()-1) + 46;
                LeavingMessageClient c = new LeavingMessageClient(account,
                        openid, client_name, headimgurl, "wx");
                Map<String, LeavingMessageClient> c_map = CheckLeavingMessageJob.leavingMessageClientList.get(tenantUn);
                if (c_map == null) {
                    c_map = new HashMap<String, LeavingMessageClient>();
                    CheckLeavingMessageJob.leavingMessageClientList.put(tenantUn, c_map);
                }
                c_map.put(openid, c);
            }
        } catch (Exception e) {
            log.error("Request to leave message failed: " + e.toString());
            e.printStackTrace();
        }
    }
    
    private void expressMessage(Map<String, String> message, String text) {
        try {
            String openid = message.get(API.MESSAGE_FROM_TAG);
            String sToken = getAccessToken(message.get(API.MESSAGE_TO_TAG)); //ContextPreloader.Account_Map.get(message.get(API.MESSAGE_TO_TAG)).getToken();
            
            StaffSessionInfo s = activeStaffMap.get(openid);
            if (null != s && s.isBusy()) {
                if (s.getClient_type().equalsIgnoreCase("wx")) {
                    String token = getAccessToken(s.getClient_account());
//                            ContextPreloader.Account_Map.get(
//                            s.getClient_account()).getToken();
                    sendMessage(s.getClient_openid(), token, text, API.TEXT_MESSAGE);
                    sendMessage(openid, sToken, text, API.TEXT_MESSAGE);
                    s.setLastReceived(new Date());
                }
                
                if (s.getClient_type().equalsIgnoreCase("wb")) {
                    JSONObject weibo_request = new JSONObject();
                    weibo_request.put("user_id", s.getClient_openid());
                    weibo_request.put("tenantUn", s.getTenantUn());
                    weibo_request.put("msgtype", API.TEXT_MESSAGE);
                    weibo_request.put("content", text);
                    
                    JSONObject weibo_ret = WeChatHttpsUtil.httpPostRequest(API.WEIBO_SEND_MESSAGE_URL, weibo_request.toString(), 0);
                    log.info("Send weibo message return: "+ weibo_ret.toString());
                    sendMessage(openid, sToken, text, API.TEXT_MESSAGE);
                    s.setLastReceived(new Date());
                }
            } else {
                String warning = "系统消息：目前没有和客户建立连接，无法使用常用语。";
                sendMessage(openid, sToken, warning, API.TEXT_MESSAGE);
            }
        } catch (Exception e) {
            log.error("Request to leave message failed: " + e.toString());
            e.printStackTrace();
        }
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
        } else if (type.equals("video")) {
            content.put("media_id", text);
        }
        message.put(type, content);
        message.put("access_token", token);
        try {
            MessageExecutor.messageToSendQueue.put(message);
        } catch (InterruptedException e) {
            log.error("Put message to queue error: "+e.toString());
        }
    }
    
    public static void sendWebMessage(String msgtype, String content, String channelId, String sender, String staff_uuid) {
        JSONObject json = new JSONObject();
        json.put("msgtype", msgtype);
        json.put("content", content);
        json.put("channelId", channelId);
        json.put("senderName", sender);
        String url = String.format("http://localhost:8080/weixinAPI/webchat/%s/sendWebMessage", staff_uuid);
        WeChatHttpsUtil.httpPostRequest(url, json.toString(), 0);
    }
    
    private void recordMessage(StaffSessionInfo s, Map<String, String> message, String type, String source, boolean isClient) {
        JSONObject messageToRecord = new JSONObject();
        messageToRecord.put("session_id", s.getSession());
        messageToRecord.put("content", message.get(API.MESSAGE_CONTENT_TAG));
        messageToRecord.put("message_type", type);
        messageToRecord.put("message_source", source);
        messageToRecord.put("tenant_code", s.getTenantUn());
        messageToRecord.put("time", API.TIME_FORMAT.format(new Date()));
        if (isClient) {
            messageToRecord.put("sender_openid", s.getClient_openid());
            messageToRecord.put("sender_name", s.getClient_name());
            messageToRecord.put("sender_type", "client");
            messageToRecord.put("sender_public_account", s.getClient_account());
            messageToRecord.put("receiver_openid", s.getOpenid());
            messageToRecord.put("receiver_name", s.getName());
            messageToRecord.put("receiver_type", "staff");
            messageToRecord.put("receiver_public_account", s.getAccount());
        } else {
            messageToRecord.put("sender_openid", s.getOpenid());
            messageToRecord.put("sender_name", s.getName());
            messageToRecord.put("sender_type", "staff");
            messageToRecord.put("sender_public_account", s.getAccount());
            messageToRecord.put("receiver_openid", s.getClient_openid());
            messageToRecord.put("receiver_name", s.getClient_name());
            messageToRecord.put("receiver_type", "client");
            messageToRecord.put("receiver_public_account", s.getClient_account());
        }
        
        JSONObject json_request = new JSONObject();
        json_request.put("session", "hold");
//        json_request.put("module_name", "save_chat_history");
        json_request.put("name_value_list", messageToRecord.toString());
        json_request.put("method", "saveChatHistory");
        
        try {
            getSuaRequestToExecuteQueue().put(json_request);
        } catch (InterruptedException e) {
            log.error("Put message to record queue failed: " + e.toString());
            e.printStackTrace();
        }
    }
    
    private void recordLeaveMessage(Map<String, String> message) {
        String openid = message.get(API.MESSAGE_FROM_TAG);
        String account = message.get(API.MESSAGE_TO_TAG);
        String type = message.get(API.MESSAGE_TYPE_TAG);
        String content = message.get(API.MESSAGE_CONTENT_TAG);
        String tenantUn = message.get("tenantUn");
        if (type.equalsIgnoreCase(API.TEXT_MESSAGE)) {
            if (content.equalsIgnoreCase("end")) {
                // End of leaving message
                CheckLeavingMessageJob.leavingMessageClientList.get(tenantUn).remove(openid);
                String text = "感谢您使用在线留言服务,客服MM将在第一时间回复您的消息!";
                String token = getAccessToken(account); //ContextPreloader.Account_Map.get(account).getToken();
                sendMessage(openid, token, text, API.TEXT_MESSAGE);
                return;
            }
        } else {
            // Only recording text message for now
            return;
        }
        
        LeavingMessageClient c = CheckLeavingMessageJob.leavingMessageClientList.get(tenantUn).get(openid);
        if (null == c) {
            log.error("LeavingMessageClient dose not exist.");
        }
        JSONObject messageToLeave = new JSONObject();
        messageToLeave.put("tenant_code", tenantUn);
        messageToLeave.put("messager_id", openid);
        messageToLeave.put("messager_name", c.getName());
        messageToLeave.put("messager_photo", c.getHeadimgurl());
        messageToLeave.put("messager_public_account", account);
        messageToLeave.put("content", content);
        messageToLeave.put("time", API.TIME_FORMAT.format(new Date()));
        messageToLeave.put("message_status", "0");
        messageToLeave.put("type", type);
        messageToLeave.put("source", "wx");
        messageToLeave.put("message_group_id", c.getUuid());
        
        JSONObject json_request = new JSONObject();
        json_request.put("session", "hold");
        json_request.put("name_value_list", messageToLeave.toString());
        json_request.put("method", "saveChatMessage");
        
        try {
            getSuaRequestToExecuteQueue().put(json_request);
        } catch (InterruptedException e) {
            log.error("Put leaving message to record queue failed: " + e.toString());
            e.printStackTrace();
        }
    }
    
    public static void recordSession(StaffSessionInfo s, int service_type) {
        JSONObject messageToRecord = new JSONObject();
        messageToRecord.put("session_id", s.getSession());
        messageToRecord.put("staff_openid", s.getOpenid());
        messageToRecord.put("staff_name", s.getName());
        messageToRecord.put("client_openid", s.getClient_openid());
        messageToRecord.put("client_name", s.getClient_name());
        messageToRecord.put("tenant_code", s.getTenantUn());
        messageToRecord.put("start_time", s.getBeginTime());
        messageToRecord.put("end_time", s.getEndTime());
        messageToRecord.put("service_type", service_type);
        messageToRecord.put("source", s.getClient_type());

        JSONObject json_request = new JSONObject();
        json_request.put("session", "hold");
        json_request.put("name_value_list", messageToRecord.toString());
        json_request.put("method", "saveStaffServiceRecord");

        try {
            suaRequestToExecuteQueue.put(json_request);
        } catch (InterruptedException e) {
            log.error("Put message to record queue failed: " + e.toString());
            e.printStackTrace();
        }
    }
    
    public static void newMessageRemaind(String tenantUn) {
        if (mulClientStaffMap.containsKey(tenantUn)) {
            Map<String, Staff> staff_map = mulClientStaffMap.get(tenantUn);
            for (String staff_uuif : staff_map.keySet()) {
                Staff staff = staff_map.get(staff_uuif);
                StaffSessionInfo session = staff.getSessionChannelList().get(0);
                String account = session.getAccount();
                JSONObject accountInfo = MessageRouter.getAccountInfo(
                        account, API.REDIS_STAFF_ACCOUNT_INFO_KEY);
                String url = String
                        .format(API.GET_LEAVED_MESSAGE_URL,
                                ContextPreloader.channelMap
                                        .get(account));
                String text = null;
                try {
                    text = String
                            .format("系统消息:有新的留言,<a href=\"https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_base&state=123#wechat_redirect\">点击查看留言</a>",
                                    accountInfo.getString("appid"),
                                    URLEncoder.encode(url, "utf8"));
                    sendMessage(session.getOpenid(), getAccessToken(account), text, API.TEXT_MESSAGE);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private static String getMatchedWord(String regex, String text) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1);
        } else {
            return null;
        }
    }
    
    public static String getAccessToken(String account) {
        Jedis jedis = ContextPreloader.jedisPool.getResource();
        String token = jedis.hget(API.REDIS_WEIXIN_ACCESS_TOKEN_KEY, account);
        ContextPreloader.jedisPool.returnResource(jedis);
        return token;
    }
    
    public static String getAccessToken(String account, Jedis jedis) {
        String token = jedis.hget(API.REDIS_WEIXIN_ACCESS_TOKEN_KEY, account);
        return token;
    }
    
    public static JSONObject getAccountInfo(String account, String redisKey) {
        Jedis jedis = ContextPreloader.jedisPool.getResource();
        String info = jedis.hget(redisKey, account);
        ContextPreloader.jedisPool.returnResource(jedis);
        return JSONObject.fromObject(info);
    }
    
    public MessageRouter(BlockingQueue<Map<String, String>> messageQueue,
            BlockingQueue<JSONObject> messageToSendQueue,
            BlockingQueue<JSONObject> suaRequestToExecuteQueue) {
        super();
        this.messageQueue = messageQueue;
        this.messageToSendQueue = messageToSendQueue;
        MessageRouter.suaRequestToExecuteQueue = suaRequestToExecuteQueue;
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

    public BlockingQueue<JSONObject> getSuaRequestToExecuteQueue() {
        return suaRequestToExecuteQueue;
    }

}
