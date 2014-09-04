package com.hesong.weChatAdapter.tools;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;

public class SignatureChecker {

    public static boolean checkSignature(String signature, String timestamp,
            String nonce, String token) {

        String[] checkInfos = new String[] { token, timestamp, nonce };
        Arrays.sort(checkInfos);
        return signature.equals(SHA1(StringUtils.join(checkInfos)));
    }
    
    public static boolean checkAPISignature(String signature, String timestamp,
            String appid, String appsecret) {

        String checkInfos = appid + appsecret + timestamp;
        return signature.equals(SHA1(checkInfos));
    }
    
    public static String createAPPSignature(String appid, String accesstoken, String timestamp) {
        return SHA1(appid + accesstoken + timestamp);
    }

    public static String SHA1(String inStr) {
        MessageDigest md = null;
        String outStr = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(inStr.getBytes());
            outStr = byteToStr(digest);
        } catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
        }
        return outStr;
    }

    private static String byteToHexStr(byte ib) {
        char[] Digit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a',
                'b', 'c', 'd', 'e', 'f' };
        char[] ob = new char[2];
        ob[0] = Digit[(ib >>> 4) & 0X0F];
        ob[1] = Digit[ib & 0X0F];

        return new String(ob);
    }

    private static String byteToStr(byte[] bytearray) {
        String strDigest = "";
        for (int i = 0; i < bytearray.length; i++) {
            strDigest += byteToHexStr(bytearray[i]);
        }
        return strDigest;
    }
}
