package com.hesong.smartbus.client;

/**
 * 流程调用模式
 * 
 * @see com.hesong.smartbus.client.net.Client#invokeFlow
 * 调用流程时候，需要指定流程调用的方式，是需要返回值的流程调用还是不需要返回值的流程调用。
 * @author tanbro
 * 
 */
public enum FlowInvokeMode {
	/**
	 * 需要返回值
	 */
	HASRESULT(0),
	/**
	 * 不需要返回值
	 */
	NORESULT(1);

	private int value;

	private FlowInvokeMode(int value) {
		this.value = value;
	}

	/**
	 * 返回该枚举所对应的整数值。
	 */
	public int getValue() {
		return this.value;
	}
}
