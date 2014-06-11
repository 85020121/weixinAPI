package com.hesong.weixinAPI.core;

import java.util.concurrent.BlockingQueue;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.log4j.Logger;

import com.hesong.sugarCRM.SugarCRMCaller;


public class MessageRecorder implements Runnable {
    private static Logger log = Logger.getLogger(MessageRecorder.class);
    
    private BlockingQueue<JSONObject> messageToRecordQueue;
    @Override
    public void run() {
        SugarCRMCaller caller = new SugarCRMCaller();
        String session = caller.login("admin", "p@ssw0rd");
        JSONObject record = new JSONObject();
        record.put("session", session);
        while(true){
            try {
                JSONObject message = messageToRecordQueue.take();
                record.put("module_name", message.getString("module_name"));
                message.remove("module_name");
                record.put("name_value_list", message.toString());
                log.info("Message to record: " + record.toString());
                String r = caller.call("set_entry", record.toString());
                JSONObject rj = (JSONObject) JSONSerializer.toJSON(r);
                if (!rj.containsKey("id")) {
                    log.warn("Session_id expired, renew one.");
                    session = caller.login("admin", "p@ssw0rd");
                    log.info("Session_id renewed: " + session);
                    record.put("session", session);
                    caller.call("set_entry", record.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
    }
    
    
    public MessageRecorder(BlockingQueue<JSONObject> messageToRecordQueue) {
        super();
        this.messageToRecordQueue = messageToRecordQueue;
    }
    public BlockingQueue<JSONObject> getMessageToRecordQueue() {
        return messageToRecordQueue;
    }
    public void setMessageToRecordQueue(
            BlockingQueue<JSONObject> messageToRecordQueue) {
        this.messageToRecordQueue = messageToRecordQueue;
    }
    
}
