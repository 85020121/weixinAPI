package com.hesong.weixinAPI.mq.events;

import javax.jms.JMSException;

import com.hesong.weixinAPI.mq.MQEvent;

/**
 * 客服签到事件
 * @author Bowen
 * hsy.weixin.MQStaffCheckinEvent
 * 消息体 : {"staff_uuid": "staff_uuid","account":"gh_0221936c0c16", "openid":"openid"}
 */
public class MQStaffCheckinEvent extends MQEvent{
    private String info;
    
    public MQStaffCheckinEvent(String info) {
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
        return "hsy.weixin.MQStaffCheckinEvent";
    }

    @Override
    public String buildMessage() throws JMSException {
        return this.info;
    }

}
