package com.hesong.weChatAdapter.runner;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import org.apache.log4j.Logger;

import com.hesong.smartbus.client.PackInfo;
import com.hesong.smartbus.client.WeChatCallback;
import com.hesong.smartbus.client.net.Client;
import com.hesong.smartbus.client.net.JniWrapper;
import com.hesong.smartbus.client.net.Client.ConnectError;

public class SmartbusExecutor {
    public static Logger SmartbusLog = Logger.getLogger(SmartbusExecutor.class);
    
    private static final int THREAD_POOL_SIZE = 10;
    private static final int MAX_HANDLER_NUM = 5;
    private static ExecutorService pool = Executors
            .newFixedThreadPool(THREAD_POOL_SIZE);

    public static String execute(byte unitId, byte clientId, String host,
            short port) {
        Client.initialize(unitId);
        Client client = new Client(clientId, (long) 11, host, port,
                "WeChat client");
        client.setCallbacks(new WeChatCallback());

        try {
            SmartbusLog.info("Connecting to smartbus...");
            client.connect();
            SmartbusLog.info("Smartbus connection successed. ");
            JniWrapper.CLIENT = client;
            BlockingQueue<PackInfo> responseQueue = new LinkedBlockingDeque<PackInfo>();
            for (int i = 0; i < MAX_HANDLER_NUM; i++) {
                JsonrpcHandlerRunner handler = new JsonrpcHandlerRunner(
                        JniWrapper.messageQueue, responseQueue, i);
                pool.execute(handler);
            }

            SmartbusClientRunner smartbus = new SmartbusClientRunner(client,
                    responseQueue);
            pool.execute(smartbus);

            return "success";
        } catch (ConnectError e) {
            SmartbusLog.error("Smartbus connect error: "+e.toString());
            return "failed";
        }
    }
}
