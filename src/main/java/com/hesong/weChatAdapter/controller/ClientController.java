package com.hesong.weChatAdapter.controller;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

import com.hesong.ftp.FTPConnectionFactory;
import com.hesong.ftp.FTPEngine;
import com.hesong.jsonrpc.WeChatMethodSet;
import com.hesong.smartbus.client.PackInfo;
import com.hesong.weChatAdapter.context.ContextPreloader;
import com.hesong.weChatAdapter.manager.MessageManager;
import com.hesong.weChatAdapter.model.ChatMessage;
import com.hesong.weChatAdapter.runner.JsonrpcHandlerRunner;
import com.hesong.weChatAdapter.runner.SmartbusExecutor;
import com.hesong.weChatAdapter.tools.API;
import com.hesong.weChatAdapter.tools.WeChatHttpsUtil;

@Controller
@RequestMapping("/client")
public class ClientController {

    private static Logger log = Logger.getLogger(ClientController.class);

    private static int DEFFER_TIME = 15000;

    private final Map<String, DeferredResult<ChatMessage>> chatRequests = new ConcurrentHashMap<String, DeferredResult<ChatMessage>>();
    private final Map<String, Set<String>> roomList = new ConcurrentHashMap<String, Set<String>>();


    //@ResponseBody
    @RequestMapping(value = "/{account}/login", method = RequestMethod.GET)
    public String login(@PathVariable String account, HttpSession session, HttpServletResponse response) {
        
//        response.addCookie(new Cookie("MOCK_CLIENT_ID", account));
//        session.setAttribute("MOCK_CLIENT_ID", account);
//        roomList.put(account, new HashSet<String>());
//        roomList.get(account).add("tmp");
        // JSONRPC PARAMS
        Map<String, Object> paramsList = new HashMap<String, Object>();
        paramsList.put("imtype", "weixin");
        paramsList.put("account", null);

        JSONObject user = new JSONObject();
        user.put("user", account);
        user.put("usertype", API.MOCK_CLIENT);

        paramsList.put("user", user);

        String id = UUID.randomUUID().toString();

        JSONObject jsonrpc = WeChatMethodSet.createJsonrpcRequest(
                "imsm.ClientLoggedIn", id, paramsList);
        PackInfo pack = new PackInfo((byte) ContextPreloader.destUnitId,
                (byte) ContextPreloader.destClientId,
                (byte) ContextPreloader.srcUnitId,
                (byte) ContextPreloader.srctClientId, jsonrpc.toString());
        try {
            BlockingQueue<String> ackRet = new LinkedBlockingDeque<String>();
            JsonrpcHandlerRunner.loginAckRetQueue.put(id, ackRet);
            SmartbusExecutor.responseQueue.put(pack);
            String ret = ackRet.poll(5, TimeUnit.SECONDS);
            ackRet = null;
            JsonrpcHandlerRunner.loginAckRetQueue.remove(id);
            log.info("Login ret: " + ret);
            if (ret == null) {
                log.error("NO LOGIN ACK RETURN for account: "+account);
                return "Failed";
            }
            if (ret.equals("OK")) {
                // TODO logged in
                response.addCookie(new Cookie("MOCK_CLIENT_ID", account));
                session.setAttribute("MOCK_CLIENT_ID", account);
                if (!roomList.containsKey(account)) {
                    roomList.put(account, new HashSet<String>());
                }
                return "chatRoom";
            }

            return "Failed";
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "Failed";
        }

    }
    
    @ResponseBody
    @RequestMapping(value = "/{account}/relogin", method = RequestMethod.GET)
    public String relogin(@PathVariable String account, HttpSession session, HttpServletResponse response) {
        Map<String, Object> paramsList = new HashMap<String, Object>();
        paramsList.put("imtype", "weixin");
        paramsList.put("account", null);

        JSONObject user = new JSONObject();
        user.put("user", account);
        user.put("usertype", API.MOCK_CLIENT);

        paramsList.put("user", user);

        String id = UUID.randomUUID().toString();

        JSONObject jsonrpc = WeChatMethodSet.createJsonrpcRequest(
                "imsm.ClientLoggedIn", id, paramsList);
        PackInfo pack = new PackInfo((byte) ContextPreloader.destUnitId,
                (byte) ContextPreloader.destClientId,
                (byte) ContextPreloader.srcUnitId,
                (byte) ContextPreloader.srctClientId, jsonrpc.toString());
        try {
            BlockingQueue<String> ackRet = new LinkedBlockingDeque<String>();
            JsonrpcHandlerRunner.loginAckRetQueue.put(id, ackRet);
            SmartbusExecutor.responseQueue.put(pack);
            String ret = ackRet.poll(5, TimeUnit.SECONDS);
            ackRet = null;
            JsonrpcHandlerRunner.loginAckRetQueue.remove(id);
            
            if (ret == null) {
                log.error("NO RELOGIN ACK RETURN for account: "+account);
                return "failed";
            }
            if (ret.equals("OK")) {
                // TODO logged in
                //response.addCookie(new Cookie("MOCK_CLIENT_ID", account));
                session.setAttribute("MOCK_CLIENT_ID", account);
                if (!roomList.containsKey(account)) {
                    roomList.put(account, new HashSet<String>());
                }
            }

            return "Sucess";
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "Failed";
        }

    }

    @RequestMapping(value = "/{account}/logout", method = RequestMethod.GET)
    public String logout(@PathVariable String account, HttpSession session) {

        // JSONRPC PARAMS
        Map<String, Object> paramsList = new HashMap<String, Object>();
        paramsList.put("imtype", "weixin");
        paramsList.put("account", null);
        paramsList.put("user", account);
        String id = UUID.randomUUID().toString();
        JSONObject jsonrpc = WeChatMethodSet.createJsonrpcRequest(
                "imsm.ClientLoggedOut", id, paramsList);
        PackInfo pack = new PackInfo((byte) ContextPreloader.destUnitId,
                (byte) ContextPreloader.destClientId,
                (byte) ContextPreloader.srcUnitId,
                (byte) ContextPreloader.srctClientId, jsonrpc.toString());
        try {
            SmartbusExecutor.responseQueue.put(pack);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            log.error("Logout exception: "+e.toString());
            return "Failed";
        }
        return "Success";

    }

    @RequestMapping(value = "/{account}/chatroom", method = RequestMethod.GET)
    @ResponseBody
    public JSONArray chatRoom(@PathVariable String account, HttpServletResponse response) {
        Map<String, Object> paramsList = new HashMap<String, Object>();
        paramsList.put("imtype", "weixin");
        paramsList.put("account", null);
        paramsList.put("user", account);

        String id = UUID.randomUUID().toString();

        JSONObject jsonrpc = WeChatMethodSet.createJsonrpcRequest(
                "imsm.GetAgentRooms", id, paramsList);
        PackInfo pack = new PackInfo((byte) ContextPreloader.destUnitId,
                (byte) ContextPreloader.destClientId,
                (byte) ContextPreloader.srcUnitId,
                (byte) ContextPreloader.srctClientId, jsonrpc.toString());
        
        try {
            BlockingQueue<Object> ackRet = new LinkedBlockingDeque<Object>();
            JsonrpcHandlerRunner.getRoomsRetQueue.put(id, ackRet);
            SmartbusExecutor.responseQueue.put(pack);
            Object ret = ackRet.poll(10, TimeUnit.SECONDS);
            ackRet = null;
            JsonrpcHandlerRunner.getRoomsRetQueue.remove(id);
            
            if (ret == null) {
                return null;
            }
            JSONArray ja = JSONArray.fromObject(ret);
            return ja;
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
            return null;
        }
    }

    @RequestMapping(value = "/getMessages", method = RequestMethod.GET)
    @ResponseBody
    public DeferredResult<ChatMessage> getMessages(HttpSession session) {
        // 取出当前登录用户
        final String clientId = (String) session.getAttribute("MOCK_CLIENT_ID");
        // 创建DeferredResult<Message>
        DeferredResult<ChatMessage> dr = new DeferredResult<ChatMessage>(DEFFER_TIME);

        // 若用户不存在则直接返回，否则将其放入用户请求列表中然后返回
        if (null == clientId) {
            return dr;
        } else {
            // 当DeferredResult对客户端响应后将其从列表中移除
            dr.onCompletion(new Runnable() {
                @Override
                public void run() {
                    // Map<String, DeferredResult<ChatMessage>> tmp =
                    // chatRoomMap.get(clientId);
                    // if (tmp != null) {
                    // tmp.remove(clientId);
                    // }
                    chatRequests.remove(clientId);
                }
            });
            // Map<String, DeferredResult<ChatMessage>> tmp =
            // chatRoomMap.get(clientId);
            // log.info("ROOOOOOOOM map:"+ chatRoomMap.toString());
            // if (tmp==null) {
            // log.info("Create new room for client: "+clientId);
            // tmp = new ConcurrentHashMap<String,
            // DeferredResult<ChatMessage>>();
            // tmp.put(clientId, dr);
            // chatRoomMap.put(clientId, tmp);
            // }else{
            // tmp.put(clientId, dr);
            // }
            chatRequests.put(clientId, dr);
            return dr;
        }
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/{account}/sendMessageRequest", method = RequestMethod.POST)
    public @ResponseBody
    JSONObject sendMessage(@PathVariable String account,
            HttpServletRequest request, HttpSession session) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> messageMap;
        try {
            messageMap = mapper.readValue(request.getInputStream(), Map.class);
            String content = messageMap.get("content");

            ChatMessage msg = new ChatMessage();
            String msgtype = messageMap.get("msgtype");
            msg.setContent(content);
            // msg.setDate(sdf.format(new Date()));
            msg.setSender(messageMap.get("sender"));
            msg.setRoomId(messageMap.get("roomId"));
            msg.setMsgtype(msgtype);
            // 发布消息给所有用户
            processMessage(msg, account);

            return WeChatHttpsUtil.getErrorMsg(0, "OK");

        } catch (Exception e) {
            log.error("Json mapper exception: " + e.toString());
            return WeChatHttpsUtil.getErrorMsg(7999, e.toString());
        }
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/{account}/msg", method = RequestMethod.POST)
    public @ResponseBody
    String sendMessageWithSmartbus(@PathVariable String account,
            HttpServletRequest request, HttpSession session) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> messageMap;
        try {
            messageMap = mapper.readValue(request.getInputStream(), Map.class);
            String content = messageMap.get("content");

            ChatMessage msg = new ChatMessage();
            String msgtype = messageMap.get("msgtype");
            msg.setContent(content);
            // msg.setDate(sdf.format(new Date()));
            msg.setSender(messageMap.get("sender"));
            msg.setRoomId(messageMap.get("roomId"));
            // msg.setRoomId(messageMap.get("roomId"));
            msg.setMsgtype(msgtype);
            // 发布消息给所有用户
            processMessage(msg, account);

            // JSONRPC PARAMS
            JSONObject jsonrpc = new JSONObject();
            jsonrpc.put("jsonrpc", "2.0");
            jsonrpc.put("method", "imsm.ImMessageReceived");
            jsonrpc.put("id", UUID.randomUUID().toString());
            Map<String, Object> paramsList = new HashMap<String, Object>();
            paramsList.put("imtype", "weixin");
            paramsList.put("account", null);

            JSONObject user = new JSONObject();
            user.put("user", account);
            user.put("usertype", API.MOCK_CLIENT);

            paramsList.put("user", user);
            paramsList.put("room_id", messageMap.get("roomId"));
            paramsList.put("msgtype", msgtype);

            JSONObject sendmsg = new JSONObject();
            sendmsg.put("msgtype", msgtype);
            sendmsg.put("room_id", messageMap.get("roomId"));
            JSONObject msgcontent = new JSONObject();
            msgcontent.put("content", content);
            sendmsg.put(msgtype, msgcontent);
            paramsList.put("msgcontent", sendmsg);

            jsonrpc.put("params", paramsList);
            // JSONRPC END

            PackInfo pack = new PackInfo((byte) ContextPreloader.destUnitId,
                    (byte) ContextPreloader.destClientId,
                    (byte) ContextPreloader.srcUnitId,
                    (byte) ContextPreloader.srctClientId, jsonrpc.toString());
            try {
                SmartbusExecutor.responseQueue.put(pack);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                //e.printStackTrace();
                log.error("Response BlockingQueue exception: "+e.toString());
                log.error("Response head: "+pack.toString());
            }
            return "Success";

        } catch (IOException e) {
            log.error("Json mapper exception: " + e.toString());
            return "Failed";
        }
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/{account}/invite", method = RequestMethod.POST)
    public @ResponseBody
    JSONObject invite(@PathVariable String account, HttpServletRequest request) {
        log.info("invite have been called.");
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, String> inviteProps = mapper.readValue(
                    request.getInputStream(), Map.class);

            ChatMessage msg = new ChatMessage();
            msg.setMsgtype("invitation");
            msg.setRoomId(inviteProps.get("roomId"));
            msg.setSender(inviteProps.get("sender"));
            processMessage(msg, account);

        } catch (IOException e) {
            log.error("Json mapper exception: " + e.toString());
            e.printStackTrace();
            return WeChatHttpsUtil.getErrorMsg(8002,
                    "Invitation request exception: " + e.toString());
        }
        return WeChatHttpsUtil.getErrorMsg(0, "Invitation received.");
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/{account}/ackInvitation", method = RequestMethod.POST)
    public @ResponseBody
    String ackInvitation(@PathVariable String account,
            HttpServletRequest request) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, String> inviteProps = mapper.readValue(
                    request.getInputStream(), Map.class);
            Map<String, Object> paramsList = new HashMap<String, Object>();
            paramsList.put("imtype", "weixin");

            JSONObject user = new JSONObject();
            user.put("user", account);
            user.put("usertype", API.MOCK_CLIENT);

            paramsList.put("user", user);
            paramsList.put("room_id", inviteProps.get("roomId"));
            paramsList.put("agreed", inviteProps.get("agreed")
                    .equalsIgnoreCase("true") ? true : false);
            String id = UUID.randomUUID().toString();
            JSONObject jsonrpc = WeChatMethodSet.createJsonrpcRequest(
                    "imsm.AckInviteEnterRoom", id, paramsList);
            PackInfo pack = new PackInfo((byte) ContextPreloader.destUnitId,
                    (byte) ContextPreloader.destClientId,
                    (byte) ContextPreloader.srcUnitId,
                    (byte) ContextPreloader.srctClientId, jsonrpc.toString());
            try {
                SmartbusExecutor.responseQueue.put(pack);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                //e.printStackTrace();
                log.error("Response BlockingQueue exception: "+e.toString());
                log.error("Response head: "+pack.toString());
            }
        } catch (IOException e) {
            log.error("Json mapper exception: " + e.toString());
            e.printStackTrace();
            return "Failed.";
        }
        return "Success.";
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/{account}/enter_room", method = RequestMethod.POST)
    public @ResponseBody
    String enterRoom(@PathVariable String account, HttpServletRequest request) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, String> requestMap = mapper.readValue(
                    request.getInputStream(), Map.class);
            Map<String, Object> paramsList = new HashMap<String, Object>();
            paramsList.put("imtype", "weixin");

            JSONObject user = new JSONObject();
            user.put("user", account);
            user.put("usertype", API.MOCK_CLIENT);

            paramsList.put("user", user);
            paramsList.put("room_id", requestMap.get("roomId"));
            paramsList.put("options", null);
            String id = UUID.randomUUID().toString();
            JSONObject jsonrpc = WeChatMethodSet.createJsonrpcRequest(
                    "imsm.EnterRoom", id, paramsList);
            PackInfo pack = new PackInfo((byte) ContextPreloader.destUnitId,
                    (byte) ContextPreloader.destClientId,
                    (byte) ContextPreloader.srcUnitId,
                    (byte) ContextPreloader.srctClientId, jsonrpc.toString());
            try {
                SmartbusExecutor.responseQueue.put(pack);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
//                e.printStackTrace();
                log.error("Response BlockingQueue exception: "+e.toString());
                log.error("Response head: "+pack.toString());
            }
        } catch (IOException e) {
            log.error("Json mapper exception: " + e.toString());
            e.printStackTrace();
            return "Failed.";
        }
        return "Success.";
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/{account}/exit_room", method = RequestMethod.POST)
    public @ResponseBody
    String exitRoom(@PathVariable String account, HttpServletRequest request) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, String> requestMap = mapper.readValue(
                    request.getInputStream(), Map.class);
            Map<String, Object> paramsList = new HashMap<String, Object>();
            paramsList.put("imtype", "weixin");

            JSONObject user = new JSONObject();
            user.put("user", account);
            user.put("usertype", API.MOCK_CLIENT);

            paramsList.put("user", user);
            paramsList.put("room_id", requestMap.get("roomId"));
            String id = UUID.randomUUID().toString();
            JSONObject jsonrpc = WeChatMethodSet.createJsonrpcRequest(
                    "imsm.ExitRoom", id, paramsList);
            PackInfo pack = new PackInfo((byte) ContextPreloader.destUnitId,
                    (byte) ContextPreloader.destClientId,
                    (byte) ContextPreloader.srcUnitId,
                    (byte) ContextPreloader.srctClientId, jsonrpc.toString());
            try {
                SmartbusExecutor.responseQueue.put(pack);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
//                e.printStackTrace();
                log.error("Response BlockingQueue exception: "+e.toString());
                log.error("Response head: "+pack.toString());
            }
        } catch (IOException e) {
            log.error("Json mapper exception: " + e.toString());
            e.printStackTrace();
            return "Failed.";
        }
        return "Success.";
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/{account}/entered_room", method = RequestMethod.POST)
    @ResponseBody
    public JSONObject enteredRoom(@PathVariable String account,
            HttpServletRequest request, HttpSession session) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, String> requestMap = mapper.readValue(
                    request.getInputStream(), Map.class);

            ChatMessage msg = new ChatMessage();
            msg.setMsgtype("enteredRoom");
            msg.setRoomId(requestMap.get("roomId"));
            String sender  = requestMap.get("sender");
            msg.setSender(sender);
            processMessage(msg, account);
            if(account.equals(sender)) {
                if (roomList.containsKey(sender)) {
                    roomList.get(sender).add(requestMap.get("roomId"));
                } else {
                    Set<String> roomIdSet = new HashSet<String>();
                    roomIdSet.add(requestMap.get("roomId"));
                    roomList.put(sender, roomIdSet);
                }
                
            }

        } catch (Exception e) {
            log.error("Json mapper exception: " + e.toString());
            e.printStackTrace();
            return WeChatHttpsUtil.getErrorMsg(8002,
                    "Entered room request exception: " + e.toString());
        }
        return WeChatHttpsUtil.getErrorMsg(0, "Entered room.");
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/{account}/exited_room", method = RequestMethod.POST)
    @ResponseBody
    public JSONObject exitedRoom(@PathVariable String account,
            HttpServletRequest request, HttpSession session) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, String> requestMap = mapper.readValue(
                    request.getInputStream(), Map.class);

            ChatMessage msg = new ChatMessage();
            msg.setMsgtype("exitedRoom");
            msg.setRoomId(requestMap.get("roomId"));
            String sender  = requestMap.get("sender");
            msg.setSender(sender);
            processMessage(msg, account);
            if(account.equals(sender) && roomList.containsKey(sender)) {
                roomList.get(sender).remove(requestMap.get("roomId"));
            }

        } catch (IOException e) {
            log.error("Json mapper exception: " + e.toString());
            e.printStackTrace();
            return WeChatHttpsUtil.getErrorMsg(8002,
                    "Exited room request exception: " + e.toString());
        }
        return WeChatHttpsUtil.getErrorMsg(0, "Exited room.");
    }
    
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/{account}/dispose_room", method = RequestMethod.POST)
    @ResponseBody
    public JSONObject disposeRoom(@PathVariable String account,
            HttpServletRequest request, HttpSession session) {
        log.info("DisposeRoom have been called.");
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, String> requestMap = mapper.readValue(
                    request.getInputStream(), Map.class);

            ChatMessage msg = new ChatMessage();
            msg.setMsgtype("disposeRoom");
            msg.setRoomId(requestMap.get("roomId"));
            processMessage(msg, account);
            log.info("Dispose room msg processed.");

        } catch (Exception e) {
            e.printStackTrace();
            return WeChatHttpsUtil.getErrorMsg(8002,
                    "Dipose room request exception: " + e.toString());
        }
        return WeChatHttpsUtil.getErrorMsg(0, "Room disposed.");
    }
    
    @RequestMapping(value = "/{account}/upload", method = RequestMethod.POST)
    @ResponseBody
    public String upload(@PathVariable String account, @RequestParam("Filedata") MultipartFile mulpartFile,
            @RequestParam("roomId") String roomId, HttpServletRequest request) {
        if (roomId == null) {
            log.error("RoomId is not exist.");
            return "Failed";
        }
        
        try {
            
            FTPClient ftp = FTPConnectionFactory.getDefaultFTPConnection();
            // Upload image to FTP server
            String filename = MessageManager.sdf_time.format(new Date())+"_"+mulpartFile.getOriginalFilename();
            String dirPath = MessageManager.getDirName("image/send", account);
            if(FTPEngine.uploadFile(ftp, dirPath, filename, mulpartFile.getInputStream())){
                
                String media_id = dirPath+filename;
                String msgtype = "image";
                
                // JSONRPC PARAMS
                JSONObject jsonrpc = new JSONObject();
                jsonrpc.put("jsonrpc", "2.0");
                jsonrpc.put("method", "imsm.ImMessageReceived");
                jsonrpc.put("id", UUID.randomUUID().toString());
                Map<String, Object> paramsList = new HashMap<String, Object>();
                paramsList.put("imtype", "weixin");
                paramsList.put("account", null);

                JSONObject user = new JSONObject();
                user.put("user", account);
                user.put("usertype", API.MOCK_CLIENT);

                paramsList.put("user", user);
                paramsList.put("room_id", roomId);
                paramsList.put("msgtype", msgtype);

                JSONObject sendmsg = new JSONObject();
                sendmsg.put("msgtype", msgtype);
                sendmsg.put("room_id", roomId);
                JSONObject msgcontent = new JSONObject();
                msgcontent.put("media_id", media_id);
                sendmsg.put(msgtype, msgcontent);
                paramsList.put("msgcontent", sendmsg);

                jsonrpc.put("params", paramsList);
                // JSONRPC END

                PackInfo pack = new PackInfo((byte) ContextPreloader.destUnitId,
                        (byte) ContextPreloader.destClientId,
                        (byte) ContextPreloader.srcUnitId,
                        (byte) ContextPreloader.srctClientId, jsonrpc.toString());
                try {
                    SmartbusExecutor.responseQueue.put(pack);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                    log.error("Response BlockingQueue exception: "+e.toString());
                    log.error("Response head: "+pack.toString());
                    return "Falied";
                }
                
                return media_id;
            } else {
                log.error("FTP upload image failed.");
            }
            
        } catch (IllegalStateException | IOException e) {
            log.error("Upload image exception: "+e.toString());
            e.printStackTrace();
        }
        return "Failed";
        
    }
    

    private void processMessage(ChatMessage msg, String clientId) {
        DeferredResult<ChatMessage> tmp = chatRequests.get(clientId);
        if (tmp == null) {
            return;
        } else {
            tmp.setResult(msg);
        }
    }

}
