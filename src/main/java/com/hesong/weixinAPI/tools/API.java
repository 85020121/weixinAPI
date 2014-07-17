package com.hesong.weixinAPI.tools;

import java.text.SimpleDateFormat;

public class API {
    public final static String TENANTUN = "tenantUn";

    public static String TOKEN = "weixin";
    public static String APPID = "wx5c19ccf5d7bdce97";
    public static String APP_SECRET = "b3b2b1f0cb0babc572adbd48dd072fb6";
    
    public final static String REDIS_WEIXIN_ACCESS_TOKEN_KEY = "weixin_access_token";
    public final static String REDIS_CLIENT_ACCOUNT_INFO_KEY = "weixin_client_account_info";
    public final static String REDIS_STAFF_ACCOUNT_INFO_KEY = "weixin_staff_account_info";
    public final static String REDIS_WEB_STAFF_OPENID_ACCOUNT = "web_staff_openid_account";
    
    //IVR
    public final static String REDIS_CLIENT_TEXT_IVR = "weixin_client_text_ivr_";
    public final static String REDIS_CLIENT_EVENT_IVR = "weixin_client_event_ivr_";
    public final static String REDIS_CLIENT_KEYWORDS_REGEX = "weixin_client_keywords_regex";
    public final static String REDIS_CLIENT_EVENT_LIST = "weixin_client_event_list";

    public final static String MESSAGE_TYPE_TAG = "MsgType";
    public final static String MESSAGE_FROM_TAG = "FromUserName";
    public final static String MESSAGE_TO_TAG = "ToUserName";
    public final static String MESSAGE_CREATE_TIME_TAG = "CreateTime";
    public final static String MESSAGE_CONTENT_TAG = "Content";
    public final static String MESSAGE_ID_TAG = "MsgId";
    public final static String MESSAGE_PIC_URL_TAG = "PicUrl";
    public final static String MESSAGE_MEDIA_ID_TAG = "MediaId";
    public final static String MESSAGE_EVENT_TAG = "Event";
    public final static String MESSAGE_EVENT_KEY_TAG = "EventKey";
    public final static String MESSAGE_TICKET_TAG = "Ticket";
    
    // 消息类型
    public final static String TEXT_MESSAGE = "text";
    public final static String IMAGE_MESSAGE = "image";
    public final static String VOICE_MESSAGE = "voice";
    public final static String VIDEO_MESSAGE = "video";
    public final static String LOCATION_MESSAGE = "location";
    public final static String LINK_MESSAGE = "link";
    public final static String EVENT_MESSAGE = "event";
    
    // 事件类型
    public final static String SUBSCRIBE_EVENT = "subscribe";
    public final static String UNSUBSCRIBE_EVENT = "unsubscribe";
    public final static String SCAN_EVENT = "SCAN";
    public final static String LOCATION_EVENT = "LOCATION";
    public final static String CLICK_EVENT = "CLICK";
    
    // 用户类型定义
    public final static int USER_UNDEFINE = 0;
    public final static int REAL_USER = 1;
    public final static int REAL_WEIXIN_CLIENT = 2;
    public final static int REAL_CLIENT = 3;
    public final static int MOCK_USER = 4;
    public final static int MOCK_WEIXIN_CLIENT = 5;
    public final static int MOCK_CLIENT = 6;
    
    public static String INVITE_REQUEST_URL = "http://localhost:8080/weChatAdapter/client/ACCOUNT/invite";
    public static String ENTER_ROOM_REQUEST_URL = "http://localhost:8080/weChatAdapter/client/ACCOUNT/entered_room";
    public static String EXIT_ROOM_REQUEST_URL = "http://localhost:8080/weChatAdapter/client/ACCOUNT/exited_room";
    public static String SEND_MESSAGE_REQUEST_URL = "http://localhost:8080/weChatAdapter/client/TOUSER/sendMessageRequest";
    public static String DISPOSE_ROOM_REQUEST_URL = "http://localhost:8080/weChatAdapter/client/ACCOUNT/dispose_room";
//    public static String INVITE_REQUEST_URL = "http://10.4.62.41:8080/weChatAdapter/client/ACCOUNT/invite";
//    public static String ENTER_ROOM_REQUEST_URL = "http://10.4.62.41:8080/weChatAdapter/client/ACCOUNT/entered_room";
//    public static String EXIT_ROOM_REQUEST_URL = "http://10.4.62.41:8080/weChatAdapter/client/ACCOUNT/exited_room";
//    public static String DISPOSE_ROOM_REQUEST_URL = "http://10.4.62.41:8080/weChatAdapter/client/ACCOUNT/dispose_room";
//    public static String SEND_MESSAGE_REQUEST_URL = "http://10.4.62.41:8080/weChatAdapter/client/TOUSER/sendMessageRequest";
    
    public static String PULLING_MEDIA_URL = "http://file.api.weixin.qq.com/cgi-bin/media/get?access_token=ACCESS_TOKEN&media_id=";
    public static String UPLOAD_IMAGE_REQUEST_URL = "http://file.api.weixin.qq.com/cgi-bin/media/upload?access_token=ACCESS_TOKEN&type=";

    public static String CONTENT_TYPE_IMAGE = "image/*";
    public static String CONTENT_TYPE_VOICE = "audio/amr";
    public static String CONTENT_TYPE_VIDEO = "video/mpeg4";

    public static String FTP_HTTP_ADDRESS = "http://10.4.62.41:8370";
    
    public static String WEIBO_SEND_MESSAGE_URL = "http://www.clouduc.cn:8080/weibo_robot/send";
    
    // SUA urls
    public static String SUA_DEL_STAFF_URL = "http://www.clouduc.cn/sua/rest/n/tenant/channel/del?openid=";

    public static SimpleDateFormat TIME_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");
    
    public final static String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=APPID&secret=APPSECRET";
    public final static String GET_FOLLOWER_LIST = "https://api.weixin.qq.com/cgi-bin/user/get?access_token=ACCESS_TOKEN&next_openid=";
}
