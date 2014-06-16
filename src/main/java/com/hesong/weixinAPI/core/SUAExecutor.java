package com.hesong.weixinAPI.core;

import java.util.concurrent.BlockingQueue;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.log4j.Logger;

import com.hesong.sugarCRM.SugarCRMCaller;


public class SUAExecutor implements Runnable {
    private static Logger log = Logger.getLogger(SUAExecutor.class);
    
    private BlockingQueue<JSONObject> suaRequestToExecuteQueue;
    @Override
    public void run() {
        SugarCRMCaller caller = new SugarCRMCaller();
        String session = caller.login("admin", "p@ssw0rd");
        while(true){
            try {
                JSONObject message = suaRequestToExecuteQueue.take();
                
                String method = message.getString("method");
                message.remove("method");
                message.put("session", session);
                log.info("Message to record: " + message.toString());
                String r = caller.call(method, message.toString());
                JSONObject rj = (JSONObject) JSONSerializer.toJSON(r);
                if (!rj.containsKey("id")) {
                    log.warn("Session_id expired, renew one.");
                    session = caller.login("admin", "p@ssw0rd");
                    log.info("Session_id renewed: " + session);
                    message.put("session", session);
                    r = caller.call(method, message.toString());
                    rj = (JSONObject) JSONSerializer.toJSON(r);
                }
            } catch (Exception e) {
                log.error("Record message failed: " + e.toString());
                e.printStackTrace();
            }
        }
        
    }
    public SUAExecutor(BlockingQueue<JSONObject> suaRequestToExecuteQueue) {
        super();
        this.suaRequestToExecuteQueue = suaRequestToExecuteQueue;
    }
    public BlockingQueue<JSONObject> getSuaRequestToExecuteQueue() {
        return suaRequestToExecuteQueue;
    }
    public void setSuaRequestToExecuteQueue(
            BlockingQueue<JSONObject> suaRequestToExecuteQueue) {
        this.suaRequestToExecuteQueue = suaRequestToExecuteQueue;
    }
    
    
}
