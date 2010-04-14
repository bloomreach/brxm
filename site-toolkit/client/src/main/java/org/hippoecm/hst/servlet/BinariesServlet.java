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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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

import org.apache.commons.io.IOUtils;
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
import org.hippoecm.hst.util.ServletConfigUtils;
import org.hippoecm.hst.utils.EncodingUtils;
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
 * The contentDispositionFilenameProperty is being encoded.  By default, we try to do this user-agent-agnostic. This is preferrable when using 
 * caching reverse proxies in front of your application. In user-agent-agnostic mode, we try to convert filenames consisting non ascii chars to their
 * base form, in other words, replace diacritics. However for for example a language like Chinese this won't work. Then, you might want to opt for the 
 * user-agent-specific mode. You then have to take care of your reverse proxies taking care of the user-agent. They thus should take the user-agent into account.
 * Also see {@link #encodeContentDispositionFileName(HttpServletRequest, HttpServletResponse, String)}. 
 * Changing the default user-agent-agnostic mode to user-agent-specific mode can be done by adding the init-param:
 * 
 * <pre>
 * &lt;init-param&gt;
 *     &lt;param-name&gt;contentDispositionFilenameEncoding&lt;/param-name&gt;
 *     &lt;param-value&gt;user-agent-specific&lt;/param-value&gt;
 * &lt;/init-param&gt;
 * </pre>
 * 
 * You can also configure multiple JCR property names in the above init parameter by comma-separated value. 
 * 
 * @version $Id$
 */
public class BinariesServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    
    public static final String BASE_BINARIES_CONTENT_PATH_INIT_PARAM = "baseBinariesContentPath";
    
    public static final String CONTENT_DISPOSITION_CONTENT_TYPES_INIT_PARAM = "contentDispositionContentTypes";
    
    public static final String CONTENT_DISPOSITION_FILENAME_PROPERTY_INIT_PARAM = "contentDispositionFilenameProperty";

    /**
     * The init param indicating whether the fileName for the content disposition can be encoded 'user-agent-specific' or 
     * 'user-agent-agnostic', also see {@link #encodeContentDispositionFileName(HttpServletRequest, HttpServletResponse, String)}
     */
    public static final String CONTENT_DISPOSITION_FILENAME_ENCODING_INIT_PARAM = "contentDispositionFilenameEncoding";
    
    /**
     * The default encoding for content disposition fileName is 'user-agent-agnostic', also see 
     * {@link #encodeContentDispositionFileName(HttpServletRequest, HttpServletResponse, String)}
     */
    public static final String USER_AGENT_AGNOSTIC_CONTENT_DISPOSITION_FILENAME_ENCODING = "user-agent-agnostic";

    public static final String USER_AGENT_SPECIFIC_CONTENT_DISPOSITION_FILENAME_ENCODING = "user-agent-specific";
    
    public static final String BINARY_RESOURCE_NODE_TYPE_INIT_PARAM = "binaryResourceNodeType";
    
    public static final String BINARY_DATA_PROP_NAME_INIT_PARAM = "binaryDataPropName";
    
    public static final String BINARY_MIME_TYPE_PROP_NAME_INIT_PARAM = "binaryMimeTypePropName";
    
    public static final String BINARY_LAST_MODIFIED_PROP_NAME_INIT_PARAM = "binaryLastModifiedPropName";
    
    public static final String DEFAULT_BASE_BINARIES_CONTENT_PATH = "";
    
    public static final String DEFAULT_BINARY_RESOURCE_NODE_TYPE = HippoNodeType.NT_RESOURCE;
    
    public static final String DEFAULT_BINARY_DATA_PROP_NAME = "jcr:data";
    
    public static final String DEFAULT_BINARY_MIME_TYPE_PROP_NAME = "jcr:mimeType";
    
    public static final String DEFAULT_BINARY_LAST_MODIFIED_PROP_NAME = "jcr:lastModified";
    
    private static Logger log = LoggerFactory.getLogger(BinariesServlet.class);
    
    private Repository repository;
    
    private Credentials binariesCredentials;
    
    protected String baseBinariesContentPath = DEFAULT_BASE_BINARIES_CONTENT_PATH;

    protected Set<String> contentDispositionContentTypes;

    protected String [] contentDispositionFilenamePropertyNames;
    
    protected Map<String, List<ResourceContainer>> prefix2ResourceContainer;
    
    protected List<ResourceContainer> allResourceContainers;
    
    private boolean initialized = false;
    
    private String binaryResourceNodeType = DEFAULT_BINARY_RESOURCE_NODE_TYPE;
    
    private String binaryDataPropName = DEFAULT_BINARY_DATA_PROP_NAME;
    
    private String binaryMimeTypePropName = DEFAULT_BINARY_MIME_TYPE_PROP_NAME;
    
    private String binaryLastModifiedPropName = DEFAULT_BINARY_LAST_MODIFIED_PROP_NAME;
    
    protected  String contentDispositionFileNameEncoding = USER_AGENT_AGNOSTIC_CONTENT_DISPOSITION_FILENAME_ENCODING;
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        
        baseBinariesContentPath = ServletConfigUtils.getInitParameter(config, null, BASE_BINARIES_CONTENT_PATH_INIT_PARAM, baseBinariesContentPath);
        contentDispositionFilenamePropertyNames = StringUtils.split(ServletConfigUtils.getInitParameter(config, null, CONTENT_DISPOSITION_FILENAME_PROPERTY_INIT_PARAM, null), ", \t\r\n");
        
        // Parse mime types from init-param
        contentDispositionContentTypes = new HashSet<String>();
        String mimeTypesString = ServletConfigUtils.getInitParameter(config, null, CONTENT_DISPOSITION_CONTENT_TYPES_INIT_PARAM, null);
        if (mimeTypesString != null) {
            contentDispositionContentTypes.addAll(Arrays.asList(StringUtils.split(mimeTypesString, ", \t\r\n")));
        }
        
        binaryDataPropName = ServletConfigUtils.getInitParameter(config, null, BINARY_DATA_PROP_NAME_INIT_PARAM, binaryDataPropName);
        binaryMimeTypePropName = ServletConfigUtils.getInitParameter(config, null, BINARY_MIME_TYPE_PROP_NAME_INIT_PARAM, binaryMimeTypePropName);
        binaryLastModifiedPropName = ServletConfigUtils.getInitParameter(config, null, BINARY_LAST_MODIFIED_PROP_NAME_INIT_PARAM, binaryLastModifiedPropName);
        contentDispositionFileNameEncoding = ServletConfigUtils.getInitParameter(config, null, CONTENT_DISPOSITION_FILENAME_ENCODING_INIT_PARAM , contentDispositionFileNameEncoding);
        if(!USER_AGENT_AGNOSTIC_CONTENT_DISPOSITION_FILENAME_ENCODING.equals(contentDispositionFileNameEncoding) && !USER_AGENT_SPECIFIC_CONTENT_DISPOSITION_FILENAME_ENCODING.equals(contentDispositionFileNameEncoding)) {
            throw new ServletException("Invalid init-param: the only allowed values for contentDispositionFilenameEncoding are '"+USER_AGENT_AGNOSTIC_CONTENT_DISPOSITION_FILENAME_ENCODING+"' or '"+USER_AGENT_SPECIFIC_CONTENT_DISPOSITION_FILENAME_ENCODING+"'");
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String relPath = getResourceRelPath(request);
        String resourcePath = null;
        Session session = null;
        
        if (!initialized) {
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
            
            if (resourcePath == null || !resourcePath.startsWith("/")) {
                if (log.isWarnEnabled()) {
                    log.warn("item at path " + resourcePath + " not found, response status = 404)");
                }
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            session = getSession(request);
            
            Node resourceNode = lookUpResource(session, resourcePath);
            
            if (resourceNode == null) {
                log.warn("item at path " + resourcePath + " cannot be found.");
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            if (!resourceNode.isNodeType(binaryResourceNodeType)) {
                response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                log.warn("Found node is not of type '{}' but was of type '{}'. Return HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE", binaryResourceNodeType, resourceNode.getPrimaryNodeType().getName());
                return;
            }
            
            if (!resourceNode.hasProperty(binaryMimeTypePropName)) {
                if (log.isWarnEnabled()) {
                    log.warn("item at path " + resourcePath + " does not have a property: {}. The response status will be 415", binaryMimeTypePropName);
                }
                
                response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
                return;
            }
            
            String mimeType = resourceNode.getProperty(binaryMimeTypePropName).getString();

            if (!resourceNode.hasProperty(binaryDataPropName)) {
                if (log.isWarnEnabled()) {
                    log.warn("item at path " + resourcePath + " does not have a property: {}. The response status will be 404", binaryDataPropName);
                }
                
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            Calendar lastModifiedDate = getLastModifiedDate(resourceNode);
            long ifModifiedSince = -1L;
            
            try {
                ifModifiedSince = request.getDateHeader("If-Modified-Since");
            } catch (IllegalArgumentException ignore) {
                if (log.isWarnEnabled()) {
                    log.warn("The header, If-Modified-Since, contains invalid value: " + request.getHeader("If-Modified-Since"));
                }
            }
            
            if (ifModifiedSince != -1 && ifModifiedSince / 1000 >= lastModifiedDate.getTimeInMillis() / 1000) {
            //    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            //    return;
            }
            
            InputStream input = null;
            OutputStream output = null;
            
            try {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType(mimeType);
                
                // Add the Content-Disposition header for configured content types
                addContentDispositionHeader(request, response, mimeType, resourceNode);
                
                // TODO add a configurable factor + default minimum for expires. Ideally, this value is
                // stored in the repository
                if (lastModifiedDate != null) {
                    long expires = System.currentTimeMillis() - lastModifiedDate.getTimeInMillis();
                    if (expires > 0) {
                        response.setDateHeader("Expires", expires + System.currentTimeMillis());
                        response.setDateHeader("Last-Modified", lastModifiedDate.getTimeInMillis()); 
                        response.setHeader("Cache-Control", "max-age=" + (expires / 1000));
                    }
                } else {
                    response.setDateHeader("Expires", 0);
                    response.setHeader("Cache-Control", "max-age=0");
                }
                
                Property data = resourceNode.getProperty(binaryDataPropName);
                input = data.getStream();
                output = response.getOutputStream();
                IOUtils.copy(input, output);
                output.flush();
            } finally {
                if (input != null) {
                    IOUtils.closeQuietly(input);
                }
                if (output != null) {
                    IOUtils.closeQuietly(output);
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
    
    
    /**
     * <p>When the <code>fileName</code> consists only of US-ASCII chars, we can safely return the <code>fileName</code> <b>as is</b>. However, when the  <code>fileName</code>
     * does contains non-ascii-chars there is a problem because of different browsers expect different encoding: there is no uniform version that works for 
     * all browsers. So, we are either stuck to user-agent sniffing to return the correct encoding, or try to return a US-ASCII form as best as we can.</p>
     *  
     * <p>The problem with user-agent sniffing is that in general, when you use reverse proxies, you do not want to cache pages <b>per</b> browser type. If one version
     * is demanded for all different user agents, the best we can do is trying to bring the fileName back to its base form, thus, replacing diacritics by their
     * base form. This will work fine for most Latin alphabets.</p> 
     * 
     * <p>However, a language like Chinese won't be applicable for this approach. The only way to have it correct in such languages, is to return 
     * a different version for different browsers</p>
     * 
     * <p>To be able to serve both usecases, we make it optional <b>how</b> you'd like your encoding strategy to be. The default strategy, is assuming
     * Latin alphabets and try to get the non-diacritical version of a fileName: The default strategy is thus (browser) user-agent-agnostic.</p>
     * 
     * <p>For languages like Chinese, you can if you do want all browser version to display the correct fileName for the Content-Disposition, tell the 
     * binaries servlet to do so with the following init-param. Note that in this case, you might have to account for the user-agent in your reverse proxy setup</p>
     * 
     * <pre>
     * &lt;init-param&gt;
     *     &lt;param-name&gt;contentDispositionFilenameEncoding&lt;/param-name&gt;
     *     &lt;param-value&gt;user-agent-specific&lt;/param-value&gt;
     * &lt;/init-param&gt;
     * </pre>
     * 
     * @param request
     * @param response
     * @param fileName the un-encoded filename
     * @return
     */
    protected String encodeContentDispositionFileName(HttpServletRequest request, HttpServletResponse response, String fileName) {
        
        try {
            String responseEncoding = response.getCharacterEncoding();
            String encodedFileName =  URLEncoder.encode(fileName, responseEncoding != null ? responseEncoding : "UTF-8");
            
            if(encodedFileName.equals(fileName)) {
                log.debug("The filename did not contains non-ascii chars: we can safely return an un-encoded version");
                return fileName;
            }
            
            if(USER_AGENT_AGNOSTIC_CONTENT_DISPOSITION_FILENAME_ENCODING.equals(contentDispositionFileNameEncoding)){
                // let's try to bring the filename to it's baseform by replacing diacritics: if then still there are non-ascii chars, we log an info 
                // message that you might need user-agent-specific mode, and return a utf-8 encoded version. 
                
                String asciiFileName = EncodingUtils.isoLatin1AccentReplacer(fileName); 
                
                // now check whether the asciiFileName really only contains ascii chars:
                String encodedAsciiFileName = URLEncoder.encode(asciiFileName, responseEncoding != null ? responseEncoding : "UTF-8");
                if(encodedAsciiFileName.equals(asciiFileName)) {
                    log.debug("Replaced fileName '{}' with its un-accented equivalent '{}'", fileName, asciiFileName);
                    return asciiFileName;
                } else {
                    log.info("Filename '{}' consists of non latin chars. We have to utf-8 encode the filename, which might be shown with unencoded in some browsers." +
                    		" If you want to avoid this, use '{}'. However, this influences reverse proxies.", fileName, USER_AGENT_SPECIFIC_CONTENT_DISPOSITION_FILENAME_ENCODING);
                    return encodedAsciiFileName;
                }
                
            } else if(USER_AGENT_SPECIFIC_CONTENT_DISPOSITION_FILENAME_ENCODING.equals(contentDispositionFileNameEncoding)) {
                String userAgent = request.getHeader("User-Agent");
                if (userAgent != null && (userAgent.contains("MSIE") || userAgent.contains("Opera"))) {
                    return URLEncoder.encode(fileName, responseEncoding != null ? responseEncoding : "UTF-8");
                } else {
                    return EncoderUtil.encodeEncodedWord(fileName, EncoderUtil.Usage.WORD_ENTITY, 0, Charset.forName("UTF-8"), EncoderUtil.Encoding.B);
                }
            } else {
                log.warn("Invalid encoding strategy: only allowed is '"+USER_AGENT_AGNOSTIC_CONTENT_DISPOSITION_FILENAME_ENCODING+"' or '"+USER_AGENT_AGNOSTIC_CONTENT_DISPOSITION_FILENAME_ENCODING+"'. Return utf-8 encoded version.");
                return URLEncoder.encode(fileName, responseEncoding != null ? responseEncoding : "UTF-8");
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
    
    protected Calendar getLastModifiedDate(Node resourceNode) {
        Calendar lastModifiedDate = null;
        
        try {
            if (resourceNode.hasProperty(binaryLastModifiedPropName)) {
                lastModifiedDate = resourceNode.getProperty(binaryLastModifiedPropName).getDate();
            }
        } catch (ValueFormatException e) {
            if (log.isWarnEnabled()) {
                log.warn("The property, {}, is not in valid format. {}", binaryLastModifiedPropName, e);
            }
        } catch (PathNotFoundException e) {
            if (log.isWarnEnabled()) {
                log.warn("The property, {}, is not found. {}", binaryLastModifiedPropName, e);
            }
        } catch (RepositoryException e) {
            if (log.isWarnEnabled()) {
                log.warn("The property, {}, cannot be retrieved. {}", binaryLastModifiedPropName, e);
            }
        }
        
        return lastModifiedDate;
    }
    
}
