package com.hesong.smartbus.client.net;

import com.hesong.smartbus.client.PackInfo;

import java.util.Map;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * smartbus 网络客户端 JNI 包装类
 * 
 * @author tanbro
 * 
 */
public class JniWrapper {
    /**
     * 初始化 smartbus net 客户端
     * 
     * 对应 C-API 函数 SmartBusNetCli_Init
     * 
     * @param unitid
     *            该客户端在 smartbus 网络环境中的全局单元ID，其值必须大于等于16。
     * @return 0表示成功
     */
    public native static int Init(byte unitid);

    /**
     * 释放 smartbus net 客户端
     * 
     * 对应 C-API 函数 SmartBusNetCli_Release
     */
    public native static void Release();

    /**
     * 新建一个 smartbus net 客户端
     * 
     * 对应 C-API 函数 SmartBusNetCli_CreateConnect
     * 
     * @param local_clientid
     *            客户端ID
     * @param local_clienttype
     *            客户端类型
     * @param masterip
     *            目标主IP地址
     * @param masterport
     *            目标主端口
     * @param slaverip
     *            目标从IP地址。没有从地址的，填写0，或者""
     * @param slaverport
     *            目标从端口。没有从端口的，填写0xFFFF
     * @param author_username
     *            验证用户名
     * @param author_pwd
     *            验证密码
     * @param add_info
     *            附加信息
     * @return 0表示成功
     */
    public native static int CreateConnect(byte local_clientid,
            long local_clienttype, String masterip, short masterport,
            String slaverip, short slaverport, String author_username,
            String author_pwd, String add_info);

    /**
     * 发送文本数据
     * 
     * 对应 C-API 函数 SmartBusNetCli_SendData
     * 
     * @param local_clientid
     *            发送数据的本地客户端ID
     * @param cmd
     *            数据的命令关键字
     * @param cmdtype
     *            数据的命令类型
     * @param dst_unitid
     *            目的单元ID
     * @param dst_clientid
     *            目的客户端ID
     * @param dst_clienttype
     *            目的客户端类型
     * @param txt
     *            发送的文本正文
     * @return 0表示成功
     */
    public native static int SendText(byte local_clientid, byte cmd,
            byte cmdtype, int dst_unitid, int dst_clientid, int dst_clienttype,
            String txt);

    /**
     * 调用 Smartbus 服务器所在机器的 Flow
     * 
     * 对应 C-API 函数 SmartBusNetCli_RemoteInvokeFlow
     * 
     * @param local_clientid
     *            发送流程调用命令的本地客户端ID
     * @param server_unitid
     *            smarbus服务器的单元ID
     * @param ipscindex
     *            smart flow 的运行时进程索引
     * @param projectid
     *            flow 项目ID
     * @param flowid
     *            flow ID
     * @param mode
     *            调用模式
     * @param timeout
     *            等待流程返回结果超时值
     * @param in_valuelist
     *            流程传入函数列表
     * @return 如果 mode 为 0 ，则返回值表示该次请求的ID，用于在 cb_invokeflowret 回调中处理请求-回复的配对
     */
    public native static int RemoteInvokeFlow(byte local_clientid,
            int server_unitid, int ipscindex, String projectid, String flowid,
            int mode, int timeout, String in_valuelist);

    static {
        System.loadLibrary("smartbus_net_cli_jni");
    }

    /**
     * 该库中，客户端ID->客户端实例 对照表
     */
    public static Map<Byte, Client> instances = new ConcurrentHashMap<Byte, Client>();

    protected static void cb_connection(int arg, byte local_clientid,
            int accesspoint_unitid, int ack) {
        Client inst = JniWrapper.instances.get(local_clientid);
        if (inst != null) {
            if (ack == 0) {
                inst.getCallbacks().onConnectSuccess();
            } else {
                inst.getCallbacks().onConnectFail(ack);
            }
        }
    }

    protected static void cb_disconnect(int arg, byte local_clientid) {
        Client inst = instances.get(local_clientid);
        if (inst != null) {
            inst.getCallbacks().onDisconnect();
        }
    }

    protected static void cb_recvdata(int arg, byte local_clientid,
            PackInfo head, String txt) {
        System.out.println("Recv clientId = "+local_clientid);
        Client inst = instances.get(local_clientid);
        System.out.println("Client instance:"+inst);
        if (inst != null) {
            inst.getCallbacks().onReceiveText(head, txt);
        }
    }

    protected static void cb_invokeflowret(int arg, byte local_clientid,
            PackInfo head, String projectid, int invoke_id, int ret,
            String param) {
        Client inst = instances.get(local_clientid);
        if (inst != null) {
            if (ret == 1) {
                inst.getCallbacks().onFlowReturn(head, projectid, invoke_id,
                        param);
            } else {
                inst.getCallbacks().onFlowTimeout(head, projectid, invoke_id);
            }
        }
    }

    protected static void cb_globalconnect(int arg, byte unitid, byte clientid,
            byte clienttype, byte status, String addinfo) {
        System.out.println(instances);
        Iterator<Entry<Byte, Client>> iter = instances.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Byte, Client> pair = (Map.Entry<Byte, Client>) iter
                    .next();
            Client inst = pair.getValue();
            inst.getCallbacks().onGlobalConnectInfo(unitid, clientid,
                    clienttype, status, addinfo);
        }
    }
    
}
