package com.hesong.weixinAPI.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hesong.weixinAPI.context.ContextPreloader;
import com.hesong.weixinAPI.tools.WeChatHttpsUtil;


@Controller
@RequestMapping("/menu")
public class MenuController {
    
    private static Logger log = Logger.getLogger(MenuController.class);
    
    private static String MANAGE_MENU_REQUEST_URL = "https://api.weixin.qq.com/cgi-bin/menu/ACTION?access_token=";

   
    @RequestMapping(value = "/{account}/create", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    public @ResponseBody JSONObject create(@PathVariable String account, HttpServletRequest request){
        try {
            ObjectMapper mapper = new ObjectMapper();
            String menuContent = null;
            menuContent = ((JSONObject) JSONSerializer.toJSON(mapper.readValue(request.getInputStream(), Map.class))).toString();
            log.info("Menu content: "+ menuContent);
            String accessToken = ContextPreloader.Account_Map.get(account).getToken();
            JSONObject jo = manageMenu(accessToken, "create", menuContent);
            return jo;

        } catch (Exception e) {
            log.error("Json mapper exception: " + e.toString());
            e.printStackTrace();
            return null;
        }
    }
    
    @RequestMapping(value = "/{account}/get", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
    public @ResponseBody JSONObject get(@PathVariable String account, HttpServletRequest request){
        try {
            String accessToken = ContextPreloader.Account_Map.get(account).getToken();
            JSONObject jo = manageMenu(accessToken, "get", null);
            return jo;

        } catch (Exception e) {
            log.error("Json mapper exception: " + e.toString());
            e.printStackTrace();
            return null;
        }
    }
    
    @RequestMapping(value = "/{account}/delete", method = RequestMethod.GET, produces = "application/json; charset=utf-8")
    public @ResponseBody JSONObject delete(@PathVariable String account, HttpServletRequest request){
        try {
            String accessToken = ContextPreloader.Account_Map.get(account).getToken();
            JSONObject jo = manageMenu(accessToken, "delete", null);
            return jo;

        } catch (Exception e) {
            log.error("Json mapper exception: " + e.toString());
            e.printStackTrace();
            return null;
        }
    }

    private JSONObject manageMenu(String accessToken, String action,
            String jsonMenu) {
        String request = (MANAGE_MENU_REQUEST_URL+accessToken).replace("ACTION", action);
    
        JSONObject jObject;
        if (action.equals("create")) {
            jObject = WeChatHttpsUtil.httpsRequest(request, "POST",
                    jsonMenu);
        } else {
            jObject = WeChatHttpsUtil.httpsRequest(request, "GET", null);
        }
        return jObject;
    }
}
