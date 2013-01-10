/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet for serving resources on the classpath. Adapted from deprecated Spring 2 (HST) ResourceServlet.
 */
public class ResourceServlet extends HttpServlet {
    
    private static final String LOGGER_CATEGORY_NAME = ResourceServlet.class.getName();
    
    private static final Pattern PROTECTED_PATH = Pattern.compile("/?WEB-INF/.*");
    
    private static final String HTTP_LAST_MODIFIED_HEADER = "Last-Modified";
    
    private static final String HTTP_EXPIRES_HEADER = "Expires";
    
    private static final String HTTP_CACHE_CONTROL_HEADER = "Cache-Control";
    
    private Logger logger = LoggerFactory.getLogger(LOGGER_CATEGORY_NAME);
    
    private Set<Pattern> allowedResourcePaths = new HashSet<Pattern>();
    {
        allowedResourcePaths.add(Pattern.compile("^/.*\\.js"));
        allowedResourcePaths.add(Pattern.compile("^/.*\\.css"));
        allowedResourcePaths.add(Pattern.compile("^/.*\\.png"));
        allowedResourcePaths.add(Pattern.compile("^/.*\\.gif"));
        allowedResourcePaths.add(Pattern.compile("^/.*\\.ico"));
        allowedResourcePaths.add(Pattern.compile("^/.*\\.jpg"));
        allowedResourcePaths.add(Pattern.compile("^/.*\\.jpeg"));
    }
    
    private Map<String, String> defaultMimeTypes = new HashMap<String, String>();
    {
        defaultMimeTypes.put(".css", "text/css");
        defaultMimeTypes.put(".js", "text/javascript");
        defaultMimeTypes.put(".gif", "image/gif");
        defaultMimeTypes.put(".png", "image/png");
        defaultMimeTypes.put(".ico", "image/vnd.microsoft.icon");
        defaultMimeTypes.put(".jpg", "image/jpeg");
        defaultMimeTypes.put(".jpeg", "image/jpeg");
    }
    
    private Set<Pattern> compressedMimeTypes = new HashSet<Pattern>();
    {
        compressedMimeTypes.add(Pattern.compile("text/.*"));
    }

    private int cacheTimeout;
    
    private boolean gzipEnabled;
    
    
    @Override
    public void init() throws ServletException {
        cacheTimeout = Integer.parseInt(getInitParameter("cacheTimeout", "31556926"));
        gzipEnabled = Boolean.parseBoolean(getInitParameter("gzipEnabled", "true"));
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
        String resourcePath = request.getPathInfo();
        
        if (logger.isDebugEnabled()) {
            logger.debug("Processing request for resource " + resourcePath);
        }
        
        URL resource = getResourceURL(resourcePath);
        
        if (resource == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Resource not found: " + resourcePath);
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        long ifModifiedSince = request.getDateHeader("If-Modified-Since");
        
        URLConnection conn = resource.openConnection();
        long lastModified = conn.getLastModified();
        
        if (ifModifiedSince >= lastModified) {
            if (logger.isDebugEnabled()) {
                logger.debug("Resource: " + resourcePath + " Not Modified");
            }
            response.setStatus(304);
            return;
        }
        
        int contentLength = conn.getContentLength();
        
        prepareResponse(response, resource, lastModified, contentLength);
        
        OutputStream out = selectOutputStream(request, response);
        
        try {
            InputStream is = conn.getInputStream();
            try {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            finally {
                is.close();
            }
        }
        finally {
            out.close();
        }
        
    }
    
    private URL getResourceURL(String resourcePath) throws MalformedURLException {
        if (!isAllowed(resourcePath)) {
            if (logger.isWarnEnabled()) {
                logger.warn("An attempt to access a protected resource at " + resourcePath + " was disallowed.");
            }
            return null;
        }
        URL resource = getServletContext().getResource(resourcePath);
        if (resource == null) {
            resource = getJarResource(resourcePath);
        }
        return resource;
    }
    
    private boolean isAllowed(String resourcePath) {
        if (PROTECTED_PATH.matcher(resourcePath).matches()) {
            return false;
        }
        for (Pattern p : allowedResourcePaths) {
            if (p.matcher(resourcePath).matches()) {
                return true;
            }
        }
        return false;
    }
    
    private URL getJarResource(String resourcePath) {
//        String jarResourcePath = jarPathPrefix + resourcePath;
        String jarResourcePath = resourcePath;

        if (jarResourcePath.startsWith("/")) {
            jarResourcePath = jarResourcePath.substring(1);
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("Searching classpath for resource: " + jarResourcePath);
        }
        return getDefaultClassLoader().getResource(jarResourcePath);
    }
    
    private void prepareResponse(HttpServletResponse response, URL resource, long lastModified, int contentLength) throws IOException {

        String mimeType = getServletContext().getMimeType(resource.getPath());
        if (mimeType == null) {
            String extension = resource.getPath().substring(resource.getPath().lastIndexOf('.'));
            mimeType = defaultMimeTypes.get(extension);
            if (mimeType == null) {
                if (logger.isWarnEnabled()) {
                    logger.warn("No mime-type mapping for extension: " + extension);
                }
            }
        }
        response.setDateHeader(HTTP_LAST_MODIFIED_HEADER, lastModified);
        response.setContentLength(contentLength);
        response.setContentType(mimeType);
        if (cacheTimeout > 0) {
            // Http 1.0 header
            response.setDateHeader(HTTP_EXPIRES_HEADER, System.currentTimeMillis() + cacheTimeout * 1000L);
            // Http 1.1 header
            response.setHeader(HTTP_CACHE_CONTROL_HEADER, "max-age=" + cacheTimeout);
        }
    }
    
    private OutputStream selectOutputStream(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (gzipEnabled) {
            String mimeType = response.getContentType();
            if (matchesCompressedMimeTypes(mimeType)) {
                String acceptEncoding = request.getHeader("Accept-Encoding");
                if (acceptEncoding != null && acceptEncoding.indexOf("gzip") != -1) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Enabling GZIP compression for the current response.");
                    }
                    return new GZIPResponseStream(response);
                }
            }
        }
        return response.getOutputStream();
    }
    
    private boolean matchesCompressedMimeTypes(String mimeType) {
        for (Pattern pattern : compressedMimeTypes) {
            if (pattern.matcher(mimeType).matches()) {
                return true;
            }
        }
        return false;
    }
    
    private String getInitParameter(String name, String defaultValue) {
        String value = getServletConfig().getInitParameter(name);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }
    
    private static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        }
        catch (Throwable ex) {
            // Cannot access thread context ClassLoader - falling back to system class loader...
        }
        if (cl == null) {
            // No thread context class loader -> use class loader of this class.
            ResourceServlet.class.getClassLoader();
        }
        return cl;
    }
    
    private class GZIPResponseStream extends ServletOutputStream {

        private ByteArrayOutputStream byteStream = null;

        private GZIPOutputStream gzipStream = null;

        private boolean closed = false;

        private HttpServletResponse response = null;

        private ServletOutputStream servletStream = null;

        public GZIPResponseStream(HttpServletResponse response) throws IOException {
            super();
            closed = false;
            this.response = response;
            this.servletStream = response.getOutputStream();
            byteStream = new ByteArrayOutputStream();
            gzipStream = new GZIPOutputStream(byteStream);
        }

        public void close() throws IOException {
            if (closed) {
                throw new IOException("This output stream has already been closed");
            }
            gzipStream.finish();

            byte[] bytes = byteStream.toByteArray();

            response.setContentLength(bytes.length);
            response.addHeader("Content-Encoding", "gzip");
            servletStream.write(bytes);
            servletStream.flush();
            servletStream.close();
            closed = true;
        }

        public void flush() throws IOException {
            if (closed) {
                throw new IOException("Cannot flush a closed output stream");
            }
            gzipStream.flush();
        }

        public void write(int b) throws IOException {
            if (closed) {
                throw new IOException("Cannot write to a closed output stream");
            }
            gzipStream.write((byte) b);
        }

        public void write(byte b[]) throws IOException {
            write(b, 0, b.length);
        }

        public void write(byte b[], int off, int len) throws IOException {
            if (closed) {
                throw new IOException("Cannot write to a closed output stream");
            }
            gzipStream.write(b, off, len);
        }

    }
}
