package com.hesong.weChatAdapter.model;

import java.util.List;
import java.util.Map;

public class FollowersList {
    private int total;
    private int count;
    private Map<String, List<String>> data;
    private String next_openid;
    public int getTotal() {
        return total;
    }
    public void setTotal(int total) {
        this.total = total;
    }
    public int getCount() {
        return count;
    }
    public void setCount(int count) {
        this.count = count;
    }
    public Map<String, List<String>> getData() {
        return data;
    }
    public void setData(Map<String, List<String>> data) {
        this.data = data;
    }
    public String getNext_openid() {
        return next_openid;
    }
    public void setNext_openid(String next_openid) {
        this.next_openid = next_openid;
    }
    
}
