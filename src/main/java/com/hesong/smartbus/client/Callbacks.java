package com.hesong.smartbus.client;

/**
 * Smartbus客户端的回调函数接口类型
 * 
 * 每个客户端实例都应该被提供一个该类型的实现类实例。实例中的各个接口函数的实现将接收客户端实例的回调函数事件通知。
 * 
 * @author tanbro
 * 
 */
public interface Callbacks {
	/**
	 * 连接成功
	 */
	public void onConnectSuccess();

	/**
	 * 连接失败
	 * 
	 * @param errorCode
	 *            失败错误编码
	 */
	public void onConnectFail(Integer errorCode);

	/**
	 * 连接断开
	 */
	public void onDisconnect();

	/**
	 * 全局节点客户端连接、断开通知
	 * 
	 * @param unitId
	 *            节点客户端的unitid
	 * @param clientId
	 *            客户端ID。是node中心节点连接时，clientid值为-1
	 * @param clientType
	 *            客户端类型
	 * @param status
	 *            连接状态： 0 断开连接、1 新建连接、2 已有的连接
	 * @param addInfo
	 *            连接附加信息
	 */
	public void onGlobalConnectInfo(Byte unitId, Byte clientId,
			Byte clientType, Byte status, String addInfo);

	/**
	 * 收到了文本
	 * 
	 * @param head
	 *            消息头
	 * @param txt
	 *            文本
	 */
	public void onReceiveText(PackInfo head, String txt);

	/**
	 * 流程返回
	 * 
	 * @param head
	 *            消息头
	 * @param projectId
	 *            流程所属的项目ID
	 * @param invokeId
	 *            该流程返回消息所对应的流程调用的ID。流程调用的ID由
	 *            com.hesong.smartbus.client.net.Client.invokeFlow 返回
	 * @param param
	 *            流程返回的数据
	 */
	public void onFlowReturn(PackInfo head, String projectId, Integer invokeId,
			String param);

	/**
	 * 流程返回超时
	 * 
	 * @param head
	 *            消息头
	 * @param projectId
	 *            流程所属的项目ID
	 * @param invokeId
	 *            该流程返回消息所对应的流程调用的ID。流程调用的ID由
	 *            com.hesong.smartbus.client.net.Client.invokeFlow 返回
	 */
	public void onFlowTimeout(PackInfo head, String projectId, Integer invokeId);
	
}
