package com.wqq.httpclient;

import com.wqq.monitor.AppMonitorConfig;
import com.wqq.monitor.HeartbeatCheckService;

public class TestMain {
	
	public static void main(String[] args) throws Exception {
		
		HeartbeatCheckService checkService = HeartbeatCheckService.getInstance();
		AppMonitorConfig config0 = new AppMonitorConfig("http://www.baidu.com", 2, 2, 60);
		checkService.addMonitorTask(config0);
		
		//AppMonitorConfig config1 = new AppMonitorConfig("http://www.sina.com", 2, 2, 60);
		//checkService.addMonitorTask(config1);
		
		//AppMonitorConfig config2 = new AppMonitorConfig("http://www.google.com", 2, 2, 60);
		//checkService.addMonitorTask(config2);
		
		AppMonitorConfig config3 = new AppMonitorConfig("http://www.youtube.com", 2, 2, 20);
		checkService.addMonitorTask(config3);
		
		checkService.start(1);
	}
}
