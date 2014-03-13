package com.hesong.weChatAdapter.runner;

import java.util.concurrent.BlockingQueue;

import com.hesong.smartbus.client.PackInfo;
import com.hesong.smartbus.client.net.Client;
import com.hesong.smartbus.client.net.Client.SendDataError;

public class SmartbusClientRunner implements Runnable {

    private Client client;
    private BlockingQueue<PackInfo> responseQueue;

    public SmartbusClientRunner(Client client,
            BlockingQueue<PackInfo> responseQueue) {
        super();
        this.client = client;
        this.responseQueue = responseQueue;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public BlockingQueue<PackInfo> getResponseQueue() {
        return responseQueue;
    }

    public void setResponseQueue(BlockingQueue<PackInfo> responseQueue) {
        this.responseQueue = responseQueue;
    }

    @Override
    public void run() {
        while (true) {
            try {
                PackInfo pack = responseQueue.take();
                SmartbusExecutor.SmartbusLog
                        .info("New response in queue.");
                SmartbusExecutor.SmartbusLog
                .info("SEND JSONRPC OVER SMARTBUS: " + pack.toString());
                getClient().sendText(pack.getCmd(), pack.getCmdType(),
                        (int) pack.getSrcUnitId(), (int) pack.getSrcClientId(),
                        (int) pack.getSrcClientType(), pack.getText());
            } catch (InterruptedException | SendDataError e) {
                if (e instanceof InterruptedException) {
                    SmartbusExecutor.SmartbusLog
                            .info("ResponseQueue exception: " + e.toString());
                } else {
                    SmartbusExecutor.SmartbusLog
                            .info("Send data error: " + e.toString());
                }
            }

        }
    }

}
