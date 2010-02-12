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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.james.mime4j.codec.EncoderUtil;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.linking.LocationResolver;
import org.hippoecm.hst.core.linking.ResourceContainer;
import org.hippoecm.hst.core.linking.ResourceLocationResolver;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serves binary files from the repository. Binary files are represented by nodes.
 *
 * <p/>This servlet has the ability to set the "Content-Disposition" header in the HTTP response to tell browsers to
 * download a binary file instead of trying to display the file directly. This needs some configuration, which is
 * described below.
 *
 * <h2>Content disposition configuration</h2>
 * To configure which mime types to enable content dispositioning for, set the "contentDispositionContentTypes" init
 * param in the web.xml in which this servlet is defined. Example:
 * <pre>
 * &lt;init-param&gt;
 *     &lt;param-name&gt;contentDispositionContentTypes&lt;/param-name&gt;
 *     &lt;param-value&gt;
 *         application/pdf,
 *         application/rtf,
 *         application/excel
 *     &lt;/param-value&gt;
 * &lt;/init-param&gt;
 * </pre>
 * 
 * In the above init param configuration, you can also set glob style configurations such as '*&#x2F;*' or 'application&#x2F;*'.
 *
 * Also, you can configure the JCR property to get the file name from. The file name is used to send along in the
 * HTTP response for content dispositioning. To configure this, set the "contentDispositionFilenameProperty" init
 * param in the web.xml in which this servlet is defined. Example:
 *
 * <pre>
 * &lt;init-param&gt;
 *     &lt;param-name&gt;contentDispositionFilenameProperty&lt;/param-name&gt;
 *     &lt;param-value&gt;demosite:filename&lt;/param-value&gt;
 * &lt;/init-param&gt;
 * </pre>
 * 
 * You can also configure multiple JCR property names in the above init parameter by comma-separated value. 
 * 
 *
 * @author Tom van Zummeren
 * @version $Id$
 */
public class BinariesServlet extends HttpServlet {

    private static Logger log = LoggerFactory.getLogger(BinariesServlet.class);

    private static final String BASE_BINARIES_CONTENT_PATH_INIT_PARAM = "baseBinariesContentPath";

    private static final String CONTENT_DISPOSITION_CONTENT_TYPES_INIT_PARAM = "contentDispositionContentTypes";

    private static final String CONTENT_DISPOSITION_FILENAME_PROPERTY_INIT_PARAM = "contentDispositionFilenameProperty";

    private static final String DEFAULT_BASE_BINARIES_CONTENT_PATH = "";

    private static final long serialVersionUID = 1L;

    private Repository repository;
    
    private Credentials binariesCredentials;
    
    protected String baseBinariesContentPath = DEFAULT_BASE_BINARIES_CONTENT_PATH;

    protected Set<String> contentDispositionContentTypes;

    protected String [] contentDispositionFilenamePropertyNames;
    
    protected Map<String, List<ResourceContainer>> prefix2ResourceContainer;
    
    protected List<ResourceContainer> allResourceContainers;
    
    private boolean initialized = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        
        String param = config.getInitParameter(BASE_BINARIES_CONTENT_PATH_INIT_PARAM);
        
        if (param != null) {
            this.baseBinariesContentPath = param;
        }
        
        contentDispositionFilenamePropertyNames = StringUtils.split(config.getInitParameter(CONTENT_DISPOSITION_FILENAME_PROPERTY_INIT_PARAM), ", \t\r\n");

        // Parse mime types from init-param
        contentDispositionContentTypes = new HashSet<String>();
        String mimeTypesString = config.getInitParameter(CONTENT_DISPOSITION_CONTENT_TYPES_INIT_PARAM);
        if (mimeTypesString != null) {
            contentDispositionContentTypes.addAll(Arrays.asList(StringUtils.split(mimeTypesString, ", \t\r\n")));
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String relPath = getResourceRelPath(request);
        String resourcePath = null;
        Session session = null;
        
        if(!initialized) {
            synchronized(this) {
                doInit();
            }
        }
        
        try {
            String baseContentPath = this.baseBinariesContentPath;
            StringBuilder resourcePathBuilder = new StringBuilder(80);
            
            if (baseContentPath != null && !"".equals(baseContentPath)) {
                resourcePathBuilder.append('/').append(PathUtils.normalizePath(baseContentPath));
            }
            
            if (relPath != null) {
                resourcePathBuilder.append('/').append(PathUtils.normalizePath(relPath));
            }
            
            resourcePath = resourcePathBuilder.toString();
            
            if(resourcePath == null || !resourcePath.startsWith("/")) {
                if (log.isWarnEnabled()) {
                    log.warn("item at path " + resourcePath + " not found, response status = 404)");
                }
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            session = getSession(request);
            
            Node resourceNode = lookUpResource(session, resourcePath);
            
            if(resourceNode == null) {
                log.warn("item at path " + resourcePath + " cannot be found.");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            if(!resourceNode.isNodeType(HippoNodeType.NT_RESOURCE)) {
                response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                log.warn("Found node is not of type '{}' but was of type '{}'. Return HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE", HippoNodeType.NT_RESOURCE, resourceNode.getPrimaryNodeType().getName());
                return;
            }
            
            if (!resourceNode.hasProperty("jcr:mimeType")) {
                if (log.isWarnEnabled()) {
                    log.warn("item at path " + resourcePath + " has no property jcr:mimeType, response status = 415)");
                }
                
                response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                return;
            }

            String mimeType = resourceNode.getProperty("jcr:mimeType").getString();

            if (!resourceNode.hasProperty("jcr:data")) {
                if (log.isWarnEnabled()) {
                    log.warn("item at path " + resourcePath + " has no property jcr:data, response status = 404)");
                }
                
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            InputStream istream = null;
            OutputStream ostream = null;
            try {
                Property data = resourceNode.getProperty("jcr:data");
                istream = data.getStream();
    
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType(mimeType);
    
                // Add the Content-Disposition header for configured content types
                addContentDispositionHeader(request, response, mimeType, resourceNode);
    
                // TODO add a configurable factor + default minimum for expires. Ideally, this value is
                // stored in the repository
                if (resourceNode.hasProperty("jcr:lastModified")) {
                    long lastModified = 0;
                    
                    try {
                        lastModified = resourceNode.getProperty("jcr:lastModified").getDate().getTimeInMillis();
                    } catch (ValueFormatException e) {
                        if (log.isWarnEnabled()) {
                            log.warn("jcr:lastModified not of type Date");
                        }
                    }
                    
                    long expires = 0;
                    
                    if(lastModified > 0) {
                        expires = (System.currentTimeMillis() - lastModified);
                    }
                    
                    response.setDateHeader("Expires", expires + System.currentTimeMillis());
                    response.setDateHeader("Last-Modified", lastModified); 
                    response.setHeader("Cache-Control", "max-age="+(expires/1000));
                } else {
                    response.setDateHeader("Expires", 0);
                    response.setHeader("Cache-Control", "max-age=0");
                }
                
                
                ostream = response.getOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = istream.read(buffer)) >= 0) {
                    ostream.write(buffer, 0, len);
                }
                ostream.flush();
            } finally {
                if(istream != null) {
                    istream.close();
                }
                if(ostream != null) {
                    ostream.close();
                }
            }
        } catch (PathNotFoundException ex) {
            if (log.isWarnEnabled()) {
                log.warn("PathNotFoundException with message " + ex.getMessage() + " while getting binary data stream item "
                        + "at path " + resourcePath + ", response status = 404)");
            }
            
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } catch (RepositoryException ex) {
            if (log.isWarnEnabled()) {
                log.warn("Repository exception while resolving binaries request '" + request.getRequestURI() + "' : " + ex.getMessage());
            }
            
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (session != null) {
                releaseSession(request, session);
            }
        }
    }
    
    
    protected Node lookUpResource(Session session, String resourcePath) {
        Node resourceNode = null;
        
        // find the correct item
        String[] elems = resourcePath.substring(1).split("/");
        List<ResourceContainer> resourceContainersForPrefix = prefix2ResourceContainer.get(elems[0]);
        if(resourceContainersForPrefix == null) {
            for(ResourceContainer container : allResourceContainers) {
                resourceNode = container.resolveToResourceNode(session, resourcePath);
                if(resourceNode != null) {
                    return resourceNode;
                }
            }
        } else {
           // use the first resourceContainer that can fetch a resourceNode for this path
           for(ResourceContainer container : resourceContainersForPrefix) {
               resourceNode = container.resolveToResourceNode(session, resourcePath);
               if(resourceNode != null) {
                   return resourceNode;
               }
           }
           
           // we did not find a container that could resolve the node. Fallback to test any container who can resolve the path
           for(ResourceContainer container : allResourceContainers) {
               if(resourceContainersForPrefix.contains(container)) {
                   // skip already tested resource containers
                   continue;
               }
               resourceNode = container.resolveToResourceNode(session, resourcePath);
               if(resourceNode != null) {
                   return resourceNode;
               }
           }
        }
        return null;
    }
    
    protected void doInit() {
        if(initialized) {
            return;
        }
        if(HstServices.isAvailable()) {
            initPrefix2ResourceMappers();
            initAllResourceContainers();
            initialized = true;
        }
    }

    
    protected void initAllResourceContainers() {
        if (allResourceContainers != null) {
            return;
        }
        HstLinkCreator linkCreator = HstServices.getComponentManager().getComponent(HstLinkCreator.class.getName());
        if (linkCreator.getLocationResolvers() == null) {
            allResourceContainers = Collections.EMPTY_LIST;
            return;
        }
        allResourceContainers = new ArrayList<ResourceContainer>();
        for (LocationResolver resolver : linkCreator.getLocationResolvers()) {
            if (resolver instanceof ResourceLocationResolver) {
                ResourceLocationResolver resourceResolver = (ResourceLocationResolver) resolver;
                for (ResourceContainer container : resourceResolver.getResourceContainers()) {
                    allResourceContainers.add(container);
                }
            }
        }
    }

    protected void initPrefix2ResourceMappers() {

        if (prefix2ResourceContainer != null) {
            return;
        }
        HstLinkCreator linkCreator = HstServices.getComponentManager().getComponent(HstLinkCreator.class.getName());
        if (linkCreator.getLocationResolvers() == null) {
            prefix2ResourceContainer = Collections.EMPTY_MAP;
            return;
        }
        prefix2ResourceContainer = new HashMap<String, List<ResourceContainer>>();
        for (LocationResolver resolver : linkCreator.getLocationResolvers()) {
            if (resolver instanceof ResourceLocationResolver) {
                ResourceLocationResolver resourceResolver = (ResourceLocationResolver) resolver;
                for (ResourceContainer container : resourceResolver.getResourceContainers()) {
                    if (container.getMappings() == null) {
                        continue;
                    }
                    for (String prefix : container.getMappings().values()) {
                        List<ResourceContainer> resourceContainersForPrefix = prefix2ResourceContainer.get(prefix);
                        if (resourceContainersForPrefix == null) {
                            resourceContainersForPrefix = new ArrayList<ResourceContainer>();
                            prefix2ResourceContainer.put(prefix, resourceContainersForPrefix);
                        }
                        resourceContainersForPrefix.add(container);
                    }
                }
            }
        }

    }

    /**
     * Adds a Content-Disposition header to the given <code>binaryFileNode</code>. The HTTP header is only set when the
     * content type matches one of the configured <code>contentDispositionContentTypes</code>.
     *
     * <p/>When the Content-Disposition header is set, the filename is retrieved from the <code>binaryFileNode</code>
     * and added to the header value. The property in which the filename is stored should be configured using
     * <code>contentDispositionFilenameProperty</code>.
     *
     * @param request             HTTP request
     * @param response            HTTP response to set header in
     * @param responseContentType content type of the binary file
     * @param binaryFileNode      the node representing the binary file that is streamed to the client
     * @throws javax.jcr.RepositoryException when something goes wrong during repository access
     */
    protected void addContentDispositionHeader(HttpServletRequest request, HttpServletResponse response, String responseContentType, Node binaryFileNode) throws RepositoryException {
        boolean isContentDispositionType = contentDispositionContentTypes.contains(responseContentType);
        
        if (!isContentDispositionType) {
            isContentDispositionType = contentDispositionContentTypes.contains("*/*");
            
            if (!isContentDispositionType) {
                int offset = responseContentType.indexOf('/');
                if (offset != -1) {
                    isContentDispositionType = contentDispositionContentTypes.contains(responseContentType.substring(0, offset) + "/*");
                }
            }
        }
        
        if (isContentDispositionType) {
            // The response content type matches one of the configured content types so add a Content-Disposition
            // header to the response
            StringBuilder headerValue = new StringBuilder("attachment");
            
            if (contentDispositionFilenamePropertyNames != null && contentDispositionFilenamePropertyNames.length > 0) {
                String fileName = null;
                for (String name : contentDispositionFilenamePropertyNames) {
                    if (binaryFileNode.hasProperty(name)) {
                        fileName = binaryFileNode.getProperty(name).getString();
                        break;
                    }
                }
                // A filename is set for the binary node, so add this to the Content-Disposition header value
                if (!StringUtils.isBlank(fileName)) {
                    String encodedFilename = encodeContentDispositionFileName(request, response, fileName);
                    headerValue.append("; filename=\"").append(encodedFilename).append("\"");
                }
            }
            
            response.addHeader("Content-Disposition", headerValue.toString());
        }
    }
    
    protected String encodeContentDispositionFileName(HttpServletRequest request, HttpServletResponse response, String fileName) {
        String userAgent = request.getHeader("User-Agent");
        
        try {
            if (userAgent != null && (userAgent.contains("MSIE") || userAgent.contains("Opera"))) {
                String responseEncoding = response.getCharacterEncoding();
                return URLEncoder.encode(fileName, responseEncoding != null ? responseEncoding : "UTF-8");
            } else {
                return EncoderUtil.encodeEncodedWord(fileName, EncoderUtil.Usage.WORD_ENTITY, 0, Charset.forName("UTF-8"), EncoderUtil.Encoding.B);
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Failed to encode filename.", e);
            } else {
                log.warn("Failed to encode filename. {}", e.toString());
            }
        }
        
        return fileName;
    }
    
    protected Session getSession(HttpServletRequest request) throws RepositoryException {
        Session session = null;
        
        // if hstRequest is retrieved, then this servlet has been dispatched by hst component.
        HstRequest hstRequest = HstRequestUtils.getHstRequest(request);
        
        if (hstRequest != null) {
            session = hstRequest.getRequestContext().getSession();
        } else {
            if (this.repository == null) {
                if (HstServices.isAvailable()) {
                    this.binariesCredentials = HstServices.getComponentManager().getComponent(Credentials.class.getName() + ".binaries");
                    this.repository = HstServices.getComponentManager().getComponent(Repository.class.getName());
                }
            }

            if (this.repository != null) {
                if (this.binariesCredentials != null) {
                    session = this.repository.login(this.binariesCredentials);
                } else {
                    session = this.repository.login();
                }
            }
        }
        
        return session;
    }
    
    protected void releaseSession(HttpServletRequest request, Session session) {
        // if hstRequest is retrieved, then this servlet has been dispatched by hst component.
        HstRequest hstRequest = HstRequestUtils.getHstRequest(request);

        if (hstRequest == null) {
            try {
                session.logout();
            } catch (Exception e) {
            }
        }
    }
    
    protected String getResourceRelPath(HttpServletRequest request) {
        String path = null;
        
        // if hstRequest is retrieved, then this servlet has been dispatched by hst component.
        HstRequest hstRequest = HstRequestUtils.getHstRequest(request);
        
        if (hstRequest != null) {
            path = hstRequest.getResourceID();
        }

        if (path == null) {
            path = HstRequestUtils.getPathInfo(request);
            
            try {
                String characterEncoding = request.getCharacterEncoding();
                
                if (characterEncoding == null) {
                    characterEncoding = "ISO-8859-1";
                }

                path = URLDecoder.decode(path, characterEncoding);
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    log.warn("Cannot decode path: {}. {}", path, e.toString());
                }
            }
        }

        if (path != null && !path.startsWith("/") && path.indexOf(':') > 0) {
            path = path.substring(path.indexOf(':') + 1);
        }
        
        return path;
    }
    
}
