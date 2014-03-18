package com.hesong.ftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AttachmentPuller {
    
    /**
     * 文件拷贝，在用户进行附件下载的时候，可以把附件的InputStream传给用户进行下载
     * 
     * @param is
     * @param os
     * @throws IOException
     */
    public static void copy(InputStream is, OutputStream os) throws IOException {
        byte[] bytes = new byte[1024];
        int len = 0;
        while ((len = is.read(bytes)) != -1) {
            os.write(bytes, 0, len);
        }
        if (os != null)
            os.close();
        if (is != null)
            is.close();
    }

}
