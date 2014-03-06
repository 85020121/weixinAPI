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
import com.hesong.smartbus.client.net.Client.SendDataError;

public class SmartbusExecutor {
    public static Logger SmartbusLog = Logger.getLogger(SmartbusExecutor.class);
    
    private static final int THREAD_POOL_SIZE = 10;
    private static ExecutorService pool = Executors
            .newFixedThreadPool(THREAD_POOL_SIZE);

    public static String execute(byte unitId, byte clientId, String host,
            short port) {
        Client.initialize(unitId);

        Client client = new Client(clientId, (long) 11, host, port,
                "WeChat client");
        client.setCallbacks(new WeChatCallback());

        try {
            client.connect();

            BlockingQueue<PackInfo> responseQueue = new LinkedBlockingDeque<PackInfo>();
            JsonrpcHandlerRunner handler = new JsonrpcHandlerRunner(
                    JniWrapper.messageQueue, responseQueue);
            SmartbusClientRunner smartbus = new SmartbusClientRunner(client,
                    responseQueue);
            pool.execute(handler);
            pool.execute(smartbus);

            return "success";
        } catch (ConnectError e) {
            SmartbusLog.error("Smartbus connect error: "+e.toString());
            return "failed";
        }
    }
}
