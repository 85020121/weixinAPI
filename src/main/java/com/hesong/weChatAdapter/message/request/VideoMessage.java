package com.hesong.weChatAdapter.message.request;

public class VideoMessage extends Message {
    
    private String MediaId;
    private String ThumbMediaId;
    public String getMediaId() {
        return MediaId;
    }
    public void setMediaId(String mediaId) {
        MediaId = mediaId;
    }
    public String getThumbMediaId() {
        return ThumbMediaId;
    }
    public void setThumbMediaId(String thumbMediaId) {
        ThumbMediaId = thumbMediaId;
    }
    
    
}
