package com.wqq.httpclient;

import java.nio.charset.CodingErrorAction;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.Lookup;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.impl.auth.BasicSchemeFactory;
import org.apache.http.impl.auth.DigestSchemeFactory;
import org.apache.http.impl.auth.KerberosSchemeFactory;
import org.apache.http.impl.auth.NTLMSchemeFactory;
import org.apache.http.impl.auth.SPNegoSchemeFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.ssl.SSLContexts;
 

/**
 * 封装HttpClient异步实现，可设置代理，可设置认证，SSL
 * 
 * @author wangqq
 *
 */
public class HttpAsyncClient {
 
	/**
	 * 设置等待数据超时时间5秒钟, 根据业务调整, 单位毫秒
	 */
	private static int socketTimeout = 5000;
 
	/**
	 * 连接超时, 单位毫秒
	 */
	private static int connectTimeout = 3000;
 
	/**
	 * 连接池最大连接数
	 */
	private static int poolSize = 500;
 
	/**
	 * 每个路由的最大并发连接数
	 */
	private static int maxPerRoute = 200;
 
	/**
	 * 异步httpclient
	 */
	private CloseableHttpAsyncClient asyncHttpClient;
 
 
	public HttpAsyncClient(boolean needSSL) throws IOReactorException {
		this.asyncHttpClient = createAsyncClient(needSSL, null, null, null, 0);
		asyncHttpClient.start();
	}
	
	public HttpAsyncClient(boolean needSSL, String username, String password) throws IOReactorException {
		this.asyncHttpClient = createAsyncClient(needSSL, username, password, null, 0);
		asyncHttpClient.start();
	}
	
	public HttpAsyncClient(boolean needSSL, String proxyHost, int proxyPort) throws IOReactorException {
		this.asyncHttpClient = createAsyncClient(needSSL, null, null, proxyHost, proxyPort);
		asyncHttpClient.start();
	}
 
	public HttpAsyncClient(boolean needSSL, String username, String password, String proxyHost, int proxyPort) throws IOReactorException {
		
		this.asyncHttpClient = createAsyncClient(needSSL, username, password, proxyHost, proxyPort);
		asyncHttpClient.start();
	}
 
	public CloseableHttpAsyncClient createAsyncClient(boolean needSSL, String username, String password, String proxyHost, int proxyPort) throws IOReactorException {
		boolean needAuth = false;
		if(StringUtils.isNotBlank(username)) {
			needAuth = true;
		}
		
		boolean needProxy = false;
		if(StringUtils.isNotBlank(proxyHost)) {
			needProxy = true;
		}
		
		// Create a registry of custom connection session strategies for supported
		RegistryBuilder<SchemeIOSessionStrategy> registryBuilder = RegistryBuilder.<SchemeIOSessionStrategy> create();
		registryBuilder.register("http", NoopIOSessionStrategy.INSTANCE);
		if(needSSL) {
			SSLContext sslcontext = SSLContexts.createDefault();
			registryBuilder.register("https", new SSLIOSessionStrategy(sslcontext));
		}
		Registry<SchemeIOSessionStrategy> sessionStrategyRegistry = registryBuilder.build();

		// 配置IO Reactor
		IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
				.setIoThreadCount(Runtime.getRuntime().availableProcessors())
				//.setConnectTimeout(30000)
                //.setSoTimeout(30000)
				.build();
		
		ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(ioReactorConfig);
		PoolingNHttpClientConnectionManager connectionManager = new PoolingNHttpClientConnectionManager(
				ioReactor, sessionStrategyRegistry);
		// 设置连接池大小
		if (poolSize > 0) {
			connectionManager.setMaxTotal(poolSize);
		}
        // 每个路由(主机)最大的并发量
		if (maxPerRoute > 0) {
			connectionManager.setDefaultMaxPerRoute(maxPerRoute);
		} else {
			connectionManager.setDefaultMaxPerRoute(10);
		}
		// 设置到某个路由(主机)的最大连接数，会覆盖defaultMaxPerRoute
		// connectionManager.setMaxPerRoute(new HttpRoute(new HttpHost("somehost", 80)), 150);
       
        // 连接相关配置
		ConnectionConfig connectionConfig = ConnectionConfig.custom()
				.setMalformedInputAction(CodingErrorAction.IGNORE)
				.setUnmappableInputAction(CodingErrorAction.IGNORE)
				.setCharset(Consts.UTF_8).build();
		connectionManager.setDefaultConnectionConfig(connectionConfig);
 
		// 请求相关配置
		// setConnectTimeout：设置连接超时时间，单位毫秒
		// setConnectionRequestTimeout：设置从连接池获取Connection的超时时间，单位毫秒。
		// setSocketTimeout：请求获取数据的超时时间(即响应时间)，单位毫秒。
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectTimeout(connectTimeout)
				.setSocketTimeout(socketTimeout)
				.build();
		
		
		HttpAsyncClientBuilder httpAsyncClientBuilder = HttpAsyncClients.custom();
		httpAsyncClientBuilder.setConnectionManager(connectionManager);
		httpAsyncClientBuilder.setDefaultRequestConfig(requestConfig);
		
		if(needAuth) {
			Lookup<AuthSchemeProvider> authSchemeRegistry = RegistryBuilder
					.<AuthSchemeProvider> create()
					.register(AuthSchemes.BASIC, new BasicSchemeFactory())
					.register(AuthSchemes.DIGEST, new DigestSchemeFactory())
					.register(AuthSchemes.NTLM, new NTLMSchemeFactory())
					.register(AuthSchemes.SPNEGO, new SPNegoSchemeFactory())
					.register(AuthSchemes.KERBEROS, new KerberosSchemeFactory())
					.build();
			
			//Http认证
			UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(
					username, password);
	 
			CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			credentialsProvider.setCredentials(AuthScope.ANY, credentials);
			
			httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
			httpAsyncClientBuilder.setDefaultAuthSchemeRegistry(authSchemeRegistry);
		}
		
		if(needProxy) {
			httpAsyncClientBuilder.setProxy(new HttpHost(proxyHost, proxyPort));
		}
		
		return httpAsyncClientBuilder.build();
	}
 
	/**
	 * 获取异步HttpClient
	 * @return
	 */
	public CloseableHttpAsyncClient getAsyncHttpClient() {
		return asyncHttpClient;
	}
 
	public static int getSocketTimeout() {
		return socketTimeout;
	}

	public static void setSocketTimeout(int socketTimeout) {
		HttpAsyncClient.socketTimeout = socketTimeout;
	}

	public static int getConnectTimeout() {
		return connectTimeout;
	}

	public static void setConnectTimeout(int connectTimeout) {
		HttpAsyncClient.connectTimeout = connectTimeout;
	}

	public static int getPoolSize() {
		return poolSize;
	}

	public static void setPoolSize(int poolSize) {
		HttpAsyncClient.poolSize = poolSize;
	}

	public static int getMaxPerRoute() {
		return maxPerRoute;
	}

	public static void setMaxPerRoute(int maxPerRoute) {
		HttpAsyncClient.maxPerRoute = maxPerRoute;
	}
}


