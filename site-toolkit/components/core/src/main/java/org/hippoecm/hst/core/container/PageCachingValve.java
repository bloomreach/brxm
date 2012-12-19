/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.hst.core.container;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.cache.CacheElement;
import org.hippoecm.hst.cache.HstCache;
import org.hippoecm.hst.cache.HstCacheException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.diagnosis.HDC;
import org.hippoecm.hst.diagnosis.Task;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.ehcache.constructs.web.AlreadyGzippedException;
import net.sf.ehcache.constructs.web.GenericResponseWrapper;
import net.sf.ehcache.constructs.web.Header;
import net.sf.ehcache.constructs.web.PageInfo;
import net.sf.ehcache.constructs.web.SerializableCookie;

public class PageCachingValve extends AbstractValve {
    
    private static final Logger log = LoggerFactory.getLogger(PageCachingValve.class);

    /**
     * The cache holding the web pages. Ensure that all threads for a given
     * cache name are using the same instance of this.
     */
    //protected BlockingCache blockingCache;

    /**
     * The cache holding the web pages. Ensure that all threads for a given
     * cache name are using the same instance of this.
     */
    protected HstCache pageCache;

    public void setPageCache(HstCache pageCache) {
        this.pageCache = pageCache;
    }


    @Override
    public void invoke(ValveContext context) throws ContainerException
    {

        if (!isRequestCacheable(context)) {
            context.invokeNext();
            return;
        }

        Task task = null;
        try {
            if (HDC.isStarted()) {
                task = HDC.getCurrentTask().startSubtask("PageCachingValve");
            }
            appendRequestInfoToCacheKey(context);
            PageInfo pageInfo = getPageInfoFromCacheOrBuild(context);
            if (pageInfo == null) {
                throw new ContainerException("PageInfo null. ");
            }
            if (pageInfo instanceof ForwardPlaceHolderPageInfo) {
                log.debug("Page '{}' is being forwarded internally.", context.getServletRequest().getRequestURI());
                // we need to set the forwarded info again on the request as it might now be not yet set because 
                // ForwardPlaceHolderPageInfo might come from the cache
                String forwardPathInfo = ((ForwardPlaceHolderPageInfo)pageInfo).forwardPathInfo;
                context.getServletRequest().setAttribute(ContainerConstants.HST_FORWARD_PATH_INFO, forwardPathInfo);
                return;
            }
            if (pageInfo.isOk()) {
                HttpServletResponse response = context.getServletResponse();
                if (response.isCommitted()) {
                    throw new ContainerException("Response already committed after doing buildPageInfo"
                                    + " but before writing response from PageInfo.");
                }
                writeResponse(response, pageInfo);
            }
        } catch (HstCacheException e) {
            throw new ContainerException("Cache exception : ", e);
        } catch (Exception e) {
            throw new ContainerException(e);
        } finally {
            if (task != null) {
                task.stop();
            }
        }

    }

    private boolean isRequestCacheable(final ValveContext context) throws ContainerException {
        if (!context.getPageCacheContext().isCacheable()) {
            log.debug("Request '{}' is not cacheable because PageCacheContext is marked to not cache this request: {} ", context.getServletRequest().getRequestURI(), context.getPageCacheContext().getReasonsUncacheable());
            return false;
        }

        HstRequestContext requestContext = context.getRequestContext();
        if (requestContext.isCmsRequest()) {
            log.debug("Request '{}' is not cacheable because request is cms request", context.getServletRequest().getRequestURI());
            return false;
        }
        if (requestContext.getResolvedMount().getMount().isPreview()) {
            log.debug("Request '{}' is not cacheable because request is preview request", context.getServletRequest().getRequestURI());
            return false;
        }

        ResolvedSiteMapItem resolvedSitemapItem = requestContext.getResolvedSiteMapItem();
        if (resolvedSitemapItem != null) {
            if(!isSiteMapItemAndComponentConfigCacheable(resolvedSitemapItem, context)) {
                return false;
            }
        } else if (!requestContext.getResolvedMount().getMount().isCacheable()) {
            log.debug("Request '{}' is not cacheable because mount '{}' is not cacheable.", context.getServletRequest().getRequestURI(),
                    requestContext.getResolvedMount().getMount().getName());
            return false;
        }
        return true;
    }

    private boolean isSiteMapItemAndComponentConfigCacheable(final ResolvedSiteMapItem resolvedSitemapItem,
                                                             final ValveContext context) throws ContainerException {
        if (!resolvedSitemapItem.getHstSiteMapItem().isCacheable()) {
            log.debug("Request '{}' is not cacheable because hst sitemapitem '{}' is not cacheable.", context.getServletRequest().getRequestURI(),
                    resolvedSitemapItem.getHstSiteMapItem().getId());
            return false;
        }


        // check whether component rendering is true: For component rendering, we need to check whether the specific sub
        // component (tree) is cacheable
        String componentRenderingWindowReferenceNamespace = context.getRequestContext().getBaseURL().getComponentRenderingWindowReferenceNamespace();
        if (componentRenderingWindowReferenceNamespace != null) {
            HstComponentWindow window = findComponentWindow(context.getRootComponentWindow(), componentRenderingWindowReferenceNamespace);
            if (window == null) {
                // incorrect request.
                return false;
            }
            if (window.getComponentInfo().isStandalone()) {
                return window.getComponentInfo().isCompositeCacheable();
            }
            // normally component rendering is standalone, however, if not standalone, than also the
            // ancestors need to be cacheable because all components will be rendered
            if (!resolvedSitemapItem.getHstComponentConfiguration().isCompositeCacheable()) {
                log.debug("Request '{}' is not cacheable because hst component '{}' is not cacheable.", context.getServletRequest().getRequestURI(),
                        resolvedSitemapItem.getHstComponentConfiguration().getId());
                return false;
            }
        } else if (!resolvedSitemapItem.getHstComponentConfiguration().isCompositeCacheable()) {
            log.debug("Request '{}' is not cacheable because hst component '{}' is not cacheable.", context.getServletRequest().getRequestURI(),
                    resolvedSitemapItem.getHstComponentConfiguration().getId());
            return false;
        }

        return true;
    }

    private void appendRequestInfoToCacheKey(final ValveContext context) {
        final PageCacheKey pageCacheKey = context.getPageCacheContext().getPageCacheKey();
        final HttpServletRequest request = context.getServletRequest();
        // Implementers should differentiate between GET and HEAD requests otherwise blank pages
        //  can result.
        pageCacheKey.append(request.getMethod());
        pageCacheKey.append(HstRequestUtils.getFarthestRequestHost(request));
        pageCacheKey.append(request.getRequestURI());
        pageCacheKey.append(request.getQueryString());

        // AFter an internal HST FORWARD, all the above parts are the same because same http request,
        // but the base URL pathInfo has been changed. Hence, we need to account for pathInfo
        // to make sure that in a FORWARDED request we do not get the same cached entry
        pageCacheKey.append(context.getRequestContext().getBaseURL().getPathInfo());
    }


    /**
     * Build page info either using the cache or building the page directly.
     */
    protected PageInfo getPageInfoFromCacheOrBuild(final ValveContext context) throws Exception {
        final PageCacheKey keyPage = context.getPageCacheContext().getPageCacheKey();
        CacheElement element = pageCache.get(keyPage, new Callable<CacheElement>() {
            @Override
            public CacheElement call() throws Exception {
                PageInfo pageInfo = buildPageInfo(context);
                if (pageInfo.isOk()) {
                    if (isNoCacheHeaderPresent(pageInfo, context)) {
                        log.debug("Creating uncacheable element for page '{}' with keyPage '{}' because contains no cache header.",
                                context.getServletRequest().getRequestURI(), keyPage);
                        return pageCache.createUncacheableElement(keyPage, pageInfo);
                    } else {
                        log.debug("Caching request '{}' with keyPage '{}'", context.getServletRequest().getRequestURI(), keyPage);
                        return pageCache.createElement(keyPage, pageInfo);
                    }
                } else {
                    log.debug("PageInfo was not ok(200). Putting null into cache with keyPage {} ", keyPage);
                    return pageCache.createUncacheableElement(keyPage, pageInfo);
                }
            }
        });
        return (PageInfo) element.getContent();
    }

    private boolean isNoCacheHeaderPresent(final PageInfo pageInfo, final ValveContext context) {
        final List<Header<? extends Serializable>> headers = pageInfo.getHeaders();
        for (Header<? extends Serializable> header : headers) {
            if ("Pragma".equalsIgnoreCase(header.getName()) && "no-cache".equalsIgnoreCase(String.valueOf(header.getValue()))) {
                log.debug("We do not cache '{}' because header Pragma no-cache was set: ", context.getServletRequest().getRequestURI());
                return true;
            } else if ("Cache-Control".equalsIgnoreCase(header.getName()) && "no-cache".equalsIgnoreCase(String.valueOf(header.getValue()))) {
                log.debug("We do not cache '{}' because header Cache-Control no-cache was set: ", context.getServletRequest().getRequestURI());
                return true;
            } else if ("Expires".equals(header.getName())) {
                Serializable o = header.getValue();
                try {
                    long expires = Long.parseLong(String.valueOf(header.getValue()));
                    if (expires <= 0) {
                        log.debug("We do not cache '{}' because header Expires o or lower was set: ", context.getServletRequest().getRequestURI());
                        return true;
                    }
                } catch (NumberFormatException e) {
                    // could not parse expires to long, ignore
                }
            }
        }
        return false;
    }


    /**
     * Builds the PageInfo object by passing the request along the filter chain
     *
     * @param context
     * @return a Serializable value object for the page or page fragment
     * @throws Exception
     */
    protected PageInfo buildPageInfo(final ValveContext context)
            throws Exception {

        final HttpServletResponse nonWrappedReponse = context.getServletResponse();
        try {
            final ByteArrayOutputStream outstr = new ByteArrayOutputStream(4096);
            final GenericResponseWrapper responseWrapper = new GenericResponseWrapper(context.getServletResponse(), outstr);

            context.setHttpServletResponse(responseWrapper);
            context.invokeNext();
            responseWrapper.flush();

            long timeToLiveSeconds = pageCache.getTimeToLiveSeconds();

            // Return the page info
            boolean storeGzipped = false;
            if (context.getServletRequest().getAttribute(ContainerConstants.HST_FORWARD_PATH_INFO) != null) {
                // page is forwarded. We cache empty placeholder ForwardPageInfo
               String forwardPathInfo = (String) context.getServletRequest().getAttribute(ContainerConstants.HST_FORWARD_PATH_INFO);
               return new ForwardPlaceHolderPageInfo(forwardPathInfo);
            }
            return new PageInfo(responseWrapper.getStatus(), responseWrapper.getContentType(),
                    responseWrapper.getCookies(), outstr.toByteArray(), storeGzipped,
                    timeToLiveSeconds, responseWrapper.getAllHeaders());
        } finally {
            context.setHttpServletResponse(nonWrappedReponse);
        }
    }


    private class ForwardPlaceHolderPageInfo extends PageInfo {
        
        private final String forwardPathInfo;

        private ForwardPlaceHolderPageInfo(String forwardPathInfo) throws AlreadyGzippedException {
            super(HttpServletResponse.SC_OK, null, null, null, false, 0, null);
            this.forwardPathInfo = forwardPathInfo;
        }
    }

    /**
     * Writes the response from a PageInfo object.
     * <p/>
     * Headers are set last so that there is an opportunity to override
     *
     * @throws net.sf.ehcache.constructs.web.ResponseHeadersNotModifiableException
     *
     */
    protected void writeResponse(final HttpServletResponse response,
                                 final PageInfo pageInfo)  throws IOException
                                {

        setStatus(response, pageInfo);
        setContentType(response, pageInfo);
        setHeaders(pageInfo, response);
        writeContent(response, pageInfo);
    }

    /**
     * Set the content type.
     *
     * @param response
     * @param pageInfo
     */
    protected void setContentType(final HttpServletResponse response,
                                  final PageInfo pageInfo) {
        String contentType = pageInfo.getContentType();
        if (contentType != null && contentType.length() > 0) {
            response.setContentType(contentType);
        }
    }

    /**
     * Status code
     *
     * @param response
     * @param pageInfo
     */
    protected void setStatus(final HttpServletResponse response,
                             final PageInfo pageInfo) {
        response.setStatus(pageInfo.getStatusCode());
    }

    /**
     * Set the headers in the response object
     *
     * @param pageInfo
     * @param response
     */
    protected void setHeaders(final PageInfo pageInfo,
                              final HttpServletResponse response) {

        final Collection<Header<? extends Serializable>> headers = pageInfo.getHeaders();

        // Track which headers have been set so all headers of the same name
        // after the first are added
        final TreeSet<String> setHeaders = new TreeSet<String>(
                String.CASE_INSENSITIVE_ORDER);

        for (final Header<? extends Serializable> header : headers) {
            final String name = header.getName();

            switch (header.getType()) {
                case STRING:
                    if (setHeaders.contains(name)) {
                        response.addHeader(name, (String) header.getValue());
                    } else {
                        setHeaders.add(name);
                        response.setHeader(name, (String) header.getValue());
                    }
                    break;
                case DATE:
                    if (setHeaders.contains(name)) {
                        response.addDateHeader(name, (Long) header.getValue());
                    } else {
                        setHeaders.add(name);
                        response.setDateHeader(name, (Long) header.getValue());
                    }
                    break;
                case INT:
                    if (setHeaders.contains(name)) {
                        response.addIntHeader(name, (Integer) header.getValue());
                    } else {
                        setHeaders.add(name);
                        response.setIntHeader(name, (Integer) header.getValue());
                    }
                    break;
                default:
                    throw new IllegalArgumentException("No mapping for Header: "
                            + header);
            }
        }
    }

    protected void writeContent(final HttpServletResponse response, final PageInfo pageInfo)
            throws IOException {
        byte[] body;

        if (pageInfo.getStatusCode() == HttpServletResponse.SC_NO_CONTENT ||
                pageInfo.getStatusCode() == HttpServletResponse.SC_NOT_MODIFIED) {
            body = new byte[0];
        } else {
            body = pageInfo.getUngzippedBody();
        }
        response.setContentLength(body.length);
        OutputStream out = new BufferedOutputStream(response.getOutputStream());
        out.write(body);
        out.flush();
    }

}
