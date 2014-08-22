package com.hesong.weChatAdapter.context;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
import com.hesong.smartbus.client.WeChatCallback;
import com.hesong.smartbus.client.net.Client;
import com.hesong.smartbus.client.net.Client.ConnectError;
import com.hesong.weChatAdapter.account.Account;
import com.hesong.weChatAdapter.account.AccountBo;
import com.hesong.weChatAdapter.model.AccessToken;
import com.hesong.weChatAdapter.runner.SmartbusExecutor;
import com.hesong.weChatAdapter.tools.API;
import com.hesong.weChatAdapter.tools.WeChatHttpsUtil;

@SuppressWarnings("rawtypes")
public class ContextPreloader extends HttpServlet{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    public static Logger ContextLog = Logger.getLogger(ContextPreloader.class);
    
    public static Map<String, AccessToken> Account_Map = new ConcurrentHashMap<String, AccessToken>();
    
    public static Map<String,String> appid_appsecret = new ConcurrentHashMap<String, String>();
    
    public static List<Map<String, Byte>> busList = new ArrayList<>();
    
    public static JedisPool jedisPool;
    
    public static int REDIS_DB_NUM;
    public static int REDIS_KEY_EXPIRE;

    static{
        ContextLog.info("ContextPreloader");
        
        ResourceBundle bundle = ResourceBundle.getBundle("redis");
        if (null == bundle) {
            ContextLog.error("redis.properties not found!");
        }
        
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(Integer.parseInt(bundle.getString("redis.pool.maxTotal")));
        config.setMaxIdle(Integer.parseInt(bundle.getString("redis.pool.maxIdle")));
        config.setMaxWaitMillis(Integer.parseInt(bundle.getString("redis.pool.maxWait")));
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        
        REDIS_DB_NUM = Integer.parseInt(bundle.getString("redis.db_num"));
        REDIS_KEY_EXPIRE = Integer.parseInt(bundle.getString("redis.key_expire"));
        
        jedisPool = new JedisPool(config, bundle.getString("redis.host"),
                Integer.parseInt(bundle.getString("redis.port")), 30000);
        
        ApplicationContext ctx = AppContext.getApplicationContext();
        File f = new File(getSettingFilePath(ctx));
        Map<String, String> ftp_setting = null;
        if (f.exists()){
            SAXReader reader = new SAXReader();
            ftp_setting = new HashMap<String, String>();
            try {
                
                Document document = reader.read(f);
                Element rootElmt = document.getRootElement();
                
                Element smartbusElmt = rootElmt.element("smartbus");
                
                Iterator iter = smartbusElmt.elementIterator();
                while (iter.hasNext()) {
                    Element bus = (Element)iter.next();
                    Map<String, Byte> busInfo = new HashMap<>();
                    String host = bus.elementText("host");
                    short port = Short.parseShort(bus.elementText("port"));
                    byte unitid = Byte.parseByte(bus.elementText("unitid"));
                    byte clientid = Byte.parseByte(bus.elementText("clientid"));
                    byte clienttype = Byte.parseByte(bus.elementText("clienttype"));
                    busInfo.put("unitid", unitid);
                    busInfo.put("clientid", clientid);
                    busInfo.put("clienttype", clienttype);
                    busInfo.put("destunitid", Byte.parseByte(bus.elementText("destunitid")));
                    busInfo.put("destclientid", Byte.parseByte(bus.elementText("destclientid")));
                    busList.add(busInfo);
                    
                    try {
                        Client.initialize(unitid);
                        Client client = new Client(clientid,
                                (long) clienttype, host, port, "Weixin client");
                        client.setCallbacks(new WeChatCallback());
                        client.connect();
                        SmartbusExecutor.smartbusClients.add(client);
                    } catch (ConnectError e) {
                        e.printStackTrace();
                    }
                }
                
                ContextLog.info("Clients: "+SmartbusExecutor.smartbusClients.toString());
                SmartbusExecutor.execute();
                
                Element ftpElmt = rootElmt.element("ftp");
                ftp_setting.put("host", ftpElmt.elementText("host"));
                ftp_setting.put("username", ftpElmt.elementText("username"));
                ftp_setting.put("password", ftpElmt.elementText("password"));
                ContextLog.info("FTP setting: "+ftp_setting.toString());
                
                Element apiElmt = rootElmt.element("apisecurity");
                Iterator apiIter = apiElmt.elementIterator();
                while (apiIter.hasNext()) {
                    Element apiInfo = (Element)apiIter.next();
                    appid_appsecret.put(apiInfo.elementText("appid"), apiInfo.elementText("appsecret"));
                }
                ContextLog.info("Api info: " + appid_appsecret.toString());
                
                document.clearContent();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            ContextLog.error("Setting file not exist.");
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
