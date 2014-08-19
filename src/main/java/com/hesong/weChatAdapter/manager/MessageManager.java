package com.hesong.weChatAdapter.manager;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.log4j.Logger;

import net.sf.json.JSONObject;

import com.hesong.ftp.FTPConnectionFactory;
import com.hesong.ftp.FTPEngine;
import com.hesong.jsonrpc.WeChatMethodSet;
import com.hesong.smartbus.client.PackInfo;
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

    public static SimpleDateFormat sdf_time = new SimpleDateFormat(
            "HH-mm-ss");
    public static SimpleDateFormat sdf_today = new SimpleDateFormat(
            "yyyy-MM-dd");
    
    public static String getResponseMessage(Map<String, String> message) {
        if (message.get(API.MESSAGE_TYPE_TAG) != null) {
            return messageRouter(message);
        }
        log.error("MsgType is null");
        return "";
    }

    public static String messageRouter(Map<String, String> message) {
        JSONObject jo = new JSONObject();
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
        paramsList.put("room_id", null);

        switch (message.get(API.MESSAGE_TYPE_TAG)) {
        case API.TEXT_MESSAGE:
            paramsList.put("msgtype", API.TEXT_MESSAGE);
            break;
        case API.IMAGE_MESSAGE:
            paramsList.put("msgtype", API.IMAGE_MESSAGE);
            String filename = sdf_time.format(new Date())+"_"+message.get(API.MESSAGE_FROM_TAG)+".jpg";
            String account = message.get(API.MESSAGE_TO_TAG);
            String dirPath = getDirName("image/received", account);
            if(uploadMediaFile(message, filename, dirPath, API.CONTENT_TYPE_IMAGE)){
                message.put(API.MESSAGE_PIC_URL_TAG, dirPath+filename);
            }
            break;
        case API.VOICE_MESSAGE:
            paramsList.put("msgtype", API.VOICE_MESSAGE);
            String voice_filename = sdf_time.format(new Date())+"_"+message.get(API.MESSAGE_FROM_TAG)+".amr";
            String voice_account = message.get(API.MESSAGE_TO_TAG);
            String voice_dirPath = getDirName("voice", voice_account);
            if(uploadMediaFile(message, voice_filename, voice_dirPath, API.CONTENT_TYPE_IMAGE)){
                message.put("VoiceUrl", voice_dirPath+voice_filename);
            }
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

        paramsList.put("msgcontent", message);
        jo.put("params", paramsList);
//        PackInfo pack = new PackInfo((byte)ContextPreloader.destUnitId, (byte)ContextPreloader.destClientId, (byte)ContextPreloader.srcUnitId, (byte)ContextPreloader.srctClientId, jo.toString());
        try {
            log.info("Put pack to SmartbusExecutor.responseQueue");
            SmartbusExecutor.responseQueue.put(jo.toString());
        } catch (InterruptedException e) {
            log.error("Puc packinfo into response queue failed: " + e.toString());
//            log.error("Packinfo: " + pack.toString());
        }

//        smartbusSendMessage(jo.toString());
        return "";
    }

    public static JSONObject sendMessage(String msg, String token) {
        String request = SEND_MESSAGE_REQUEST_URL + token;
        JSONObject jo = WeChatHttpsUtil.httpsRequest(request, POST, msg);
        return jo;
    }
    
    public static JSONObject getClientInfo(String accessToken, String openid){
        String request = (GET_CLIENT_INFO_REQUEST_URL).replace(ACCESS_TOKEN_TAG, accessToken).replace(OPENID_TAG, openid);
        JSONObject jo = WeChatHttpsUtil.httpsRequest(request, GET, null);
        return jo;
    }

    public static JSONObject manageMenu(String accessToken, String action,
            String jsonMenu) {
        String request = (MANAGE_MENU_REQUEST_URL+accessToken).replace("ACTION", action);
    
        JSONObject jObject;
        if (action.equals("create")) {
            jObject = WeChatHttpsUtil.httpsRequest(request, POST,
                    jsonMenu);
        } else {
            jObject = WeChatHttpsUtil.httpsRequest(request, GET, null);
        }
        return jObject;
    }

    public static JSONObject getFollowersList(String access_token) {
        String request = GET_FOLLOWERS_OPENID_REQUEST_URL + access_token;
        JSONObject jo = WeChatHttpsUtil.httpsRequest(request, GET, null);
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

    
    public static String getDirName(String type, String account) {
        String attachmtDirName = String.format(
                "weixin/%s/%s/%s/",
                type, account, sdf_today.format(new Date()));
        return attachmtDirName;
    }
    
    public static String getMediaPullUrl(String account, String mediaId){
        String token = WeChatMethodSet.getAccessToken(account);
        return API.PULLING_MEDIA_URL.replace("ACCESS_TOKEN", token) + mediaId;
    }
    
    private static boolean uploadMediaFile(Map<String, String> message, String filename, String dirPath, String type){
        try {
            log.info("Ftp path: "+dirPath);
            String mediaId = message.get(API.MESSAGE_MEDIA_ID_TAG);
            String account = message.get(API.MESSAGE_TO_TAG);
            FTPClient ftp = FTPConnectionFactory.getDefaultFTPConnection();
            String request = getMediaPullUrl(account, mediaId);
            InputStream input = WeChatHttpsUtil.httpGetInputStream(request, API.CONTENT_TYPE_IMAGE);
            FTPEngine.uploadFile(ftp, dirPath, filename, input);
            return true;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }

}
