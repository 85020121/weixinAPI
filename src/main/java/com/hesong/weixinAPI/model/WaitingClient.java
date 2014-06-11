package com.hesong.weixinAPI.model;

public class WaitingClient {
    
    private String openid;
    private String account;
    private String name;
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
    public WaitingClient(String openid, String account, String name) {
        super();
        this.openid = openid;
        this.account = account;
        this.name = name;
    }
    @Override
    public String toString() {
        return "WaitingClient [openid=" + openid + ", account=" + account
                + ", name=" + name + "]";
    }

}
