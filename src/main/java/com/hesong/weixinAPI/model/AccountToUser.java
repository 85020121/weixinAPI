package com.hesong.weixinAPI.model;

public class AccountToUser {
    private String account;
    private String toUser;
    public AccountToUser(String account, String toUser) {
        super();
        this.account = account;
        this.toUser = toUser;
    }
    public String getAccount() {
        return account;
    }
    public void setAccount(String account) {
        this.account = account;
    }
    public String getToUser() {
        return toUser;
    }
    public void setToUser(String toUser) {
        this.toUser = toUser;
    }
    
}
