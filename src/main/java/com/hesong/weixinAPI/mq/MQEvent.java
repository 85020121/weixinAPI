package com.hesong.weixinAPI.mq;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jms.core.MessageCreator;

public abstract class MQEvent implements MessageCreator {
	public abstract String getTopicName();
	private static Log logger = LogFactory.getLog(MQEvent.class);
//	
//	public Destination getTopicDest(){
//		
//	}
//	  
	@Override
	public Message createMessage(Session session) throws JMSException {
		logger.debug("主题："+this.getTopicName());
		String message = this.buildMessage();
		logger.debug("消息体："+message);
		Message txtMessage = session.createTextMessage(message);
		txtMessage.setStringProperty("eventName", this.getTopicName());
		return txtMessage;
	}

	public abstract String buildMessage()throws JMSException ;
}
