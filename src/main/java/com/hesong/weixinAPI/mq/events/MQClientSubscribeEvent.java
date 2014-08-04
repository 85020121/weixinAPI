package com.hesong.weixinAPI.mq.events;

import javax.jms.JMSException;

import com.hesong.weixinAPI.mq.MQEvent;

/**
 * 客户关注事件
 * @author Bowen
 * hsy.weixin.MQClientSubscribeEvent
 * 消息体 : 
 */
public class MQClientSubscribeEvent extends MQEvent{
    private String info;
    
    public MQClientSubscribeEvent(String info) {
        super();
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
        return "hsy.weixin.MQClientSubscribeEvent";
    }

    @Override
    public String buildMessage() throws JMSException {
        return this.info;
    }

}
