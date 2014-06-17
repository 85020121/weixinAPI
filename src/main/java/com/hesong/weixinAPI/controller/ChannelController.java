package com.hesong.weixinAPI.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hesong.weixinAPI.core.MessageRouter;


@Controller
@RequestMapping("/channel")
public class ChannelController {

    
    @ResponseBody
    @RequestMapping(value = "/wb/sx", method = RequestMethod.GET)
    public int getQRcodeList(HttpServletRequest request) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JSONObject json = (JSONObject) JSONSerializer.toJSON(mapper
                    .readValue(request.getInputStream(), Map.class));
            String tanentUn = json.getString("tanentUn");
            String user_id = json.getString("user_id");
            String user_name = json.getString("user_name");
            String content = json.getString("content");
            String msgtype = json.getString("msgtype");
            JSONObject returnparams = json.getJSONObject("returnparams");
            
            if (null == tanentUn || !MessageRouter.mulClientStaffMap.containsKey(tanentUn) || MessageRouter.mulClientStaffMap.get(tanentUn).isEmpty()) {
                // TODO No available staff
            } else {
                
            }
            return 200;
        } catch (Exception e) {
            e.printStackTrace();
            return 500;
        }
    }
    
    @ResponseBody
    @RequestMapping(value = "/multiChannelMessage", method = RequestMethod.POST)
    public int multiChannelMessage(HttpServletRequest request) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JSONObject json = (JSONObject) JSONSerializer.toJSON(mapper
                    .readValue(request.getInputStream(), Map.class));

            return 200;
        } catch (Exception e) {
            e.printStackTrace();
            return 500;
        }
    }
}
