package com.hesong.weixinAPI.context;


import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

public class SpringDatasource extends JdbcDaoSupport {


    public JdbcTemplate getJT(){
        return getJdbcTemplate();
    }
    
    
}
