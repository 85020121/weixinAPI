package com.hesong.weixinAPI.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/oauth")
public class OauthController {
    
    
    @ResponseBody
    @RequestMapping(method = RequestMethod.GET)
    public String getCode(HttpServletRequest request, HttpServletResponse response) {
        String code = request.getParameter("code");
        System.out.println("code: "+code);

        // TODO: Use code to get client's openid then check in db if this client is
        // already authed, if not, then redirect to auth url
/*            try {
                System.out.println("Redirect");
                response.sendRedirect("https://open.weixin.qq.com/connect/oauth2/authorize?appid=wx735e58e85eb3614a&redirect_uri=http://183.61.81.71:8080/weixinAPI/oauth&response_type=code&scope=snsapi_userinfo&state=test#wechat_redirect");
                System.out.println("Redirect done");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }*/

        return code;
    }

}
