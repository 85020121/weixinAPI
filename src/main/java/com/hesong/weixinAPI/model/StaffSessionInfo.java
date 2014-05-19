package com.hesong.weixinAPI.model;

import java.util.Date;

public class StaffSessionInfo {

    private String account;
    private String openid;
    private Date lastReceived;

    public StaffSessionInfo(String account, String openid) {
        super();
        this.account = account;
        this.openid = openid;
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
    public Date getLastReceived() {
        return lastReceived;
    }
    public void setLastReceived(Date lastReceived) {
        this.lastReceived = lastReceived;
    }
    
}
