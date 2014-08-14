package com.hesong.weChatAdapter.context;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServlet;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.quartz.SchedulerException;
import org.springframework.context.ApplicationContext;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import com.hesong.ftp.FTPConnectionFactory;
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
    public static int srcUnitId;
    public static int srctClientId;
    
    public static Map<String,String> appid_appsecret = new HashMap<String, String>();
//    public static JedisPool jedisPool;
//    public static int redis_db_num;
//    public static int redis_key_expire;
    
    static{
        
        appid_appsecret.put("1001", "abcdef");
        
//        ResourceBundle bundle = ResourceBundle.getBundle("redis");
//        if (null == bundle) {
//            ContextLog.error("redis.properties not found!");
//        }
//        
//        JedisPoolConfig config = new JedisPoolConfig();
//        config.setMaxTotal(Integer.parseInt(bundle.getString("redis.pool.maxTotal")));
//        config.setMaxIdle(Integer.parseInt(bundle.getString("redis.pool.maxIdle")));
//        config.setMaxWaitMillis(Integer.parseInt(bundle.getString("redis.pool.maxWait")));
//        config.setTestOnBorrow(true);
//        config.setTestOnReturn(true);
//        
//        redis_db_num = Integer.parseInt(bundle.getString("redis.db_num"));
//        redis_key_expire = Integer.parseInt(bundle.getString("redis.key_expire"));
//        
//        jedisPool = new JedisPool(config, bundle.getString("redis.host"),
//                Integer.parseInt(bundle.getString("redis.port")), 30000);
        
        ApplicationContext ctx = AppContext.getApplicationContext();
        File f = new File(getSettingFilePath(ctx));
        Map<String, String> smartbus_setting = null;
        Map<String, String> ftp_setting = null;
        if (f.exists()){
            SAXReader reader = new SAXReader();
            smartbus_setting = new HashMap<String, String>();
            ftp_setting = new HashMap<String, String>();
            try {
                
                Document document = reader.read(f);
                Element rootElmt = document.getRootElement();
                
               Element smartbusElmt = rootElmt.element("smartbus");
                smartbus_setting.put("host", smartbusElmt.elementText("host"));
                smartbus_setting.put("port", smartbusElmt.elementText("port"));
                smartbus_setting.put("unitid", smartbusElmt.elementText("unitid"));
                smartbus_setting.put("clientid", smartbusElmt.elementText("clientid"));
                smartbus_setting.put("destunitid", smartbusElmt.elementText("destunitid"));
                smartbus_setting.put("destclientid", smartbusElmt.elementText("destclientid"));
                ContextLog.info("Smartbus setting: "+smartbus_setting.toString());
                
                Element ftpElmt = rootElmt.element("ftp");
                ftp_setting.put("host", ftpElmt.elementText("host"));
                ftp_setting.put("username", ftpElmt.elementText("username"));
                ftp_setting.put("password", ftpElmt.elementText("password"));
                ContextLog.info("FTP setting: "+ftp_setting.toString());

            } catch (Exception e) {
                smartbus_setting = null;
                e.printStackTrace();
            }
        } else {
            ContextLog.error("Setting file not exist.");
        }
        
        // Set smartbus connection
        if (smartbus_setting != null) {
            srcUnitId = Byte.parseByte(smartbus_setting.get("unitid"));
            srctClientId = Byte.parseByte(smartbus_setting.get("clientid"));
            SmartbusExecutor.execute(Byte.parseByte(smartbus_setting.get("unitid")), Byte.parseByte(smartbus_setting.get("clientid")), smartbus_setting.get("host"), Short.parseShort(smartbus_setting.get("port")));
            destUnitId = Integer.parseInt(smartbus_setting.get("destunitid"));
            destClientId = Integer.parseInt(smartbus_setting.get("destclientid"));
        } else {
            ContextLog.info("Default smartbus.");
            SmartbusExecutor.execute((byte)33, (byte)33, "10.4.62.45", (short)8089);
            destUnitId = 0;
            destClientId = 14;
        }
        
        AccountBo accountBo = (AccountBo) ctx.getBean("accountBo");
        @SuppressWarnings("unchecked")
        List<Account> list = (List<Account>) accountBo.findByAcctype(API.TOKEN);

        if (list != null) {
            for (Account account : list) {
                try {
                    JSONObject jo = JSONObject.fromObject(account.getAccdata());
                    Account_Map.put(account.getAccname(), WeChatHttpsUtil.getAccessToken(jo.getString("appid"), jo.getString("appsecret")));
                } catch (Exception e) {
                    continue;
                }
                
            }
            ContextLog.info("Account_Map:"+Account_Map.toString());
            try {
                new UpdateAccessTokenRunner().task();
            } catch (IOException | SchedulerException e) {
                ContextLog.error(e.toString());
            }
        } else {
            ContextLog.error("No account object present in database.");
        }

        // Init default FTP connection
        try {
            if (ftp_setting != null) {
                FTPConnectionFactory.initDefualtFTPclientConnection(
                        ftp_setting.get("host"), 21,
                        ftp_setting.get("username"),
                        ftp_setting.get("password"));
            } else {
                ContextLog.info("Default ftp.");
                FTPConnectionFactory.initDefualtFTPclientConnection(
                        "10.4.62.41", 21, "Administrator", "Ky6241");
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    
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
        fileName = fileName + "/setting.xml";
        return fileName;
    }

}
