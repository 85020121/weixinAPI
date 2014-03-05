package com.hesong.jsonrpc;

import java.io.IOException;
import java.util.Map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import net.sf.json.JSONObject;

import com.hesong.weChatAdapter.manager.MessageManager;
import com.hesong.weChatAdapter.message.send.TextMessageToSend;

public class WeChatMethodSet {

    public String echo(String msg) {
        System.out.println("msg: "+msg);
        return msg;
    }

    public JSONObject sendMessage(String toUser, String msgType,
            Map<String, String> text) throws IOException {
        TextMessageToSend msg = new TextMessageToSend();
        msg.setTouser(toUser);
        msg.setMsgtype(msgType);
        msg.setText(text);
        return MessageManager.sendMessage(msg);
    }

}
