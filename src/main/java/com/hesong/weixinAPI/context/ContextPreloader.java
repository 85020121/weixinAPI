package com.hesong.weixinAPI.context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServlet;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import com.hesong.sugarCRM.HttpClientUtil;
import com.hesong.weixinAPI.core.MessageExecutor;
import com.hesong.weixinAPI.job.JobRunner;
import com.hesong.weixinAPI.model.AccessToken;
import com.hesong.weixinAPI.tools.WeChatHttpsUtil;

public class ContextPreloader extends HttpServlet{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    public static Logger ContextLog = Logger.getLogger(ContextPreloader.class);
    
    public static Map<String, AccessToken> Account_Map = new ConcurrentHashMap<String, AccessToken>();
    public static List<String> staffAccountList = new ArrayList<String>();
    public final static String HESONG_ACCOUNT = "gh_be994485fbce";
    public static Map<String, String> channelMap = new HashMap<String, String>();
    
    static{
        
//        try {
//            // Staff service account
//            staffAccountList.add("gh_0221936c0c16");
//            staffAccountList.add("gh_510fe6f15310");
//
//            channelMap.put("gh_0221936c0c16", "1");
//            channelMap.put("gh_510fe6f15310", "2");
//
//            AccessToken service1 = new AccessToken("gh_0221936c0c16",
//                    "wx735e58e85eb3614a", "d21d943d536c383c9e60053ff15996c2");
//            AccessToken service2 = new AccessToken("gh_510fe6f15310",
//                    "wx96bebe11dbeb1c22", "e376af7623f1051fa42693966f13f77c");
//            Account_Map.put(service1.getAccount(),
//                    WeChatHttpsUtil.getAccessToken(service1));
//            Account_Map.put(service2.getAccount(),
//                    WeChatHttpsUtil.getAccessToken(service2));
//
//            String r = HttpClientUtil
//                    .httpGet("http://www.clouduc.cn/sua/rest/n/tenant/listwxparams");
//            JSONArray ret = JSONArray.fromObject(r);
//            ContextLog.info("Account list: " + ret.toString());
//            for (int i = 0; i < ret.size(); i++) {
//                JSONObject client_account = ret.getJSONObject(i);
//                String tenantUn = client_account.getString("tenantUn");
//                JSONObject info = client_account
//                        .getJSONObject("tenantSnsWeixin");
//                String appid = info.getString("appid");
//                String appsecret = info.getString("secretKey");
//                String account = info.getString("publicid");
//                AccessToken t = new AccessToken(tenantUn, account, appid,
//                        appsecret);
//                Account_Map.put(account, WeChatHttpsUtil.getAccessToken(t));
//            }
//            ContextLog.info("Account_Map: " + Account_Map.toString());
//
//            // ApplicationContext ctx = AppContext.getApplicationContext();
////        File f = new File(getSettingFilePath(ctx));
////        Map<String, String> ftp_setting = null;
////        if (f.exists()){
////            SAXReader reader = new SAXReader();
////            ftp_setting = new HashMap<String, String>();
////            try {
////                
////                Document document = reader.read(f);
////                Element rootElmt = document.getRootElement();
////                
////                Element ftpElmt = rootElmt.element("ftp");
////                ftp_setting.put("host", ftpElmt.elementText("host"));
////                ftp_setting.put("username", ftpElmt.elementText("username"));
////                ftp_setting.put("password", ftpElmt.elementText("password"));
////                ContextLog.info("FTP setting: "+ftp_setting.toString());
////
////            } catch (Exception e) {
////                e.printStackTrace();
////            }
////        } else {
////            ContextLog.error("Setting file not exist.");
////        }
//        
////        AccountBo accountBo = (AccountBo) ctx.getBean("accountBo");
////        @SuppressWarnings("unchecked")
////        List<Account> list = (List<Account>) accountBo.getAllAccount();//.findByAcctype(API.TOKEN);
////        //ContextLog.info("Account list size: "+list.size());
////        if (list != null) {
////            for (Account account : list) {
////                try {
////                    JSONObject jo = JSONObject.fromObject(account.getAccdata());
////                    Account_Map.put(account.getAccname(), WeChatHttpsUtil.getAccessToken(jo.getString("appid"), jo.getString("appsecret")));
////                } catch (Exception e) {
////                    continue;
////                }
////                
////            }
////            ContextLog.info("Account_Map:"+Account_Map.toString());
////            try {
////                new JobRunner().task();
////            } catch (IOException | SchedulerException e) {
////                ContextLog.error(e.toString());
////            }
////        } else {
////            ContextLog.error("No account object present in database.");
////        }
//
//        // Init default FTP connection
////        try {
////            if (ftp_setting != null) {
////                FTPConnectionFactory.initDefualtFTPclientConnection(
////                        ftp_setting.get("host"), 21,
////                        ftp_setting.get("username"),
////                        ftp_setting.get("password"));
////            } else {
////                ContextLog.info("Default ftp.");
////                FTPConnectionFactory.initDefualtFTPclientConnection(
////                        "10.4.62.41", 21, "Administrator", "Ky6241");
////            }
////        } catch (IOException e) {
////            // TODO Auto-generated catch block
////            e.printStackTrace();
////        }
//        
//            MessageExecutor.execute();
//            new JobRunner().task();
//        } catch (Exception e) {
//            // TODO: handle exception
//            e.printStackTrace();
//        }
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