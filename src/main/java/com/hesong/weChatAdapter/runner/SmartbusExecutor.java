package com.hesong.weChatAdapter.runner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import org.apache.log4j.Logger;

import com.hesong.smartbus.client.PackInfo;
import com.hesong.smartbus.client.net.Client;
import com.hesong.smartbus.client.net.JniWrapper;

public class SmartbusExecutor {
    public static Logger SmartbusLog = Logger.getLogger(SmartbusExecutor.class);

    private static final int THREAD_POOL_SIZE = 10;
    private static final int MAX_HANDLER_NUM = 5;
    private static ExecutorService pool = Executors
            .newFixedThreadPool(THREAD_POOL_SIZE);
    public static BlockingQueue<String> responseQueue = new LinkedBlockingDeque<String>();

    public static List<Client> smartbusClients = new ArrayList<Client>();

    public static String execute() {

        for (int i = 0; i < MAX_HANDLER_NUM; i++) {
            JsonrpcHandlerRunner handler = new JsonrpcHandlerRunner(
                    JniWrapper.messageQueue, responseQueue, i);
            pool.execute(handler);
        }

        SmartbusClientRunner smartbus = new SmartbusClientRunner(responseQueue);
        pool.execute(smartbus);

        return "success";
    }
}
