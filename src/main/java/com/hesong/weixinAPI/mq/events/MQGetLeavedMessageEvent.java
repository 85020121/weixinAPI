package com.hesong.weixinAPI.mq.events;

import javax.jms.JMSException;

import com.hesong.weixinAPI.mq.MQEvent;

/**
 * 查看留言事件
 * @author Bowen
 * hsy.weixin.MQGetLeavedMessageEvent
 * 消息体 : {"message_group_id":"留言组id(多条用，隔开如 a,b,c)", "staff_suaid":"客服uuid", "staff_name":"客服昵称", "staff_openid":"openid", "account":"客服通道ID", "text":"消息体"}
 *
 */
public class MQGetLeavedMessageEvent extends MQEvent{
    private String info;
    
    public MQGetLeavedMessageEvent(String info) {
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
        return "hsy.weixin.MQGetLeavedMessageEvent";
    }

    @Override
    public String buildMessage() throws JMSException {
        return this.info;
    }


}
