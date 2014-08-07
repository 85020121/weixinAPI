package com.hesong.mq;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.hesong.ws.WeChatMessageAnnotation;


@Component
public class MessageReceiver implements MessageListener  {
    
    private static Logger log = Logger.getLogger(MessageReceiver.class); 

	public void onMessage(Message message) {
		if(message instanceof TextMessage){
			TextMessage tm = (TextMessage) message;
			try {
				String topicName = tm.getStringProperty("eventName");
				log.info("topicName: " + topicName);
				if (WeChatMessageAnnotation.session_map.containsKey(topicName)) {
                    WeChatMessageAnnotation.sendMessage(topicName, tm.getText());
                    log.info("Send message: " + tm.getText());
                }
			} catch (Exception e) {
				e.printStackTrace();
				log.error("MessageReceiver: " + e.toString());
			}
		}	
	}

}
