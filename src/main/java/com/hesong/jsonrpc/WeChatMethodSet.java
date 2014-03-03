package com.hesong.jsonrpc;

import java.util.Map;

import com.hesong.weChatAdapter.manager.MessageManager;
import com.hesong.weChatAdapter.message.send.TextMessageToSend;

public class WeChatMethodSet {

    public String echo(String msg) {
        System.out.println("msg: "+msg);
        return msg;
    }

    public String sendMessage(String toUser, String msgType,
            Map<String, String> text) {
        TextMessageToSend msg = new TextMessageToSend();
        msg.setTouser(toUser);
        msg.setMsgtype(msgType);
        msg.setText(text);
        MessageManager.sendMessage(msg);
        return "200 OK";
    }

}
