package com.hesong.weixinAPI.core;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.hesong.weixinAPI.context.AppContext;
import com.hesong.weixinAPI.context.SpringDatasource;
import com.hesong.weixinAPI.tools.WeChatHttpsUtil;

public class GroupMessagesSender implements Runnable {
    private static Logger log = Logger.getLogger(MessageSender.class);
    private static String SEND_MESSAGE_REQUEST_URL = "https://api.weixin.qq.com/cgi-bin/message/custom/send?access_token=";

    private BlockingQueue<JSONObject> groupMessagesQueue;

    public GroupMessagesSender(BlockingQueue<JSONObject> groupMessagesQueue) {
        super();
        this.groupMessagesQueue = groupMessagesQueue;
    }

    public BlockingQueue<JSONObject> getGroupMessagesQueue() {
        return groupMessagesQueue;
    }

    public void setGroupMessagesQueue(BlockingQueue<JSONObject> groupMessagesQueue) {
        this.groupMessagesQueue = groupMessagesQueue;
    }

    @Override
    public void run() {
        while(true){
            try {
                JSONObject message = groupMessagesQueue.take();
                log.info("Message to send: "+message.toString());
                String access_token = message.getString("access_token");
                message.remove("access_token");
                String sql = message.getString("sql");
                message.remove("sql");
                
                SpringDatasource sd = (SpringDatasource) AppContext.getApplicationContext().getBean("springDatasourceImp");
                JdbcTemplate jt = sd.getJT();
                if (jt == null) {
                    log.error("Build database connection failed.");
                } else {
                    jt.query(sql, new MyRowCallbackHandler(message, access_token));
                }
                
            } catch (Exception e) {
                log.error("Send group message error: "+e.toString());
                e.printStackTrace();
            }
        }
        
    }
    
    class MyRowCallbackHandler implements RowCallbackHandler{
        private JSONObject message;
        private String token;
        
        public MyRowCallbackHandler(JSONObject message, String token) {
            super();
            this.message = message;
            this.token = token;
        }

        public JSONObject getMessage() {
            return message;
        }

        public void setMessage(JSONObject message) {
            this.message = message;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        @Override
        public void processRow(ResultSet rs) throws SQLException {
            // TODO Auto-generated method stub
            String openid = rs.getString("weixinid_c");
            log.info("Client openid: "+ openid);
            message.put("touser", openid);
            String request = SEND_MESSAGE_REQUEST_URL + token;
            JSONObject ret = WeChatHttpsUtil.httpsRequest(request, "POST", message.toString());
            log.info("Send message ret: "+ret.toString());
        }
        
    }
}
