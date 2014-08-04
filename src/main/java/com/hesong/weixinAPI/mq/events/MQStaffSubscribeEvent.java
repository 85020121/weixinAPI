package com.hesong.weixinAPI.mq.events;

import javax.jms.JMSException;

import com.hesong.weixinAPI.mq.MQEvent;

/**
 * 客服关注事件
 * @author Bowen
 * hsy.weixin.MQStaffSubscribeEvent
 * 消息体 : {"weixin_public_id":"gh_0221936c0c16", "tenantid":"1", "userinfo":{"subscribe":1,"openid":"ogfGduA0yfPY_aET7do8GvE5Bm4w","nickname":"Bowen","sex":1,"language":"en","city":"广州","province":"广东","country":"中国","headimgurl":"http://wx.qlogo.cn/mmopen/WWxicToNQlgxvdF4V3yM5IncQQjXk7pPgkaeglBxcRg1lHwcREca2OdMxhn6biaoT8qDz2mL8ibvQxVnvZfxpicQLPIvtnagxnKA/0","subscribe_time":1406370240,"remark":""}}
 */
public class MQStaffSubscribeEvent extends MQEvent{
    private String info;
    
    public MQStaffSubscribeEvent(String info) {
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
        return "hsy.weixin.MQStaffSubscribeEvent";
    }

    @Override
    public String buildMessage() throws JMSException {
        return this.info;
    }

}
