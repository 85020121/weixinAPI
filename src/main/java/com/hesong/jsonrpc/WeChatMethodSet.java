package com.hesong.jsonrpc;

import org.apache.log4j.Logger;

import net.sf.json.JSONObject;
import com.hesong.weChatAdapter.context.ContextPreloader;
import com.hesong.weChatAdapter.manager.MessageManager;
import com.hesong.weChatAdapter.model.AccessToken;

public class WeChatMethodSet {
    private static Logger log = Logger.getLogger(WeChatMethodSet.class);

    public String echo(String msg) {
        System.out.println("msg: " + msg);
        return msg;
    }

    public JSONObject SendImMessage(String account, String touser,
            String msgcontent) {
        log.info("SendImMessage have been called: " + msgcontent);
        JSONObject jo = MessageManager.getJsonContent(msgcontent);
        if (jo ==null) {
            return createErrorMsg(9922, "Check your json content: "+msgcontent);
        }
        log.info("JSONObject: "+jo);

        jo.put("touser", touser);
        
        String token = getAccessToken(account);
        if (token != null) {
            return MessageManager.sendMessage(jo.toString(), token);
        } else{
            return createErrorMsg(9911, "Invalide WeChat account.");
        }
    }

    public JSONObject SendImMessage(String account, String fromuser,
            String touser, String room, String msgcontent) {
        log.info("SendImMessage have been called: " + msgcontent);
        JSONObject jo = MessageManager.getJsonContent(msgcontent);
        if (jo ==null) {
            return createErrorMsg(9922, "Check your json content: "+msgcontent);
        }
        log.info("JSONObject: "+jo);

        jo.put("touser", touser);
        String token = getAccessToken(account);
        if (token != null) {
            return MessageManager.sendMessage(jo.toString(), token);
        } else{
            return createErrorMsg(9911, "Invalide WeChat account.");
        }
    }
    
    public JSONObject ManageMenu(String account, String action, String menucontent){
        log.info("ManageMenu have been called with action: "+action);
        String token = getAccessToken(account);
        if (token == null) {
            return createErrorMsg(9911, "Invalide WeChat account.");
        }
        return MessageManager.manageMenu(token, action, menucontent);
    }

    private JSONObject createErrorMsg(int errcode, String errmsg) {
        JSONObject jo = new JSONObject();
        jo.put("errcode", errcode);
        jo.put("errmsg", errmsg);
        return jo;
    }
    
    private String getAccessToken(String account) {
        AccessToken ac = ContextPreloader.Account_Map.get(account);
        if (ac == null) {
            return null;
        }
        String token = ContextPreloader.Account_Map.get(account).getToken();
        if (token != null) {
            return token;
        } else{
            return null;
        }
    }

}
