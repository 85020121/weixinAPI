package com.hesong.weChatAdapter.tools;

public class API {
    
    public static String TOKEN = "weixin";
    public static String APPID = "wx5c19ccf5d7bdce97";
    public static String APP_SECRET = "b3b2b1f0cb0babc572adbd48dd072fb6";
    
    public final static String MESSAGE_TYPE_TAG = "MsgType";
    public final static String MESSAGE_FROM_TAG = "FromUserName";
    public final static String MESSAGE_TO_TAG = "ToUserName";
    public final static String MESSAGE_CREATE_TIME_TAG = "CreateTime";
    public final static String MESSAGE_CONTENT_TAG = "Content";
    public final static String MESSAGE_ID_TAG = "MsgId";
    public final static String MESSAGE_PIC_URL_TAG = "PicUrl";
    public final static String MESSAGE_MEDIA_ID_tAG = "MediaId";
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

}
