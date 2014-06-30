package com.hesong.weixinAPI.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hesong.weixinAPI.context.ContextPreloader;
import com.hesong.weixinAPI.core.MessageExecutor;
import com.hesong.weixinAPI.model.AccessToken;
import com.hesong.weixinAPI.tools.WeChatHttpsUtil;

@Controller
@RequestMapping("/utils")
public class UtilsController {
    
    @ResponseBody
    @RequestMapping(value = "/{scene_id}/getQRcodeList", method = RequestMethod.GET)
    public String getQRcodeList(@PathVariable int scene_id) {
        JSONArray qrCodeList = new JSONArray();
        ContextPreloader.ContextLog.info("Create QRCode with scene_id: " + scene_id);
        for (String account : ContextPreloader.staffAccountList) {
            String token = ContextPreloader.Account_Map.get(account).getToken();
            String qrCode = WeChatHttpsUtil.getQRCode(token, scene_id);
            if(null == qrCode) continue;
            qrCodeList.add(qrCode);
        }
        return qrCodeList.toString();
    }
    
    @ResponseBody
    @RequestMapping(value = "/sendMessage", method = RequestMethod.POST)
    public String sendMessage(HttpServletRequest request) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JSONObject json = (JSONObject) JSONSerializer.toJSON(mapper
                    .readValue(request.getInputStream(), Map.class));
            String account = json.getString("account");
            json.remove("account");
            String token = ContextPreloader.Account_Map.get(account).getToken();
            String type = json.getString("msgtype");
            String content = json.getString("content");
            
            JSONArray openid_list = json.getJSONArray("touser");
            for (Object openid : openid_list) {
                MessageExecutor.sendMessage((String)openid, token, content, type);
            }
            return WeChatHttpsUtil.getErrorMsg(0, "Message sended.").toString();
        } catch (Exception e) {
            e.printStackTrace();
            return WeChatHttpsUtil.getErrorMsg(1, "Send message failed: " + e.toString()).toString();
        }
    }
    
    @ResponseBody
    @RequestMapping(value = "/addNewClientAccount", method = RequestMethod.POST)
    public String addNewClient(HttpServletRequest request) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JSONObject json = (JSONObject) JSONSerializer.toJSON(mapper
                    .readValue(request.getInputStream(), Map.class));
            String tenantUn = json.getString("tenantUn");
            String account = json.getString("account");
            String appid = json.getString("appid");
            String appsecret = json.getString("appsecret");
            AccessToken ac = new AccessToken(tenantUn, account, appid, appsecret);
            ContextPreloader.Account_Map.put(account, WeChatHttpsUtil.getAccessToken(ac));
            ContextPreloader.ContextLog.info("New client added into Account_Map: " + ac.toString());
            return WeChatHttpsUtil.getErrorMsg(0, "Client added.").toString();
        } catch (Exception e) {
            e.printStackTrace();
            ContextPreloader.ContextLog.error("Add new client account failed: " + e.toString());
            return WeChatHttpsUtil.getErrorMsg(1, "Add client failed: " + e.toString()).toString();
        }
    }
    
    
}