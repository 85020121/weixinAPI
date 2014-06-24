package com.hesong.weixinAPI.model;

import java.util.Date;

/**
 * 
 * @author Bowen
 * 微信端客户--客服会话信息
 */

public class StaffSessionInfo {

    private String session;
    private String tenantUn;
    private String account;
    private String openid;
    private String staffid; // Staff working num
    private String name;
    private String client_openid;
    private String client_account;
    private String client_name;
    private String client_type;
    private String client_image;
    private boolean isBusy;
    private Date lastReceived;
    
    public StaffSessionInfo(String tenantUn, String account, String openid, String staffid,
            String name) {
        super();
        this.tenantUn = tenantUn;
        this.account = account;
        this.openid = openid;
        this.staffid = staffid;
        this.name = name;
        this.isBusy = false;
    }
    
    public String getTenantUn() {
        return tenantUn;
    }

    public void setTenantUn(String tenantUn) {
        this.tenantUn = tenantUn;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getSession() {
        return session;
    }
    public void setSession(String session) {
        this.session = session;
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
    
    public String getClient_type() {
        return client_type;
    }

    public void setClient_type(String client_type) {
        this.client_type = client_type;
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
    public String getClient_image() {
        return client_image;
    }
    public void setClient_image(String client_image) {
        this.client_image = client_image;
    }

    @Override
    public String toString() {
        return "StaffSessionInfo [session=" + session + ", tenantUn="
                + tenantUn + ", account=" + account + ", openid=" + openid
                + ", staffid=" + staffid + ", name=" + name
                + ", client_openid=" + client_openid + ", client_account="
                + client_account + ", client_name=" + client_name
                + ", client_type=" + client_type + ", client_image="
                + client_image + ", isBusy=" + isBusy + ", lastReceived="
                + lastReceived + "]";
    }

}
