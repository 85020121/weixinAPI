package com.hesong.weChatAdapter.account;

import java.io.Serializable;

public class Account implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String unitid;
    private String acctype;
    private String accname;
    private String accdata;
    private String displayname;
    private String remark;
    private byte enabled;
    
    public Account() {
    }
    
    public Account(String id, String unitid, String acctype, String accname,
            String accdata, String displayname, String remark, byte enabled) {
        super();
        this.id = id;
        this.unitid = unitid;
        this.acctype = acctype;
        this.accname = accname;
        this.accdata = accdata;
        this.displayname = displayname;
        this.remark = remark;
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getUnitid() {
        return unitid;
    }
    public void setUnitid(String unitid) {
        this.unitid = unitid;
    }
    public String getAcctype() {
        return acctype;
    }
    public void setAcctype(String acctype) {
        this.acctype = acctype;
    }
    public String getAccname() {
        return accname;
    }
    public void setAccname(String accname) {
        this.accname = accname;
    }
    public String getAccdata() {
        return accdata;
    }
    public void setAccdata(String accdata) {
        this.accdata = accdata;
    }
    public String getDisplayname() {
        return displayname;
    }
    public void setDisplayname(String displayname) {
        this.displayname = displayname;
    }
    public String getRemark() {
        return remark;
    }
    public void setRemark(String remark) {
        this.remark = remark;
    }
    public byte getEnabled() {
        return enabled;
    }
    public void setEnabled(byte enabled) {
        this.enabled = enabled;
    }
    public static long getSerialversionuid() {
        return serialVersionUID;
    }
    

}
