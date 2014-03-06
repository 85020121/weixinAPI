package com.hesong.weChatAdapter.message.send;

import java.util.HashMap;
import java.util.Map;

public class TextMessageToSend extends BaseMessage {
    private Map<String,String> text;
    public Map<String, String> getText() {
        return text;
    }
    public void setText(String msg) {
        this.text = new HashMap<String, String>();
        this.text.put("content", msg);
    } 

}
