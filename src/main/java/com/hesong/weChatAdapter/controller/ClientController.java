package com.hesong.weChatAdapter.controller;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
@RequestMapping("/client")
public class ClientController {
    
    private static Logger log = Logger.getLogger(ClientController.class);
    
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/{account}/login", method = RequestMethod.POST)
    public @ResponseBody String login(@PathVariable String account, @RequestParam("login") String login){
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> loginProps;
        try {
            loginProps = mapper.readValue(login, Map.class);

            String password = loginProps.get("password");
            log.info("Account: "+account+" Password: "+password);

        } catch (IOException e) {
            log.info("Json mapper exception: " + e.toString());
            return "failed";
        }
        return "success";
    }

}
