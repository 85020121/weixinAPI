package com.hesong.weChatAdapter.context;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServlet;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import com.hesong.weChatAdapter.account.Account;
import com.hesong.weChatAdapter.account.AccountBo;
import com.hesong.weChatAdapter.model.AccessToken;
import com.hesong.weChatAdapter.tools.API;
import com.hesong.weChatAdapter.tools.WeChatHttpsUtil;

public class ContextPreloader extends HttpServlet{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    public static Logger ContextLog = Logger.getLogger(ContextPreloader.class);
    
    public static Map<String, AccessToken> Account_Map = new ConcurrentHashMap<String, AccessToken>();
    
    static{
        ApplicationContext ctx = AppContext.getApplicationContext();
        AccountBo accountBo = (AccountBo) ctx.getBean("accountBo");
        @SuppressWarnings("unchecked")
        List<Account> list = (List<Account>) accountBo.findByAcctype(API.TOKEN);
        for (Account account : list) {
            JSONObject jo = JSONObject.fromObject(account.getAccdata());
            ContextLog.info("Account: "+jo);
            Account_Map.put(account.getAccname(), WeChatHttpsUtil.getAccessToken(jo.getString("appid"), jo.getString("appsecret")));
        }
    }

}
