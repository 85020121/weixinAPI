package com.hesong.weChatAdapter.runner;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import com.hesong.jsonrpc.JsonrpcHandler;
import com.hesong.jsonrpc.WeChatMethodSet;
import com.hesong.smartbus.client.PackInfo;

public class JsonrpcHandlerRunner implements Runnable {
    
    public static Map<String, BlockingQueue<String>> loginAckRetQueue = new ConcurrentHashMap<String, BlockingQueue<String>>();
    public static Map<String, BlockingQueue<Object>> getRoomsRetQueue = new ConcurrentHashMap<String, BlockingQueue<Object>>();

    private BlockingQueue<PackInfo> messageQueue;
    private BlockingQueue<String> responseQueue;
    private JsonrpcHandler handler;
    private int id;

    public JsonrpcHandlerRunner(BlockingQueue<PackInfo> messageQueue,
            BlockingQueue<String> responseQueue, int id) {
        super();
        this.messageQueue = messageQueue;
        this.responseQueue = responseQueue;
        this.handler = new JsonrpcHandler(new WeChatMethodSet());
        this.id = id;
        SmartbusExecutor.SmartbusLog.info("Handler number "+id+" loaded.");
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public BlockingQueue<PackInfo> getMessageQueue() {
        return messageQueue;
    }

    public void setMessageQueue(BlockingQueue<PackInfo> messageQueue) {
        this.messageQueue = messageQueue;
    }

    public BlockingQueue<String> getResponseQueue() {
        return responseQueue;
    }

    public void setResponseQueue(BlockingQueue<String> responseQueue) {
        this.responseQueue = responseQueue;
    }

    public JsonrpcHandler getHandler() {
        return handler;
    }

    public void setHandler(JsonrpcHandler handler) {
        this.handler = handler;
    }

    @Override
    public void run() {
        while (true) {
            try {
                PackInfo pack = getMessageQueue().take();
                String response = handler.handle(pack.getText());
                if (response != null) {
//                    pack.setText(response);
                    responseQueue.put(response);
                }
            } catch (InterruptedException e) {
                SmartbusExecutor.SmartbusLog.error("BlockingQueue exception: "
                        + e.toString());
            }
        }

    }

}
