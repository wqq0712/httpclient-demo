package com.wqq.httpclient;

import java.util.UUID;

public class UUIDMain {
	public static void main(String[] args) {
		System.out.println(UUID.randomUUID().toString().replaceAll("-", ""));
		String property = "java.io.tmpdir";

	    String tempDir = System.getProperty(property);
	    System.out.println("OS current temporary directory is " + tempDir);
	}
}
