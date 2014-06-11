package com.hesong.weixinAPI.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/utils")
public class UtilsController {
    
    @RequestMapping(value = "/{clientId}/getQRcodeList}", method = RequestMethod.GET)
    public String getQRcodeList(@PathVariable String clientId) {
        
        return "null";
    }
}
