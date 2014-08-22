package com.hesong.weixinAPI.model;

import java.util.List;

public class Staff {
    
    private String id;
    private String name;
    private String tanentUn;
    //private String wx_account;
    private String work_num;
    private List<StaffSessionInfo> sessionChannelList;
    private List<String> skills;
    
    public Staff(String id, String name, String tanentUn,
            String work_num, List<StaffSessionInfo> sessionChannelList,
            List<String> skills) {
        super();
        this.id = id;
        this.name = name;
        this.tanentUn = tanentUn;
        this.work_num = work_num;
        this.sessionChannelList = sessionChannelList;
        this.skills = skills;
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

    public String getTanentUn() {
        return tanentUn;
    }

    public void setTanentUn(String tanentUn) {
        this.tanentUn = tanentUn;
    }

    public String getWork_num() {
        return work_num;
    }

    public void setWork_num(String work_num) {
        this.work_num = work_num;
    }

    public List<StaffSessionInfo> getSessionChannelList() {
        return sessionChannelList;
    }

    public void setSessionChannelList(List<StaffSessionInfo> sessionChannelList) {
        this.sessionChannelList = sessionChannelList;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    @Override
    public String toString() {
        return "Staff [id=" + id + ", name=" + name + ", tanentUn=" + tanentUn
                + ", work_num=" + work_num + ", sessionChannelList="
                + sessionChannelList + ", skills=" + skills + "]";
    }

}
