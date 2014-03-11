package com.hesong.weChatAdapter.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/chat")
public class ChatController {

    @RequestMapping(method = RequestMethod.GET)
    public String show(){
        return "chat";
    }
}
