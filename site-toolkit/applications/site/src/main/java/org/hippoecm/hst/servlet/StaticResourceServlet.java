package org.hippoecm.hst.servlet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.component.HstRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StaticResourceServlet extends HttpServlet {
    
    static Logger log = LoggerFactory.getLogger(StaticResourceServlet.class);
    
    private static final int BUF_SIZE = 4096;
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        String resourceId = null;
        
        if (request instanceof HstRequest) {
            resourceId = ((HstRequest) request).getResourceID();
        }
        
        if (log.isDebugEnabled()) {
            log.debug("resource ID: {}", resourceId);
        }
        
        if (resourceId != null) {
            ServletContext context = getServletConfig().getServletContext();
            
            InputStream is = null;
            BufferedInputStream bis = null;
            ServletOutputStream sos = null;
            BufferedOutputStream bos = null;
            
            try {
                String mimeType = context.getMimeType(resourceId);
                
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }
                
                response.setContentType(mimeType);
                
                is = context.getResourceAsStream(resourceId);
                bis = new BufferedInputStream(is);
                sos = response.getOutputStream();
                bos = new BufferedOutputStream(sos);
                
                byte [] buffer = new byte[BUF_SIZE];
                
                int readLen = bis.read(buffer, 0, BUF_SIZE);
                
                while (readLen != -1) {
                    bos.write(buffer, 0, readLen);
                    readLen = bis.read(buffer, 0, BUF_SIZE);
                }
                
                bos.flush();
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.warn("Exception during writing content: {}", e.getMessage(), e);
                } else if (log.isWarnEnabled()) {
                    log.warn("Exception during writing content: {}", e.getMessage());
                }
            } finally {
                if (bos != null) try { bos.close(); } catch (Exception ce) { }
                if (sos != null) try { sos.close(); } catch (Exception ce) { }
                if (bis != null) try { bis.close(); } catch (Exception ce) { }
                if (is != null) try { is.close(); } catch (Exception ce) { }
            }
        }
    }
}
