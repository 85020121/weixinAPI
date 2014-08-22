package com.hesong.weixinAPI.mq;

import java.util.ArrayList;
import java.util.List;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import redis.clients.jedis.Jedis;

import com.hesong.weixinAPI.context.ContextPreloader;
import com.hesong.weixinAPI.core.MessageRouter;
import com.hesong.weixinAPI.model.Staff;
import com.hesong.weixinAPI.model.StaffSessionInfo;
import com.hesong.weixinAPI.tools.API;

@Component
public class MessageReceiver implements MessageListener  {
    
    private static Logger log = Logger.getLogger(MessageReceiver.class); 

	public void onMessage(Message message) {
		if(message instanceof TextMessage){
			TextMessage tm = (TextMessage) message;
			try {
				String topicName = tm.getStringProperty("eventName");
				log.debug("Message body: " + tm.getText());
                JSONObject messageBody = JSONObject.fromObject(tm.getText());
				if (topicName.equalsIgnoreCase("hsy.sua.TenantUserSkillChangeEvent")) {
				    tenantUserSkillChanged(messageBody);
				} else if(topicName.equalsIgnoreCase("hsy.sua.TenantUserCheckCompleteEvent")){
				    tenantUserCheckCompleteEvent(messageBody);
				} else if(topicName.equalsIgnoreCase("hsy.sua.TenantWXConfigChangedEvent")){
				    tenantWXConfigChangedEvent(messageBody);
                }
			} catch (Exception e) {
				e.printStackTrace();
				log.error("MessageReceiver: " + e.toString());
			}
		}	
	}
	
	private static void tenantUserSkillChanged(JSONObject messageBody) throws Exception {
	    JSONObject tenant = messageBody.getJSONObject("tenant");
        String tenantUn = tenant.getString("tenantUn");
        String staff_uuid = messageBody.getString("id");

        if (!MessageRouter.mulClientStaffMap.containsKey(tenantUn) 
               || !MessageRouter.mulClientStaffMap.get(tenantUn).containsKey(staff_uuid) ) {
            log.debug("该客服没有签到，直接返回");
            return;
        }
        JSONArray skills = messageBody.getJSONArray("skills");
        List<String> skillList = new ArrayList<String>();
        String skillsName = "";
        for (int i = 0; i < skills.size(); i++) {
            JSONObject skill = skills.getJSONObject(i);
            skillList.add(skill.getString("code"));
            if (i == 0) {
                skillsName = skillsName + skill.getString("name");
            } else {
                skillsName = skillsName + "、" + skill.getString("name");
            }
        }
        
        Staff staff = MessageRouter.mulClientStaffMap.get(tenantUn).get(staff_uuid);
        staff.setSkills(skillList);
        String text = String.format("您的技能权限发生了变化，您目前所属的技能组为：%s", skillsName);
        for (StaffSessionInfo session : staff.getSessionChannelList()) {
            String openid = session.getOpenid();
            String token = MessageRouter.getAccessToken(session.getAccount());
            MessageRouter.sendMessage(openid, token, text, API.TEXT_MESSAGE);
        }
	}
	
	public static void tenantUserCheckCompleteEvent(JSONObject message) throws Exception {
	    String staff_uuid = message.getString("id");
	    JSONObject tenane_user = message.getJSONObject("tenantUser");
	    String tenantUn = tenane_user.getJSONObject("tenant").getString("tenantUn");
	    String openid = message.getString("openId");
	    
	    // Remove staff openid from no checked list
	    Jedis jedis = ContextPreloader.jedisPool.getResource();
	    jedis.hdel(API.REDIS_NO_CHECKED_STAFF_LIST, openid);
	    ContextPreloader.jedisPool.returnResource(jedis);
	    
	    if (MessageRouter.mulClientStaffMap.containsKey(tenantUn)
	            && MessageRouter.mulClientStaffMap.get(tenantUn).containsKey(staff_uuid)) {
            Staff staff = MessageRouter.mulClientStaffMap.get(tenantUn).get(staff_uuid);
            String channelId = message.getString("channelId");
            String account = ContextPreloader.accountMap.get(channelId);
            StaffSessionInfo session = new StaffSessionInfo(tenantUn, account, openid,
                    tenane_user.getString("number"), tenane_user.getString("displayName"), staff_uuid);
            staff.getSessionChannelList().add(session);
            MessageRouter.activeStaffMap.put(openid, session);
        }
	}
	
    public static void tenantWXConfigChangedEvent(JSONObject message)
            throws Exception {
        String tenantUn = message.getJSONObject("tenant").getString("tenantUn");
        String waitingTimeout = message.getString("queueTimeout") + "000";
        String sessionimeout = message.getString("nomsgTimeout") + "000";
        String endSessionimeout = message.getString("quitChatWaitTime") + "000";
        
        Jedis jedis = ContextPreloader.jedisPool.getResource();
        jedis.hset(API.REDIS_TENANT_WAITING_DURATION, tenantUn, waitingTimeout);
        jedis.hset(API.REDIS_TENANT_SESSION_AVAILABLE_DURATION, tenantUn, sessionimeout);
        jedis.hset(API.REDIS_TENANT_END_SESSION_DURATION_DURATION, tenantUn, endSessionimeout);
        ContextPreloader.jedisPool.returnResource(jedis);
    }

}
