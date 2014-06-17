package com.hesong.weixinAPI.core;

import java.io.InputStream;
import java.text.SimpleDateFormat;
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
import com.hesong.weixinAPI.model.LeavingMessageClient;
import com.hesong.weixinAPI.model.Staff;
import com.hesong.weixinAPI.model.StaffSessionInfo;
import com.hesong.weixinAPI.model.WaitingClient;
import com.hesong.weixinAPI.tools.API;
import com.hesong.weixinAPI.tools.WeChatHttpsUtil;

public class MessageRouter implements Runnable {

    private static Logger log = Logger.getLogger(MessageRouter.class);
    
    public static SimpleDateFormat TIME_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");
    
    private static String USER_INFO_URL = "https://api.weixin.qq.com/cgi-bin/user/info?access_token=ACCESS_TOKEN&openid=OPENID&lang=zh_CN";
    private static String QRCODE_URL = "https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=";
    private static String GET_QRCODE_TICKETS_URL = "http://www.clouduc.cn/sua/rest/n/tenant/codetokens";
    
    private BlockingQueue<Map<String, String>> messageQueue;
    private BlockingQueue<JSONObject> messageToSendQueue;
    private BlockingQueue<JSONObject> suaRequestToExecuteQueue;
    
    public static Map<String, Map<String, Staff>> mulClientStaffMap = new HashMap<String, Map<String, Staff>>();
    public static Map<String, StaffSessionInfo> activeStaffMap = new HashMap<String, StaffSessionInfo>();
    public static Queue<WaitingClient> waitingList = new LinkedList<WaitingClient>();
    public static Set<String>  waitingListIDs= new HashSet<String>();
    public static Map<String, JSONObject> staffIdList = new HashMap<String, JSONObject>();
    public static Map<String, String> account_tanentUn = new HashMap<String, String>();
    
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
                        case "TAKE_CLIENT":
                            takeClient(message);  // 抢接
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
        
        // Leaving message
        if (CheckLeavingMessageJob.leavingMessageClientList.containsKey(from_openid)) {
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
                String cToken = ContextPreloader.Account_Map.get(s.getClient_account()).getToken();
                String content = String.format("客服%s: %s", s.getStaffid(), message.get(API.MESSAGE_CONTENT_TAG));
                sendMessage(s.getClient_openid(), cToken, content, API.TEXT_MESSAGE);
                s.setLastReceived(new Date());
                recordMessage(s, message, API.TEXT_MESSAGE, "wx", false);
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
        String voice_id = getMediaId(message.get(API.MESSAGE_MEDIA_ID_TAG), voice_Token, voice_toToken, "voice", "audio/amr");
        if (voice_id.equals("error")) {
            sendMessage(voice_openid, voice_Token, "系统消息:发送语音消息失败", API.TEXT_MESSAGE);
            return;
        }
        sendMessage(voice_to_openid, voice_toToken, voice_id, API.VOICE_MESSAGE);
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
            
            // Insert client info to SugerCRM
            String url = USER_INFO_URL.replace("ACCESS_TOKEN", toToken).replace("OPENID", openid);
            JSONObject user_info = WeChatHttpsUtil.httpsRequest(url, "GET", null);
            String tanentUn = message.get("tanentUn");
            // TODO 
            if (null == tanentUn) {
                tanentUn = "null";
            }
            if (user_info.containsKey("errcode")) {
                log.error("Get client info failed: "+user_info.toString());
            } else {
                log.info("Client info: " + user_info.toString());
                user_info.put("tenant_code", tanentUn);
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
        String openid = message.get(API.MESSAGE_TO_TAG);
        JSONObject staff_account_id = staffIdList.get(openid);
        if (null != staff_account_id) {
            String wx_account = staff_account_id.getString("wx_account");
            String tanentUn = account_tanentUn.get(wx_account);
            String staff_id = staff_account_id.getString("staffid"); 
            Map<String, Staff> staff_map = mulClientStaffMap.get(tanentUn);
            Staff staff = staff_map.get(staff_id);
            List<StaffSessionInfo> sessionList = staff.getSessionChannelList();
            for (StaffSessionInfo s : sessionList) {
                if (s.isBusy()) {
                    // Remaind client that staff is leaving.
                    String token = ContextPreloader.Account_Map.get(s.getClient_account()).getToken();
                    String text = "对不起，客服MM有急事下线了,会话已结束。您可以使用留言功能,客服MM将会在第一时间给您回复[微笑]";
                    sendMessage(s.getClient_openid(), token, text, "text");
                }
            }
            staff_map.remove(staff_id);
            staffIdList.remove(openid);
            if (staff_map.isEmpty()) {
                mulClientStaffMap.remove(tanentUn);
                account_tanentUn.remove(wx_account);
            }
        }
    }
    
    private void staffService(Map<String, String> message) {
        String client_openid = message.get(API.MESSAGE_FROM_TAG);
        String client_account = message.get(API.MESSAGE_TO_TAG);
        String tanentUn = account_tanentUn.get(client_account);
        String cToken = ContextPreloader.Account_Map.get(client_account).getToken();
        
        JSONObject crmRequest = new JSONObject();
        crmRequest.put("method", "convertProspectsToLeads");
        crmRequest.put("$prospectsOpenid", client_openid);
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
        if (null == tanentUn || !mulClientStaffMap.containsKey(tanentUn) || mulClientStaffMap.get(tanentUn).isEmpty()) {
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
        
        WaitingClient c = new WaitingClient(client_openid, client_account, client_name);
        waitingList.offer(c);
        waitingListIDs.add(client_openid);
        
        // Broadcast client request to all staffs
        boolean isAllBusy = true;
        for (Staff staff : mulClientStaffMap.get(tanentUn).values()) {
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
            if (CheckLeavingMessageJob.leavingMessageClientList
                    .containsKey(openid)) {
                String content = "您正在使用留言功能,赶紧发送您的留言吧!";
                sendMessage(openid, token, content, API.TEXT_MESSAGE);
                return;
            }
            String content = "欢迎使用在线留言服务,和声云客服MM将会在第一时间处理您的留言。请您在五分钟内完成留言,发送END(大小写不限)结束留言,否则系统将在五分钟后自动结束您的留言请求!";
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
                LeavingMessageClient c = new LeavingMessageClient(account,
                        openid, client_name, headimgurl);
                CheckLeavingMessageJob.leavingMessageClientList.put(openid, c);
            }
        } catch (Exception e) {
            log.error("Request to leave message failed: " + e.toString());
            e.printStackTrace();
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
    
    private void recordMessage(StaffSessionInfo s, Map<String, String> message, String type, String source, boolean isClient) {
        JSONObject messageToRecord = new JSONObject();
        messageToRecord.put("session_id", s.getSession());
        messageToRecord.put("content", message.get(API.MESSAGE_CONTENT_TAG));
        messageToRecord.put("message_type", type);
        messageToRecord.put("message_source", source);
        Date d = new Date(Long.parseLong(message.get("CreateTime")));
        messageToRecord.put("time", TIME_FORMAT.format(d));
        if (isClient) {
            messageToRecord.put("sender_openid", s.getOpenid());
            messageToRecord.put("sender_name", s.getName());
            messageToRecord.put("sender_type", "staff");
            messageToRecord.put("sender_public_account", s.getAccount());
            messageToRecord.put("receiver_openid", s.getClient_openid());
            messageToRecord.put("receiver_name", s.getClient_name());
            messageToRecord.put("receiver_type", "client");
            messageToRecord.put("receiver_public_account", s.getClient_account());
        } else {
            messageToRecord.put("sender_openid", s.getClient_openid());
            messageToRecord.put("sender_name", s.getClient_name());
            messageToRecord.put("sender_type", "client");
            messageToRecord.put("sender_public_account", s.getClient_account());
            messageToRecord.put("receiver_openid", s.getOpenid());
            messageToRecord.put("receiver_name", s.getName());
            messageToRecord.put("receiver_type", "staff");
            messageToRecord.put("receiver_public_account", s.getAccount());
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
        if (type.equalsIgnoreCase(API.TEXT_MESSAGE)) {
            if (content.equalsIgnoreCase("end")) {
                // End of leaving message
                CheckLeavingMessageJob.leavingMessageClientList.remove(openid);
                String text = "感谢您使用在线留言服务,客服MM将在第一时间回复您的消息!";
                String token = ContextPreloader.Account_Map.get(account).getToken();
                sendMessage(openid, token, text, API.TEXT_MESSAGE);
            }
        } else {
            // Only recording text message for now
            return;
        }
        
        LeavingMessageClient c = CheckLeavingMessageJob.leavingMessageClientList.get(openid);
        
        JSONObject messageToLeave = new JSONObject();
        messageToLeave.put("messager_id", openid);
        messageToLeave.put("messager_name", c.getName());
        messageToLeave.put("messager_photo", c.getHeadimgurl());
        messageToLeave.put("messager_public_account", account);
        messageToLeave.put("content", content);
        messageToLeave.put("time", TIME_FORMAT.format(new Date()));
        messageToLeave.put("message_status", "0");
        messageToLeave.put("type", type);
        messageToLeave.put("source", "wx");
        messageToLeave.put("message_group_id", c.getUuid());
        
        JSONObject json_request = new JSONObject();
        json_request.put("session", "hold");
        json_request.put("module_name", "chat_message");
        json_request.put("name_value_list", messageToLeave.toString());
        json_request.put("method", "set_entry");
        
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
