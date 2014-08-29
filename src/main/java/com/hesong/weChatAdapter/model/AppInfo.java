package com.hesong.weChatAdapter.model;

public class AppInfo {
    private String appid;
    private String appsecret;
    private String access_token;
    private String url_prefix;
    
    public AppInfo() {
        super();
    }
    public AppInfo(String appid, String appsecret, String access_token,
            String url_prefix) {
        super();
        this.appid = appid;
        this.appsecret = appsecret;
        this.access_token = access_token;
        this.url_prefix = url_prefix;
    }
    public String getAppid() {
        return appid;
    }
    public void setAppid(String appid) {
        this.appid = appid;
    }
    public String getAppsecret() {
        return appsecret;
    }
    public void setAppsecret(String appsecret) {
        this.appsecret = appsecret;
    }
    public String getAccess_token() {
        return access_token;
    }
    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }
    public String getUrl_prefix() {
        return url_prefix;
    }
    public void setUrl_prefix(String url_prefix) {
        this.url_prefix = url_prefix;
    }
    @Override
    public String toString() {
        return "AppInfo [appid=" + appid + ", appsecret=" + appsecret
                + ", access_token=" + access_token + ", url_prefix="
                + url_prefix + "]";
    }
    
}
