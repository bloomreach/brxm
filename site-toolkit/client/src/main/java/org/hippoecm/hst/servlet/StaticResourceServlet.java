/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.servlet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.InputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.util.ServletConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serves static resource files from the web application context.
 *
 * <P>
 * This servlet has the ability to configure allowed resource path prefix/postfix and forbidden resource path prefix/postfix.
 * This needs some configuration, which is described below.
 * </P>
 * 
 * <H2>Allowed/Forbidden Resource Path Prefix/Postfix Configuration</H2>
 * <pre>
 * &lt;init-param&gt;
 *     &lt;param-name&gt;forbiddenStaticResourcePathPrefixes&lt;/param-name&gt;
 *     &lt;param-value&gt;
 *         /WEB-INF/, /META-INF/
 *     &lt;/param-value&gt;
 * &lt;/init-param&gt;
 * &lt;init-param&gt;
 *     &lt;param-name&gt;forbiddenStaticResourcePathPostfixes&lt;/param-name&gt;
 *     &lt;param-value&gt;
 *         .jsp, .ftl, .vm
 *     &lt;/param-value&gt;
 * &lt;/init-param&gt;
 * &lt;init-param&gt;
 *     &lt;param-name&gt;allowedStaticResourcePathPrefixes&lt;/param-name&gt;
 *     &lt;param-value&gt;
 *         /images/, /javascript/, /css/
 *     &lt;/param-value&gt;
 * &lt;/init-param&gt;
 * &lt;init-param&gt;
 *     &lt;param-name&gt;allowedStaticResourcePathPostfixes&lt;/param-name&gt;
 *     &lt;param-value&gt;
 *          .gif, .jpg, .jpeg, .png, .ico, .js, .css
 *     &lt;/param-value&gt;
 * &lt;/init-param&gt;
 * </pre>
 * 
 * <P>
 * This servlet checks <CODE>forbiddenStaticResourcePathPrefixes</CODE> first.
 * If the requested static resource path starts with any prefix of the parameter values, 
 * then it returns the response with HTTP 403 status code.
 * </P>
 * <P>
 * If it continues, this servlet checks <CODE>forbiddenStaticResourcePathPostfixes</CODE>.
 * If the requested static resource path ends with any postfix of the parameter valuess,
 * then it returns the response with HTTP 403 status code.
 * </P>
 * <P>
 * If the requested static resource path is not forbidden, it allows downloading on the requested
 * static resource path, only when the path starts with any prefix in the <CODE>allowedStaticResourcePathPrefixes</CODE>
 * parameter values or the path ends with any postfix in the <CODE>allowedStaticResourcePathPostfixes</CODE>.
 * </P>
 * <P>
 * <EM>By default, this servlet can be regarded as configured like the following if you don't set any init parameters.</EM>
 * <pre>
 * &lt;init-param&gt;
 *     &lt;param-name&gt;forbiddenStaticResourcePathPrefixes&lt;/param-name&gt;
 *     &lt;param-value&gt;
 *         /WEB-INF/, /META-INF/
 *     &lt;/param-value&gt;
 * &lt;/init-param&gt;
 * &lt;init-param&gt;
 *     &lt;param-name&gt;forbiddenStaticResourcePathPostfixes&lt;/param-name&gt;
 *     &lt;param-value&gt;
 *         .jsp, .ftl, .vm
 *     &lt;/param-value&gt;
 * &lt;/init-param&gt;
 * &lt;init-param&gt;
 *     &lt;param-name&gt;allowedStaticResourcePathPrefixes&lt;/param-name&gt;
 *     &lt;param-value&gt;&lt;/param-value&gt;
 * &lt;/init-param&gt;
 * &lt;init-param&gt;
 *     &lt;param-name&gt;allowedStaticResourcePathPostfixes&lt;/param-name&gt;
 *     &lt;param-value&gt;
 *          .gif, .jpg, .jpeg, .png, .ico, .js, .css
 *     &lt;/param-value&gt;
 * &lt;/init-param&gt;
 * </pre>
 * </P>
 *
 * @version $Id$
 */
public class StaticResourceServlet extends HttpServlet {
    
    public static final String ALLOWED_STATIC_RESOURCE_PATH_PREFIXES_PARAM = "allowedStaticResourcePathPrefixes";
    
    public static final String ALLOWED_STATIC_RESOURCE_PATH_POSTFIXES_PARAM = "allowedStaticResourcePathPostfixes";
    
    public static final String FORBIDDEN_STATIC_RESOURCE_PATH_PREFIXES_PARAM = "forbiddenStaticResourcePathPrefixes";
    
    public static final String FORBIDDEN_STATIC_RESOURCE_PATH_POSTFIXES_PARAM = "forbiddenStaticResourcePathPostfixes";
    
    private static final long serialVersionUID = 1L;
    
    static Logger log = LoggerFactory.getLogger(StaticResourceServlet.class);
    
    private static final int BUF_SIZE = 4096;
    
    private String [] allowedStaticResourcePathPrefixes = new String [0];
    
    private String [] allowedStaticResourcePathPostfixes = new String [] { ".gif", ".jpg", ".jpeg", ".png", ".ico", ".js", ".css" };
    
    private String [] forbiddenStaticResourcePathPrefixes = new String [] { "/WEB-INF/", "/META-INF/" };
    
    private String [] forbiddenStaticResourcePathPostfixes = new String [] { ".jsp", ".vm", ".ftl" };
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        
        String param =  ServletConfigUtils.getInitParameter(config, config.getServletContext(), ALLOWED_STATIC_RESOURCE_PATH_PREFIXES_PARAM, null);
        
        if (!StringUtils.isBlank(param)) {
            allowedStaticResourcePathPrefixes = StringUtils.split(param, ", \t\r\n");
        }
        
        param =  ServletConfigUtils.getInitParameter(config, config.getServletContext(), ALLOWED_STATIC_RESOURCE_PATH_POSTFIXES_PARAM, null);
        
        if (!StringUtils.isBlank(param)) {
            allowedStaticResourcePathPostfixes = StringUtils.split(param, ", \t\r\n");
        }
        
        param =  ServletConfigUtils.getInitParameter(config, config.getServletContext(), FORBIDDEN_STATIC_RESOURCE_PATH_PREFIXES_PARAM, null);
        
        if (!StringUtils.isBlank(param)) {
            forbiddenStaticResourcePathPrefixes = StringUtils.split(param, ", \t\r\n");
        }
        
        param =  ServletConfigUtils.getInitParameter(config, config.getServletContext(), FORBIDDEN_STATIC_RESOURCE_PATH_POSTFIXES_PARAM, null);
        
        if (!StringUtils.isBlank(param)) {
            forbiddenStaticResourcePathPostfixes = StringUtils.split(param, ", \t\r\n");
        }
    }
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        String path = getResourcePath(request);
        
        if (StringUtils.isBlank(path)) {
            if (log.isWarnEnabled()) {
                log.warn("Blank resource path: " + path + ". Response status: 404");
            }
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        if (!isDownloadableStaticResource(path)) {
            if (log.isWarnEnabled()) {
                log.warn("Forbidden resource path: " + path + ". Response status: 403");
            }
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        
        InputStream is = null;
        BufferedInputStream bis = null;
        ServletOutputStream sos = null;
        BufferedOutputStream bos = null;
        
        try {
            
            long lastModified = 0L;
            
            ServletContext context = getServletConfig().getServletContext();
            File file = new File(context.getRealPath(path));
            
            if (file.isFile()) {
                lastModified = file.lastModified();
            }
            
            if (lastModified > 0L) {
                response.setDateHeader("Last-Modified", lastModified);
                long expires = (System.currentTimeMillis() - lastModified);
                response.setDateHeader("Expires", expires + System.currentTimeMillis());
                response.setHeader("Cache-Control", "max-age=" + (expires / 1000));
            }
            
            String mimeType = context.getMimeType(path);
            
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            
            response.setContentType(mimeType);
            
            is = context.getResourceAsStream(path);
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
            if (log.isWarnEnabled()) {
                log.warn("Exception during writing content of {}: {}", path, e.toString());
            }
        } finally {
            if (bos != null) try { bos.close(); } catch (Exception ce) { }
            if (sos != null) try { sos.close(); } catch (Exception ce) { }
            if (bis != null) try { bis.close(); } catch (Exception ce) { }
            if (is != null) try { is.close(); } catch (Exception ce) { }
        }
    }
    
    private String getResourcePath(HttpServletRequest request) {
        String path = null;
        
        // if hstRequest is retrieved, then this servlet has been dispatched by hst component.
        HstRequest hstRequest = HstRequestUtils.getHstRequest(request);

        if (hstRequest != null) {
            path = hstRequest.getResourceID();
        }
        
        if (path == null) {
            path = HstRequestUtils.getPathInfo(request);
        }

        if (path != null && !path.startsWith("/") && path.indexOf(':') > 0) {
            path = path.substring(path.indexOf(':') + 1);
        }
        
        return path;
    }
    
    private boolean isDownloadableStaticResource(String path) {
        for (String prefix : forbiddenStaticResourcePathPrefixes) {
            if (StringUtils.startsWith(path, prefix)) {
                return false;
            }
        }
        for (String postfix : forbiddenStaticResourcePathPostfixes) {
            if (StringUtils.endsWith(path, postfix)) {
                return false;
            }
        }
        for (String prefix : allowedStaticResourcePathPrefixes) {
            if (StringUtils.startsWith(path, prefix)) {
                return true;
            }
        }
        for (String postfix : allowedStaticResourcePathPostfixes) {
            if (StringUtils.endsWith(path, postfix)) {
                return true;
            }
        }
        return false;
    }
}
