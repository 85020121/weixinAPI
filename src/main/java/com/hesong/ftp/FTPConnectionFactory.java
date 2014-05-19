package com.hesong.ftp;

import java.io.IOException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

/**
 * FTP连接工厂
 * 
 * @author Bowen
 * 
 */
public class FTPConnectionFactory {

    public static String DefaultURL;
    public static String DefualtUsername;
    public static String DefualtPassword;
    public static int DefualtPort = 0;

    /**
     * 默认的FTP连接
     */
    public static FTPClient DefaultFTP;

    /**
     * 初始化默认FTP连接
     * 
     * @param url
     *            FTP服务器地址
     * @param port
     *            端口号
     * @param username
     *            用户名
     * @param password
     *            密码
     * @throws IOException
     *             读写异常
     */
    public static void initDefualtFTPclientConnection(String url, int port,
            String username, String password) throws IOException {
        DefaultURL = url;
        DefualtUsername = username;
        DefualtPassword = password;
        DefualtPort = port;
    }

    /**
     * 获取默认FTP连接
     * 
     * @return Default FTPClient
     * @throws IOException
     */
    public static FTPClient getDefaultFTPConnection() throws IOException {
        try {
            DefaultFTP.getStatus();
            // Default FTP connection is available
            return DefaultFTP;
        } catch (Exception e) {
            // Do nothing
        }
        if (DefualtPort > 0)
            return DefaultFTP = getFTPClientConnection(DefaultURL, DefualtPort,
                    DefualtUsername, DefualtPassword);
        else
            return DefaultFTP = getFTPClientConnection(DefaultURL,
                    DefualtUsername, DefualtPassword);

    }

    /**
     * 建立FTP连接
     * 
     * @param url
     *            服务器地址
     * @param port
     *            端口号
     * @param username
     *            用户名
     * @param password
     *            密码
     * @return FTP连接
     * @throws IOException
     */
    public static FTPClient getFTPClientConnection(String url, int port,
            String username, String password) throws IOException {
        FTPClient ftp = new FTPClient();
        int reply;
        ftp.connect(url, port);// 杩炴帴FTP鏈嶅姟鍣�
        ftp.login(username, password);// 鐧诲綍

        reply = ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftp.disconnect();
            return null; // 杩炴帴寤虹珛澶辫触
        }

        ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
        // ftp.enterLocalPassiveMode();

        return ftp;
    }

    public static FTPClient getFTPClientConnection(String url, String username,
            String password) throws IOException {
        FTPClient ftp = new FTPClient();
        int reply;
        ftp.connect(url);// 杩炴帴FTP鏈嶅姟鍣�
        ftp.login(username, password);// 鐧诲綍

        reply = ftp.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftp.disconnect();
            return null; // 杩炴帴寤虹珛澶辫触
        }

        return ftp;
    }
}
