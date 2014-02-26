package com.hesong.weChatAdapter.message.send;

import java.util.Map;

public class TextMessageToSend {
    private String touser;
    private String msgtype;
    private Map<String,String> text;
    public String getTouser() {
        return touser;
    }
    public void setTouser(String touser) {
        this.touser = touser;
    }
    public String getMsgtype() {
        return msgtype;
    }
    public void setMsgtype(String msgtype) {
        this.msgtype = msgtype;
    }
    public Map<String, String> getText() {
        return text;
    }
    public void setText(Map<String, String> text) {
        this.text = text;
    } 

}
