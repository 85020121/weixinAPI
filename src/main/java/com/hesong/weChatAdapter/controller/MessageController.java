package com.hesong.weChatAdapter.controller;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dom4j.DocumentException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.hesong.weChatAdapter.manager.MessageManager;
import com.hesong.weChatAdapter.tools.API;
import com.hesong.weChatAdapter.tools.SignatureChecker;
import com.hesong.weChatAdapter.tools.WeChatXMLParser;

@Controller
@RequestMapping("/message")
public class MessageController {

    public static String SIGNATURE = "signature";
    public static String TIMESTAMP = "timestamp";
    public static String NONCE = "nonce";
    public static String ECHOSTR = "echostr";

    private static Logger log = Logger.getLogger(MessageController.class);
    
    public void show(){
        System.out.println("show");
    }

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
    public void response(HttpServletRequest request,
            HttpServletResponse response) {
        // request.setCharacterEncoding("UTF-8");
        // response.setCharacterEncoding("UTF-8");
        log.info("In response...");
        
        try {
            PrintWriter out = response.getWriter();
            out.print(MessageManager.getResponseMessage(WeChatXMLParser.parseXML(request)));
            out.close();
            out = null;
        } catch (IOException | DocumentException e) {
            log.info("HttpServletResponse getWriter() exception: "
                    + e.toString());
        }

    }

}
