package com.wqq.httpclient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.wqq.httpclient.HttpHeaderBuilder.Headers;

public class HttpClientTest {
	
	public static void testPing() throws HttpProcessException {
		//设置超时时间
		HCBuilder httpClientBuilder = HCBuilder.custom().timeout(5000, 10000, false);
		
		HttpConfigBuilder config = HttpConfigBuilder.custom()
				.client(httpClientBuilder.build())
				.url("http://localhost:7050/ping?rand=0d066df2-9d07-4a4e-8550-f02ac4d13358&sign=3x8pzY9o3QvYsf3VMFxCqKher6onx9D9QDqv4A==");
		
		String result = HttpClientUtil.get(config);
		System.out.println(result);
	}
	
	public static void testUpload() throws HttpProcessException, FileNotFoundException {
		//设置超时时间
		HCBuilder httpClientBuilder = HCBuilder.custom().timeout(5000, 10000, false);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("rand", "0d066df2-9d07-4a4e-8550-f02ac4d13358");
		map.put("sign", "3x8pzY9o3QvYsf3VMFxCqKher6onx9D9QDqv4A==");
		
		/*
		File f = new File("E:\\test_tool\\face.zip");
		System.out.println("File length : " + f.length());
		FileInputStream fis = new FileInputStream(f);
		ByteArrayOutputStream swapStream = null;
		try {
			swapStream = new ByteArrayOutputStream();
			byte[] buff = new byte[1024];
			int rc = 0;
			while ((rc = fis.read(buff, 0, 1024)) > 0) {
				swapStream.write(buff, 0, rc);
			}
			byte[] bb = swapStream.toByteArray();
			System.out.println("swapStream length : " + bb.length);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				swapStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		*/
		
		//不使用长连接
		Header[] headers = HttpHeaderBuilder.custom().connection(Headers.CONN_CLOSE).build();
		
		HttpConfigBuilder config = HttpConfigBuilder.custom()
				.client(httpClientBuilder.build())
				.map(map)
				.headers(headers)
				.encoding("utf-8")
				.url("http://localhost:7050/uploadFilePart")
				//支持多文件上传，多个上传的时候的时候，如果是2个文件，将会按数组顺序生成2个请求参数  deployFilePart0, deployFilePart1
				//如果只上传1个文件，那么deployFilePart将作为上传请求参数名
				.files(new String[]{"E:\\test_tool\\ocr.zip"}, "deployFilePart");
		
		String result = HttpClientUtil.post(config);
		System.out.println(result);
	}
	
	public static void main(String[] args) throws HttpProcessException, FileNotFoundException {
		//testPing();
		testUpload();
	}
	
	
	public static String testUp222() throws FileNotFoundException {
		String filename = "deployFilePart";
		byte[] fileBytes = null;
		
		File f = new File("E:\\test_tool\\1.zip");
		System.out.println("File length : " + f.length());
		FileInputStream fis = new FileInputStream(f);
		ByteArrayOutputStream swapStream = null;
		try {
			swapStream = new ByteArrayOutputStream();
			byte[] buff = new byte[1024];
			int rc = 0;
			while ((rc = fis.read(buff, 0, 1024)) > 0) {
				swapStream.write(buff, 0, rc);
			}
			fileBytes = swapStream.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				swapStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		System.out.println("swapStream length : " + fileBytes.length);
		
		//拿到fileName拼接URL
		StringBuffer sb=new StringBuffer();
		final String url = sb.append("http://localhost:7050/uploadFilePart").toString();
	    //创建HttpClient实例
		CloseableHttpClient httpClient = HttpClients.createDefault();
		//创建post方法连接实例，在post方法中传入待连接地址
		HttpPost httpPost = new HttpPost(url);
		CloseableHttpResponse response = null;

		try {
		//设置请求参数（类似html页面中name属性）
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			builder.setCharset(Charset.forName("UTF-8"));
			if(fileBytes!=null) {
			//内容类型，用于定义网络文件的类型和网页的编码，决定文件接收方将以什么形式、什么编码读取这个文件
				ContentType OCTEC_STREAM = ContentType.create("application/octet-stream", Charset.forName("UTF-8"));
				//添加文件
				builder.addBinaryBody("deployFilePart", fileBytes, OCTEC_STREAM, filename);
			}
			builder.addTextBody("rand", "0d066df2-9d07-4a4e-8550-f02ac4d13358");
			builder.addTextBody("sign", "3x8pzY9o3QvYsf3VMFxCqKher6onx9D9QDqv4A==");
			
			httpPost.setEntity(builder.build());
			//发起请求，并返回请求响应
			response = httpClient.execute(httpPost);
			String uploadResult = EntityUtils.toString(response.getEntity(), "utf-8");
			
			
			System.out.println(uploadResult);
			return uploadResult;

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				response.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return "文件上传错误";
	}

}
