package com.hesong.weChatAdapter.controller;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hesong.weChatAdapter.manager.MessageManager;

@Controller
@RequestMapping("/smartbus")
public class SmartbusController {
    private static Logger log = Logger.getLogger(SmartbusController.class);

    @RequestMapping(method = RequestMethod.GET)
    public String show(ModelMap model) {
        return "smartbusSetting";
    }

    @SuppressWarnings("unchecked")
    @RequestMapping(value = "/connect", method = RequestMethod.POST)
    public @ResponseBody
    String save(@RequestParam("smartbus") String smartbus) {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> smartbusProps;
        try {
            smartbusProps = mapper.readValue(smartbus, Map.class);

            String host = smartbusProps.get("host");
            short port = Short.parseShort(smartbusProps.get("port"));
            byte unitId = Byte.parseByte(smartbusProps.get("unitId"));
            byte clientId = Byte.parseByte(smartbusProps.get("clientId"));
            
            log.info(host + " " + port + " " + unitId + " " + clientId);
            return MessageManager.makeClient(unitId, clientId, host, port);
            
        } catch (IOException e) {
            log.info("Json mapper exception: "+e.toString());
            return "failed";
        }
    }
}
