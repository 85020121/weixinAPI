package com.hesong.weixinAPI.mq.events;

import javax.jms.JMSException;

import com.hesong.weixinAPI.mq.MQEvent;

/**
 * 客服取消关注事件
 * @author Bowen
 * hsy.weixin.MQStaffUnsubscribeEvent
 * 消息体 : {"openid":"..."}
 */
public class MQStaffUnsubscribeEvent extends MQEvent{
    private String info;
    
    public MQStaffUnsubscribeEvent(String info) {
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
        return "hsy.weixin.MQStaffUnsubscribeEvent";
    }

    @Override
    public String buildMessage() throws JMSException {
        return this.info;
    }

}
