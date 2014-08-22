package com.hesong.jsonrpc;

import java.io.InputStream;
import java.util.Map;

import org.apache.log4j.Logger;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import com.hesong.mq.MQEvent;
import com.hesong.mq.MQManager;
import com.hesong.mq.MQWeixinMessageEvent;
import com.hesong.weChatAdapter.context.AppContext;
import com.hesong.weChatAdapter.context.ContextPreloader;
import com.hesong.weChatAdapter.manager.MessageManager;
import com.hesong.weChatAdapter.model.AccessToken;
import com.hesong.weChatAdapter.tools.API;
import com.hesong.weChatAdapter.tools.WeChatHttpsUtil;

/**
 * 供JsonRPC调用的微信方法集
 * 
 * @author Bowen
 * 
 */
public class WeChatMethodSet {

    private static Logger log = Logger.getLogger(WeChatMethodSet.class);

    /**
     * 发送维系客服消息
     * 
     * @param account
     *            微信公共号，向虚拟客户端发送消息是该值可为null
     * @param fromuser
     *            消息发送方，向真实客户端发送消息时该值可为null
     * @param touser
     *            消息接收方，真实客户端的openID或者虚拟客户端ID
     * @param room
     *            接收消息的房间ID
     * @param msgcontent
     *            消息内容
     * @return 消息发送状态回馈，该返回值为JSONObject，errcode为0时表示消息发送成功，反之查看错误消息errmsg
     */
    public JSONObject SendImMessage(String imtype, String account, String fromuser,
            String touser, String room, String msgcontent) {
        log.info("SendImMessage have been called, send: " + msgcontent);
        JSONObject jo = getJsonContent(msgcontent);
        if (jo == null) {
            return createErrorMsg(9922, "Check your json content: "
                    + msgcontent);
        }
        
        if (imtype.equalsIgnoreCase("app")) {
            if (null == account) {
                // 发给虚拟客户端
                JSONObject tmp = new JSONObject();
                tmp.put("sender", fromuser);
                tmp.put("roomId", room);
                tmp.put("content", jo.getString("Content"));
                tmp.put("msgtype", jo.getString("MsgType"));

//                JSONObject result = WeChatHttpsUtil.httpPostRequest(
//                        API.SEND_MESSAGE_REQUEST_URL.replace("TOUSER", touser),
//                        tmp.toString(), TIMEOUT);
//                result = WeChatMessageAnnotation.sendMessage(touser, tmp.toString());
                return putMessageToMQ(touser, tmp.toString());
            }
            
            
            JSONObject messageToApp = new JSONObject();
            messageToApp.put("ToUserId", touser);
            messageToApp.put("WorkerNum", account);
            messageToApp.put("WorkerName", "");
            messageToApp.put("ServiceId", room);

            messageToApp.put("Tag", new JSONObject());
            
            JSONObject msg = JSONObject.fromObject(msgcontent);
            log.info("msgcontent: " + msg.toString());
            
            String msgtype = msg.getString("msgtype");
            messageToApp.put("MsgType", msgtype);
            messageToApp.put("Content", msg.getJSONObject(msgtype).getString("content"));
            String url = "http://10.4.60.105:3000/qmtapi/staffService/message";
            return WeChatHttpsUtil.httpPostRequest(url, messageToApp.toString(), 0);
        }
        
        // 给虚拟客户端发送消息
        if (account == null) {
            JSONObject tmp = new JSONObject();
            tmp.put("sender", fromuser);
            tmp.put("roomId", room);
            String msgtype = null;
            if (jo.has("msgtype")) {
                msgtype = jo.getString("msgtype");
                JSONObject msgDetail = (JSONObject) jo.get(msgtype);
                if (msgtype.equals(API.TEXT_MESSAGE)) {
                    tmp.put("content", msgDetail.get("content"));
                } else if (msgtype.equals(API.IMAGE_MESSAGE)) {
                    tmp.put("content", jo.getString("media_id"));
                }
            } else if (jo.has("MsgType")) {
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

//            JSONObject result = WeChatHttpsUtil.httpPostRequest(
//                    API.SEND_MESSAGE_REQUEST_URL.replace("TOUSER", touser),
//                    tmp.toString(), TIMEOUT);
//            result = WeChatMessageAnnotation.sendMessage(touser, tmp.toString());
            return putMessageToMQ(touser, tmp.toString());
        }

        jo.put("touser", touser);

        String token = getAccessToken(account);
        if (token != null) {
            // Image message
            if (jo.containsKey("msgtype")
                    && jo.getString("msgtype").equals(API.IMAGE_MESSAGE)) {
                JSONObject imageContent = jo.getJSONObject(API.IMAGE_MESSAGE);
                String image_url = imageContent.getString("media_id");
//                image_url = API.FTP_HTTP_ADDRESS
//                        + image_url.replace("/weixin", "");
                image_url = API.FTP_HTTP_ADDRESS + image_url;
                InputStream input = WeChatHttpsUtil.httpGetInputStream(
                        image_url, "image");

                String postUrl = API.UPLOAD_IMAGE_REQUEST_URL.replace(
                        "ACCESS_TOKEN", token);
                JSONObject mediaIDContent = WeChatHttpsUtil.httpPostFile(
                        postUrl, input);
                if (mediaIDContent.containsKey("media_id")) {
                    imageContent.put("media_id",
                            mediaIDContent.getString("media_id"));
                    jo.put(API.IMAGE_MESSAGE, imageContent);
                    log.info("Send image msg: " + jo.toString());
                } else {
                    return createErrorMsg(mediaIDContent.getInt("errcode"),
                            mediaIDContent.getString("errmsg"));
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

    /**
     * 微信公共号菜单管理
     * 
     * @param account
     *            公共号ID
     * @param action
     *            创建菜单、读取菜单和删除菜单三个动作，对应值为create、get和delete
     * @param menucontent
     *            菜单内容，动作为取回菜单和删除菜单时该值可为null
     * @return 对应各个动作的返回值
     */
    public JSONObject ManageMenu(String account, String action,
            String menucontent) {
        String token = getAccessToken(account);
        if (token == null) {
            return createErrorMsg(9911, "Invalide WeChat account.");
        }
        return MessageManager.manageMenu(token, action, menucontent);
    }

    /**
     * 邀请虚拟客户端加入会话房间
     * 
     * @param account
     * @param from_user
     *            邀请方
     * @param room_id
     *            房间ID
     * @param to_user
     *            被邀请方，虚拟客户端账号
     * @param txt
     *            邀请消息，可为null
     * @param data
     * @param expire
     *            邀请超时时间
     * @param option
     * @return
     */
    public JSONObject Invited(String imtype, String account, String from_user, String room_id,
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
        
        String fromUserNickname = "匿名用户";
        JSONObject user_info = new JSONObject();
        if (ContextPreloader.Account_Map.containsKey(account)) {
            String token = ContextPreloader.Account_Map.get(account).getToken();
            JSONObject userInfo = MessageManager.getClientInfo(token, fromUser);
            if (userInfo.containsKey("nickname")) {
                fromUserNickname = userInfo.getString("nickname");
                user_info.put("fromUserNickname", fromUserNickname);
                user_info.put("headimgurl", "");//userInfo.getString("headimgurl"));
            }
        }
        
        JSONObject post = new JSONObject();
        post.put("msgtype", "invitation");
        post.put("imtype", imtype);
        post.put("fromUserData", user_info);
        post.put("fromUser", fromUser);
        post.put("toUser", toUser);
        post.put("roomId", room_id);
        int timeout = Integer.parseInt(expire);
        post.put("expire", timeout);
//        JSONObject response = WeChatHttpsUtil.httpPostRequest(
//                API.INVITE_REQUEST_URL.replace("ACCOUNT", toUser),
//                post.toString(), timeout);
//        JSONObject response = WeChatMessageAnnotation.sendMessage(toUser, post.toString());
        return putMessageToMQ(toUser, post.toString());
    }

    /**
     * 提示虚拟客户端已被加入会话房间
     * 
     * @param to_account
     * @param to_user
     * @param cause_account
     * @param cause_user
     * @param room_id
     * @param txt
     * @return
     */
    public JSONObject EnteredRoom(String to_account, String to_user,
            String cause_account, String cause_user, String room_id, String txt) {
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
        post.put("msgtype", "enteredRoom");
        post.put("sender", causeUser);
        post.put("toUser", toUser);
        post.put("roomId", room_id);
//        JSONObject response = WeChatHttpsUtil.httpPostRequest(
//                API.ENTER_ROOM_REQUEST_URL.replace("ACCOUNT", toUser),
//                post.toString(), 0);
//        JSONObject response = WeChatMessageAnnotation.sendMessage(toUser, post.toString());
        return putMessageToMQ(toUser, post.toString());
    }

    public JSONObject ExitedRoom(String to_account, String to_user,
            String cause_account, String cause_user, String room_id, String txt) {
        log.info("ExitedRoom have been called, room: " + room_id);
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
        post.put("msgtype", "exitedRoom");
        post.put("sender", causeUser);
        post.put("toUser", toUser);
        post.put("roomId", room_id);
//        JSONObject response = WeChatHttpsUtil.httpPostRequest(
//                API.EXIT_ROOM_REQUEST_URL.replace("ACCOUNT", toUser),
//                post.toString(), 0);
        return putMessageToMQ(toUser, post.toString());
    }

    /**
     * 提示虚拟客户端房间被解散
     * 
     * @param to_account
     * @param to_user
     * @param room_id
     * @param txt
     * @return
     */
    public JSONObject RoomDisposed(String to_account, String to_user,
            String room_id, String txt) {
        log.info("RoomDisposed have been called, room: " + room_id);
        String toUser = null;
        try {
            JSONObject tmp = getJsonContent(to_user);
            toUser = tmp.getString("user");
        } catch (Exception e) {
            toUser = to_user;
        }
        JSONObject post = new JSONObject();
        post.put("msgtype", "roomDisposed");
        post.put("toUser", toUser);
        post.put("roomId", room_id);
//        JSONObject response = WeChatHttpsUtil.httpPostRequest(
//                API.DISPOSE_ROOM_REQUEST_URL.replace("ACCOUNT", toUser),
//                post.toString(), 0);
        return putMessageToMQ(toUser, post.toString());
    }

    private static JSONObject putMessageToMQ(String working_num, String info) {
        try {

            MQEvent event = new MQWeixinMessageEvent(working_num, info);
            MQManager manager = (MQManager) AppContext.getApplicationContext()
                    .getBean("MQManager");
            manager.publishTopicEvent(event);
            return createErrorMsg(0, "ok");
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorMsg(1, e.toString());
        }
    }
    
    private static JSONObject createErrorMsg(int errcode, String errmsg) {
        JSONObject jo = new JSONObject();
        jo.put("errcode", errcode);
        jo.put("errmsg", errmsg);
        return jo;
    }

    /**
     * 取回ACCESS TOKEN
     * 
     * @param account
     *            微信公共号
     * @return ACCESS TOKEN
     */
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

    public static JSONObject createJsonrpcRequest(String method, String id,
            Map<String, Object> paramsList) {
        JSONObject jsonrpc = new JSONObject();
        jsonrpc.put("jsonrpc", "2.0");
        jsonrpc.put("method", method);
        jsonrpc.put("id", id);
        jsonrpc.put("params", paramsList);
        return jsonrpc;
    }

    private static void sendMessageToApp(JSONObject message) {
        
    }
}
