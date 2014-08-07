package com.hesong.mq;

import javax.jms.Destination;

import org.apache.activemq.command.ActiveMQTopic;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component("MQManager")
public class MQManager {
	
	private static final Log logger = LogFactory.getLog(MQManager.class);

	@Autowired
	private JmsTemplate jmsTemplate;
	
	/**
	 * 向mq server发布一个事件主题
	 * @param topic
	 */
	public void publishTopicEvent(MQEvent event){
		logger.debug("发布事件："+event.getTopicName());
		Destination dest = new ActiveMQTopic("weixin.client.message.topic");
		jmsTemplate.send(dest,event);
	}
}
