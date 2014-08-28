package com.hesong.weixinAPI.mq.events;

import javax.jms.JMSException;

import com.hesong.weixinAPI.mq.MQEvent;

/**
 * 客服签出事件
 * @author Bowen
 * hsy.weixin.MQStaffCheckoutEvent
 * 消息体 : {"staff_uuid": "staff_uuid"}
 */
public class MQStaffCheckoutEvent extends MQEvent{
    private String info;
    
    public MQStaffCheckoutEvent(String info) {
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
        return "hsy.weixin.MQStaffCheckoutEvent";
    }

    @Override
    public String buildMessage() throws JMSException {
        return this.info;
    }

}
