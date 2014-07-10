package com.hesong.weixinAPI.model;

public class ChatMessage {
    private String channelId;
    private String senderName;
    private String content;
    private String date;
    private String msgtype;
    public String getChannelId() {
        return channelId;
    }
    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }
    public String getSenderName() {
        return senderName;
    }
    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }
    public String getMsgtype() {
        return msgtype;
    }
    public void setMsgtype(String msgtype) {
        this.msgtype = msgtype;
    }
    @Override
    public String toString() {
        return "ChatMessage [channelId=" + channelId + ", senderName="
                + senderName + ", content=" + content + ", date=" + date
                + ", msgtype=" + msgtype + "]";
    }
    
}
