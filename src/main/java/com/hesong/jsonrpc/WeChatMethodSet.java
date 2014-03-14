package com.hesong.jsonrpc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import com.hesong.weChatAdapter.context.ContextPreloader;
import com.hesong.weChatAdapter.manager.MessageManager;
import com.hesong.weChatAdapter.model.AccessToken;
import com.hesong.weChatAdapter.tools.API;
import com.hesong.weChatAdapter.tools.WeChatHttpsUtil;

public class WeChatMethodSet {
    
    // TODO: use application context to get father url
    private static String INVITE_REQUEST_URL = "http://localhost:8080/weChatAdapter/client/ACCOUNT/invite";
    private static String SEND_MESSAGE_REQUEST_URL = "http://localhost:8080/weChatAdapter/client/TOUSER/sendMessageRequest";
    private static int TIMEOUT = 120000;
    
    private static Logger log = Logger.getLogger(WeChatMethodSet.class);

    public String echo(String msg) {
        System.out.println("msg: " + msg);
        return msg;
    }

    public JSONObject SendImMessage(String account, String touser,
            String msgcontent) {
        log.info("SendImMessage have been called: " + msgcontent);
        JSONObject jo = getJsonContent(msgcontent);
        if (jo == null) {
            return createErrorMsg(9922, "Check your json content: "
                    + msgcontent);
        }
        jo.put("touser", touser);
        log.info("JSONObject: " + jo);

        String token = getAccessToken(account);
        if (token != null) {
            return MessageManager.sendMessage(jo.toString(), token);
        } else {
            return createErrorMsg(9911, "Invalide WeChat account: " + account);
        }
    }

    public JSONObject SendImMessage(String account, String fromuser,
            String touser, String room, String msgcontent) {
        log.info("SendImMessage have been called: " + msgcontent);
        JSONObject jo = getJsonContent(msgcontent);
        if (jo == null) {
            return createErrorMsg(9922, "Check your json content: "
                    + msgcontent);
        }
        if (account == null) {
            log.info("Send message to client.");
            JSONObject tmp = new JSONObject();
            tmp.put("sender", fromuser);
            tmp.put("roomId", room);
            String msgtype = null;
            if (jo.has("msgtype")) {
                msgtype = jo.getString("msgtype");
                JSONObject msgDetail = (JSONObject)jo.get(msgtype);
                if (msgtype.equals("text")) {
                    tmp.put("content", msgDetail.get("content"));
                }
            }else if (jo.has("MsgType")){
                msgtype = jo.getString("MsgType");
                tmp.put("content", jo.getString("Content"));
            }
            tmp.put("msgtype", msgtype);
            
            JSONObject result = WeChatHttpsUtil.httpPostRequest(SEND_MESSAGE_REQUEST_URL.replace("TOUSER", touser), tmp.toString(), TIMEOUT);
            return result;
        }

        jo.put("touser", touser);
        log.info("JSONObject: " + jo);

        String token = getAccessToken(account);
        if (token != null) {
            return MessageManager.sendMessage(jo.toString(), token);
        } else {
            return createErrorMsg(9911, "Invalide WeChat account: " + account);
        }
    }

    // public JSONObject GetFollowersInfo(String account, String user) {
    // log.info("GetClient have been called, users: " + user);
    // JSONArray ja = null;
    // try {
    // ja = JSONArray.fromObject(user);
    // } catch (Exception e) {
    // return createErrorMsg(9923, "User must be a string or an array: "+user);
    // }
    // if (ja.size() == 0) {
    // return createErrorMsg(9922, "Check your json content: "+user);
    // }
    // String accessToken = getAccessToken(account);
    // if (accessToken == null) {
    // return createErrorMsg(9911, "Invalide WeChat account: "+account);
    // }
    // Object[] userList = ja.toArray();
    // if (userList.length == 1) {
    // return MessageManager.getClientInfo(accessToken, (String)userList[0]);
    // }
    // JSONObject jo = new JSONObject();
    // JSONArray clients = new JSONArray();
    //
    // for (int i = 0; i < userList.length; i++) {
    // clients.add(MessageManager.getClientInfo(accessToken,
    // (String)userList[i]));
    // //jo.put(userList[i], MessageManager.getClientInfo(accessToken,
    // (String)userList[i]));
    // }
    // jo.put("FollowersInfo", clients);
    // log.info("Clients info list: "+jo.toString());
    // return jo;
    // }
    //
    // public JSONObject GetFollowersCount(String account, String
    // status_filter){
    // log.info("GetClientCount have been called for account: " + account);
    // String accessToken = getAccessToken(account);
    // if (accessToken == null) {
    // return createErrorMsg(9911, "Invalide WeChat account: "+account);
    // }
    // JSONObject jo = MessageManager.getFollowersList(accessToken);
    // if (jo.has("total")) {
    // JSONObject result = new JSONObject();
    // result.put("FollowersCount", jo.get("total"));
    // return result;
    // }
    // return jo;
    // }

    public JSONObject ManageMenu(String account, String action,
            String menucontent) {
        log.info("ManageMenu have been called with action: " + action);
        String token = getAccessToken(account);
        if (token == null) {
            return createErrorMsg(9911, "Invalide WeChat account.");
        }
        return MessageManager.manageMenu(token, action, menucontent);
    }

    public JSONObject Invited(String account, String from_user, String room_id,
            String to_user, String txt, String data, String expire,
            String option) {
        log.info("Invited have been called.");
        String toUser = null;
        try {
            log.info("Mark0");
            JSONObject tmp = getJsonContent(to_user);
            toUser = tmp.getString("user");
            // TODO
        } catch (Exception e) {
            log.info("Mark1");
            toUser = to_user;
        }
        log.info("Mark3");
        log.info(from_user+" invit "+toUser+" to room "+room_id);
        JSONObject post = new JSONObject();
        post.put("msgtype", "invitation");
        post.put("fromUser", from_user);
        post.put("toUser", toUser);
        post.put("roomId", room_id);
        int timeout = Integer.parseInt(expire);
        JSONObject response = WeChatHttpsUtil.httpPostRequest(INVITE_REQUEST_URL.replace("ACCOUNT", toUser), post.toString(), timeout);
        return response;
    }

    private JSONObject createErrorMsg(int errcode, String errmsg) {
        JSONObject jo = new JSONObject(); 
        jo.put("errcode", errcode);
        jo.put("errmsg", errmsg);
        return jo;
    }

    public static String getAccessToken(String account) {
        // return
        // "hLIacNToLXQDrQs3HygeHuoULV-xB2pGLNlNOr8DnV85Ofam11lBc3S8BAyRfJfkKHzYYMcQPUGjFyc9WDIqFTcEQBsAnc3ej2QPHe4o5oQg3EKKwzh3t1BuK18QUsXjaqgXkqVpPL-Oz2iZJ01mcQ";
        AccessToken ac = ContextPreloader.Account_Map.get(account);
        if (ac == null) {
            return null;
        }
        String token = ContextPreloader.Account_Map.get(account).getToken();
        if (token != null) {
            return token;
        } else {
            return null;
        }
    }

    private JSONObject getJsonContent(String content) {
        try {
            return (JSONObject) JSONSerializer.toJSON(content.toString());
        } catch (Exception e) {
            return null;
        }
    }
    
    public static JSONObject createJsonrpcRequest(String method, String id, Map<String, Object> paramsList){
        JSONObject jsonrpc = new JSONObject();
        jsonrpc.put("jsonrpc", "2.0");
        jsonrpc.put("method", method);
        jsonrpc.put("id", id);
        jsonrpc.put("params", paramsList);
        return jsonrpc;
    }

}
