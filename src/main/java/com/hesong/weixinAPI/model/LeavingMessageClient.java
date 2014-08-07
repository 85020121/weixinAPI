package com.hesong.weixinAPI.model;

import java.util.Date;
import java.util.UUID;

public class LeavingMessageClient {
    private String account;
    private String openid;
    private String name;
    private String headimgurl;
    private String source;
    private String uuid;
    private int msgCount;
    private long date;
    public LeavingMessageClient(String account, String openid, String name, String headimgurl, String source) {
        super();
        this.account = account;
        this.openid = openid;
        this.name = name;
        if (source.equalsIgnoreCase("wx")) {
            this.headimgurl = headimgurl.substring(0, headimgurl.length()-1) + "46";
        }
        this.headimgurl = headimgurl;
        this.source = source;
        this.date = new Date().getTime();
        this.msgCount = 0;
        this.uuid = UUID.randomUUID().toString();
    }
    public String getAccount() {
        return account;
    }
    public void setAccount(String account) {
        this.account = account;
    }
    public String getOpenid() {
        return openid;
    }
    public void setOpenid(String openid) {
        this.openid = openid;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public long getDate() {
        return date;
    }
    public void setDate(long date) {
        this.date = date;
    }
    public String getUuid() {
        return uuid;
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    public String getHeadimgurl() {
        return headimgurl;
    }
    public void setHeadimgurl(String headimgurl) {
        this.headimgurl = headimgurl;
    }
    public String getSource() {
        return source;
    }
    public void setSource(String source) {
        this.source = source;
    }
    public int getMsgCount() {
        return msgCount;
    }
    public void setMsgCount(int msgCount) {
        this.msgCount = msgCount;
    }
    public void incCount() {
        this.msgCount = this.msgCount + 1;
    }
    @Override
    public String toString() {
        return "LeavingMessageClient [account=" + account + ", openid="
                + openid + ", name=" + name + ", headimgurl=" + headimgurl
                + ", source=" + source + ", uuid=" + uuid + ", date=" + date
                + "]";
    }
}
