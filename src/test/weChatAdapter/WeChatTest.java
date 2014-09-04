package weChatAdapter;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import net.sf.json.JSONObject;

import org.apache.commons.net.ftp.FTPClient;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import com.hesong.ftp.AttachmentPuller;
import com.hesong.ftp.FTPConnectionFactory;
import com.hesong.ftp.FTPEngine;
import com.hesong.jsonrpc.JsonrpcHandler;
import com.hesong.jsonrpc.WeChatMethodSet;
import com.hesong.weChatAdapter.context.AppContext;
import com.hesong.weChatAdapter.context.ContextPreloader;
import com.hesong.weChatAdapter.manager.MessageManager;
import com.hesong.weChatAdapter.tools.API;
import com.hesong.weChatAdapter.tools.SignatureChecker;
import com.hesong.weChatAdapter.tools.WeChatHttpsUtil;
import com.hesong.weChatAdapter.tools.WeChatXMLParser;

public class WeChatTest {

    @Test
    public void sha1test() {
        System.out.println(SignatureChecker.SHA1("woshishei"));
        assertTrue(SignatureChecker.SHA1("woshishei").equals(
                "5c01324982957013fa166fa246fef976a9102279"));
    }

    @Test
    public void getTokenTest() {
        System.out.println(WeChatHttpsUtil.getAccessToken("wx735e58e85eb3614a",
                "d21d943d536c383c9e60053ff15996c2"));

    }


    @Test
    public void sendMsgTest() {
        JsonrpcHandler handler = new JsonrpcHandler(new WeChatMethodSet());
        String msg = "{\"jsonrpc\": \"2.0\", \"method\": \"SendImMessage\", \"params\": [null, \"sender01\",\"oJr3Ht02_3HFoKDl0WwkdBlLvc3o\", \"room1\", { \"msgtype\":\"text\", \"text\": { \"content\":\"Hello'\\\" *&^%$#你\\n好啊[]{}World\" } } ], \"id\": 3}";
        System.out.println(handler.handle(msg));
    }

    @Test
    public void manageMenuTest() {
//        JsonrpcHandler handler = new JsonrpcHandler(new WeChatMethodSet());
//        String get = "{\"jsonrpc\": \"2.0\", \"method\": \"ManageMenu\", \"params\": [\"account\",\"get\", null], \"id\": 2}";
//        String create = "{\"jsonrpc\": \"2.0\", \"method\": \"ManageMenu\", \"params\": [\"account\",\"create\",{ \"button\":[ { \"type\":\"click\", \"name\":\"人工服务\", \"key\":\"RG_001\" }, { \"type\":\"click\", \"name\":\"歌手简介\", \"key\":\"V1001_TODAY_SINGER\" }, { \"name\":\"菜单\", \"sub_button\":[ { \"type\":\"view\", \"name\":\"搜索\", \"url\":\"http://www.soso.com/\" }, { \"type\":\"view\", \"name\":\"视频\", \"url\":\"http://v.qq.com/\" }, { \"type\":\"click\", \"name\":\"赞一下我们\", \"key\":\"V1001_GOOD\" }] }] } ], \"id\": 2}";
//        String delete = "{\"jsonrpc\": \"2.0\", \"method\": \"ManageMenu\", \"params\": [\"account\",\"delete\", null], \"id\": 2}";
        String token = "hun4a7id0VYP1aEKskh8nOLTfQEQcsgVbUMoCDDGJ5OMUwEKz-j2Ltz68iQ21afg";
        //        System.out.println(handler.handle(create));
//        System.out.println(handler.handle(get));
        String create = "{ \"button\":[ { \"type\":\"click\", \"name\":\"人工服务\", \"key\":\"RG_001\" }, { \"type\":\"click\", \"name\":\"歌手简介\", \"key\":\"V1001_TODAY_SINGER\" }, { \"name\":\"菜单\", \"sub_button\":[ { \"type\":\"view\", \"name\":\"搜索\", \"url\":\"http://www.soso.com/\" }, { \"type\":\"view\", \"name\":\"视频\", \"url\":\"http://v.qq.com/\" }, { \"type\":\"click\", \"name\":\"赞一下我们\", \"key\":\"V1001_GOOD\" }] }] }";

        System.out.println(MessageManager.manageMenu(token, "create", create));
    }
    
    @Test
    public void getClientInfoTest(){    
        JsonrpcHandler handler = new JsonrpcHandler(new WeChatMethodSet());
        String jsonrpc = "{\"jsonrpc\": \"2.0\", \"method\": \"GetClient\", \"params\": [\"abcdef\",[\"oJr3Ht02_3HFoKDl0WwkdBlLvc3o\", \"oJr3Ht294GRv1J0fJYvGlF6kfNSo\",\"123asd\"] ], \"id\": 3}";
        handler.handle(jsonrpc);
    }
    
    @Test
    public void test(){
        JSONObject jo = new JSONObject();
        jo.put("nihao", "nihao");
        String jsonrpc = "{\"jsonrpc\": \"2.0\", \"method\": \"Invited\", \"params\": [null,\"fromuser123\",\"room0\", \"1234\",\"comeon in\",null,12000,null], \"id\": 3}";
        JsonrpcHandler handler = new JsonrpcHandler(new WeChatMethodSet());
        String j = handler.handle(jsonrpc);
        System.out.println(j);
    }
    
    @Test
    public void reqTest(){
        JSONObject jo = new JSONObject();
        jo.put("content", "wexin test");
        WeChatHttpsUtil.httpPostRequest("http://localhost:8080/weChatAdapter/chat/sendMessageQuest", jo.toString(), 2000);
    }

//    @Test
//    public void getFollowersTest() {
//        MessageManager.getFollowersList(API.ACCESS_TOKEN);
//    }
//
//    @Test
//    public void getFollowersFromTest() {
//        MessageManager.getFollowersFrom(API.ACCESS_TOKEN,
//                "ogfGduNuSPi5TyIcYOyMzvlnRF9c");
//    }
    
    @Test
    public void ftpTest() throws Exception{
        FTPClient ftp = FTPConnectionFactory.getFTPClientConnection("10.4.62.41", 21, "Administrator",
                "Ky6241");
//        FTPClient ftp = FTPConnectionFactory.getFTPClientConnection("127.0.0.1", 21, "bowen",
//                "waiwai");
        String request = "http://file.api.weixin.qq.com/cgi-bin/media/get?access_token=lv3s0iunvVvEj9K3bz12xlcp5BD-J197i9ewxR0Y8jkDBSIEadTyZuBP1uDl5rl98XNCmrfGF8vCv5dTwDiv5-KRerMNM8rYR8L7atU4uyo70NE130TSV80C_ArGD_YS-G57nlqKrhdL7DhDvLVAhw&media_id=tTKFC0uxe8WMLJekyZWBBpyppnLjb8R1wg_Mn6q0GwA9bVveYNpQpgZ0nxh2lyMG";
        InputStream in = WeChatHttpsUtil.httpGetInputStream(request, API.CONTENT_TYPE_VOICE);
        String filename = MessageManager.sdf_time.format(new Date())+".amr";
        
        String dir = MessageManager.getDirName("image", "bowen");
        FTPEngine.uploadFile(ftp, dir, filename, in);
    }
    
    @Test
    public void postTest(){
        String request = API.UPLOAD_IMAGE_REQUEST_URL.replace("ACCESS_TOKEN", "l-m8pxzdEFcZzhgAanr7MHuhKHwcYeNAdv3k590h38_FzXFhK2jfYKQHDXxLeza8");
        InputStream in = WeChatHttpsUtil.httpGetInputStream("http://10.4.62.41:8370/image/send/10013/2014-03-24/16-36-55_9450853.jpg", "image");
        JSONObject jo = WeChatHttpsUtil.httpPostFile(request, in);
        System.out.println("Res:"+jo.toString());
        
    }
    
//    @Test
//    public void thumbnailTest() throws IOException{
//        File f = new File("D:\\test.JPG");
//        System.out.println("Length : "+f.length());
//        FTPClient ftp = FTPConnectionFactory.getFTPClientConnection("127.0.0.1", 21, "bowen",
//              "waiwai");
//        ftp.changeToParentDirectory();
//        float scale = 120000f/f.length();
//        OutputStream output = ftp.storeFileStream("/test.jpg");
//
//        try {
//            Thumbnails.of(f).scale(0.9).toOutputStream(output);
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
////        if (!ftp.completePendingCommand()) {
////            System.out.println("File transfer failed");
////        }
//
//    }
    
    @Test
    public void random() throws DocumentException{
        File f = new File("D:\\setting.xml");
        System.out.println(f.exists());
        SAXReader reader = new SAXReader();
        Document document = reader.read(f);
        Element rootElmt = document.getRootElement();
        
        Element smartbusElmt = rootElmt.element("smartbus");
        Iterator iter = smartbusElmt.elementIterator();
        while (iter.hasNext()) {
            Element elmt = (Element)iter.next();
            System.out.println(elmt.toString());
            System.out.println(elmt.elementText("destclientid"));
        }
    }
    
    @Test
    public void time(){
        long d = new Date().getTime();
        System.out.println(d);
        System.out.println(Long.toString(d));
        System.out.println(String.valueOf(d));
    }
    
    @Test
    public void sha1(){
        System.out.println(SignatureChecker.SHA1("1001"+"abcdef"+"1409764132451"));
    }
}
