package com.hesong.weixinAPI.model;

import java.util.List;

public class Staff {
    
    private String id;
    private String name;
    private String account;
    private String work_num;
    private List<StaffSessionInfo> sessionChannelList;
    
    
    public Staff(String id, String name, String account, String work_num,
            List<StaffSessionInfo> sessionChannelList) {
        super();
        this.id = id;
        this.name = name;
        this.account = account;
        this.work_num = work_num;
        this.sessionChannelList = sessionChannelList;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getAccount() {
        return account;
    }
    public String getWork_num() {
        return work_num;
    }
    public void setWork_num(String work_num) {
        this.work_num = work_num;
    }
    public void setAccount(String account) {
        this.account = account;
    }
    public List<StaffSessionInfo> getSessionChannelList() {
        return sessionChannelList;
    }
    public void setSessionChannelList(List<StaffSessionInfo> sessionChannelList) {
        this.sessionChannelList = sessionChannelList;
    }
    @Override
    public String toString() {
        return "Staff [id=" + id + ", name=" + name + ", account=" + account
                + ", work_num=" + work_num + ", sessionChannelList="
                + sessionChannelList + "]";
    }
    
}
