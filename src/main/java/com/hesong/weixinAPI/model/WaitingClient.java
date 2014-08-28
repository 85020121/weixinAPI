package com.hesong.weixinAPI.model;

public class WaitingClient {
    
    private String tenantUn;
    private String openid;
    private String account;
    private String name;
    private String image;
    private int sex;
    private String city;
    private String province;
    private long time;
    
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
    public long getTime() {
        return time;
    }
    public void setTime(long time) {
        this.time = time;
    }
    public String getImage() {
        return image;
    }
    public void setImage(String image) {
        this.image = image;
    }
    public int getSex() {
        return sex;
    }
    public void setSex(int sex) {
        this.sex = sex;
    }
    public String getCity() {
        return city;
    }
    public void setCity(String city) {
        this.city = city;
    }
    public String getProvince() {
        return province;
    }
    public void setProvince(String province) {
        this.province = province;
    }
    public WaitingClient(String tenantUn, String openid, String account,
            String name, String image, int sex, String city, String province,
            long time) {
        super();
        this.tenantUn = tenantUn;
        this.openid = openid;
        this.account = account;
        this.name = name;
        this.image = image;
        this.sex = sex;
        this.city = city;
        this.province = province;
        this.time = time;
    }

}
