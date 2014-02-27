package com.hesong.smartbus.client;

import org.apache.log4j.Logger;

public class WeChatCallback implements Callbacks {

    private static Logger log = Logger.getLogger(WeChatCallback.class);

    @Override
    public void onConnectSuccess() {
        log.info("Connection successed.");
    }

    @Override
    public void onConnectFail(Integer errorCode) {
        log.info("Connection failed.");
    }

    @Override
    public void onDisconnect() {
        log.info("Disconnect");
    }

    @Override
    public void onGlobalConnectInfo(Byte unitId, Byte clientId,
            Byte clientType, Byte status, String addInfo) {
        log.info("Smartbus global connection info: " + "unitId=" + unitId
                + " clientId=" + clientId + " clientType=" + clientType
                + " status=" + status + " addInfo=" + addInfo);
    }

    @Override
    public void onReceiveText(PackInfo head, String txt) {
        log.info("Receive text: "+txt);
    }

    @Override
    public void onFlowReturn(PackInfo head, String projectId, Integer invokeId,
            String param) {
        log.info("onFlowReturn");
    }

    @Override
    public void onFlowTimeout(PackInfo head, String projectId, Integer invokeId) {
        log.info("onFlowTimeout");
    }


}
