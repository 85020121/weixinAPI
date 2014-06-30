package com.hesong.weixinAPI.job;

import java.util.LinkedList;
import java.util.Queue;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.hesong.sugarCRM.SugarCRMCaller;
import com.hesong.weixinAPI.context.ContextPreloader;
import com.hesong.weixinAPI.core.MessageRouter;
import com.hesong.weixinAPI.core.SUAExecutor;
import com.hesong.weixinAPI.model.Staff;
import com.hesong.weixinAPI.model.StaffSessionInfo;
import com.hesong.weixinAPI.tools.API;

public class GetLeavedMessageJob implements Job {
    public static final String GET_LEAVED_MESSAGE_GROUP = "GetLeavedMessageJob";
    
    private SugarCRMCaller sua;
    
    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        if (!sua.check_oauth(SUAExecutor.session)) {
            SUAExecutor.session = sua.login("admin",
                    "p@ssw0rd");
            ContextPreloader.ContextLog.info("getMessageNoticeForWX renew session: " + SUAExecutor.session);
        }
        String r = sua.getMessageNoticeForWX(SUAExecutor.session);
        JSONObject sua_ret = JSONObject.fromObject(r);
//        if (!sua_ret.getBoolean("success")) {
//            SUAExecutor.session = sua.login("admin", "p@ssw0rd");
//            ContextPreloader.ContextLog.info("getMessageNoticeForWX renew session: " + SUAExecutor.session);
//            sua_ret = JSONObject.fromObject(sua.getMessageNoticeForWX(SUAExecutor.session));
//        }

        if (sua_ret.containsKey("result")) {
            JSONArray results = sua_ret.getJSONArray("result");
            for (int i = 0; i < results.size(); i++) {
                JSONObject result = results.getJSONObject(i);
                String tenant_code = result.getString("tenant_code");
                if (MessageRouter.leavedMessageMap.containsKey(tenant_code)) {
                    MessageRouter.leavedMessageMap.get(tenant_code).offer(result);
                } else {
                    Queue<JSONObject> messages = new LinkedList<JSONObject>();
                    messages.offer(result);
                    MessageRouter.leavedMessageMap.put(tenant_code, messages);
                }
            }
        }
        
        ContextPreloader.ContextLog.info("Messages: " + MessageRouter.leavedMessageMap.toString());
        
        for (String tenantUn : MessageRouter.leavedMessageMap.keySet()) {
                int count = MessageRouter.leavedMessageMap.get(tenantUn).size();
                if (count == 0) {
                    continue;
                }
                for (Staff staff : MessageRouter.mulClientStaffMap.get(tenantUn).values()) {
                    for (StaffSessionInfo s : staff.getSessionChannelList()) {
                        if (!s.isBusy()) {
                            String text = String.format("有%d条留言等待处理,请点击查看留言按钮获取留言信息.", count);
                            String sToken = ContextPreloader.Account_Map.get(s.getAccount()).getToken();
                            MessageRouter.sendMessage(s.getOpenid(), sToken, text, API.TEXT_MESSAGE);
                            break; // Only one message for each staff
                        }
                    }
                }
            
        }

    }

    public SugarCRMCaller getSua() {
        return sua;
    }

    public void setSua(SugarCRMCaller sua) {
        this.sua = sua;
    }

    public GetLeavedMessageJob() {
        super();
        sua = new SugarCRMCaller();
    }
    
    

}
