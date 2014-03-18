package com.hesong.ftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.log4j.Logger;



/**
 * FTP事件引擎
 * @author Bowen
 *
 */
public class FTPEngine {

   public static Logger FTPLogger = Logger.getLogger(FTPEngine.class); 

    
    /**
     * 断开FTP连接
     * @param ftp FTPClient对象
     * @throws IOException 
     */
    public static void disconnect(FTPClient ftp) throws IOException {
        ftp.logout();
        ftp.disconnect();
    }

    public static boolean mkdir(FTPClient ftp, String path) throws IOException {
        if (!ftp.changeWorkingDirectory(path)) {
            if (FTPReply.isPositiveCompletion(ftp.mkd(path))) {
                ftp.changeWorkingDirectory(path);
            } else {
                FTPLogger.info("Make dir failed with path: " + path);
                return false;
            }
        }

        FTPLogger.info("Destination directory: "
                + ftp.printWorkingDirectory());
        return true;
    }

    /**
     * 
     * @param ftp
     *            FTP链接
     * @param path
     *            目标文件夹路径
     * @param filename
     *            待上传文件名
     * @param input
     *            输入流
     * @return True：上传成功， False：失败
     * @throws IOException
     */
    public static boolean uploadFile(FTPClient ftp, String path,
            String filename, InputStream input) throws IOException {
        if (!mkdir(ftp, path)) {
            return false;
        }
        
        // FTP协议里面，规定文件名编码为iso-8859-1，所以目录名或文件名需要转码
        String isoFileName = new String(filename.getBytes("UTF-8"),"ISO-8859-1");
        OutputStream output = ftp.storeFileStream(isoFileName);
        AttachmentPuller.copy(input, output);

        if (!ftp.completePendingCommand()) {
            FTPLogger.info("File transfer failed.");
            // TO DO
            // ftp.logout();
            // ftp.disconnect();
            // System.exit(1);
            return false;
        }
        return true;
    }

    public static boolean uploadFile(String url, int port, String username,
            String password, String path, String filename, InputStream input)
            throws IOException {
        FTPClient ftp = FTPConnectionFactory.getFTPClientConnection(url, port,
                username, password);
        if (ftp == null)
            return false;
        boolean success = uploadFile(ftp, path, filename, input);
        disconnect(ftp);
        return success;
    }
    

}
