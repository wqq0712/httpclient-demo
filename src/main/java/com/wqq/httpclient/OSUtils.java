package com.wqq.httpclient;

public class OSUtils {
	
	public static String assemblyPath(String... strInputs) {
		StringBuilder ret = new StringBuilder();
		String prev = null;
		int i=0;
		for(String strItem : strInputs) {
			if(i==0) {
				ret.append(strItem);
				prev = strItem;
			} else {
				if(prev.startsWith("/")) {
					if(strItem.startsWith("/")) {
						ret.append(strItem.substring(1));
					} else {
						ret.append(strItem);
					}
				} else {
					if(strItem.startsWith("/")) {
						ret.append(strItem);
					} else {
						ret.append("/").append(strItem);
					}
				}
				prev = strItem;	
			}
			i++;
		} 
		return ret.toString();
		
	}
}
