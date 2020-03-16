package com.wqq.httpclient;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.nio.reactor.IOReactorException;

/**
 * 心跳检测服务
 * 
 * @author wangqq
 *
 */
public class HeartbeatCheckService {

	private static HeartbeatCheckService instance;
	
	/**
	 * 异步HttpClient
	 */
	private CloseableHttpAsyncClient httpAsyncClient;
	
	/**
	 * 已加入监控列表的监控数据
	 */
	private ConcurrentHashMap<String, AppMonitorData> monitorDatas;
	
	/**
	 * 待处理的监控任务
	 */
	private CopyOnWriteArrayList<AppMonitorConfig> monitorTasks;
	
	/**
	 * 运行状态
	 */
	private AtomicBoolean running;
	
	/**
	 * 定时任务执行器
	 */
	private ScheduledExecutorService scheduExec;
	
	private HeartbeatCheckService() throws IOReactorException {
		HttpAsyncClient client = new HttpAsyncClient(true);
		httpAsyncClient = client.getAsyncHttpClient();
		running = new AtomicBoolean(false);
		monitorTasks = new CopyOnWriteArrayList<AppMonitorConfig>();
		monitorDatas = new ConcurrentHashMap<String, AppMonitorData>();
		scheduExec = Executors.newSingleThreadScheduledExecutor();
	}
	
	public static HeartbeatCheckService getInstance() throws Exception {
		if(instance==null){
			synchronized(HeartbeatCheckService.class){
				if(instance==null){
					instance = new HeartbeatCheckService();
				}
			}
		}
		return instance;
	}
	
	/**
	 * 开启监控
	 */
	public void start(long periodWithSecond) {
		//防止多次调用start
		if(running.compareAndSet(false, true)) {
			Runnable monitorTask = () -> {  
                try {
                	//检测是否存在需要监控的任务2
                	for(AppMonitorConfig monitorConfig : monitorTasks) {
                		String appUrl = monitorConfig.getAppUrl();
                		//已存在的不加入监控
                		if(StringUtils.isBlank(appUrl) || monitorDatas.containsKey(appUrl)) {
                			monitorTasks.remove(monitorConfig);
                			continue;
                		}
                		AppMonitorData monitorData = new AppMonitorData(monitorConfig);
                		monitorDatas.put(appUrl, monitorData);
                		monitorTasks.remove(monitorConfig);
                	}
                	Collection<AppMonitorData> coll = monitorDatas.values();
                	for(AppMonitorData monitorData : coll) {
                		System.out.println("开始执行.............");
                		//判断是否符合执行条件
                		long currentInterval = monitorData.getCurrentInterval() * 1000L;
                		long lastAccessStartTime = monitorData.getLastAccessStartTime();
                		long now = System.currentTimeMillis();
                		if(currentInterval + lastAccessStartTime < now) {
                			monitorData.setLastAccessStartTime(now);
                			//执行异步get
                    		httpAsyncGet(monitorData.getConfig().getAppUrl(), new HeartbeatFutureCallback(monitorData));
                		}
                	}
                } catch (Exception e) {
                	//需要捕获异常，否则会阻塞该Task
                	//TODO 日志记录异常
                	e.printStackTrace();
                }
	        };
	        scheduExec.scheduleWithFixedDelay(monitorTask, 0, periodWithSecond, TimeUnit.SECONDS);
		}
	}
	
	/**
	 * 停止监控
	 */
	public void stop() {
		scheduExec.shutdown();
	}
	
	/**
	 * 添加到监控任务列表
	 * @param appMonitorConfig
	 */
	public void addMonitorTask(AppMonitorConfig appMonitorConfig) {
		monitorTasks.add(appMonitorConfig);
	}

	/**
	 * 向指定的URL发送一次异步get请求
	 * 心跳检测不需要参数，如果一定要加参数，请自行拼接到Url后面
	 * @param baseUrl 请求地址
	 * @param callback 回调类
	 * @throws Exception
	 */
	public void httpAsyncGet(String baseUrl, FutureCallback<HttpResponse> callback) throws Exception {
		if (StringUtils.isBlank(baseUrl)) {
			throw new IllegalArgumentException("baseUrl is blank");
		}
		HttpGet httpGet = new HttpGet(baseUrl);
		//取消长连接
		httpGet.setHeader("Connection", "close");
		httpAsyncClient.execute(httpGet, callback);
	}
}
