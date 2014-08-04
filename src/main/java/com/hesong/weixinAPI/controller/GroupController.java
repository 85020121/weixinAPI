package com.hesong.weixinAPI.controller;


import net.sf.json.JSONObject;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hesong.weixinAPI.core.MessageRouter;
import com.hesong.weixinAPI.tools.WeChatHttpsUtil;


@Controller
@RequestMapping("/group")
public class GroupController {
    
    private static String CREATE_GROUP_URL = "https://api.weixin.qq.com/cgi-bin/groups/create?access_token=";
    
    @ResponseBody
    @RequestMapping(value = "/{account}/create/{name}", method = RequestMethod.GET)
    public JSONObject createGroup(@PathVariable String account, @PathVariable String name){
        try {
            String token = MessageRouter.getAccessToken(account);
            JSONObject group = new JSONObject();
            JSONObject group_name = new JSONObject();
            group_name.put("name", name);
            group.put("group", group);
            String query = CREATE_GROUP_URL+token;
            return WeChatHttpsUtil.httpsRequest(query, "POST", group.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}
