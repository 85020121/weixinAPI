package com.hesong.weixinAPI.model;

import java.util.Date;

public class StaffSessionInfo {

    private String account;
    private String openid;
    private String staffid;
    private String client_openid;
    private String client_account;
    private String client_name;
    private boolean isBusy;
    private Date lastReceived;

    public StaffSessionInfo(String account, String openid, String staffid) {
        super();
        this.account = account;
        this.openid = openid;
        this.staffid = staffid;
        this.isBusy = false;
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
    public boolean isBusy() {
        return isBusy;
    }
    public void setBusy(boolean isBusy) {
        this.isBusy = isBusy;
    }
    public String getClient_openid() {
        return client_openid;
    }
    public void setClient_openid(String client_openid) {
        this.client_openid = client_openid;
    }
    
    public String getClient_account() {
        return client_account;
    }
    public void setClient_account(String client_account) {
        this.client_account = client_account;
    }
    public String getStaffid() {
        return staffid;
    }
    public void setStaffid(String staffid) {
        this.staffid = staffid;
    }
    public String getClient_name() {
        return client_name;
    }
    public void setClient_name(String client_name) {
        this.client_name = client_name;
    }
    @Override
    public String toString() {
        return "StaffSessionInfo [account=" + account + ", openid=" + openid
                + ", staffid=" + staffid + ", client_openid=" + client_openid
                + ", client_account=" + client_account + ", isBusy=" + isBusy
                + ", lastReceived=" + lastReceived + "]";
    }
}
