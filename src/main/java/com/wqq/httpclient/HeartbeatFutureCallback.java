package com.wqq.httpclient;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.concurrent.FutureCallback;

/**
 * Http心跳检测异步回调处理类
 * 
 * @author wangqq
 *
 */
public class HeartbeatFutureCallback implements FutureCallback<HttpResponse>{

	/**
	 * 监控数据
	 */
	private AppMonitorData monitorData;
	
	public HeartbeatFutureCallback(AppMonitorData monitorData) {
		super();
		if(monitorData == null) {
			throw new IllegalArgumentException("参数有误");
		}
		this.monitorData = monitorData;
	}

	/**
	 * 请求完成
	 */
	@Override
	public void completed(HttpResponse response) {
		System.out.println("成功返回： " + response.getStatusLine().getStatusCode());
		if(isSuccess(response)) {
			
			monitorData.setLastAccessResult(true);
			monitorData.setLastAccessEndTime(System.currentTimeMillis());
			resetFrequency();
		} else {
			monitorData.setLastAccessResult(false);
			monitorData.setLastAccessEndTime(System.currentTimeMillis());
			reduceFrequency();
		}
	}
	
	/**
	 * 重置为初始频率
	 */
	private void resetFrequency() {
		monitorData.setCurrentInterval(monitorData.getConfig().getInitInterval());
	}
	
	/**
	 * 降低检测频率
	 */
	private void reduceFrequency() {
		long curInterval = monitorData.getCurrentInterval();
		if(curInterval != monitorData.getConfig().getLastInterval()) {
			//频率衰减因子
			long reduction = monitorData.getConfig().getReduction();
			long lastInterval = monitorData.getConfig().getLastInterval();
			long tempInterval = curInterval + reduction;
			long newInterval = tempInterval >= lastInterval? lastInterval : tempInterval;
			System.out.println("设置下一次执行间隔为: " + newInterval + " 秒");
			monitorData.setCurrentInterval(newInterval);
		}
	}
	
	/**
	 * 请求失败
	 */
	@Override
	public void failed(Exception ex) {
		// TODO Auto-generated method stub
		//ConnectException ConnectTimeoutException HttpHostConnectException
		
		System.out.println("这里要记录日志，出错了。。。。。");
		ex.printStackTrace();
		monitorData.setLastAccessResult(false);
		monitorData.setLastAccessEndTime(System.currentTimeMillis());
		reduceFrequency();
	}

	/**
	 * 请求取消
	 */
	@Override
	public void cancelled() {
		System.out.println("这里要记录日志，请求被取消了。。。。。");
		monitorData.setLastAccessResult(false);
		monitorData.setLastAccessEndTime(System.currentTimeMillis());
		reduceFrequency();
	}
	
	/**
	 * 暂时设置为只有返回2xx, 3xx的认为是成功的
	 * @param response
	 * @return
	 */
	protected boolean isSuccess(HttpResponse response) {
		
		
		StatusLine statusLine = response.getStatusLine();
		int statusCode = statusLine.getStatusCode();
		/*
		//简单点，直接判断200-400区间
		if(statusCode >= 200 && statusCode < 400) {
			return true;
		} else {
			return false;
		}
		*/
		boolean success = false;
		switch (statusCode) {
		case HttpStatus.SC_OK:
			success = true;
			break;
		case HttpStatus.SC_CREATED:
			success = true;
			break;
		case HttpStatus.SC_ACCEPTED:
			success = true;
			break;
		case HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION:
			success = true;
			break;
		case HttpStatus.SC_NO_CONTENT:
			success = true;
			break;
		case HttpStatus.SC_RESET_CONTENT:
			success = true;
			break;
		case HttpStatus.SC_PARTIAL_CONTENT:
			success = true;
			break;
		case HttpStatus.SC_MULTI_STATUS:
			success = true;
			break;
		case HttpStatus.SC_MULTIPLE_CHOICES:
			success = true;
			break;
		case HttpStatus.SC_MOVED_PERMANENTLY:
			success = true;
			break;
		case HttpStatus.SC_MOVED_TEMPORARILY:
			success = true;
			break;
		case HttpStatus.SC_SEE_OTHER:
			success = true;
			break;
		case HttpStatus.SC_NOT_MODIFIED:
			success = true;
			break;
		case HttpStatus.SC_USE_PROXY:
			success = true;
			break;
		case HttpStatus.SC_TEMPORARY_REDIRECT:
			success = true;
			break;
		default:
			success = false;
			break;
		}
		return success;
	}
	
}
