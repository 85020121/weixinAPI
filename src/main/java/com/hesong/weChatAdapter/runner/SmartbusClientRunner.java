package com.hesong.weChatAdapter.runner;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

import redis.clients.jedis.Jedis;
import net.sf.json.JSONObject;

import com.hesong.smartbus.client.PackInfo;
import com.hesong.smartbus.client.net.Client;
import com.hesong.smartbus.client.net.Client.SendDataError;
import com.hesong.weChatAdapter.context.ContextPreloader;

public class SmartbusClientRunner implements Runnable {

    private BlockingQueue<String> responseQueue;
    private Random r = new Random();

    private void sendText(String text) throws SendDataError {
        int index = r.nextInt(SmartbusExecutor.smartbusClients.size());
        SmartbusExecutor.SmartbusLog.info("index="+index);
        Client client = SmartbusExecutor.smartbusClients.get(index);
        
        Map<String, Byte> busInfo = ContextPreloader.busList.get(index);
        
        if (null!=client && null!=busInfo) {
            PackInfo pack = null;
            JSONObject msg = JSONObject.fromObject(text);
            if (msg.containsKey("id")) {
                String id = msg.getString("id");
                Jedis jedis = ContextPreloader.jedisPool.getResource();
                jedis.select(ContextPreloader.REDIS_DB_NUM);
                SmartbusExecutor.SmartbusLog.info("Existed:"+jedis.exists(id)+"  id="+id);
                if (jedis.exists(id)) {
                    String desInfo = jedis.get(id);
                    JSONObject desJo = JSONObject.fromObject(desInfo);
                    pack = new PackInfo((byte)desJo.getInt("desUnit"), (byte)desJo.getInt("desClient"), busInfo.get("unitid"), busInfo.get("clientid"), text);
                    ContextPreloader.jedisPool.returnResource(jedis);
                } else {
                    ContextPreloader.jedisPool.returnResource(jedis);
                    pack = new PackInfo(busInfo.get("destunitid"), busInfo.get("destclientid"), busInfo.get("unitid"), busInfo.get("clientid"), text);
                }
            }
            
            SmartbusExecutor.SmartbusLog.info("Pack: "+pack.toString());
            client.sendText(pack.getCmd(), pack.getCmdType(),
                  (int) pack.getSrcUnitId(), (int) pack.getSrcClientId(),
                  (int) pack.getSrcClientType(), pack.getText());
        } else {
            SmartbusExecutor.SmartbusLog.error("No client: " + client + "   " + busInfo);
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
//                PackInfo pack = responseQueue.take();
//                takeClient().sendText(pack.getCmd(), pack.getCmdType(),
//                        (int) pack.getSrcUnitId(), (int) pack.getSrcClientId(),
//                        (int) pack.getSrcClientType(), pack.getText());
                String response = responseQueue.take();
                SmartbusExecutor.SmartbusLog.info("Response: " + response);;
                sendText(response);
            } catch (InterruptedException | SendDataError e) {
                if (e instanceof InterruptedException) {
                    SmartbusExecutor.SmartbusLog
                            .error("ResponseQueue exception: " + e.toString());
                } else {
                    SmartbusExecutor.SmartbusLog
                            .error("Send data error: " + e.toString());
                }
            }

        }
    }
    
    public SmartbusClientRunner(BlockingQueue<String> responseQueue) {
        super();
        this.responseQueue = responseQueue;
    }

    public BlockingQueue<String> getResponseQueue() {
        return responseQueue;
    }

    public void setResponseQueue(BlockingQueue<String> responseQueue) {
        this.responseQueue = responseQueue;
    }

}
