package com.hesong.weixinAPI.model;

import java.util.HashMap;
import java.util.Map;

public class Staff {
    
    private int openChannel;
    private Map<String, String> idMap;
    public Staff() {
        super();
        openChannel = 0;
        idMap = new HashMap<String, String>();
    }
    public int getOpenChannel() {
        return openChannel;
    }
    public void setOpenChannel(int openChannel) {
        this.openChannel = openChannel;
    }
    public Map<String, String> getIdMap() {
        return idMap;
    }
    public void setIdMap(Map<String, String> idMap) {
        this.idMap = idMap;
    }
    @Override
    public String toString() {
        return "Staff [openChannel=" + openChannel + ", idMap=" + idMap + "]";
    }
    
}
