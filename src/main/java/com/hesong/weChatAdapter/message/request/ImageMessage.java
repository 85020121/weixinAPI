package com.hesong.weChatAdapter.message.request;

public class ImageMessage extends Message {
    
    private String PicUrl;
    private String MediaId;
    
    public String getPicUrl() {
        return PicUrl;
    }
    public void setPicUrl(String picUrl) {
        PicUrl = picUrl;
    }
    public String getMediaId() {
        return MediaId;
    }
    public void setMediaId(String mediaId) {
        MediaId = mediaId;
    }
}
