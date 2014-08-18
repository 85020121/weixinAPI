package weChatAdapter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import net.coobird.thumbnailator.Thumbnails;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.net.ftp.FTPClient;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import com.hesong.ftp.FTPConnectionFactory;
import com.hesong.sugarCRM.HttpClientUtil;
import com.hesong.sugarCRM.SugarCRMCaller;
import com.hesong.weixinAPI.tools.API;
import com.hesong.weixinAPI.tools.WeChatHttpsUtil;

public class WeChatTest {

    @Test
    public void updateTest(){
        String requestUrl = API.ACCESS_TOKEN_URL.replace("APPID", "wx735e58e85eb3614a").replace(
                "APPSECRET", "d21d943d536c383c9e60053ff15996c2");
        JSONObject jo = WeChatHttpsUtil.httpsRequest(requestUrl, "GET", null);
        System.out.println(jo.toString());
    }
    
    @Test
    public void uploadTest() {
        String media_id = "CxGxPEdGRvGt3ubCgRGmMSCbKKXFiWLn9jOtNRt0Pa4AJhRcrZhtA_8OmWlJaMV9";
        String token = "7h7UN1yUxNSvlqpUtIfAcydtkwquWQPI0mH98VS2SwPiXqi5Y1jhEony1DLIBgdFYsJi7ADasqjhAoMnidt2ww";
        String pull_url = API.PULLING_MEDIA_URL.replace("ACCESS_TOKEN", token)
                + media_id;
        InputStream input = WeChatHttpsUtil.httpGetInputStream(pull_url,
                API.CONTENT_TYPE_VIDEO);
        String post_url = API.UPLOAD_IMAGE_REQUEST_URL.replace("ACCESS_TOKEN",
                token) + "video";
        JSONObject ret = WeChatHttpsUtil.httpPostFile(post_url, input, UUID
                .randomUUID().toString() + ".mp4");
        System.out.println(ret.toString());
    }

    @Test
    public void thumbnailTest() throws IOException {
        File f = new File("D:\\test.JPG");
        System.out.println("Length : " + f.length());
        FTPClient ftp = FTPConnectionFactory.getFTPClientConnection(
                "127.0.0.1", 21, "bowen", "waiwai");
        ftp.changeToParentDirectory();
        float scale = 120000f / f.length();
        OutputStream output = ftp.storeFileStream("/test.jpg");

        try {
            Thumbnails.of(f).scale(0.9).toOutputStream(output);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // if (!ftp.completePendingCommand()) {
        // System.out.println("File transfer failed");
        // }

    }

    @Test
    public void httpTest() {
        Date d1 = new Date();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println(new Date().getTime() - d1.getTime());
    }

    @Test
    public void getQRcodeTest() {
        String token = "YR-95efFKazBBttpfQD5Aiv4gqg7nR4VSfrUA1TwMWCYtMYbQXT2NJ9E2ClGkwF1fOoQK-i3pEEG-mPlG-Zkfw";
        System.out.println(WeChatHttpsUtil.getQRCode(token, 1));
    }

    @Test
    public void recordTest() {
        SimpleDateFormat time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SugarCRMCaller crmCaller = new SugarCRMCaller();
        String sessionid = crmCaller.login("admin", "p@ssw0rd");
        JSONObject session = new JSONObject();
        session.put("session", "adasdasd121");
        session.put("module_name", "chat_ChatHistory");

        JSONObject message = new JSONObject();
        message.put("session_id", "testtttttt123");
        message.put("sender_openid", "test");
        message.put("sender_name", "test");
        message.put("sender_type", "staff");
        message.put("receiver_openid", "test");
        message.put("receiver_name", "test");
        message.put("receiver_type", "client");
        message.put("content", "test");
        message.put("time", time.format(new Date()));
        message.put("message_type", "weixin");

        session.put("name_value_list", message);

        System.out.println(session.toString());
        System.out.println(crmCaller.call("set_entry", session.toString()));
    }

    @Test
    public void sessionTest() {
        SugarCRMCaller sc = new SugarCRMCaller();
        String session = sc.login("admin", "p@ssw0rd");
        sc.check_oauth(session);
        // JSONObject rest = new JSONObject();
        // rest.put("session", "1234sadasd");
        // System.out.println(session);
        // System.out.println(sc.call("oauth_access", rest.toString()));
    }

    @Test
    public void messageTest() {
        // SugarCRMCaller s = new SugarCRMCaller();
        // String session = s.login("admin", "p@ssw0rd");
        // System.out.println(s.getMessageNoticeForWX(session));
        String s = "12345";
        System.out.println(s.substring(0, s.length() - 1));
    }

    @Test
    public void urlTest() {
        String url = "http://www.clouduc.cn/crm/mobile/replymessage/index.php?message_group_id=1&channel=1";
        try {
            System.out.println(URLEncoder.encode(url, "utf8"));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void redisTest() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(500);
        config.setMaxIdle(100);
        config.setMaxWaitMillis(1000);
        JedisPool pool = new JedisPool(config, "localhost",
                6379);
        Jedis jedis = pool.getResource();
        System.out.println(jedis.isConnected());
        String r = HttpClientUtil
                .httpGet("http://www.clouduc.cn/sua/rest/n/tenant/listwxparams");
        System.out.println(r);
        JSONArray ja = JSONArray.fromObject(r);
        for (Object object : ja) {
            JSONObject jo = ((JSONObject)object).getJSONObject("tenantSnsWeixin");
            JSONObject ac = new JSONObject();
            ac.put("appid", jo.getString("appid"));
            ac.put("secretKey", jo.getString("secretKey"));
            ac.put("tenantUn", ((JSONObject)object).getString("tenantUn"));
            jedis.hset("weixin_access_token", jo.getString("publicid"), ac.toString());
        }
        System.out.println(jedis.hgetAll("weixin_access_token"));
        System.out.println(jedis.hexists("weixin_access_token", "gh_be994485fbce"));
        jedis.hdel("weixin_access_token", "gh_be994485fbce");
        jedis.hdel("weixin_access_token", "gh_108cbaea5b15");
        System.out.println(jedis.hexists("weixin_access_token", "gh_be994485fbce"));
        System.out.println(jedis.hgetAll("weixin_access_token"));
        pool.returnBrokenResource(jedis);
        pool.destroy();
    }
    
    @Test
    public void keyWords(){
        String reg = ".*(你好|大|萨).*";
        String reg2 = "\b(STAFF|SERVICE)";
        Pattern p = Pattern.compile(reg);  
        String text = "你213好";
        String text1 = "你2好大哈阿萨德你好啊";
        String text2 = "STAFF";
        String text3 = "sSTAFFF";
        Matcher m = p.matcher(text1);
        if (m.find()) {
            System.out.println(m.group());
            System.out.println(m.group(1));
        }
        
        Pattern p2 = Pattern.compile(reg2);
        System.out.println(p2.matcher(text2).matches());
        m = p2.matcher(text2);
        if (m.find()) {
            System.out.println(m.group());
            System.out.println(m.group(1));
        }
        m = p2.matcher(text3);
        if (m.find()) {
            System.out.println(m.group());
            System.out.println(m.group(1));
        }
    }
    
    @Test
    public void descTest() throws Exception{
        
        String password = "376B4A409E5789CE";
        try{  
            SecureRandom random = new SecureRandom();  
            DESKeySpec desKey = new DESKeySpec(password.getBytes());  
            //创建一个密匙工厂，然后用它把DESKeySpec转换成  
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");  
            SecretKey securekey = keyFactory.generateSecret(desKey);  
            //Cipher对象实际完成加密操作  
            Cipher cipher = Cipher.getInstance("DES");  
            //用密匙初始化Cipher对象  
            cipher.init(Cipher.ENCRYPT_MODE, securekey, random);  
            //现在，获取数据并加密  
            //正式执行加密操作  
            System.out.println(cipher.doFinal("blskyo71ds".getBytes()).toString());  
            System.out.println("blskyo71ds".getBytes());
            }catch(Throwable e){  
                    e.printStackTrace();  
            }  
    }
}
