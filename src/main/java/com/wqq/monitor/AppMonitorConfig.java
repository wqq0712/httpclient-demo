package com.wqq.monitor;

/**
 * 应用监控配置
 * @author wangqq
 *
 */
public class AppMonitorConfig {
	
	/**
	 * 监控应用地址
	 */
	private String appUrl;
	
	/**
	 *  初始监控时间频率, 单位秒
	 */
	private long initInterval;
	
	/**
	 *  监控时间频率衰减量（降频检测）, 单位秒
	 */
	private long reduction;
	
	/**
	 *  最终监控时间频率, 单位秒
	 */
	private long lastInterval;
	
	public AppMonitorConfig(String appUrl, long initInterval, long reduction, long lastInterval) {
		super();
		this.appUrl = appUrl;
		this.initInterval = initInterval;
		this.reduction = reduction;
		this.lastInterval = lastInterval;
	}

	public String getAppUrl() {
		return appUrl;
	}

	public void setAppUrl(String appUrl) {
		this.appUrl = appUrl;
	}

	public long getInitInterval() {
		return initInterval;
	}

	public void setInitInterval(long initInterval) {
		this.initInterval = initInterval;
	}

	public long getReduction() {
		return reduction;
	}

	public void setReduction(long reduction) {
		this.reduction = reduction;
	}

	public long getLastInterval() {
		return lastInterval;
	}

	public void setLastInterval(long lastInterval) {
		this.lastInterval = lastInterval;
	}
	
	
}
