package com.hesong.weChatAdapter.tools;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

/**
 * http client工具方法
 * @author T420
 *
 */
public class HttpClientUtil {

	/**
	 * 调用http get
	 * @param url
	 * @return
	 */
	public static String httpGet(String url){
		 BufferedReader in = null;  
		  
	        String content = null;  
	        try {  
	            // 定义HttpClient  
	            HttpClient client = new DefaultHttpClient();  
	            // 实例化HTTP方法  
	            HttpGet request = new HttpGet();  
	            request.setURI(new URI(url));  
	            HttpResponse response = client.execute(request);  
		        HttpEntity resEntity = response.getEntity();
		        InputStream is = resEntity.getContent();
	           content = valueOf(is, "UTF-8");
	           
	        }catch(Exception ex){
	        	ex.printStackTrace();
	        } finally {  
	            if (in != null) {  
	                try {  
	                    in.close();// 最后要关闭BufferedReader  
	                } catch (Exception e) {  
	                    e.printStackTrace();  
	                }  
	            }  
	        }  
	        return content;  
	}
	
	/**
	 * 调用http post
	 * @param url
	 * @param params
	 * @return
	 */
	public static String httpPost(String url,Map<String,String> params){
		  // Create a new HttpClient and Post Header
		String result = null;
	    HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost(url);

	    try {
	        // Add your data
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			for (String key : params.keySet()) {
				String value = params.get(key);
				nameValuePairs.add(new BasicNameValuePair(key, value)); 
			}
	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs,"UTF-8"));
	        // Execute HTTP Post Request
	        HttpResponse response = httpclient.execute(httppost);
	        HttpEntity resEntity = response.getEntity();
	        InputStream is = resEntity.getContent();
	        result = valueOf(is, "UTF-8");
	        
	    } catch (Exception e) {
	      e.printStackTrace();
	    } 
	    return result;
	}
	

	/**
	 * 从InputStream中获取字符串对象
	 * @param is
	 * @param charset
	 * @return
	 */
	public static String valueOf(InputStream is,String charset){
		String result = null;
		if(is != null){
			try{
				byte[] buffer = new byte[1024];
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				int l = is.read(buffer);
				while(l!=-1){
					baos.write(buffer, 0, l);
					l = is.read(buffer);
				}
				is.close();
				result = new String(baos.toByteArray(),charset);
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
		return result;
	}
	
	
}
