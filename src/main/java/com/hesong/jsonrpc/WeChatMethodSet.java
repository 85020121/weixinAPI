package com.hesong.jsonrpc;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import net.sf.json.JSONObject;

import com.hesong.weChatAdapter.manager.MessageManager;
import com.hesong.weChatAdapter.message.send.TextMessageToSend;
import com.hesong.weChatAdapter.tools.API;

public class WeChatMethodSet {
    private static Logger log = Logger.getLogger(WeChatMethodSet.class);

    private static String MSG_TYPE = "msgtype";

    public String echo(String msg) {
        System.out.println("msg: " + msg);
        return msg;
    }

    public JSONObject SendImMessage(String account, String fromuser,
            String touser, String room, JSONObject msgcontent) {
        log.info("SendImMessage have been called: " + msgcontent);
        log.info("toString: " + msgcontent.toString());
        log.info("msgtype:" + msgcontent.get("msgcontent").toString());
        JSONObject msg = (JSONObject)msgcontent.get("msgcontent");
        log.info("msg:"+msg.toString());
        msg.put("touser", touser);
        log.info("msg2:"+msg.toString());
        
        return MessageManager.sendMessage(msg.toString());
//        switch (type) {
//        case API.TEXT_MESSAGE:
//            log.info("iiiiiiiiiiiiiimmmmmmmmmmmmmmmmmmmmmmmiiiiiiiiiiiiiiiiiiinnnnnnnnnnnnnn");
//            TextMessageToSend msg = new TextMessageToSend();
//            msg.setTouser(touser);
//            msg.setMsgtype(API.TEXT_MESSAGE);
//            String text = msgcontent.findValue(API.TEXT_MESSAGE)
//                    .findValue("content").toString();
//            log.info("text=" + text);
//            msg.setText(text);
//            return MessageManager.sendMessage(msg);
//
//        default:
//            break;
//        }
    }

    private JSONObject createErrorMsg(int errcode, String errmsg) {
        JSONObject jo = new JSONObject();
        jo.put("errcode", errcode);
        jo.put("errmsg", errmsg);
        return jo;
    }

}
