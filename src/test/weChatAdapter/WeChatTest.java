package weChatAdapter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import net.coobird.thumbnailator.Thumbnails;
import net.sf.json.JSONObject;

import org.apache.commons.net.ftp.FTPClient;
import org.junit.Test;

import com.hesong.ftp.FTPConnectionFactory;
import com.hesong.sugarCRM.SugarCRMCaller;
import com.hesong.weixinAPI.tools.API;
import com.hesong.weixinAPI.tools.WeChatHttpsUtil;

public class WeChatTest {
    
    @Test
    public void uploadTest(){
        String media_id = "t1mrG5Q4Vf7znli5ZsU87lWc5N129sVNZDOi9MO9axlHSsYIRMv2UFejSMa_s7l-";
        String token = "nDjWI161_CPLPplbmngjScd8rmC3XjKRiaIlVWtMk1bYOyHosSDZ4Xwxa0MkHY4c";
        String pull_url = API.PULLING_MEDIA_URL.replace("ACCESS_TOKEN", token) + media_id;
        InputStream input = WeChatHttpsUtil.httpGetInputStream(pull_url, API.CONTENT_TYPE_VOICE);
        String post_url = API.UPLOAD_IMAGE_REQUEST_URL.replace("ACCESS_TOKEN", token) + "voice";
        JSONObject ret = WeChatHttpsUtil.httpPostFile(post_url, input, UUID.randomUUID().toString()+".amr");
        System.out.println(ret.toString());
    }

    @Test
    public void thumbnailTest() throws IOException{
        File f = new File("D:\\test.JPG");
        System.out.println("Length : "+f.length());
        FTPClient ftp = FTPConnectionFactory.getFTPClientConnection("127.0.0.1", 21, "bowen",
              "waiwai");
        ftp.changeToParentDirectory();
        float scale = 120000f/f.length();
        OutputStream output = ftp.storeFileStream("/test.jpg");

        try {
            Thumbnails.of(f).scale(0.9).toOutputStream(output);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
//        if (!ftp.completePendingCommand()) {
//            System.out.println("File transfer failed");
//        }

    }
    
    @Test
    public void httpTest(){
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
    public void getQRcodeTest(){
        String token = "YR-95efFKazBBttpfQD5Aiv4gqg7nR4VSfrUA1TwMWCYtMYbQXT2NJ9E2ClGkwF1fOoQK-i3pEEG-mPlG-Zkfw";
        System.out.println(WeChatHttpsUtil.getQRCode(token, 1));
    }
    
    @Test
    public void recordTest(){
        SimpleDateFormat time = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss");
        SugarCRMCaller crmCaller = new SugarCRMCaller();
        String sessionid = crmCaller.login("admin",
                "p@ssw0rd");
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
    public void sessionTest(){
        SugarCRMCaller sc = new SugarCRMCaller();
        String session = sc.login("admin",
                "p@ssw0rd");
        JSONObject rest = new JSONObject();
        rest.put("session", "1234sadasd");
        System.out.println(session);
        System.out.println(sc.call("oauth_access", rest.toString()));
    }
}
