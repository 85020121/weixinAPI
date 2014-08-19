package com.hesong.weChatAdapter.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hesong.smartbus.client.PackInfo;
import com.hesong.weChatAdapter.context.ContextPreloader;
import com.hesong.weChatAdapter.runner.SmartbusExecutor;
import com.hesong.weChatAdapter.tools.API;
import com.hesong.weChatAdapter.tools.SignatureChecker;
import com.hesong.weChatAdapter.tools.WeChatHttpsUtil;

@Controller
@RequestMapping("/api/v1")
public class APIController {
    
    private static Logger log = Logger.getLogger(APIController.class);
    
    @SuppressWarnings("unchecked")
    @ResponseBody
    @RequestMapping(value = "/{appid}/staffService/message", method = RequestMethod.POST)
    public String sendMessage(@PathVariable String appid, HttpServletRequest request) {
        JSONObject check = checkSignature(request, appid);
        if (check.getInt("errcode") != 0) {
            return check.toString();
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> messageMap;
            messageMap = mapper.readValue(request.getInputStream(), Map.class);
            String content = messageMap.get("content");
            String msgtype = messageMap.get("msgtype");
            String room_id = messageMap.get("service_id");

            // JSONRPC PARAMS
            JSONObject jsonrpc = new JSONObject();
            jsonrpc.put("jsonrpc", "2.0");
            jsonrpc.put("method", "imsm.ImMessageReceived");
            jsonrpc.put("id", UUID.randomUUID().toString());
            Map<String, Object> paramsList = new HashMap<String, Object>();
            paramsList.put("imtype", "weixin");
            paramsList.put("account", null);

            JSONObject user = new JSONObject();
            user.put("user", messageMap.get("user_id"));
            user.put("usertype", API.MOCK_WEIXIN_CLIENT);

            paramsList.put("user", user);
            paramsList.put("room_id", room_id);
            paramsList.put("msgtype", msgtype);

            JSONObject sendmsg = new JSONObject();
            sendmsg.put("msgtype", msgtype);
            sendmsg.put("room_id", room_id);
            JSONObject msgcontent = new JSONObject();
            msgcontent.put("content", content);
            sendmsg.put(msgtype, msgcontent);
            paramsList.put("msgcontent", sendmsg);

            jsonrpc.put("params", paramsList);
            // JSONRPC END

//            PackInfo pack = new PackInfo((byte) ContextPreloader.destUnitId,
//                    (byte) ContextPreloader.destClientId,
//                    (byte) ContextPreloader.srcUnitId,
//                    (byte) ContextPreloader.srctClientId, jsonrpc.toString());
            try {
                SmartbusExecutor.responseQueue.put(jsonrpc.toString());
                return WeChatHttpsUtil.getErrorMsg(0, "ok").toString();
            } catch (InterruptedException e) {
                e.printStackTrace();
                log.error("Response BlockingQueue exception: "+e.toString());
                return WeChatHttpsUtil.getErrorMsg(30004, e.toString()).toString();
            }
            
        } catch (IOException e) {
            log.error("Json mapper exception: " + e.toString());
            return WeChatHttpsUtil.getErrorMsg(30005, e.toString()).toString();
        }
    }
    
    @SuppressWarnings("unchecked")
    @ResponseBody
    @RequestMapping(value = "/{appid}/staffService/demand", method = RequestMethod.POST)
    public String demande(@PathVariable String appid, HttpServletRequest request) {
        JSONObject check = checkSignature(request, appid);
        if (check.getInt("errcode") != 0) {
            return check.toString();
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> messageMap;
            messageMap = mapper.readValue(request.getInputStream(), Map.class);
            String content = messageMap.get("content");
            String msgtype = messageMap.get("msgtype");
            String room_id = messageMap.get("service_id");

            // JSONRPC PARAMS
            JSONObject jsonrpc = new JSONObject();
            jsonrpc.put("jsonrpc", "2.0");
            jsonrpc.put("method", "imsm.ImMessageReceived");
            jsonrpc.put("id", UUID.randomUUID().toString());
            Map<String, Object> paramsList = new HashMap<String, Object>();
            paramsList.put("imtype", "weixin");
            paramsList.put("account", null);

            JSONObject user = new JSONObject();
            user.put("user", messageMap.get("user_id"));
            user.put("usertype", API.MOCK_WEIXIN_CLIENT);

            paramsList.put("user", user);
            paramsList.put("room_id", room_id);
            paramsList.put("msgtype", msgtype);

            JSONObject sendmsg = new JSONObject();
            sendmsg.put("msgtype", msgtype);
            sendmsg.put("room_id", room_id);
            JSONObject msgcontent = new JSONObject();
            msgcontent.put("content", content);
            sendmsg.put(msgtype, msgcontent);
            paramsList.put("msgcontent", sendmsg);

            jsonrpc.put("params", paramsList);
            // JSONRPC END

//            PackInfo pack = new PackInfo((byte) ContextPreloader.destUnitId,
//                    (byte) ContextPreloader.destClientId,
//                    (byte) ContextPreloader.srcUnitId,
//                    (byte) ContextPreloader.srctClientId, jsonrpc.toString());
            try {
                SmartbusExecutor.responseQueue.put(jsonrpc.toString());
                return WeChatHttpsUtil.getErrorMsg(0, "ok").toString();
            } catch (InterruptedException e) {
                e.printStackTrace();
                log.error("Response BlockingQueue exception: "+e.toString());
                return WeChatHttpsUtil.getErrorMsg(30004, e.toString()).toString();
            }
            
        } catch (IOException e) {
            log.error("Json mapper exception: " + e.toString());
            return WeChatHttpsUtil.getErrorMsg(30005, e.toString()).toString();
        }
    }
 
    private static JSONObject checkSignature(HttpServletRequest request, String appid) {
        String signature = request.getParameter("signature");
        String timestamp = request.getParameter("timestamp");
        if (null == signature || null == timestamp) {
            return WeChatHttpsUtil.getErrorMsg(30001, "signature或timestamp参数不存在。");
        }
        String appsecret = ContextPreloader.appid_appsecret.get(appid);
        if (null == appsecret) {
            return WeChatHttpsUtil.getErrorMsg(30002, "Appsecret不存在，请检查appid参数。");
        }
        if (!SignatureChecker.checkAPISignature(signature, timestamp, appid, appsecret)) {
            return WeChatHttpsUtil.getErrorMsg(30003, "Signature验证失败。");
        }
        return WeChatHttpsUtil.getErrorMsg(0, "ok");
    }
}
