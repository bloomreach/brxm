/*
 *  Copyright 2010-2017 Hippo B.V. (http://www.onehippo.com)
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.cache.HstCache;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.linking.LocationResolver;
import org.hippoecm.hst.core.linking.ResourceContainer;
import org.hippoecm.hst.core.linking.ResourceLocationResolver;
import org.hippoecm.hst.servlet.utils.BinariesCache;
import org.hippoecm.hst.servlet.utils.BinaryPage;
import org.hippoecm.hst.servlet.utils.BinaryPage.CacheKey;
import org.hippoecm.hst.servlet.utils.ByteRange;
import org.hippoecm.hst.servlet.utils.ByteRangeUtils;
import org.hippoecm.hst.servlet.utils.ContentDispositionUtils;
import org.hippoecm.hst.servlet.utils.HeaderUtils;
import org.hippoecm.hst.servlet.utils.ResourceUtils;
import org.hippoecm.hst.servlet.utils.SessionUtils;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.ServletConfigUtils;
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
 * Also see {@link org.hippoecm.hst.servlet.utils.ContentDispositionUtils#encodeFileName(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, java.lang.String)}.
 * Changing the default user-agent-agnostic mode to user-agent-specific mode can be done by adding the init-param:
 * 
 * <pre>
 * &lt;init-param&gt;
 *     &lt;param-name&gt;contentDispositionFilenameEncoding&lt;/param-name&gt;
 *     &lt;param-value&gt;user-agent-specific&lt;/param-value&gt;
 * &lt;/init-param&gt;
 * </pre>
 * 
 * <p>You can also configure multiple JCR property names in the above init parameter by comma-separated value.</p>
 *
 * <p>Sometimes you do not want to set the content disposition headers for all files of a certain mime-type.
 * You can do this by adding a request parameter to the url to the resource. In the template you should add the request param to the link.</p>
 *
 * <pre>
 * &lt;hst:link var="link" hippobean="${item}"&gt;
 *     &lt;hst:param name="forceDownload" value="true"/&gt;
 * &lt;/hst:link&gt;
 * </pre>
 *
 * <p>If you want to have a different name for the parameter you can explicitly set the name as an init parameter of the BinariesServlet.</p>
 *
 * <pre>
 * &lt;init-param&gt;
 *     &lt;param-name&gt;forceContentDispositionRequestParamName&lt;/param-name&gt;
 *     &lt;param-value&gt;forceDownload&lt;/param-value&gt;
 * &lt;/init-param&gt;
 * </pre>
 */
public class BinariesServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(BinariesServlet.class);

    private static final String CACHE_NAME_INIT_PARAM = "cache-name";
    
    private static final String CACHE_MAX_OBJECT_SIZE_BYTES_INIT_PARAM = "cache-max-object-size-bytes";
    
    private static final String VALIDITY_CHECK_INTERVAL_SECONDS = "validity-check-interval-seconds";

    private static final String SET_EXPIRES_HEADERS_INIT_PARAM = "set-expires-headers";

    public static final String SET_CONTENT_LENGTH_HEADER_INIT_PARAM = "set-content-length-header";

    public static final String USE_ACCEPT_RANGES_HEADER_INIT_PARAM = "use-accept-ranges-header";

    public static final String BASE_BINARIES_CONTENT_PATH_INIT_PARAM = "baseBinariesContentPath";

    public static final String CONTENT_DISPOSITION_CONTENT_TYPES_INIT_PARAM = "contentDispositionContentTypes";

    public static final String CONTENT_DISPOSITION_FILENAME_PROPERTY_INIT_PARAM = "contentDispositionFilenameProperty";

    /**
     * The init param indicating whether the fileName for the content disposition can be encoded 'user-agent-specific' or 
     * 'user-agent-agnostic', also see {@link org.hippoecm.hst.servlet.utils.ContentDispositionUtils#encodeFileName(javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse, java.lang.String, java.lang.String)}
     */
    public static final String CONTENT_DISPOSITION_FILENAME_ENCODING_INIT_PARAM = "contentDispositionFilenameEncoding";

    public static final String MIME_SEPARATION_INIT_PARAM = "mimeSeparation";

    public static final String FORCE_CONTENT_DISPOSITION_INIT_PARAM = "forceContentDispositionRequestParamName";

    public static final String DEFAULT_FORCE_CONTENT_DISPOSITION_PARAM_NAME = "forceDownload";

    public static final String BINARY_RESOURCE_NODE_TYPE_INIT_PARAM = "binaryResourceNodeType";

    public static final String BINARY_DATA_PROP_NAME_INIT_PARAM = "binaryDataPropName";

    public static final String BINARY_MIME_TYPE_PROP_NAME_INIT_PARAM = "binaryMimeTypePropName";

    public static final String BINARY_LAST_MODIFIED_PROP_NAME_INIT_PARAM = "binaryLastModifiedPropName";

    private static final boolean DEFAULT_SET_EXPIRES_HEADERS = true;
    
    private static final boolean DEFAULT_SET_CONTENT_LENGTH_HEADERS = true;

    private static final boolean DEFAULT_USE_ACCEPT_RANGES = true;

    /**
     * MIME multipart separation string
     */
    private static final String DEFAULT_MIME_SEPARATION = "TRIBES_MIME_BOUNDARY";

    private String baseBinariesContentPath = ResourceUtils.DEFAULT_BASE_BINARIES_CONTENT_PATH;

    private Set<String> contentDispositionContentTypes;

    private String[] contentDispositionFilenamePropertyNames;

    private String contentDispositionFileNameEncoding = ContentDispositionUtils.USER_AGENT_AGNOSTIC_CONTENT_DISPOSITION_FILENAME_ENCODING;

    private Map<String, List<ResourceContainer>> prefix2ResourceContainer;

    private List<ResourceContainer> allResourceContainers;

    private boolean initialized = false;

    private boolean setExpires = DEFAULT_SET_EXPIRES_HEADERS;
    
    private boolean setContentLength = DEFAULT_SET_CONTENT_LENGTH_HEADERS;

    private boolean useAcceptRanges = DEFAULT_USE_ACCEPT_RANGES;

    private String binaryResourceNodeType = ResourceUtils.DEFAULT_BINARY_RESOURCE_NODE_TYPE;

    private String binaryDataPropName = ResourceUtils.DEFAULT_BINARY_DATA_PROP_NAME;

    private String binaryMimeTypePropName = ResourceUtils.DEFAULT_BINARY_MIME_TYPE_PROP_NAME;

    private String binaryLastModifiedPropName = ResourceUtils.DEFAULT_BINARY_LAST_MODIFIED_PROP_NAME;

    private String forceContentDispositionRequestParamName = DEFAULT_FORCE_CONTENT_DISPOSITION_PARAM_NAME;

    private String mimeSeparation = DEFAULT_MIME_SEPARATION;

    /** FIXME: BinariesCache is not serializable. */
    private BinariesCache binariesCache;
    
    private String binariesCacheComponentName;

    private BinaryPageFactory binaryPageFactory;

    protected class DefaultBinaryPageFactory implements BinaryPageFactory {

        @Override
        public BinaryPage createBinaryPage(final String resourcePath, final Session session) throws RepositoryException{
            final BinaryPage binaryPage = new BinaryPage(resourcePath);
            binaryPage.setCacheKey(new CacheKey(session.getUserID(), resourcePath));
            initBinaryPageValues(session, binaryPage);
            return binaryPage;
        }
    }

    protected BinaryPageFactory createBinaryPageFactory() {
        return new DefaultBinaryPageFactory();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        initBinariesConfig();
        initContentDispostion();
        initExpires();
        initSetContentLengthHeader();
        initUseAcceptRangesHeader();
        binaryPageFactory = createBinaryPageFactory();
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!initialized) {
            synchronized (this) {
                doInit();
            }
        }
        
        // HST Container Component Manager can be reloaded; so cache bean should be reset when reloaded.
        if (HstServices.isAvailable()) {
            HstCache cache = HstServices.getComponentManager().getComponent(binariesCacheComponentName);
            if (cache != null && binariesCache.getHstCache() != cache) {
                binariesCache.setHstCache(cache);
            }
        }
        try {
            final BinaryPage page = getPageFromCacheOrLoadPage(request);

            if (page.getStatus() != HttpServletResponse.SC_OK) {
                // nothing left to do
                response.sendError(page.getStatus());
                return;
            }

            writeResponse(request, response, page);
        } catch (IllegalStateException e) {
            log.info("Could not serve request '{}' : {}", request, e.toString());
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
    }

    protected void writeResponse(final HttpServletRequest request, final HttpServletResponse response, final BinaryPage page) throws IOException {
        response.setStatus(page.getStatus());
        boolean setExpiresNeeded = setExpires;

        final String pageMimeType = page.getMimeType();

        if (ContentDispositionUtils.isContentDispositionType(pageMimeType, contentDispositionContentTypes) ||
                (request.getParameter(forceContentDispositionRequestParamName) != null &&
                        Boolean.parseBoolean(request.getParameter(forceContentDispositionRequestParamName)))) {
            setExpiresNeeded = false;
            ContentDispositionUtils.addContentDispositionHeader(request, response, page.getFileName(),
                    contentDispositionFileNameEncoding);
        }

        HeaderUtils.setLastModifiedHeaders(response, page);
        if (setExpiresNeeded) {
            HeaderUtils.setExpiresHeaders(response, page);
        }

        /**
         * NOTE: RFC 2616 the 304 response should *not* contain a message body,
         * hence it should not contain a {@code Content-Length} header either.
         */
        if (HeaderUtils.hasMatchingEtag(request, page)) {
            log.debug("Matching ETag for uri {} , page {}", request.getRequestURI(), page.getResourcePath());
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }
        if (!HeaderUtils.isModifiedSince(request, page)) {
            log.debug("Page not modified for uri {} , page {}", request.getRequestURI(), page.getResourcePath());
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }

        response.setHeader("ETag", page.getETag());

        if (setContentLength) {
            HeaderUtils.setContentLengthHeader(response, page);
        }

        if (useAcceptRanges) {
            response.setHeader("Accept-Ranges", "bytes");
        }

        List<ByteRange> byteRanges = ByteRangeUtils.parseRanges(request, response, page);

        ServletOutputStream output = null;

        try {
            output = response.getOutputStream();

            // only when byte range request comes...
            if (byteRanges != null && !byteRanges.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

                if (byteRanges.size() == 1) {
                    response.setContentType(pageMimeType);
                    ByteRange range = byteRanges.get(0);
                    response.addHeader("Content-Range", "bytes " + range.start + "-" + range.end + "/" + range.length);
                    response.setHeader("Content-Length", Long.toString(range.end - range.start + 1));
                    copyPageToResponse(request, response, output, page, range.start, range.end - range.start + 1);
                } else {
                    response.setContentType("multipart/byteranges; boundary=" + mimeSeparation);
                    for (ByteRange range : byteRanges) {
                        // Writing MIME header.
                        output.println();
                        output.println("--" + mimeSeparation);
                        if (pageMimeType != null) {
                            output.println("Content-Type: " + pageMimeType);
                        }
                        output.println("Content-Range: bytes " + range.start + "-" + range.end + "/" + range.length);
                        output.println();
                        // Printing content
                        copyPageToResponse(request, response, output, page, range.start, range.end - range.start + 1);
                    }
                }
            }
            // keep the old default behavior for full compatibility...
            else {
                response.setContentType(pageMimeType);
                copyPageToResponse(request, response, output, page, -1, -1);
            }
        } finally {
            IOUtils.closeQuietly(output);
        }
    }

    /**
     * Copy binary page bytes data from {@code offset} index up to {@code length} bytes in total.
     * If {@code offset} and {@code length} are both negative, the whole bytes data will be copied.
     * @param request request
     * @param response response
     * @param output servlet output stream
     * @param page binary page
     * @param offset offset index
     * @param length max length to copy
     * @throws IOException if IO exception occurs
     */
    private void copyPageToResponse(HttpServletRequest request, HttpServletResponse response,
            final ServletOutputStream output, final BinaryPage page, final long offset, final long length)
                    throws IOException {
        InputStream input = null;
        Session session = null;

        try {
            if (page.containsData()) {
                input = page.getStream();
            } else {
                session = SessionUtils.getBinariesSession(request);
                input = getRepositoryResourceStream(session, page);
            }

            if (input == null) {
                log.warn("Could not find binary for uri '{}'", request.getRequestURI());
                page.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            if (offset >= 0 || length >= 0) {
                IOUtils.copyLarge(input, output, offset, length);
            } else {
                IOUtils.copyLarge(input, output);
            }

            output.flush();
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("RepositoryException while getting stream for binaries request '" + request.getRequestURI() + "'", e);
            } else {
                log.warn("RepositoryException while getting stream for binaries request '{}'. {}", request.getRequestURI(), e);
            }
            binariesCache.removePage(page);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to stream binary content to client: " + e.getMessage());
        } catch (IOException e) {
            if (log.isDebugEnabled()) {
                log.warn("Stream Read/Write failed for binaries request '" + request.getRequestURI() + "'", e);
            } else {
                log.warn("Stream Read/Write failed for binaries request '{}'. {}", request.getRequestURI(), e);
            }
        } finally {
            IOUtils.closeQuietly(input);
            SessionUtils.releaseSession(request, session);
        }
    }

    /**
     * @param session
     * @param page
     * @return the input stream for a looked up binary for {@link BinaryPage#getResourcePath()}, or <code>null</code> if
     * no binary could be found.
     * @throws RepositoryException
     */
    protected InputStream getRepositoryResourceStream(Session session, BinaryPage page) throws RepositoryException {
        if (!session.nodeExists(page.getRepositoryPath())) {
            return null;
        }
        Node resourceNode = session.getNode(page.getRepositoryPath());
        return resourceNode.getProperty(binaryDataPropName).getBinary().getStream();
    }

    protected BinaryPage getPageFromCacheOrLoadPage(HttpServletRequest request) {
        final String resourcePath = ResourceUtils.getResourcePath(request, baseBinariesContentPath);
        final String sessionUserID = getSessionUserID(request);
        CacheKey cacheKey = new CacheKey(sessionUserID, resourcePath);
        BinaryPage page = binariesCache.getPageFromBlockingCache(cacheKey);
        if (page != null) {
            page = getValidatedPageFromCache(request, page);
        } else {
            try {
                page = getBinaryPage(request, resourcePath);
                binariesCache.putPage(page);
            } catch (RuntimeException e) {
                binariesCache.clearBlockingLock(cacheKey);
                throw e;
            }
        }
        return page;
    }

    protected BinaryPage getValidatedPageFromCache(HttpServletRequest request, BinaryPage page) {
        if (HeaderUtils.isForcedCheck(request) || binariesCache.mustCheckValidity(page)) {
            long lastModified = getLastModifiedFromResource(request, page.getResourcePath());
            if (binariesCache.isPageStale(page, lastModified) || isPageStale(request, page)) {
                binariesCache.removePage(page);
                page = getBinaryPage(request, page.getResourcePath());
                binariesCache.putPage(page);
            } else {
                binariesCache.updateNextValidityCheckTime(page);
            }
        }
        return page;
    }

    /**
     * Hook for subclasses to check whether a previously cached page should be recreated. The default implementation
     * in this {@link BinariesServlet} returns false
     *
     * @param request      current HTTP request
     * @param page         current cached binary page
     * @return             true if the page should be recreated, false otherwise
     */
    protected boolean isPageStale(HttpServletRequest request, BinaryPage page) {
        return false;
    }

    protected long getLastModifiedFromResource(HttpServletRequest request, String resourcePath) {
        Session session = null;
        try {
            session = SessionUtils.getBinariesSession(request);
            Node resourceNode = getResourceNode(session, resourcePath);
            return ResourceUtils.getLastModifiedDate(resourceNode, binaryLastModifiedPropName);
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Repository exception while resolving binaries request '" + request.getRequestURI() + "' : " + e, e);
            } else {
                log.warn("Repository exception while resolving binaries request '{}'. {}", request.getRequestURI(), e);
            }
        } finally {
            SessionUtils.releaseSession(request, session);
        }
        return -1L;
    }

    protected final String getSessionUserID(HttpServletRequest request) {
        Session session = null;
        try {
            session = SessionUtils.getBinariesSession(request);
            return session.getUserID();
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Repository exception while resolving binaries request '" + request.getRequestURI() + "' : " + e, e);
            } else {
                log.warn("Repository exception while resolving binaries request '{}'. {}", request.getRequestURI(), e);
            }
            throw new IllegalStateException(String.format("Repository exception while resolving binaries request '%s' : %s", request.getRequestURI(), e.toString()));
        } finally {
            SessionUtils.releaseSession(request, session);
        }
    }

    /**
     * Retrieve the JCR node representing a resource
     *
     * @param session      the jcr session to fetch the resource with
     * @param resourcePath path of resource
     * @return             resource node if found, or <code>null</code> if cannot be found or a
     * {@link javax.jcr.RepositoryException} happened.
     */
    protected Node getResourceNode(Session session, String resourcePath) {
        Node resourceNode = ResourceUtils.lookUpResource(session, resourcePath, prefix2ResourceContainer, allResourceContainers);
        if (resourceNode == null) {
            log.info("Could not resolving jcr node for binaries request for '{}' : ", resourcePath);
        }
        return resourceNode;
    }

    protected BinaryPage getBinaryPage(HttpServletRequest request, String resourcePath) {
        Session session = null;
        try {
            session = SessionUtils.getBinariesSession(request);
            final BinaryPage page = binaryPageFactory.createBinaryPage(resourcePath, session);
            log.info("Page loaded: {}", page);
            return page;
        } catch (RepositoryException e) {
            if (session == null) {
                // NoAvailableSessionException is wrapped in RepositoryException
                throw new IllegalStateException(String.format("Repository exception while resolving binaries request '%s' : %s", request.getRequestURI(), e.toString()));
            }
            BinaryPage errorPage = new BinaryPage(resourcePath);
            // error page requires the same cachekey but is not set through the constructor. Unfortunately....
            // if we don't set the cache, the blocking cache will never release the lock
            errorPage.setCacheKey(new CacheKey(session.getUserID(), resourcePath));
            errorPage.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            if (log.isDebugEnabled()) {
                log.warn("Repository exception while resolving binaries request '" + request.getRequestURI() + "' : " + e, e);
            } else {
                log.warn("Repository exception while resolving binaries request '{}'. {}", request.getRequestURI(), e);
            }
            return errorPage;
        } finally {
            SessionUtils.releaseSession(request, session);
        }
    }

    protected void initBinaryPageValues(Session session, BinaryPage page) throws RepositoryException {
        // even if not found, do not check for every request but only on next validity check time
        page.setNextValidityCheckTime(System.currentTimeMillis() + binariesCache.getValidityCheckIntervalMillis());
        if (!ResourceUtils.isValidResourcePath(page.getResourcePath())) {
            page.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Node resourceNode = getResourceNode(session, page.getResourcePath());
        if (resourceNode == null) {
            page.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        page.setRepositoryPath(resourceNode.getPath());

        if (!ResourceUtils.hasValideType(resourceNode, binaryResourceNodeType)) {
            page.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            return;
        }

        if (!ResourceUtils.hasBinaryProperty(resourceNode, binaryDataPropName)) {
            page.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (!ResourceUtils.hasMimeTypeProperty(resourceNode, binaryMimeTypePropName)) {
            page.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            return;
        }

        page.setStatus(HttpServletResponse.SC_OK);
        page.setMimeType(resourceNode.getProperty(binaryMimeTypePropName).getString());
        page.setLastModified(ResourceUtils.getLastModifiedDate(resourceNode, binaryLastModifiedPropName));
        page.setFileName(ResourceUtils.getFileName(resourceNode, contentDispositionFilenamePropertyNames));
        page.setLength(ResourceUtils.getDataLength(resourceNode, binaryDataPropName));

        storeResourceOnBinaryPage(page, resourceNode);
    }

    /**
     * Stores the binary content of <code>resourceNode</code> on the <code>page</code> unless the <code>page</code>
     * is marked to be uncacheable.
     */
    protected void storeResourceOnBinaryPage(BinaryPage page, Node resourceNode) {
        if (!page.isCacheable()) {
            return;
        }
        if (!binariesCache.isBinaryDataCacheable(page)) {
            return;
        }
        try {
            InputStream input = resourceNode.getProperty(binaryDataPropName).getBinary().getStream();
            page.loadDataFromStream(input);
        } catch (RepositoryException e) {
            if (log.isDebugEnabled()) {
                log.warn("Unable to cache page data for " + page.getResourcePath(), e);
            } else {
                log.warn("Unable to cache page data for '{}'. {}", page.getResourcePath(), e);
            }
        } catch (IOException e) {
            page.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            if (log.isDebugEnabled()) {
                log.warn("Error while copying datastream from resource node " + page.getResourcePath(), e);
            } else {
                log.warn("Error while copying datastream from resource node '{}'. {}", page.getResourcePath(), e);
            }
        }
    }

    protected void doInit() {
        if (initialized) {
            return;
        }
        
        if (HstServices.isAvailable()) {
            initPrefix2ResourceMappers();
            initAllResourceContainers();
            initBinariesCache();
            initialized = true;
        }
    }

    private void initPrefix2ResourceMappers() {
        if (prefix2ResourceContainer != null) {
            return;
        }
        HstLinkCreator linkCreator = HstServices.getComponentManager().getComponent(HstLinkCreator.class.getName());
        if (linkCreator.getLocationResolvers() == null) {
            prefix2ResourceContainer = Collections.emptyMap();
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

    private void initAllResourceContainers() {
        if (allResourceContainers != null) {
            return;
        }
        HstLinkCreator linkCreator = HstServices.getComponentManager().getComponent(HstLinkCreator.class.getName());
        if (linkCreator.getLocationResolvers() == null) {
            allResourceContainers = Collections.emptyList();
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

    private void initBinariesConfig() {
        baseBinariesContentPath = getInitParameter(BASE_BINARIES_CONTENT_PATH_INIT_PARAM, baseBinariesContentPath);
        binaryResourceNodeType = getInitParameter(BINARY_RESOURCE_NODE_TYPE_INIT_PARAM, binaryResourceNodeType);
        binaryDataPropName = getInitParameter(BINARY_DATA_PROP_NAME_INIT_PARAM, binaryDataPropName);
        binaryMimeTypePropName = getInitParameter(BINARY_MIME_TYPE_PROP_NAME_INIT_PARAM, binaryMimeTypePropName);
        binaryLastModifiedPropName = getInitParameter(BINARY_LAST_MODIFIED_PROP_NAME_INIT_PARAM,
                binaryLastModifiedPropName);
    }

    private void initContentDispostion() throws ServletException {

        mimeSeparation =
                StringUtils.defaultIfEmpty(StringUtils.trim(getInitParameter(MIME_SEPARATION_INIT_PARAM, null)),
                        DEFAULT_MIME_SEPARATION);

        forceContentDispositionRequestParamName = getInitParameter(FORCE_CONTENT_DISPOSITION_INIT_PARAM, DEFAULT_FORCE_CONTENT_DISPOSITION_PARAM_NAME);

        contentDispositionFilenamePropertyNames = StringUtils.split(getInitParameter(
                CONTENT_DISPOSITION_FILENAME_PROPERTY_INIT_PARAM, null), ", \t\r\n");

        // Parse mime types from init-param
        contentDispositionContentTypes = new HashSet<String>();
        String mimeTypesString = getInitParameter(CONTENT_DISPOSITION_CONTENT_TYPES_INIT_PARAM, null);
        if (mimeTypesString != null) {
            contentDispositionContentTypes.addAll(Arrays.asList(StringUtils.split(mimeTypesString, ", \t\r\n")));
        }

        contentDispositionFileNameEncoding = getInitParameter(CONTENT_DISPOSITION_FILENAME_ENCODING_INIT_PARAM,
                contentDispositionFileNameEncoding);
        if (!ContentDispositionUtils.USER_AGENT_AGNOSTIC_CONTENT_DISPOSITION_FILENAME_ENCODING
                .equals(contentDispositionFileNameEncoding)
                && !ContentDispositionUtils.USER_AGENT_SPECIFIC_CONTENT_DISPOSITION_FILENAME_ENCODING
                        .equals(contentDispositionFileNameEncoding)) {
            throw new ServletException(
                    "Invalid init-param: the only allowed values for contentDispositionFilenameEncoding are '"
                            + ContentDispositionUtils.USER_AGENT_AGNOSTIC_CONTENT_DISPOSITION_FILENAME_ENCODING
                            + "' or '"
                            + ContentDispositionUtils.USER_AGENT_SPECIFIC_CONTENT_DISPOSITION_FILENAME_ENCODING + "'");
        }
    }

    private void initExpires() {
        setExpires = getBooleanInitParameter(SET_EXPIRES_HEADERS_INIT_PARAM, DEFAULT_SET_EXPIRES_HEADERS);
    }
    
    private void initSetContentLengthHeader() {
        setContentLength = getBooleanInitParameter(SET_CONTENT_LENGTH_HEADER_INIT_PARAM, DEFAULT_SET_CONTENT_LENGTH_HEADERS);
    }

    private void initUseAcceptRangesHeader() {
        useAcceptRanges = getBooleanInitParameter(USE_ACCEPT_RANGES_HEADER_INIT_PARAM, DEFAULT_USE_ACCEPT_RANGES);
    }
    private void initBinariesCache() {
        binariesCacheComponentName = getInitParameter(CACHE_NAME_INIT_PARAM, "defaultBinariesCache");
        HstCache cache = HstServices.getComponentManager().getComponent(binariesCacheComponentName);
        binariesCache = new BinariesCache(cache);
        binariesCache.setMaxObjectSizeBytes(getLongInitParameter(CACHE_MAX_OBJECT_SIZE_BYTES_INIT_PARAM,
                BinariesCache.DEFAULT_MAX_OBJECT_SIZE_BYTES));
        binariesCache.setValidityCheckIntervalMillis(getLongInitParameter(VALIDITY_CHECK_INTERVAL_SECONDS,
                BinariesCache.DEFAULT_VALIDITY_CHECK_INTERVAL_MILLIS/ 1000 ));
    }

    protected String getInitParameter(String paramName, String defaultValue) {
        return ServletConfigUtils.getInitParameter(getServletConfig(), null, paramName, defaultValue);
    }

    protected boolean getBooleanInitParameter(String paramName, boolean defaultValue) {
        String value = ServletConfigUtils.getInitParameter(getServletConfig(), null, paramName, null);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }
    
    protected long getLongInitParameter(String paramName, long defaultValue) {
        String value = ServletConfigUtils.getInitParameter(getServletConfig(), null, paramName, null);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                log.warn("Expecting type long for parameter '{}', got '{}'", paramName, value);
            }
        }
        return defaultValue;
    }

}
