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

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.log4j.Logger;

import com.hesong.sugarCRM.HttpClientUtil;
import com.hesong.sugarCRM.SugarCRMCaller;
import com.hesong.weixinAPI.context.ContextPreloader;
import com.hesong.weixinAPI.job.CheckLeavingMessageJob;
import com.hesong.weixinAPI.job.CheckSessionAvailableJob;
import com.hesong.weixinAPI.job.CheckWeiboSessionAvailableJob;
import com.hesong.weixinAPI.model.AccessToken;
import com.hesong.weixinAPI.model.LeavingMessageClient;
import com.hesong.weixinAPI.model.Staff;
import com.hesong.weixinAPI.model.StaffSessionInfo;
import com.hesong.weixinAPI.model.WaitingClient;
import com.hesong.weixinAPI.tools.API;
import com.hesong.weixinAPI.tools.WeChatHttpsUtil;

public class MessageRouter implements Runnable {

    private static Logger log = Logger.getLogger(MessageRouter.class);
    
    private static String USER_INFO_URL = "https://api.weixin.qq.com/cgi-bin/user/info?access_token=ACCESS_TOKEN&openid=OPENID&lang=zh_CN";
    private static String QRCODE_URL = "https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=";
    private static String GET_QRCODE_TICKETS_URL = "http://www.clouduc.cn/sua/rest/n/tenant/codetokens";
    private static String CHECKIN_URL = "http://www.clouduc.cn/sua/rest/n/tenant/kfCheckInInfo?openid=";
    
    private BlockingQueue<Map<String, String>> messageQueue;
    private BlockingQueue<JSONObject> messageToSendQueue;
    private BlockingQueue<JSONObject> suaRequestToExecuteQueue;
    
    /**
     * 租户的坐席表
     * <tenantUn, <staff_uuid, staff>>
     */
    public static Map<String, Map<String, Staff>> mulClientStaffMap = new HashMap<String, Map<String, Staff>>();
    public static Map<String, StaffSessionInfo> activeStaffMap = new HashMap<String, StaffSessionInfo>();   // <Staff openid, StaffSessionInfo>
    
    /**
     * 已发起人工服务请求并处于等待状态的客户排队列表
     */
    public static Queue<WaitingClient> waitingList = new LinkedList<WaitingClient>();
    
    /**
     * 排队中的客户openid集合
     */
    public static Set<String>  waitingListIDs= new HashSet<String>();
    public static Map<String, JSONObject> staffIdList = new HashMap<String, JSONObject>();
    public static Map<String, Queue<JSONObject>> leavedMessageMap = new HashMap<String, Queue<JSONObject>>();  // <tenantUn, Queue>
    
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
                        case "STAFF_SERVICE":  // 人工服务
                            staffService(message);
                            break;
                        case "END_SESSION":    // 结束对话
                            endSession(message);
                            break;
                        case "TAKE_CLIENT":
                            takeClient(message);  // 抢接
                            break;
                        case "GET_MESSAGE":
                            getMessage(message);  // 获取留言
                            break;
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
        if (CheckLeavingMessageJob.leavingMessageClientList.containsKey(tenantUn) && CheckLeavingMessageJob.leavingMessageClientList.get(tenantUn).containsKey(from_openid)) {
            recordLeaveMessage(message);
            return;
        }
        
        if (!ContextPreloader.staffAccountList.contains(to_account)) {
            // Message from client
            if (CheckSessionAvailableJob.clientMap.containsKey(from_openid)) {
                StaffSessionInfo s = CheckSessionAvailableJob.sessionMap.get(from_openid);
                if (s != null && s.isBusy()) {
                    String sToken = ContextPreloader.Account_Map.get(s.getAccount()).getToken();
                    String content = String.format("%s: %s", s.getClient_name(), message.get(API.MESSAGE_CONTENT_TAG));
                    sendMessage(s.getOpenid(), sToken, content, API.TEXT_MESSAGE);
                    s.setLastReceived(new Date());
                    recordMessage(s, message, API.TEXT_MESSAGE, "wx", true);
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
                String cToken = ContextPreloader.Account_Map.get(s.getClient_account()).getToken();
                
                sendMessage(s.getClient_openid(), cToken, content, API.TEXT_MESSAGE);
                s.setLastReceived(new Date());
                String message_source = s.getClient_type();
                recordMessage(s, message, API.TEXT_MESSAGE, message_source, false);
                return;
            }
        }
        
        log.info("This is not an available session.");
//        String token = ContextPreloader.Account_Map.get(to_account).getToken();
//        String content = "欢迎使用和声云服务,您可以使用人工服务与客服MM对话[微笑]";
//        sendMessage(from_openid, token, content, API.TEXT_MESSAGE);
    }
    
    private void imageMessage(Map<String, String> message) {
        String img_account = message.get(API.MESSAGE_TO_TAG);
        String img_Token = ContextPreloader.Account_Map.get(img_account).getToken();
        String img_openid = message.get(API.MESSAGE_FROM_TAG);
        String img_toToken = "";
        String img_to_openid = "";
        if (!ContextPreloader.staffAccountList.contains(img_account)) {
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
        String img_id = getMediaId(message.get(API.MESSAGE_MEDIA_ID_TAG), img_Token, img_toToken, "image", API.CONTENT_TYPE_IMAGE);
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
        if (!ContextPreloader.staffAccountList.contains(voice_account)) {
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
        String voice_id = getMediaId(message.get(API.MESSAGE_MEDIA_ID_TAG), voice_Token, voice_toToken, "voice", API.CONTENT_TYPE_VOICE);
        if (voice_id.equals("error")) {
            sendMessage(voice_openid, voice_Token, "系统消息:发送语音消息失败", API.TEXT_MESSAGE);
            return;
        }
        sendMessage(voice_to_openid, voice_toToken, voice_id, API.VOICE_MESSAGE);
    }
    
    private void videoMessage(Map<String, String> message) {
        String video_account = message.get(API.MESSAGE_TO_TAG);
        String video_openid = message.get(API.MESSAGE_FROM_TAG);
        String video_Token = ContextPreloader.Account_Map.get(video_account).getToken();
        String video_toToken = "";
        String video_to_openid = "";
        if (!ContextPreloader.staffAccountList.contains(video_account)) {
            // Message from client
            if (CheckSessionAvailableJob.clientMap.containsKey(video_openid)) {
                StaffSessionInfo s = CheckSessionAvailableJob.sessionMap.get(video_openid);
                video_toToken = ContextPreloader.Account_Map.get(s.getAccount()).getToken();
                video_to_openid = s.getOpenid();
                s.setLastReceived(new Date());
            }
        } else {
            // Message from staff
            StaffSessionInfo s = activeStaffMap.get(video_openid);
            if (s != null && s.isBusy()) {
                video_toToken = ContextPreloader.Account_Map.get(s.getClient_account()).getToken();
                video_to_openid = s.getClient_openid();
                s.setLastReceived(new Date());
            }
        }
        String voice_id = getMediaId(message.get(API.MESSAGE_MEDIA_ID_TAG), video_Token, video_toToken, "video", API.CONTENT_TYPE_VIDEO);
        if (voice_id.equals("error")) {
            sendMessage(video_openid, video_Token, "系统消息:发送视频消息失败", API.TEXT_MESSAGE);
            return;
        }
        sendMessage(video_to_openid, video_toToken, voice_id, API.VIDEO_MESSAGE);
    }
    
    private void subscribe(Map<String, String> message) throws InterruptedException {
        String account = message.get(API.MESSAGE_TO_TAG);
        if (ContextPreloader.staffAccountList.contains(account)) {
            // Staff subscribe
            String toToken = ContextPreloader.Account_Map.get(account).getToken();
            String openid =  message.get(API.MESSAGE_FROM_TAG);
            // String content = "欢迎使用和声云，<a href=\"https://open.weixin.qq.com/connect/oauth2/authorize?appid=wx735e58e85eb3614a&redirect_uri=http://www.clouduc.cn/crm/mobile/auth/index.php&response_type=code&scope=snsapi_base&state=123#wechat_redirect\">点击绑定</a>获得CRM账户[微笑]";
            
            if (message.containsKey(API.MESSAGE_EVENT_KEY_TAG)) {
                String qrscene = message.get(API.MESSAGE_EVENT_KEY_TAG);
                String scene_id = qrscene.replace("qrscene_", "");
                log.info("qrscene is: " + qrscene + " id = " + scene_id);
                // Send to CRM
                String url = USER_INFO_URL.replace("ACCESS_TOKEN", toToken).replace("OPENID", openid);
                JSONObject user_info = WeChatHttpsUtil.httpsRequest(url, "GET", null);
                if (user_info.containsKey("errcode")) {
                    log.error("Get client info failed: "+user_info.toString());
                } else {
                    log.info("Client info: " + user_info.toString());
                    try {
//                        String sua_url = String.format("http://www.clouduc.cn/sua/rest/n/kf/register?userinfo=%s&tenantid=%s&weixin_public_id=%s", user_info.toString(), scene_id, account);
                        String sua_url = "http://www.clouduc.cn/sua/rest/n/kf/register";

                        Map<String, String> params = new HashMap<String, String>();
                        params.put("userinfo", user_info.toString());
                        params.put("tenantid", scene_id);
                        params.put("weixin_public_id", account);
                        JSONObject ret = (JSONObject) JSONSerializer.toJSON(HttpClientUtil.httpPost(sua_url, params));
                        log.info("Staff register: "+ ret.toString());
                        
                        if (!ret.getBoolean("success")) {
                            // Staff already registed in CRM
                            return;
                        }
                        String content = "欢迎使用和声云客服,系统正在为您绑定,请耐心等待...[微笑]";
                        sendMessage(openid, toToken, content, API.TEXT_MESSAGE);
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error("CRM registe failed: "+e.toString());
                        
                        // TODO Send error message to client
                    }
                    
                }
            }
        } else {
            String toToken = ContextPreloader.Account_Map.get(account).getToken();
            String openid = message.get(API.MESSAGE_FROM_TAG);
            

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
            } else {
                String text = "感谢您关注我们的服务号，我们将为您提供最优质的服务[微笑]";
                sendMessage(openid, toToken, text, API.TEXT_MESSAGE);
            }
            
            // Insert client info to SugerCRM
            String url = USER_INFO_URL.replace("ACCESS_TOKEN", toToken).replace("OPENID", openid);
            JSONObject user_info = WeChatHttpsUtil.httpsRequest(url, "GET", null);
            String tenantUn = message.get("tenantUn");
            // TODO 
            if (null == tenantUn) {
                tenantUn = "null";
            }
            if (user_info.containsKey("errcode")) {
                log.error("Get client info failed: "+user_info.toString());
            } else {
                log.info("Client info: " + user_info.toString());
                user_info.put("tenant_code", tenantUn);
                user_info.put("source", "wx");
                SugarCRMCaller crmCaller = new SugarCRMCaller();
                
                if (!crmCaller.check_oauth(SUAExecutor.session)) {
                    SUAExecutor.session = crmCaller.login("admin",
                            "p@ssw0rd");
                }
                String session = SUAExecutor.session;
                
                if (!crmCaller.isOpenidBinded(session, openid)) {
                    String insert_recall = crmCaller
                            .insertToCRM_Prospects(session,
                                    user_info);
                    log.info("insert_recall: " + insert_recall);
                }
            }
            
        }
    }
    
    private void unsubscribe(Map<String, String> message){
        String account = message.get(API.MESSAGE_TO_TAG);
        if (ContextPreloader.staffAccountList.contains(account)) {
            String openid = message.get(API.MESSAGE_FROM_TAG);
            String r = HttpClientUtil.httpGet(API.SUA_DEL_STAFF_URL + openid);
            JSONObject rj = JSONObject.fromObject(r);
            log.info("Staff unsubscribe return: " + rj.toString());
        }
        // TODO remove active session
        
    }
    
    private void scan(Map<String, String> message) {
        String openid = message.get(API.MESSAGE_FROM_TAG);
        String toToken = ContextPreloader.Account_Map.get(
                message.get(API.MESSAGE_TO_TAG)).getToken();
        String content = "亲,您已经关注了该公众号!";
        sendMessage(openid, toToken, content, API.TEXT_MESSAGE);
    }
    
    private void clientInfo(Map<String, String> message) {
        String openid = message.get(API.MESSAGE_FROM_TAG);
        StaffSessionInfo s = activeStaffMap.get(openid);
        String account = message.get(API.MESSAGE_TO_TAG);
        AccessToken ac = ContextPreloader.Account_Map.get(account);
        if (s != null && s.isBusy()) {
            String url = String.format("http://www.clouduc.cn/crm/mobile/weixin/prospectsDetail.php?kh_weixin_openid=%s&channel=%s", s.getClient_openid(), ContextPreloader.channelMap.get(account));
            try {
                String text = String.format("<a href=\"https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_base&state=123#wechat_redirect\">请查看</a>", ac.getAppid(), URLEncoder.encode(url, "utf8"));
                sendMessage(openid, ac.getToken(), text, API.TEXT_MESSAGE);
            } catch (UnsupportedEncodingException e) {
                log.error("URLEncoder error: " + e.toString() + ", URL = " + url);
                e.printStackTrace();
            }
        } else {
            String text = "目前没有和客户建立连接，无法查看详情.";
            sendMessage(openid, ac.getToken(), text, API.TEXT_MESSAGE);
        }
    }
    
    private void getChatHistory(Map<String, String> message) {
        String openid = message.get(API.MESSAGE_FROM_TAG);
        StaffSessionInfo s = activeStaffMap.get(openid);
        String account = message.get(API.MESSAGE_TO_TAG);
        AccessToken ac = ContextPreloader.Account_Map.get(account);
        String url = null;
        if (s != null && s.isBusy()) {
            url = String
                    .format("http://www.clouduc.cn/crm/mobile/chathistory/chathistory.php?client_openid=%s&staff_openid=%s&channel=%s",
                            s.getClient_openid(), openid,
                            ContextPreloader.channelMap.get(account));
        } else {
            url = String
                    .format("http://www.clouduc.cn/crm/mobile/chathistory/chathistorylist.php?staff_openid=%s&channel=%s",
                            openid, ContextPreloader.channelMap.get(account));
        }
        try {
            String text = String
                    .format("<a href=\"https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_base&state=123#wechat_redirect\">请点击查看历史记录</a>",
                            ac.getAppid(), URLEncoder.encode(url, "utf8"));
            sendMessage(openid, ac.getToken(), text, API.TEXT_MESSAGE);
        } catch (UnsupportedEncodingException e) {
            log.error("URLEncoder error: " + e.toString() + ", URL = " + url);
            e.printStackTrace();
        }
    }
    
    private void checkin(Map<String, String> message) {
        String openid = message.get(API.MESSAGE_FROM_TAG);
        String account = message.get(API.MESSAGE_TO_TAG);
        String url = CHECKIN_URL + openid;
        String token = ContextPreloader.Account_Map.get(account).getToken();
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
                List<StaffSessionInfo> sessionChannelList = new ArrayList<StaffSessionInfo>();
                for (int i = 0; i < channel_list.size(); i++) {
                    JSONObject channel = channel_list.getJSONObject(i);
                    String staff_account = channel.getString("account");
                    String staff_openid = channel.getString("openid");
                    StaffSessionInfo s = new StaffSessionInfo(tenantUn, staff_account, staff_openid, staff_working_num, staff_name);
                    MessageRouter.activeStaffMap.put(staff_openid, s);
                    sessionChannelList.add(s);
                    JSONObject staff_account_id = new JSONObject();
                    staff_account_id.put("wx_account", staff_account);
                    staff_account_id.put("staffid", staff_uuid);
                    staff_account_id.put("tenantUn", tenantUn);
                    staffIdList.put(staff_openid, staff_account_id);

                }
                log.info("staffIdList: "+MessageRouter.staffIdList.toString());
                Staff staff = new Staff(staff_uuid, staff_name, tenantUn, wx_account, staff_working_num, sessionChannelList);
                staff_map.put(staff_uuid, staff);
                log.info("Staff checked in: " + staff.toString());
                String text = String.format("系统提示:签到成功,你的工号是%s.", staff_working_num);
                MessageRouter.sendMessage(openid, token, text, API.TEXT_MESSAGE);
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
            for (StaffSessionInfo s : sessionList) {
                if (s.isBusy()) {
                    s.setBusy(false);
                    
                    if (s.getClient_type().equalsIgnoreCase("wx")) {
                     // Remaind client that staff is leaving.
                        String token = ContextPreloader.Account_Map.get(s.getClient_account()).getToken();
                        String text = "对不起，客服MM有急事下线了,会话已结束。您可以使用留言功能,客服MM将会在第一时间给您回复[微笑]";
                        sendMessage(s.getClient_openid(), token, text, "text");
                        // TODO remove session from CheckLeavingMessageJob.leavingMessageClientList
                        CheckSessionAvailableJob.sessionMap.remove(s.getClient_openid());
                        CheckSessionAvailableJob.clientMap.remove(s.getClient_openid());
                    }
                    
                    if (CheckWeiboSessionAvailableJob.weiboSessionMap.containsKey(tenantUn) && 
                            CheckWeiboSessionAvailableJob.weiboSessionMap.get(tenantUn).containsKey(s.getClient_openid())) {
                        CheckWeiboSessionAvailableJob.weiboSessionMap.get(tenantUn).remove(s.getClient_openid());
                    }
                }
                String token = ContextPreloader.Account_Map.get(s.getAccount()).getToken();
                sendMessage(s.getOpenid(), token, "系统消息:您已成功签出!", API.TEXT_MESSAGE);
                activeStaffMap.remove(s.getOpenid());
                
            }
            staff_map.remove(staff_id);
            log.info("staff_map after remove: "+staff_map.toString());
            log.info("mulClientStaffMap after remove: "+mulClientStaffMap.get(tenantUn).toString());
            staffIdList.remove(openid);
            if (staff_map.isEmpty()) {
                mulClientStaffMap.remove(tenantUn);
            }
        }
    }
    
    private void staffService(Map<String, String> message) {
        String client_openid = message.get(API.MESSAGE_FROM_TAG);
        String client_account = message.get(API.MESSAGE_TO_TAG);
        String tenantUn = message.get("tenantUn");
        String cToken = ContextPreloader.Account_Map.get(client_account).getToken();
        
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
        
        if (CheckSessionAvailableJob.clientMap.containsKey(client_openid)) {
            sendMessage(client_openid, cToken, "您已经接通人工服务!", API.TEXT_MESSAGE);
            return;
        }
        String content = "正在为您接通人工服务,请稍等...";
        if (null == tenantUn || !mulClientStaffMap.containsKey(tenantUn) || mulClientStaffMap.get(tenantUn).isEmpty()) {
            content = "暂时没有空闲客服,请稍后再试!您可以使用留言功能，客服MM将第一时间给您回复[微笑]";
            sendMessage(client_openid, cToken, content, API.TEXT_MESSAGE);
            return;
        }
        
        if (waitingListIDs.contains(client_openid)) {
            content = "您已经发出人工服务请求,我们将尽快为您接通,请耐心等候!";
            sendMessage(client_openid, cToken, content, API.TEXT_MESSAGE);
            return;
        }
        
        sendMessage(client_openid, cToken, content, API.TEXT_MESSAGE);
        
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
        
        WaitingClient c = new WaitingClient(tenantUn, client_openid, client_account, client_name);
        waitingList.offer(c);
        waitingListIDs.add(client_openid);
        
        // Broadcast client request to all staffs
        boolean isAllBusy = true;
        for (Staff staff : mulClientStaffMap.get(tenantUn).values()) {
            for (StaffSessionInfo s : staff.getSessionChannelList()) {
                if (!s.isBusy()) {
                    isAllBusy = false;
                    int num = waitingList.size();
                    //String url = String.format("客户:%s,寻求人工对话服务,<a href=\"http://www.clouduc.cn/wx/staff/takeSession?clientid=%s&account=%s&clientname=%s&staffid=%s\">点击抢单接入会话</a>", client_name, client_openid,client_account, client_name, s.getOpenid());
                    String text = String.format("有%d个客户寻求人工服务,请点击抢单按钮接通会话.", num);
                    String sToken = ContextPreloader.Account_Map.get(s.getAccount()).getToken();
                    sendMessage(s.getOpenid(), sToken, text, API.TEXT_MESSAGE);
                    break; // Only one message for each staff
                }
            }
        }
        
        if (isAllBusy) {
            String text = "暂时没有空闲客服,请稍后再试!您可以使用留言功能，客服MM将第一时间给您回复[微笑]";
            sendMessage(client_openid, cToken, text, API.TEXT_MESSAGE);
            return;
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
    
    private synchronized void takeClient(Map<String, String> message) {
        String staff_openid = message.get(API.MESSAGE_FROM_TAG);
        String sToken = ContextPreloader.Account_Map.get(message.get(API.MESSAGE_TO_TAG)).getToken();
        
        WaitingClient c = waitingList.poll();
        String text = "";
        
        if (c != null && CheckSessionAvailableJob.clientMap.get(c.getOpenid()) == null) {
            CheckSessionAvailableJob.clientMap.put(c.getOpenid(), c.getAccount());
            StaffSessionInfo s = MessageRouter.activeStaffMap.get(staff_openid);
            if (s == null) {
                text = "系统错误";
                log.error("Get staff returns null, staff openid: " + staff_openid);
                sendMessage(staff_openid, sToken, text, "text");
                return;
            }
            if (s.isBusy()) {
                text = "您正在和客户通话,无法实施该操作.";
                sendMessage(staff_openid, sToken, text, "text");
                return;
            }
            
            // Remove client openid from waiting list
            waitingListIDs.remove(c.getOpenid());

            s.setBusy(true);
            s.setLastReceived(new Date());
            s.setClient_openid(c.getOpenid());
            s.setClient_account(c.getAccount());
            s.setClient_name(c.getName());
            s.setClient_type("wx");
            s.setSession(UUID.randomUUID().toString());
            CheckSessionAvailableJob.sessionMap.put(c.getOpenid(), s);
            
            // To staff
            text = String.format("您已经和客户:%s 建立通话.", c.getName());
            sendMessage(staff_openid, sToken, text, API.TEXT_MESSAGE);
            // To client
            text = String.format("会话已建立,客服%s将为您服务[微笑]", s.getStaffid());
            sendMessage(c.getOpenid(), ContextPreloader.Account_Map.get(c.getAccount()).getToken(), text, API.TEXT_MESSAGE);
        } else {
            text = "请求已被其他坐席抢接或没有客户发起人工请求.";
            sendMessage(staff_openid, sToken, text, "text");
        }
        
    }
    
    private synchronized void getMessage(Map<String, String> message) {
        String openid = message.get(API.MESSAGE_FROM_TAG);
        String account = message.get(API.MESSAGE_TO_TAG);
        String token = ContextPreloader.Account_Map.get(account).getToken();
        
        StaffSessionInfo s = activeStaffMap.get(openid);
        String tenantUn = s.getTenantUn();
        
        JSONObject leavedMessage = null;
        if (leavedMessageMap.containsKey(tenantUn)) {
            leavedMessage = leavedMessageMap.get(tenantUn).poll();
        }

        if (null != leavedMessage) {
            log.info("Leaved message: " + leavedMessage.toString());
            String appid = ContextPreloader.Account_Map.get(account).getAppid();
            String channel = ContextPreloader.channelMap.get(account);
            
            String source = leavedMessage.getString("source");
            if (source.equalsIgnoreCase("wx")) {
                source = "微信";
            } else if (source.equalsIgnoreCase("wb")) {
                source = "微博";
            } else if (source.equalsIgnoreCase("tb")) {
                String text = leavedMessage.getString("content");
                text = text.replace("$%7Bappid%7D", appid);
                text = text.replace("%24%7Bchannel%7D", channel);
                sendMessage(openid, token, text, API.TEXT_MESSAGE);
                return;
            }
            String url = String.format("http://www.clouduc.cn/crm/mobile/replymessage/index.php?message_group_id=%s&channel=%s", leavedMessage.getString("message_group_id"), channel);
            try {
                String text = String.format("读取到一条来自%s的留言,<a href=\"https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_base&state=123#wechat_redirect\">请查看</a>", source, appid, URLEncoder.encode(url, "utf8"));
                sendMessage(openid, token, text, API.TEXT_MESSAGE);
            } catch (UnsupportedEncodingException e) {
                log.error("URLEncoder error: " + e.toString() + ", URL = " + url);
                e.printStackTrace();
            }
            
        } else {
            String text = "留言已被其他客服提取，或目前没有留言需要处理。";
            sendMessage(openid, token, text, API.TEXT_MESSAGE);
        }
        
    }
    
    private synchronized void checkClientNum(Map<String, String> message) {
        String staff_openid = message.get(API.MESSAGE_FROM_TAG);
        String sToken = ContextPreloader.Account_Map.get(message.get(API.MESSAGE_TO_TAG)).getToken(); 
        String content = String.format("有%d个客户正在等待人工服务", waitingList.size());
        sendMessage(staff_openid, sToken, content, API.TEXT_MESSAGE);
    }
    
    private void getQRCode(Map<String, String> message) {
        String openid = message.get(API.MESSAGE_FROM_TAG);
        String account = message.get(API.MESSAGE_TO_TAG);
        String token = ContextPreloader.Account_Map.get(account).getToken();
        
        String url = String.format("%s?openid=%s", GET_QRCODE_TICKETS_URL, openid);
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
            String token = ContextPreloader.Account_Map.get(
                    message.get(API.MESSAGE_TO_TAG)).getToken();
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
            
            if (CheckSessionAvailableJob.sessionMap.containsKey(openid)) {
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
            String sToken = ContextPreloader.Account_Map.get(message.get(API.MESSAGE_TO_TAG)).getToken();
            
            StaffSessionInfo s = activeStaffMap.get(openid);
            if (null != s && s.isBusy()) {
                if (s.getClient_type().equalsIgnoreCase("wx")) {
                    String token = ContextPreloader.Account_Map.get(
                            s.getClient_account()).getToken();
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
                String token = ContextPreloader.Account_Map.get(account).getToken();
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
    
    public MessageRouter(BlockingQueue<Map<String, String>> messageQueue,
            BlockingQueue<JSONObject> messageToSendQueue,
            BlockingQueue<JSONObject> suaRequestToExecuteQueue) {
        super();
        this.messageQueue = messageQueue;
        this.messageToSendQueue = messageToSendQueue;
        this.suaRequestToExecuteQueue = suaRequestToExecuteQueue;
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

    public void setSuaRequestToExecuteQueue(
            BlockingQueue<JSONObject> suaRequestToExecuteQueue) {
        this.suaRequestToExecuteQueue = suaRequestToExecuteQueue;
    }

    
}
