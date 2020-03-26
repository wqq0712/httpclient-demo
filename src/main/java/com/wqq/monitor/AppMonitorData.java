package com.wqq.monitor;

import java.util.Date;

/**
 * 应用监控数据
 * @author wangqq
 *
 */
public class AppMonitorData {
	
	/**
	 * 应用监控配置
	 */
	private AppMonitorConfig appMonitorConfig;
	
	/**
	 * 最后访问开始时间
	 */
	private long lastAccessStartTime;
	
	/**
	 * 最后访问结束时间
	 */
	private long lastAccessEndTime;
	
	/**
	 * 最后访问结果
	 */
	private boolean lastAccessResult;
	
	/**
	 * 当前监控时间频率
	 */
	private long currentInterval;
	
	public AppMonitorData(AppMonitorConfig appMonitorConfig) {
		super();
		this.appMonitorConfig = appMonitorConfig;
		//将当前频率设置为初始频率
		this.currentInterval = appMonitorConfig.getInitInterval();
	}

	public AppMonitorConfig getConfig() {
		return appMonitorConfig;
	}


	public long getLastAccessStartTime() {
		return lastAccessStartTime;
	}

	public void setLastAccessStartTime(long lastAccessStartTime) {
		this.lastAccessStartTime = lastAccessStartTime;
	}

	public long getLastAccessEndTime() {
		return lastAccessEndTime;
	}

	public void setLastAccessEndTime(long lastAccessEndTime) {
		this.lastAccessEndTime = lastAccessEndTime;
	}

	public boolean isLastAccessResult() {
		return lastAccessResult;
	}

	public void setLastAccessResult(boolean lastAccessResult) {
		this.lastAccessResult = lastAccessResult;
	}

	public long getCurrentInterval() {
		return currentInterval;
	}

	public void setCurrentInterval(long currentInterval) {
		this.currentInterval = currentInterval;
	}
	
	
}
