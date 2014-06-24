package com.hesong.weixinAPI.model;

public class WaitingClient {
    
    private String tenantUn;
    private String openid;
    private String account;
    private String name;
    
    public String getTenantUn() {
        return tenantUn;
    }
    public void setTenantUn(String tenantUn) {
        this.tenantUn = tenantUn;
    }
    public String getOpenid() {
        return openid;
    }
    public void setOpenid(String openid) {
        this.openid = openid;
    }
    public String getAccount() {
        return account;
    }
    public void setAccount(String account) {
        this.account = account;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public WaitingClient(String tenantUn, String openid, String account,
            String name) {
        super();
        this.tenantUn = tenantUn;
        this.openid = openid;
        this.account = account;
        this.name = name;
    }
    @Override
    public String toString() {
        return "WaitingClient [tenantUn=" + tenantUn + ", openid=" + openid
                + ", account=" + account + ", name=" + name + "]";
    }

}
