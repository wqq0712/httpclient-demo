package com.wqq.httpclient;

import java.util.HashMap;

/**
 * 测试文件切片和分片上传
 * @author qq.wang
 *
 */
public class UploadPartFileTest {
	
	public static void main(String[] args) {
		
		String commonFileUploadUrl = "http://localhost:7050/uploadFile";
		String partFileUploadUrl = "http://localhost:7050/uploadFilePart";
		String partMergeUrl = "http://localhost:7050/mergeFile";
		String appId = "28847818f954463db92f5160f63894b4";
		String filepath = "F:\\迅雷下载\\eclipse-SDK-4.11-win32-x86_64(1).zip";
		
		SmartUploader uploader = new SmartUploader(commonFileUploadUrl, partFileUploadUrl, partMergeUrl);
		uploader.setAbortWholeOnFail(true);
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("rand", "0d066df2-9d07-4a4e-8550-f02ac4d13358");
		params.put("sign", "3x8pzY9o3QvYsf3VMFxCqKher6onx9D9QDqv4A==");
		
		uploader.addFile(filepath, appId, "uploadFile", params, null);
		uploader.upload();
		
		System.out.println(uploader.getTaskMap());
		
	}
	
}
