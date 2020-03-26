package com.wqq.httpclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wqq.entity.ResponseModel;
import com.wqq.httpclient.HttpHeaderBuilder.Headers;

/**
 * 文件上传组件，处理文件上传
 * 当文件小于指定大小，则执行通用文件上传，反之执行文件分片上传
 * 
 * @author qq.wang
 *
 */
public class SmartUploader {
	
	private static Logger logger = LoggerFactory.getLogger(SmartUploader.class);
	
	//默认分片文件大小为10M
	public static final long DEF_PART_SIZE = 10 * 1024 * 1024L;
	
	//默认连接超时
	public static final int CONNECT_TIMEOUT = 5000;
	
	//默认读超时
	public static final int SOCKET_TIMEOUT = 30000;
	
	private long partSize = DEF_PART_SIZE;
	
	/**
	 * 对应Agent端通用文件上传URL
	 */
	private String commonFileUploadUrl;
	
	/**
	 * 对应Agent端分片文件上传URL
	 */
	private String partFileUploadUrl;
	
	/**
	 * 对应Agent端文件合并URL
	 */
	private String partMergeUrl;
	
	/**
	 * 临时目录
	 */
	private String tempDir;
	
	/**
	 * 文件上传任务Map
	 */
	private Map<String, FileUploadTask> taskMap = new LinkedHashMap<String, FileUploadTask>();
	
	/**
	 * 其中一个上传Task失败的时候是否中断整个上传操作
	 */
	private boolean abortWholeOnFail = false;
	
	public SmartUploader(String commonFileUploadUrl, String partFileUploadUrl, String partMergeUrl) {
		super();
		this.commonFileUploadUrl = commonFileUploadUrl;
		this.partFileUploadUrl = partFileUploadUrl;
		this.partMergeUrl = partMergeUrl;
		this.tempDir = System.getProperty("java.io.tmpdir");
	}

	public SmartUploader(String commonFileUploadUrl, String partFileUploadUrl, String partMergeUrl, int partSize) {
		super();
		if(partSize < 1024) {
			throw new IllegalArgumentException("分片大小设置要大于1024");
		}
		this.partSize = partSize;
		this.commonFileUploadUrl = commonFileUploadUrl;
		this.partFileUploadUrl = partFileUploadUrl;
		this.partMergeUrl = partMergeUrl;
		this.tempDir = System.getProperty("java.io.tmpdir");
	}

	/**
	 * 添加需要上传的文件
	 * @param filepath 待上传的文件路径
	 * @param appId 应用id
	 * @param fileParamKey 文件上传的参数名
	 * @param params 上传的相关参数和参数值
	 * @param type 类型，当前仅支持zip
	 */
	public void addFile(String filepath, String appId, String fileParamKey, HashMap<String, Object> params, String type) {
		if(StringUtils.isBlank(filepath)) {
			throw new IllegalArgumentException("参数filepath为空");
		}
		if(StringUtils.isBlank(appId)) {
			throw new IllegalArgumentException("参数appId为空");
		}
		Map<String, Object> newParams = params;
		if(newParams == null) {
			newParams = new HashMap<String, Object>();
		}
		if(!newParams.containsKey("id")) {
			newParams.put("id", appId);
		}
		File file = new File(filepath);
		if(!file.exists() || !file.isFile()) {
			logger.error("{}不存在或者不是一个文件", filepath);
		}
		//加入到待上传列表
		FileUploadTask task = new FileUploadTask(appId, filepath, fileParamKey, type, params);
		taskMap.put(appId, task);
	}
	
	/**
	 * 执行上传动作
	 * @throws HttpProcessException 
	 */
	public void upload() {
		//先做成单线程运行，后续有需要再改造成多线程
		//如果需要展示总体进度的话，这里可以根据任务总数来进行进度计算
		Collection<FileUploadTask> tasks = taskMap.values();
		Iterator<FileUploadTask> it = tasks.iterator();
		while(it.hasNext()) {
			FileUploadTask task = it.next();
			boolean isOk = executeTask(task);
			if(!isOk && abortWholeOnFail) {
				break;	//中断后续task
			}
		}
	}
	
	/**
	 * 执行单个上传任务
	 * @param task
	 * @throws HttpProcessException 
	 */
	private boolean executeTask(FileUploadTask task) {
		//判断文件大小，如果文件小于指定大小，则执行文件上传，反之则执行分片上传
		File file = new File(task.filepath);
		long fileSize = file.length();
		if(fileSize <= partSize) {
			//执行普通文件上传
			logger.debug("执行普通文件上传");
			return uploadCommon(task);
		} else {
			//执行文件分片上传
			logger.debug("执行文件分片上传");
			return uploadPart(task);
		}
	}
	
	/**
	 * 普通上传
	 */
	private boolean uploadCommon(FileUploadTask task) {
		task.status = FileUploadTask.STAT_RUNNING;
		try {
			HCBuilder httpClientBuilder = HCBuilder.custom().timeout(CONNECT_TIMEOUT, SOCKET_TIMEOUT, false);
			//不使用长连接
			Header[] headers = HttpHeaderBuilder.custom().connection(Headers.CONN_CLOSE).build();
			HttpConfigBuilder config = HttpConfigBuilder.custom()
					.client(httpClientBuilder.build())
					.map(task.params)
					.headers(headers)
					.encoding("utf-8")
					.url(commonFileUploadUrl)
					.files(new String[]{task.filepath}, task.fileParamKey);
			String result = HttpClientUtil.post(config);
			
			ResponseModel responseModel = GsonUtil.jsonToBean(result, ResponseModel.class);
			if(responseModel.getCode() == 0) {	
				//成功
				task.status = FileUploadTask.STAT_COMPLETED;
				return true;
			} else {
				logger.error("普通文件上传返回失败，返回结果: {}", responseModel.toString());
				task.status = FileUploadTask.STAT_FAIL;
				task.errorMsg = responseModel.getMsg();
				return false;
			}
		} catch (HttpProcessException e) {
			logger.error(e.getMessage(), e);
			task.status = FileUploadTask.STAT_FAIL;
			task.errorMsg = e.getMessage();
			return false;
		}
	}
	
	/**
	 * 切片上传
	 * @param task
	 * @return
	 */
	private boolean uploadPart(FileUploadTask task) {
		task.status = FileUploadTask.STAT_RUNNING;
		Map<Integer, String> files = null;
		//执行文件切片
		try {
			files = splitPart(task);
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
			task.status = FileUploadTask.STAT_FAIL;
			task.errorMsg = "文件切片失败： " + e.getMessage();
			return false;
		}
		try {
			boolean result = true;
			String errorMsg = "";
			Set<Map.Entry<Integer, String>> set = files.entrySet();
			Iterator<Map.Entry<Integer, String>> it = set.iterator();
			while(it.hasNext()) {
				Map.Entry<Integer, String> entry = it.next();
				Integer index = entry.getKey();
				String partPath = entry.getValue();
				//单个切片最多重试3次
				int retryCount = 0;
				do {
					logger.debug("开始第{}次上传分片文件: {}", retryCount + 1, partPath);
					try {
						ResponseModel responseModel = doUploadPart(index, partPath, task);
						if(responseModel.getCode() == 0) {
							result = true;
						} else {
							logger.error("分片文件[{}]上传返回失败，返回结果: {}", partPath, responseModel.toString());
							result = false;
							errorMsg = responseModel.getMsg();
						}
					} catch(Exception e) {
						logger.error(e.getMessage(), e);
						result = false;
						errorMsg = e.getMessage();
					}
					retryCount++;
				} while (!result && retryCount < 3);
				
				if(!result) {
					break;
				}
			}
			if(!result) {
				task.status = FileUploadTask.STAT_FAIL;
				task.errorMsg = errorMsg;
				return result;
			} 
			
			//通知Agent执行文件合并
			logger.debug("开始通知Agent执行文件合并");
			ResponseModel responseModel = mergePart(task);
			if(responseModel.getCode() == 0) {	
				//成功
				task.status = FileUploadTask.STAT_COMPLETED;
				return true;
			} else {
				logger.error("文件合并操作返回失败，返回结果: {}", responseModel.toString());
				task.status = FileUploadTask.STAT_FAIL;
				task.errorMsg = responseModel.getMsg();
				return false;
			}
		} catch (HttpProcessException e) {
			logger.error(e.getMessage(), e);
			task.status = FileUploadTask.STAT_FAIL;
			task.errorMsg = e.getMessage();
			return false;
		} finally {
			//清除本地临时文件
			Collection<String> partFilePaths = files.values();
			if(partFilePaths != null && !partFilePaths.isEmpty()) {
				partFilePaths.forEach(path -> {
				    FileUtils.deleteQuietly(new File(path));
				});
			}
		}
	}
	
	/**
	 * 通知Agent合并文件
	 */
	private ResponseModel mergePart(FileUploadTask task) throws HttpProcessException {
		HCBuilder httpClientBuilder = HCBuilder.custom().timeout(CONNECT_TIMEOUT, SOCKET_TIMEOUT, false);
		//不使用长连接
		Header[] headers = HttpHeaderBuilder.custom().connection(Headers.CONN_CLOSE).build();
		HttpConfigBuilder config = HttpConfigBuilder.custom()
				.client(httpClientBuilder.build())
				.map(task.params)
				.headers(headers)
				.encoding("utf-8")
				.url(partMergeUrl);
		//这里可以用get，但是需要将params拼接参数，懒，所以直接使用post
		String result = HttpClientUtil.post(config);
		ResponseModel responseModel = GsonUtil.jsonToBean(result, ResponseModel.class);
		return responseModel;
	}
	
	@SuppressWarnings("unchecked")
	private ResponseModel doUploadPart(int index, String partPath, FileUploadTask task) throws HttpProcessException {
		//增加分片序号参数
		HashMap<String, Object> params = (HashMap<String, Object>)task.params.clone();
		params.put("index", index);
		HCBuilder httpClientBuilder = HCBuilder.custom().timeout(CONNECT_TIMEOUT, SOCKET_TIMEOUT, false);
		//不使用长连接
		Header[] headers = HttpHeaderBuilder.custom().connection(Headers.CONN_CLOSE).build();
		HttpConfigBuilder config = HttpConfigBuilder.custom()
				.client(httpClientBuilder.build())
				.map(params)
				.headers(headers)
				.encoding("utf-8")
				.url(partFileUploadUrl)
				.files(new String[]{partPath}, task.fileParamKey);
		String result = HttpClientUtil.post(config);
		ResponseModel responseModel = GsonUtil.jsonToBean(result, ResponseModel.class);
		return responseModel;
	}
	
	/**
	 * 文件切片，返回切片文件列表
	 * @throws IOException 
	 */
	private Map<Integer, String> splitPart(FileUploadTask task) throws IOException {
		
		Map<Integer, String> partFiles = new LinkedHashMap<Integer, String>();
		
		File srcFile = new File(task.filepath);
		long fileSize = srcFile.length();
		//计算切片数量
		//最简洁的计算方法:
		//long partCount = (fileSize  +  partSize  - 1) / partSize;
		long mod = fileSize % partSize;
		long partCount = fileSize / partSize ;
		if(mod > 0) {
			partCount++;
		}
		//切片文件暂存系统临时文件夹
		String partFileDir = tempDir;
		String partFileName = UUID.randomUUID().toString().replaceAll("-", "");
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(srcFile);
			for (int i = 0; i < partCount; i++) {
				long total = 0L;
				String partFile = OSUtils.assemblyPath(partFileDir, partFileName + "-" + i + ".tmp");
				FileOutputStream fos = null;
				try {
					fos = new FileOutputStream(new File(partFile));
					//定义一个缓冲区buff, 
					//注意注意: buff一定比partSize小
					byte[] buff = new byte[1024];
					int n = 0;
					while(total < partSize && (n = fis.read(buff)) != -1){
						fos.write(buff, 0, n);
						total = total + n;
		            }
					fos.flush();
				} finally {
					if(fos != null) {
						try {
							fos.close();
						} catch(IOException e) {
							logger.error(e.getMessage(), e);
						}
					}
				}
				partFiles.put(i, partFile);
				total = 0L;
			}
		} catch (IOException e) {
			throw e;
		} finally {
			if(fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
        return partFiles;
	}
	
	public boolean isAbortWholeOnFail() {
		return abortWholeOnFail;
	}

	public void setAbortWholeOnFail(boolean abortWholeOnFail) {
		this.abortWholeOnFail = abortWholeOnFail;
	}

	public Map<String, FileUploadTask> getTaskMap() {
		return taskMap;
	}

	class FileUploadTask {
		public static final int STAT_WAIT = 0;
		public static final int STAT_RUNNING = 1;
		public static final int STAT_COMPLETED = 2;
		public static final int STAT_FAIL = 3;
		public String appId;
		public String filepath;
		public String type;
		public HashMap<String, Object> params;
		public String fileParamKey;
		//0: 未开始  1:执行中  2:完成
		public int status;
		public String errorMsg;
		//进度, 单个任务自己的进度
		public int percent;
		public FileUploadTask(String appId, String filepath, String fileParamKey, String type, HashMap<String, Object> params) {
			super();
			this.appId = appId;
			this.filepath = filepath;
			this.fileParamKey = fileParamKey;
			this.type = type;
			this.status = STAT_WAIT;
			this.percent = 0;
			this.params = params;
		}
		
		@Override
		public String toString() {
			return "FileUploadTask [appId=" + appId + ", filepath=" + filepath + ", type=" + type + ", params=" + params
					+ ", fileParamKey=" + fileParamKey + ", status=" + status + ", errorMsg=" + errorMsg + ", percent="
					+ percent + "]";
		}
		
		
	}
	
}
