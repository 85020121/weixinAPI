package com.hesong.jsonrpc;

import java.io.InputStream;
import java.util.Map;

import org.apache.log4j.Logger;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import com.hesong.weChatAdapter.context.ContextPreloader;
import com.hesong.weChatAdapter.manager.MessageManager;
import com.hesong.weChatAdapter.model.AccessToken;
import com.hesong.weChatAdapter.tools.API;
import com.hesong.weChatAdapter.tools.WeChatHttpsUtil;

public class WeChatMethodSet {
    
    private static int TIMEOUT = 120000;
    
    private static Logger log = Logger.getLogger(WeChatMethodSet.class);
    
    public JSONObject SendImMessage(String account, String fromuser,
            String touser, String room, String msgcontent) {
        log.info("SendImMessage have been called, send: " + msgcontent);
        JSONObject jo = getJsonContent(msgcontent);
        if (jo == null) {
            return createErrorMsg(9922, "Check your json content: "
                    + msgcontent);
        }
        
        // 给虚拟客户端发送消息
        if (account == null) {
            JSONObject tmp = new JSONObject();
            tmp.put("sender", fromuser);
            tmp.put("roomId", room);
            String msgtype = null;
            if (jo.has("msgtype")) {
                msgtype = jo.getString("msgtype");
                JSONObject msgDetail = (JSONObject)jo.get(msgtype);
                if (msgtype.equals(API.TEXT_MESSAGE)) {
                    tmp.put("content", msgDetail.get("content"));
                } else if (msgtype.equals(API.IMAGE_MESSAGE)) {
                    tmp.put("content", jo.getString("media_id"));
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

        String token = getAccessToken(account);
        if (token != null) {
            // Image message
            if (jo.containsKey("msgtype") && jo.getString("msgtype").equals(API.IMAGE_MESSAGE)) {
                JSONObject imageContent = jo.getJSONObject(API.IMAGE_MESSAGE);
                String image_url = imageContent.getString("media_id");
                image_url = API.FTP_HTTP_ADDRESS + image_url.replace("/weixin", "");
                InputStream input = WeChatHttpsUtil.httpGetInputStream(image_url, "image");
                
                String postUrl = API.UPLOAD_IMAGE_REQUEST_URL.replace("ACCESS_TOKEN", token);
                JSONObject mediaIDContent = WeChatHttpsUtil.httpPostFile(postUrl, input);
                if(mediaIDContent.containsKey("media_id")){
                    imageContent.put("media_id", mediaIDContent.getString("media_id"));
                    jo.put(API.IMAGE_MESSAGE, imageContent);
                    log.info("Send image msg: " + jo.toString());
                } else {
                    return createErrorMsg(mediaIDContent.getInt("errcode"), mediaIDContent.getString("errmsg"));
                }
                
            }
            
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
        String token = getAccessToken(account);
        if (token == null) {
            return createErrorMsg(9911, "Invalide WeChat account.");
        }
        return MessageManager.manageMenu(token, action, menucontent);
    }

    public JSONObject Invited(String account, String from_user, String room_id,
            String to_user, String txt, String data, String expire,
            String option) {
        String toUser = null;
        try {
            JSONObject tmp = getJsonContent(to_user);
            toUser = tmp.getString("user");
        } catch (Exception e) {
            toUser = to_user;
        }
        String fromUser = null;
        try {
            JSONObject tmp = getJsonContent(from_user);
            fromUser = tmp.getString("user");
        } catch (Exception e) {
            fromUser = from_user;
        }
        JSONObject post = new JSONObject();
        post.put("msgtype", "invitation");
        post.put("fromUser", fromUser);
        post.put("toUser", toUser);
        post.put("roomId", room_id);
        int timeout = Integer.parseInt(expire);
        JSONObject response = WeChatHttpsUtil.httpPostRequest(API.INVITE_REQUEST_URL.replace("ACCOUNT", toUser), post.toString(), timeout);
        return response;
    }
    
    public JSONObject EnteredRoom(String to_account, String to_user, String cause_account, String cause_user, String room_id, String txt){
        log.info("EnteredRoom have been called.");
        String toUser = null;
        try {
            JSONObject tmp = getJsonContent(to_user);
            toUser = tmp.getString("user");
        } catch (Exception e) {
            toUser = to_user;
        }
        String causeUser = null;
        try {
            JSONObject tmp = getJsonContent(cause_user);
            causeUser = tmp.getString("user");
        } catch (Exception e) {
            causeUser = cause_user;
        }
        JSONObject post = new JSONObject();
        post.put("roomId", room_id);
        post.put("sender", causeUser);
        JSONObject response = WeChatHttpsUtil.httpPostRequest(API.ENTER_ROOM_REQUEST_URL.replace("ACCOUNT", toUser), post.toString(), 0);
        return response;
    }
    
    public JSONObject ExitedRoom(String to_account, String to_user, String cause_account, String cause_user, String room_id, String txt){
        log.info("ExitedRoom have been called, room: "+room_id);
        String toUser = null;
        try {
            JSONObject tmp = getJsonContent(to_user);
            toUser = tmp.getString("user");
        } catch (Exception e) {
            toUser = to_user;
        }
        String causeUser = null;
        try {
            JSONObject tmp = getJsonContent(cause_user);
            causeUser = tmp.getString("user");
        } catch (Exception e) {
            causeUser = cause_user;
        }
        JSONObject post = new JSONObject();
        post.put("roomId", room_id);
        post.put("sender", causeUser);
        JSONObject response = WeChatHttpsUtil.httpPostRequest(API.EXIT_ROOM_REQUEST_URL.replace("ACCOUNT", toUser), post.toString(), 0);
        return response;
    }
    
    public JSONObject RoomDisposed(String to_account, String to_user, String room_id, String txt) {
        log.info("RoomDisposed have been called, room: "+room_id);
        String toUser = null;
        try {
            JSONObject tmp = getJsonContent(to_user);
            toUser = tmp.getString("user");
        } catch (Exception e) {
            toUser = to_user;
        }
        JSONObject post = new JSONObject();
        post.put("roomId", room_id);
        JSONObject response = WeChatHttpsUtil.httpPostRequest(API.DISPOSE_ROOM_REQUEST_URL.replace("ACCOUNT", toUser), post.toString(), 0);
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
