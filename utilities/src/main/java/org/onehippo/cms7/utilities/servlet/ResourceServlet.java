/*
 *  Copyright 2011-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.utilities.servlet;

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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serves resources from either web application or classpath.
 *
 * <p/>
 * A typical configuration is to set classpath resource path and map a servlet path to this servlet.
 * 
 * <xmp>
 *  <servlet>
 *    <servlet-name>ExampleResourceServlet</servlet-name>
 *    <servlet-class>org.hippoecm.hst.servlet.ResourceServlet</servlet-class>
 *    <init-param>
 *      <param-name>jarPathPrefix</param-name>
 *      <param-value>/META-INF/example/myapp/skin</param-value>
 *    </init-param>
 *  </servlet>
 *  
 *  <servlet-mapping>
 *    <servlet-name>ExampleResourceServlet</servlet-name>
 *    <url-pattern>/myapp/skin/*</url-pattern>
 *  </servlet-mapping>
 * </xmp>
 * 
 * <p/>
 * 
 * With the configuration above, requests by paths, "/myapp/skin/*", will be served by <CODE>ExampleResourceServlet</CODE>,
 * which reads the target resource from the configured classpath resource path, "/META-INF/example/myapp/skin".
 * For example, if the request path info is "/myapp/skin/example.png", then the servlet will find the corresponding
 * classpath resource, "classpath:META-INF/example/myapp/skin/example.png", to serve the request.
 * 
 * <p/>
 * 
 * <p>The following init parameters are available:</p>
 * <table border="2">
 *   <tr>
 *     <th>Init parameter name</th>
 *     <th>Description</th>
 *     <th>Example value</th>
 *     <th>Default value</th>
 *   </tr>
 *   <tr>
 *     <td>jarPathPrefix</td>
 *     <td>Classpath resource path prefix</td>
 *     <td>META-INF/example/myapp/skin</td>
 *     <td>META-INF</td>
 *   </tr>
 *   <tr>
 *     <td>cacheTimeout</td>
 *     <td>
 *       Millisecond value to set cache control HTTP headers: 'Expires' and 'Cache-Control'.
 *       These cache control HTTP headers will be written only if this value is greater than zero.
 *       Otherwise, the cache control HTTP headers are not written.
 *     </td>
 *     <td>0</td>
 *     <td>31556926</td>
 *   </tr>
 *   <tr>
 *     <td>gzipEnabled</td>
 *     <td>Flag to enable/disable gzip encoded response for specified mimeTypes, which can be configured by 'compressedMimeTypes' init parameter.</td>
 *     <td>false</td>
 *     <td>true</td>
 *   </tr>
 *   <tr>
 *     <td>webResourceEnabled</td>
 *     <td>
 *       Flag to enable/disable to read resources from the servlet context on web application resources.
 *       If this is enabled, then the servlet will try to read a resource from the web application first by the request path info.
 *     </td>
 *     <td>false</td>
 *     <td>true</td>
 *   </tr>
 *   <tr>
 *     <td>jarResourceEnabled</td>
 *     <td>
 *       Flag to enable/disable to read resources from the classpath resources.
 *       If this is enabled, then the servlet will try to read a resource from the classpath.
 *     </td>
 *     <td>false</td>
 *     <td>true</td>
 *   </tr>
 *   <tr>
 *     <td>allowedResourcePaths</td>
 *     <td>Sets resource path regex patterns which are allowed to serve by this servlet.</td>
 *     <td><pre>
 * ^/.*\\.js,
 * ^/.*\\.css, 
 * ^/.*\\.png, 
 * ^/.*\\.gif, 
 * ^/.*\\.ico, 
 * ^/.*\\.jpg, 
 * ^/.*\\.jpeg, 
 * ^/.*\\.swf,
 * ^/.*\\.txt
 *     <pre></td>
 *     <td><pre>
 * ^/.*\\.js,
 * ^/.*\\.css, 
 * ^/.*\\.png, 
 * ^/.*\\.gif, 
 * ^/.*\\.ico, 
 * ^/.*\\.jpg, 
 * ^/.*\\.jpeg, 
 * ^/.*\\.eot,
 * ^/.*\\.otf,
 * ^/.*\\.svg,
 * ^/.*\\.swf,
 * ^/.*\\.ttf,
 * ^/.*\\.woff
 *     <pre></td>
 *   </tr>
 *   <tr>
 *     <td>mimeTypes</td>
 *     <td>
 *       Sets mimeType mappings to override (or add) from the default mimeType mappings of the web application.
 *       If a proper mimeType is not found by this mapping, then it will look up a mimeType from the web application.
 *     </td>
 *     <td><pre>
 * .css = text/css,
 * .js = text/javascript,
 * .gif = image/gif,
 * .png = image/png,
 * .ico = image/vnd.microsoft.icon,
 * .jpg = image/jpeg,
 * .jpeg = image/jpeg,
 * .swf = application/x-shockwave-flash,
 * .txt = text/plain
 *     </pre></td>
 *     <td><pre>
 * .css = text/css,
 * .js = text/javascript,
 * .gif = image/gif,
 * .png = image/png,
 * .ico = image/vnd.microsoft.icon,
 * .jpg = image/jpeg,
 * .jpeg = image/jpeg,
 * .eot = application/vnd.ms-fontobject,
 * .otf = application/vnd.ms-opentype,
 * .svg = image/svg+xml,
 * .swf = application/x-shockwave-flash
 * .ttf = application/x-font-ttf,
 * .woff = application/font-woff,
 *     </pre></td>
 *   </tr>
 *   <tr>
 *     <td>compressedMimeTypes</td>
 *     <td>
 *       Sets mimeTypes which can be compressed to serve the resource by this servlet.
 *       If a resource is in this kind of mimeTypes, then the servlet will write a compressed response in gzip encoding.
 *     </td>
 *     <td><pre>
 * text/.*,
 * application/json
 *     </pre></td>
 *     <td><pre>
 * text/.*
 *     </pre></td>
 *   </tr>
 * </table>
 * <p/>
 */
public class ResourceServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(ResourceServlet.class);
    
    private static final Pattern WEBAPP_PROTECTED_PATH = Pattern.compile("/?WEB-INF/.*");
    
    private static final String HTTP_LAST_MODIFIED_HEADER = "Last-Modified";
    
    private static final String HTTP_EXPIRES_HEADER = "Expires";
    
    private static final String HTTP_CACHE_CONTROL_HEADER = "Cache-Control";
    
    private static final Set<Pattern> DEFAULT_ALLOWED_RESOURCE_PATHS = new HashSet<Pattern>();
    static {
        DEFAULT_ALLOWED_RESOURCE_PATHS.add(Pattern.compile("^/.*\\.js"));
        DEFAULT_ALLOWED_RESOURCE_PATHS.add(Pattern.compile("^/.*\\.css"));
        DEFAULT_ALLOWED_RESOURCE_PATHS.add(Pattern.compile("^/.*\\.png"));
        DEFAULT_ALLOWED_RESOURCE_PATHS.add(Pattern.compile("^/.*\\.gif"));
        DEFAULT_ALLOWED_RESOURCE_PATHS.add(Pattern.compile("^/.*\\.ico"));
        DEFAULT_ALLOWED_RESOURCE_PATHS.add(Pattern.compile("^/.*\\.jpg"));
        DEFAULT_ALLOWED_RESOURCE_PATHS.add(Pattern.compile("^/.*\\.jpeg"));
        DEFAULT_ALLOWED_RESOURCE_PATHS.add(Pattern.compile("^/.*\\.json"));
        DEFAULT_ALLOWED_RESOURCE_PATHS.add(Pattern.compile("^/.*\\.eot"));
        DEFAULT_ALLOWED_RESOURCE_PATHS.add(Pattern.compile("^/.*\\.otf"));
        DEFAULT_ALLOWED_RESOURCE_PATHS.add(Pattern.compile("^/.*\\.svg"));
        DEFAULT_ALLOWED_RESOURCE_PATHS.add(Pattern.compile("^/.*\\.swf"));
        DEFAULT_ALLOWED_RESOURCE_PATHS.add(Pattern.compile("^/.*\\.ttf"));
        DEFAULT_ALLOWED_RESOURCE_PATHS.add(Pattern.compile("^/.*\\.woff"));
    }
    
    private static final Map<String, String> DEFAULT_MIME_TYPES = new HashMap<String, String>();
    static {
        DEFAULT_MIME_TYPES.put(".css", "text/css");
        DEFAULT_MIME_TYPES.put(".js", "text/javascript");
        DEFAULT_MIME_TYPES.put(".gif", "image/gif");
        DEFAULT_MIME_TYPES.put(".png", "image/png");
        DEFAULT_MIME_TYPES.put(".ico", "image/vnd.microsoft.icon");
        DEFAULT_MIME_TYPES.put(".jpg", "image/jpeg");
        DEFAULT_MIME_TYPES.put(".jpeg", "image/jpeg");
        DEFAULT_MIME_TYPES.put(".json", "application/json");
        DEFAULT_MIME_TYPES.put(".eot", "application/vnd.ms-fontobject");
        DEFAULT_MIME_TYPES.put(".otf", "application/vnd.ms-opentype");
        DEFAULT_MIME_TYPES.put(".svg", "image/svg+xml");
        DEFAULT_MIME_TYPES.put(".swf", "application/x-shockwave-flash");
        DEFAULT_MIME_TYPES.put(".ttf", "application/x-font-ttf");
        DEFAULT_MIME_TYPES.put(".woff", "application/font-woff");
    }
    
    private static final Set<Pattern> DEFAULT_COMPRESSED_MIME_TYPES = new HashSet<Pattern>();
    static {
        DEFAULT_COMPRESSED_MIME_TYPES.add(Pattern.compile("text/.*"));
    }
    
    private Set<Pattern> allowedResourcePaths;
    
    private Map<String, String> mimeTypes;
    
    private Set<Pattern> compressedMimeTypes;
    
    private String jarPathPrefix;
    
    private int cacheTimeout;
    
    private boolean gzipEnabled;
    
    private boolean webResourceEnabled;
    
    private boolean jarResourceEnabled;
    
    @Override
    public void init() throws ServletException {
        jarPathPrefix = getInitParameter("jarPathPrefix", "META-INF");
        cacheTimeout = Integer.parseInt(getInitParameter("cacheTimeout", "31556926"));
        gzipEnabled = Boolean.parseBoolean(getInitParameter("gzipEnabled", "true"));
        webResourceEnabled = Boolean.parseBoolean(getInitParameter("webResourceEnabled", "true"));
        jarResourceEnabled = Boolean.parseBoolean(getInitParameter("jarResourceEnabled", "true"));
        
        allowedResourcePaths = new HashSet<Pattern>(DEFAULT_ALLOWED_RESOURCE_PATHS);
        mimeTypes = new HashMap<String, String>(DEFAULT_MIME_TYPES);
        compressedMimeTypes = new HashSet<Pattern>(DEFAULT_COMPRESSED_MIME_TYPES);
        
        String param = getInitParameter("allowedResourcePaths", null);
        if (!StringUtils.isBlank(param)) {
            allowedResourcePaths = new HashSet<Pattern>();
            String [] patterns = StringUtils.split(param, ", \t\r\n");
            for (int i = 0; i < patterns.length; i++) {
                if (!StringUtils.isBlank(patterns[i])) {
                    allowedResourcePaths.add(Pattern.compile(patterns[i]));
                }
            }
        }
        
        param = getInitParameter("mimeTypes", null);
        if (!StringUtils.isBlank(param)) {
            mimeTypes = new HashMap<String, String>();
            String [] pairs = StringUtils.split(param, ",\t\r\n");
            for (int i = 0; i < pairs.length; i++) {
                if (!StringUtils.isBlank(pairs[i])) {
                    String [] pair = StringUtils.split(pairs[i], "=");
                    if (pair.length > 1) {
                        mimeTypes.put(StringUtils.trim(pair[0]), StringUtils.trim(pair[1]));
                    }
                }
            }
        }
        
        param = getInitParameter("compressedMimeTypes", null);
        if (!StringUtils.isBlank(param)) {
            compressedMimeTypes = new HashSet<Pattern>();
            String [] patterns = StringUtils.split(param, ", \t\r\n");
            for (int i = 0; i < patterns.length; i++) {
                if (!StringUtils.isBlank(patterns[i])) {
                    compressedMimeTypes.add(Pattern.compile(patterns[i]));
                }
            }
        }
        
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
        String resourcePath = StringUtils.substringBefore(request.getPathInfo(), ";");
        
        if (log.isDebugEnabled()) {
            log.debug("Processing request for resource {}.", resourcePath);
        }
        
        URL resource = getResourceURL(resourcePath);
        
        if (resource == null) {
            if (log.isDebugEnabled()) {
                log.debug("Resource not found: {}", resourcePath);
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        long ifModifiedSince = request.getDateHeader("If-Modified-Since");
        
        URLConnection conn = resource.openConnection();
        long lastModified = conn.getLastModified();
        
        if (ifModifiedSince >= lastModified) {
            if (log.isDebugEnabled()) {
                log.debug("Resource: {} Not Modified.", resourcePath);
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
                int bytesRead = -1;
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
            if (log.isWarnEnabled()) {
                log.warn("An attempt to access a protected resource at {} was disallowed.", resourcePath);
            }
            
            return null;
        }
        
        URL resource = null;
        
        if (webResourceEnabled) {
            resource = getServletContext().getResource(resourcePath);
        }
        
        if (resource == null) {
            if (jarResourceEnabled) {
                resource = getJarResource(resourcePath);
            }
        }
        
        return resource;
    }
    
    private boolean isAllowed(String resourcePath) {
        if (webResourceEnabled && WEBAPP_PROTECTED_PATH.matcher(resourcePath).matches()) {
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
        String jarResourcePath = jarPathPrefix + resourcePath;

        if (jarResourcePath.startsWith("/")) {
            jarResourcePath = jarResourcePath.substring(1);
        }
        
        if (log.isDebugEnabled()) {
            log.debug("Searching classpath for resource: {}", jarResourcePath);
        }
        
        return getDefaultClassLoader().getResource(jarResourcePath);
    }
    
    private void prepareResponse(HttpServletResponse response, URL resource, long lastModified, int contentLength) throws IOException {
        String resourcePath = resource.getPath();
        String mimeType = null;
        
        int offset = resourcePath.lastIndexOf('.');
        if (-1 != offset) {
            String extension = resource.getPath().substring(offset);
            mimeType = mimeTypes.get(extension);
        }
        
        if (mimeType == null) {
            mimeType = getServletContext().getMimeType(resourcePath);
            if (log.isDebugEnabled()) {
                log.debug("Fallback to the mimeType '{}' from servlet context for {}.", mimeType, resourcePath);
            }
        }
        
        if (mimeType == null) {
            if (log.isWarnEnabled()) {
                log.warn("No mime-type mapping for resource path: {}. Fallback to 'application/octet-stream'.", resourcePath);
            }
            mimeType = "application/octet-stream";
        }
        
        response.setDateHeader(HTTP_LAST_MODIFIED_HEADER, lastModified);
        response.setContentLength(contentLength);
        response.setContentType(mimeType);
        
        if (cacheTimeout > 0) {
            // Http 1.0 header
            response.setDateHeader(HTTP_EXPIRES_HEADER, System.currentTimeMillis() + cacheTimeout * 1000L);
            // Http 1.1 header
            response.setHeader(HTTP_CACHE_CONTROL_HEADER, "max-age=" + cacheTimeout);
        } else {
            // Http 1.0 header
            response.setDateHeader(HTTP_EXPIRES_HEADER, -1);
            // Http 1.1 header
            response.setHeader(HTTP_CACHE_CONTROL_HEADER, "no-cache");
        }
    }
    
    private OutputStream selectOutputStream(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (gzipEnabled) {
            String mimeType = response.getContentType();
            if (matchesCompressedMimeTypes(mimeType)) {
                String acceptEncoding = request.getHeader("Accept-Encoding");
                if (acceptEncoding != null && acceptEncoding.indexOf("gzip") != -1) {
                    if (log.isDebugEnabled()) {
                        log.debug("Enabling GZIP compression for the current response.");
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
    
    private ClassLoader getDefaultClassLoader() {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        }
        catch (Throwable ex) {
            // Cannot access thread context ClassLoader - falling back to system class loader...
        }
        if (cl == null) {
            // No thread context class loader -> use class loader of this class.
            cl = getClass().getClassLoader();
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
