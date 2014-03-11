package com.hesong.weChatAdapter.controller;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/service")
public class ServiceController {


    private static Logger log = Logger.getLogger(ServiceController.class);
    
    @RequestMapping(value = "/repair", method = RequestMethod.GET)
    public String repair(){
        return "repair";
    }
}
