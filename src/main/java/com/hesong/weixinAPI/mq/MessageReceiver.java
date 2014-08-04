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
				if (topicName.equalsIgnoreCase("hsy.sua.TenantUserSkillChangeEvent")) {
				    log.debug("Message body: " + tm.getText());
				    JSONObject messageBody = JSONObject.fromObject(tm.getText());
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
			} catch (Exception e) {
				e.printStackTrace();
				log.error("MessageReceiver: " + e.toString());
			}
		}	
	}

}
