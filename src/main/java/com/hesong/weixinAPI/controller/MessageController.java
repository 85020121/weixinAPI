package com.hesong.weixinAPI.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.hesong.weixinAPI.core.MessageExecutor;
import com.hesong.weixinAPI.tools.API;
import com.hesong.weixinAPI.tools.SignatureChecker;
import com.hesong.weixinAPI.tools.WeChatXMLParser;

@Controller
@RequestMapping("/message")
public class MessageController {

    public static String SIGNATURE = "signature";
    public static String TIMESTAMP = "timestamp";
    public static String NONCE = "nonce";
    public static String ECHOSTR = "echostr";

    private static Logger log = Logger.getLogger(MessageController.class);
    
    @RequestMapping(method = RequestMethod.GET)
    public void checkSignature(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        
        String signature = request.getParameter(SIGNATURE);
        String timestamp = request.getParameter(TIMESTAMP);
        String nonce = request.getParameter(NONCE);
        log.info("signature:" + signature + " timestamp:" + timestamp
                + " nonce:" + nonce);

        if (SignatureChecker.checkSignature(signature, timestamp, nonce,
                API.TOKEN)) {
            PrintWriter out = response.getWriter();
            out.print(request.getParameter(ECHOSTR));
            out.close();
            out = null;
        } else {
            log.info("Signature check failed.");
        }
        
    }

    @RequestMapping(method = RequestMethod.POST)
    public void receiveMessage(HttpServletRequest request,
            HttpServletResponse response) {
        // request.setCharacterEncoding("UTF-8");
        // response.setCharacterEncoding("UTF-8");
        
        try {
            PrintWriter out = response.getWriter();
            out.print("");
            MessageExecutor.messageQueue.put(WeChatXMLParser.parseXML(request));
            out.close();
            out = null;

        } catch (Exception e) {
            log.error("HttpServletResponse getWriter() exception: "
                    + e.toString());
        }

    }
    
    @RequestMapping(value = "/{tanentUn}/clientMessage", method = RequestMethod.POST)
    public void receiveClientMessage(@PathVariable String tanentUn, HttpServletRequest request,
            HttpServletResponse response) {
        
        try {
            PrintWriter out = response.getWriter();
            out.print("");
            Map<String, String> message = WeChatXMLParser.parseXML(request);
            message.put("tanentUn", tanentUn);
            MessageExecutor.messageQueue.put(message);
            out.close();
            out = null;

        } catch (Exception e) {
            log.error("HttpServletResponse getWriter() exception: "
                    + e.toString());
        }

    }

}
