package com.hesong.weChatAdapter.model;

public class AccessToken {
    private String appid;
    private String appSecret;
    private String token;
    private int expiresIn;
    
    public AccessToken(String appid, String appSecret, String token,
            int expiresIn) {
        super();
        this.appid = appid;
        this.appSecret = appSecret;
        this.token = token;
        this.expiresIn = expiresIn;
    }
    public String getAppid() {
        return appid;
    }
    public void setAppid(String appid) {
        this.appid = appid;
    }
    public String getAppSecret() {
        return appSecret;
    }
    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }
    public int getExpiresIn() {
        return expiresIn;
    }
    public void setExpiresIn(int expiresIn) {
        this.expiresIn = expiresIn;
    }
    @Override
    public String toString() {
        return "AccessToken [appid=" + appid + ", appSecret=" + appSecret
                + ", token=" + token + ", expiresIn=" + expiresIn + "]";
    }
    
    
    
}
