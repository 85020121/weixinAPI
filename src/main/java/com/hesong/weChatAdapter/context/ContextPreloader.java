package com.hesong.weChatAdapter.context;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServlet;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.quartz.SchedulerException;
import org.springframework.context.ApplicationContext;

import com.hesong.weChatAdapter.account.Account;
import com.hesong.weChatAdapter.account.AccountBo;
import com.hesong.weChatAdapter.model.AccessToken;
import com.hesong.weChatAdapter.runner.SmartbusExecutor;
import com.hesong.weChatAdapter.tools.API;
import com.hesong.weChatAdapter.tools.WeChatHttpsUtil;

public class ContextPreloader extends HttpServlet{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    public static Logger ContextLog = Logger.getLogger(ContextPreloader.class);
    
    public static Map<String, AccessToken> Account_Map = new ConcurrentHashMap<String, AccessToken>();
    public static int destUnitId;
    public static int destClientId;
    
//    static{
//        ApplicationContext ctx = AppContext.getApplicationContext();
//        File f = new File(getSettingFilePath(ctx));
//        Map<String, String> setting = null;
//        if (f.exists()){
//            System.out.println("Setting file exist.");
//            SAXReader reader = new SAXReader();
//            setting = new HashMap<String, String>();
//            try {
//                Document document = reader.read(f);
//                Element rootElmt = document.getRootElement();
//                setting.put("host", rootElmt.elementText("host"));
//                setting.put("port", rootElmt.elementText("port"));
//                setting.put("unitid", rootElmt.elementText("unitid"));
//                setting.put("clientid", rootElmt.elementText("clientid"));
//                setting.put("destunitid", rootElmt.elementText("destunitid"));
//                setting.put("destclientid", rootElmt.elementText("destclientid"));
//                ContextLog.info("Setting: "+setting.toString());
//            } catch (Exception e) {
//                setting = null;
//                e.printStackTrace();
//            }
//        }
//        if (setting != null) {
//            SmartbusExecutor.execute(Byte.parseByte(setting.get("unitid")), Byte.parseByte(setting.get("clientid")), setting.get("host"), Short.parseShort(setting.get("port")));
//            destUnitId = Integer.parseInt(setting.get("destunitid"));
//            destClientId = Integer.parseInt(setting.get("destclientid"));
//        } else {
//            SmartbusExecutor.execute((byte)33, (byte)33, "10.4.62.45", (short)8089);
//            destUnitId = 0;
//            destClientId = 14;
//        }
//        AccountBo accountBo = (AccountBo) ctx.getBean("accountBo");
//        @SuppressWarnings("unchecked")
//        List<Account> list = (List<Account>) accountBo.findByAcctype(API.TOKEN);
//        for (Account account : list) {
//            JSONObject jo = JSONObject.fromObject(account.getAccdata());
//            ContextLog.info("Account: "+jo);
//            Account_Map.put(account.getAccname(), WeChatHttpsUtil.getAccessToken(jo.getString("appid"), jo.getString("appsecret")));
//        }
//        ContextLog.info("Account_Map:"+Account_Map.toString());
//        try {
//            new UpdateAccessTokenRunner().task();
//        } catch (IOException | SchedulerException e) {
//            ContextLog.error(e.toString());
//        }
//        
//
//    }
    
    public static String getSettingFilePath(ApplicationContext ctx) {
        String fileName = "";
        try {
            fileName = ctx.getResource("").getFile().getAbsolutePath();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
        // fileName = fileName + "\\WEB-INF\\classes\\properties\\setting.xml";
        fileName = fileName + "\\setting.xml";
        System.out.println(fileName);
        return fileName;
    }

}
