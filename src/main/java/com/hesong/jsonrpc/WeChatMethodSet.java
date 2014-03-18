package com.hesong.jsonrpc;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.log4j.Logger;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import com.hesong.ftp.FTPConnectionFactory;
import com.hesong.ftp.FTPEngine;
import com.hesong.weChatAdapter.context.ContextPreloader;
import com.hesong.weChatAdapter.manager.MessageManager;
import com.hesong.weChatAdapter.model.AccessToken;
import com.hesong.weChatAdapter.tools.API;
import com.hesong.weChatAdapter.tools.WeChatHttpsUtil;

public class WeChatMethodSet {
    
    private static int TIMEOUT = 120000;
    
    private static Logger log = Logger.getLogger(WeChatMethodSet.class);
    
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
        
        // 给虚拟客户端发送消息
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
                if (msgtype.equals("text")) {
                    tmp.put("content", jo.getString("Content"));
                } else if (msgtype.equals("image")) {
                    tmp.put("content", jo.getString("PicUrl"));
                } else if (msgtype.equals("voice")) {
                    tmp.put("content", jo.getString("VoiceUrl"));
                }
            }
            tmp.put("msgtype", msgtype);
            
            JSONObject result = WeChatHttpsUtil.httpPostRequest(API.SEND_MESSAGE_REQUEST_URL.replace("TOUSER", touser), tmp.toString(), TIMEOUT);
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
            log.info("to_user is an object.");
            JSONObject tmp = getJsonContent(to_user);
            toUser = tmp.getString("user");
        } catch (Exception e) {
            log.info("to_user is a String.");
            toUser = to_user;
        }
        String fromUser = null;
        try {
            log.info("from_user is an object.");
            JSONObject tmp = getJsonContent(from_user);
            fromUser = tmp.getString("user");
        } catch (Exception e) {
            log.info("from_user is a String.");
            fromUser = from_user;
        }
        log.info(from_user+" invit "+toUser+" to room "+room_id);
        JSONObject post = new JSONObject();
        post.put("msgtype", "invitation");
        post.put("fromUser", fromUser);
        post.put("toUser", toUser);
        post.put("roomId", room_id);
        int timeout = Integer.parseInt(expire);
        JSONObject response = WeChatHttpsUtil.httpPostRequest(API.INVITE_REQUEST_URL.replace("ACCOUNT", toUser), post.toString(), timeout);
        return response;
    }
    
    public JSONObject EnteredRoom(String account, String from_user, String to_user, String room_id, String txt){
        log.info("EnteredRoom have been called.");
        String toUser = null;
        try {
            log.info("to_user is an object.");
            JSONObject tmp = getJsonContent(to_user);
            toUser = tmp.getString("user");
        } catch (Exception e) {
            log.info("to_user is a String.");
            toUser = to_user;
        }
        JSONObject post = new JSONObject();
        post.put("roomId", room_id);
        JSONObject response = WeChatHttpsUtil.httpPostRequest(API.ENTER_ROOM_REQUEST_URL.replace("ACCOUNT", toUser), post.toString(), 0);
        return response;
    }
    
    public JSONObject ExitedRoom(String account, String from_user, String to_user, String room_id, String txt){
        log.info("ExitedRoom have been called.");
        String toUser = null;
        try {
            log.info("to_user is an object.");
            JSONObject tmp = getJsonContent(to_user);
            toUser = tmp.getString("user");
        } catch (Exception e) {
            log.info("to_user is a String.");
            toUser = to_user;
        }
        JSONObject post = new JSONObject();
        post.put("roomId", room_id);
        JSONObject response = WeChatHttpsUtil.httpPostRequest(API.EXIT_ROOM_REQUEST_URL.replace("ACCOUNT", toUser), post.toString(), 0);
        return response;
    }

    private JSONObject createErrorMsg(int errcode, String errmsg) {
        JSONObject jo = new JSONObject(); 
        jo.put("errcode", errcode);
        jo.put("errmsg", errmsg);
        return jo;
    }

    public static String getAccessToken(String account) {
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
