package com.hesong.mq;

import javax.jms.JMSException;

public class MQWeixinMessageEvent extends MQEvent {
    private String topicName;
    private String info;
    

    public MQWeixinMessageEvent(String topicName, String info) {
        super();
        this.topicName = topicName;
        this.info = info;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    @Override
    public String getTopicName() {
        return topicName;
    }

    @Override
    public String buildMessage() throws JMSException {
        return this.info;
    }

}
