package com.wqq.entity;

public class ResponseModel {
	
	/**
	 * 响应码
	 */
	private int code;
	
	/**
	 * 响应消息
	 */
	private String msg;
	
	public ResponseModel(int code, String msg) {
		this.code = code;
		this.msg = msg;
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	@Override
	public String toString() {
		return "ResponseModel [code=" + code + ", msg=" + msg + "]";
	}
	
	
}
