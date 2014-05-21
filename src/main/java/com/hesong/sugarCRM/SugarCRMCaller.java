package com.hesong.sugarCRM;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class SugarCRMCaller {
//    private String SUGAR_CRM_URL = "http://hesong.clouduc.cn/crm/service/v4/rest.php";
    private String SUGAR_CRM_URL = "http://192.168.88.114/crm/service/v4/rest.php";
	
	/**
	 * 调用远程方法
	 * @param method
	 * @param paramMap
	 * @return
	 */
	public String call(String method,String params){
		Map<String,String> paramMap = new HashMap<String,String>(); 
		paramMap.put("method", method);
		paramMap.put("input_type", "JSON");
		paramMap.put("response_type", "JSON");
		paramMap.put("rest_data", params);
		String result = HttpClientUtil.httpPost(SUGAR_CRM_URL, paramMap);
		return result; 
	}
	
	/**
	 * 登录后返回sessionid
	 * @param username
	 * @param password
	 * @return
	 * 	sessionid
	 */
	public String login(String username,String password){
		String pwd = md5_32(password);
		System.out.println("psw:"+pwd);
		String loginParams = "{\"user_auth\":{\"user_name\":\""+username+"\",\"password\":\""+pwd+"\",\"version\":\"1\"},\"application_name\":\"Rest\",\"name_value_list\":[]}";
		String sessionid = null;
		//{\"user_auth\":{\"user_name\":\"admin\",\"password\":\"0f359740bd1cda994f8b55330c86d845\",\"version\":\"1\"},\"application_name\":\"Rest\",\"name_value_list\":[]}
		
		String jsonString = call("login",loginParams);
		if(jsonString!=null && !jsonString.equals("")){
			JSONObject jsonObj =JSONObject.fromObject(jsonString);
			sessionid = jsonObj.getString("id");
		}
		return sessionid;
	}
	
	/**
	 * 插入CRM数据_目标客户
	 * @param
	 * 	json:微信的json用户数据
	 * {
	 *  "subscribe": 1, 
	    "openid": "ogfGduA0yfPY_aET7do8GvE5Bm4w",       =>weixinid_c
	    "nickname": "Bowen", 							=>last_name
	    "sex": 1, 										=>salutation   Mr.   Ms.
	    "language": "zh_CN", 							=>language_c
	    "city": "广州", 									=>primary_address_city
	    "province": "广东", 								=>primary_address_state
	    "country": "中国", 								=>primary_address_country
	    "headimgurl": "http://wx.qlogo.cn/mmopen/WWxicToNQlgxvdF4V3yM5IncQQjXk7pPgkaeglBxcRg1lHwcREca2OdMxhn6biaoT8qDz2mL8ibvQxVnvZfxpicQLPIvtnagxnKA/0",
	     												=>headimgurl_c
	    "subscribe_time": 1398497891                    =>date_entered   日期型
		}
	 * 
	 * @return
	 * 	0-成功
	 *  1-失败
	 *  
	 */
	public String insertToCRM_Prospects(String session,String json){
		JSONObject fromJsonObject = JSONObject.fromObject(json);
		JSONArray attrs = new JSONArray();
		attrs.add(cnvp("weixinid_c",fromJsonObject.getString("openid")));
		attrs.add(cnvp("last_name",fromJsonObject.getString("nickname")));
		attrs.add(cnvp("salutation",(fromJsonObject.getInt("sex") == 1?"Mr.":"Ms.")));
		attrs.add(cnvp("language_c",fromJsonObject.getString("language")));
		attrs.add(cnvp("primary_address_city",fromJsonObject.getString("city")));
		attrs.add(cnvp("primary_address_state",fromJsonObject.getString("province")));
		attrs.add(cnvp("primary_address_country",fromJsonObject.getString("country")));
		attrs.add(cnvp("headimgurl_c",fromJsonObject.getString("headimgurl")));
		
		
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("session", session);
		jsonObject.put("module_name", "Prospects");
		jsonObject.put("name_value_list",attrs.toString());
		
		System.out.println(jsonObject);
		String result = call("set_entry",jsonObject.toString());
		System.out.println(result);
		
		return "0";
	}
	
	public boolean isOpenidBinded(String sessionid,String openid){
        boolean ret = false;
        String params = "{\"session\":\""+sessionid+"\"," +
                "\"module_name\":\"Prospects\",\"query\":\"prospects_cstm.weixinid_c='"+openid+"'\",\"order_by\":\"user_name\",\"offset\":0,\"select_fields\":[\"id\",\"weixinid_c\"],\"max_results\":10,\"deleted\":0,\"favorites\":false}";
        String json = call("get_entry_list",params);
        JSONObject jsonObject = JSONObject.fromObject(json);
        if(jsonObject.getInt("result_count")>0){
            ret = true;
        }
        return ret;
    }
	
	public boolean isKFOpenidBinded(String openid){
        boolean ret = false;
        String params = "{\"openid\":\""+openid+"\"}";
        String json = call("isKFOpenidBinded",params);
        JSONObject jsonObject = JSONObject.fromObject(json);
        if(jsonObject.getBoolean("result")){
            ret = true;
        }
        return ret;
    }

	/**
	 * 构建一个{name:'weixin_c',value:'ogfGduA0yfPY_aET7do8GvE5Bm4w'}结构的json对像
	 * @param fromJsonObject
	 * @param attrFrom
	 * @param attrTo
	 * @return
	 */
	private JSONObject cnvp(String name,String value) {
		JSONObject o = new JSONObject();
		o.put("name",name);
		o.put("value", value);
		return o;
	}
	
	
	

	/**
	 * 标准MD5加密 32位
	 * @author T61P
	 * @param str
	 * @return
	 */
	public static String md5_32(String str){  
	        MessageDigest messageDigest = null;  
	        try {  
	            messageDigest = MessageDigest.getInstance("MD5");  
	            messageDigest.reset();  
	            messageDigest.update(str.getBytes("UTF-8"));  
	        } catch (NoSuchAlgorithmException e) {  
	            System.out.println("NoSuchAlgorithmException caught!");  
	            System.exit(-1);  
	        } catch (UnsupportedEncodingException e) {  
	            e.printStackTrace();  
	        }    
	        byte[] byteArray = messageDigest.digest();  	  
	        StringBuffer md5StrBuff = new StringBuffer();  	  
	        for (int i = 0; i < byteArray.length; i++)  {  
	            if (Integer.toHexString(0xFF & byteArray[i]).length() == 1)  
	                md5StrBuff.append("0").append(Integer.toHexString(0xFF & byteArray[i]));  
	            else  
	                md5StrBuff.append(Integer.toHexString(0xFF & byteArray[i]));  
	        }  
	        return md5StrBuff.toString();  
	}
	

}
