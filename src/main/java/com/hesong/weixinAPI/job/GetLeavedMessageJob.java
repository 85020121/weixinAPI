package com.hesong.weixinAPI.job;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.hesong.sugarCRM.SugarCRMCaller;
import com.hesong.weixinAPI.context.AppContext;
import com.hesong.weixinAPI.context.ContextPreloader;
import com.hesong.weixinAPI.core.MessageRouter;
import com.hesong.weixinAPI.core.SUAExecutor;
import com.hesong.weixinAPI.model.Staff;
import com.hesong.weixinAPI.model.StaffSessionInfo;
import com.hesong.weixinAPI.mq.MQEvent;
import com.hesong.weixinAPI.mq.MQManager;
import com.hesong.weixinAPI.mq.events.MQGetLeavedMessageEvent;
import com.hesong.weixinAPI.tools.API;

public class GetLeavedMessageJob implements Job {
    public static final String GET_LEAVED_MESSAGE_GROUP = "GetLeavedMessageJob";
    
    private SugarCRMCaller sua;
    
    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        if (MessageRouter.mulClientStaffMap.isEmpty()) {
            return;
        }
        
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

        Map<String, List<String>> message_map = new HashMap<String, List<String>>();
        
        if (sua_ret.containsKey("result")) {
            JSONArray results = sua_ret.getJSONArray("result");
            
            for (int i = 0; i < results.size(); i++) {
                JSONObject result = results.getJSONObject(i);
                String tenant_code = result.getString("tenant_code");
                if (message_map.containsKey(tenant_code)) {
                    message_map.get(tenant_code).add(result.getString("message_group_id"));
                } else {
                    List<String> ids = new ArrayList<String>();
                    ids.add(result.getString("message_group_id"));
                    message_map.put(tenant_code, ids);
                }
//                if (MessageRouter.leavedMessageMap.containsKey(tenant_code)) {
//                    MessageRouter.leavedMessageMap.get(tenant_code).offer(result);
//                } else {
//                    Queue<JSONObject> messages = new LinkedList<JSONObject>();
//                    messages.offer(result);
//                    MessageRouter.leavedMessageMap.put(tenant_code, messages);
//                }
            }
        }
        
//        ContextPreloader.ContextLog.info("Messages: " + MessageRouter.leavedMessageMap.toString());
        
        for (String tenantUn : message_map.keySet()) {
                int count = message_map.get(tenantUn).size();
                if (count == 0) {
                    continue;
                }
                ContextPreloader.ContextLog.info(count + " leaved messages for client " + tenantUn);
                boolean messageSended = false;
            for (Staff staff : MessageRouter.mulClientStaffMap.get(tenantUn)
                    .values()) {
                if (messageSended) {
                    break;
                }
                for (StaffSessionInfo s : staff.getSessionChannelList()) {
                    if (!s.isBusy()) {
                        String account = s.getAccount();
                        JSONObject accountInfo = MessageRouter.getAccountInfo(
                                account, API.REDIS_STAFF_ACCOUNT_INFO_KEY);
                        String url = String
                                .format(API.GET_LEAVED_MESSAGE_URL,
                                        ContextPreloader.channelMap
                                                .get(account));
                        String text = null;
                        try {
                            text = String
                                    .format("有%d条留言等待处理,<a href=\"https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_base&state=123#wechat_redirect\">点击查看留言</a>",
                                            count,
                                            accountInfo.getString("appid"),
                                            URLEncoder.encode(url, "utf8"));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        
                        String message_group_id = "";
                        List<String> ids = message_map.get(tenantUn);
                        for (int i = 0; i < ids.size(); i++) {
                            if (i == 0) {
                                message_group_id = message_group_id + ids.get(i);
                            } else {
                                message_group_id = message_group_id + "," + ids.get(i);
                            }
                        }
                        
                        JSONObject message = new JSONObject();
                        message.put("message_group_id", message_group_id);
                        message.put("staff_suaid", s.getStaff_uuid());
                        message.put("staff_name", s.getName());
                        message.put("staff_openid", s.getOpenid());
                        message.put("account", account);
                        message.put("text", text);
                        
                        MQEvent event = new MQGetLeavedMessageEvent(message.toString());
                        MQManager manager = (MQManager)AppContext.getApplicationContext().getBean("MQManager");
                        manager.publishTopicEvent(event);
                        
                        messageSended = true;
                        break;
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
