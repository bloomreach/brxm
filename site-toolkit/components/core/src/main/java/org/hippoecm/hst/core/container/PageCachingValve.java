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
    protected HstCache cache;

    /**
     * in case of blocking caches, we want to avoid reentry. That is done through this visitLog
     */
    private final VisitLog visitLog = new VisitLog();

    public void setCache(HstCache cache) {
        this.cache = cache;
    }


    @Override
    public void invoke(ValveContext context) throws ContainerException
    {

        if (!isRequestCachable(context)) {
            context.invokeNext();
            return;
        }

        Task task = null;
        try {
            if (HDC.isStarted()) {
                task = HDC.getCurrentTask().startSubtask("PageCachingValve");
            }
            appendRequestInfoToCacheKey(context);
            PageInfo pageInfo = buildPageInfo(context);
            if (pageInfo.isOk()) {
                HttpServletResponse response = context.getServletResponse();
                if (response.isCommitted()) {
                    throw new ContainerException("Response already committed after doing buildPage"
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

    private boolean isRequestCachable(final ValveContext context) throws ContainerException {
        if (!context.getPageCacheContext().isCachable()) {
            if (context.getPageCacheContext().getReasonsUncachable().isEmpty()) {
              log.debug("Request '{}' is not cachable because PageCacheContext is marked to not cache this request: ", context.getServletRequest().getRequestURI());
            } else {
              log.debug("Request '{}' is not cachable because PageCacheContext is marked to not cache this request: {} ", context.getServletRequest().getRequestURI(), context.getPageCacheContext().getReasonsUncachable());
            }
            return false;
        }

        HstRequestContext requestContext = context.getRequestContext();
        if (requestContext.isCmsRequest()) {
            log.debug("Request '{}' is not cachable because request is cms request", context.getServletRequest().getRequestURI());
            return false;
        }
        if (requestContext.getResolvedMount().getMount().isPreview()) {
            log.debug("Request '{}' is not cachable because request is preview request", context.getServletRequest().getRequestURI());
            return false;
        }

        ResolvedSiteMapItem resolvedSitemapItem = requestContext.getResolvedSiteMapItem();
        if (resolvedSitemapItem != null) {
            if(!isSiteMapItemAndComponentConfigCachable(resolvedSitemapItem, context)) {
                return false;
            }
        } else if (!requestContext.getResolvedMount().getMount().isCachable()) {
            log.debug("Request '{}' is not cachable because mount '{}' is not cachable.", context.getServletRequest().getRequestURI(),
                    requestContext.getResolvedMount().getMount().getName());
            return false;
        }
        return true;
    }

    private boolean isSiteMapItemAndComponentConfigCachable(final ResolvedSiteMapItem resolvedSitemapItem,final ValveContext context) {
        if (!resolvedSitemapItem.getHstComponentConfiguration().isCompositeCachable()) {
            log.debug("Request '{}' is not cachable because hst component '{}' is not cachable.", context.getServletRequest().getRequestURI(),
                    resolvedSitemapItem.getHstComponentConfiguration().getId());
            return false;
        }
        if (!resolvedSitemapItem.getHstSiteMapItem().isCachable()) {
            log.debug("Request '{}' is not cachable because hst sitemapitem '{}' is not cachable.", context.getServletRequest().getRequestURI(),
                    resolvedSitemapItem.getHstSiteMapItem().getId());
            return false;
        }
        return true;
    }

    private void appendRequestInfoToCacheKey(final ValveContext context) {
        final PageCacheKey pageCacheKey = context.getPageCacheContext().getPageCacheKey();
        final HttpServletRequest request = context.getServletRequest();
        // Implementers should differentiate between GET and HEAD requests otherwise blank pages
        //  can result.
        pageCacheKey.append(String.valueOf(request.getMethod()));
        pageCacheKey.append(HstRequestUtils.getFarthestRequestHost(request));
        pageCacheKey.append(request.getRequestURI());
        pageCacheKey.append(request.getQueryString());
    }


    /**
     * Build page info either using the cache or building the page directly.
     */
    protected PageInfo buildPageInfo(final ValveContext context) throws Exception {
        final PageCacheKey keyPage = context.getPageCacheContext().getPageCacheKey();
        try {
            checkNoReentry();
            CacheElement element = cache.get(keyPage, new Callable<CacheElement>() {
                @Override
                public CacheElement call() throws Exception {
                    PageInfo pageInfo = buildPage(context);
                    if (pageInfo.isOk()) {
                        if (isNoCacheHeaderPresent(pageInfo, context)) {
                            log.debug("Creating uncachable element for page '{}' with keyPage '{}' because contains no cache header.",
                                    context.getServletRequest().getRequestURI(), keyPage);
                            return cache.createUncachableElement(keyPage, pageInfo);
                        } else {
                            log.debug("Caching request '{}' with keyPage '{}'", context.getServletRequest().getRequestURI(), keyPage);
                            return cache.createElement(keyPage, pageInfo);
                        }
                    } else {
                        log.debug("PageInfo was not ok(200). Putting null into cache with keyPage {} ", keyPage);
                        return createEmptyElement(keyPage);
                    }
                }

                private CacheElement createEmptyElement(Object key) {
                    return cache.createElement(key, null);
                }
            });
            return (PageInfo) element.getContent();
        } finally {
            // all done building page, reset the re-entrant flag
            visitLog.clear();
        }
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
    protected PageInfo buildPage(final ValveContext context)
            throws Exception {

        final HttpServletResponse nonWrappedReponse = context.getServletResponse();
        try {
            final ByteArrayOutputStream outstr = new ByteArrayOutputStream();
            final GenericResponseWrapper responseWrapper = new GenericResponseWrapper(context.getServletResponse(), outstr);

            context.setHttpServletResponse(responseWrapper);
            context.invokeNext();
            responseWrapper.flush();

            long timeToLiveSeconds = cache.getTimeToLiveSeconds();

            // Return the page info
            boolean storeGzipped = false;
            return new PageInfo(responseWrapper.getStatus(), responseWrapper.getContentType(),
                    responseWrapper.getCookies(), outstr.toByteArray(), storeGzipped,
                    timeToLiveSeconds, responseWrapper.getAllHeaders());
        } finally {
            context.setHttpServletResponse(nonWrappedReponse);
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
        setCookies(pageInfo, response);
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
     * Set the serializableCookies
     *
     * @param pageInfo
     * @param response
     */
    protected void setCookies(final PageInfo pageInfo,
                              final HttpServletResponse response) {

        final Collection cookies = pageInfo.getSerializableCookies();
        for (Iterator iterator = cookies.iterator(); iterator.hasNext();) {
            final Cookie cookie = ((SerializableCookie) iterator.next())
                    .toCookie();
            response.addCookie(cookie);
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

    /**
     * Check that this caching filter is not being reentered by the same
     * recursively. Recursive calls will block indefinitely because the first
     * request has not yet unblocked the cache.
     * <p/>
     * This condition usually indicates an error in filter chaining or
     * RequestDispatcher dispatching.
     *
     * @throws ContainerException when visit log is already entered
     */
    protected void checkNoReentry()
            throws ContainerException {
        String filterName = getClass().getName();
        if (visitLog.hasVisited()) {
            throw new ContainerException("The request thread is attempting to reenter");
        } else {
            visitLog.markAsVisited();
        }
    }

    /**
     * threadlocal class to check for reentry
     *
     * @author hhuynh
     *
     */
    private static class VisitLog extends ThreadLocal<Boolean> {
        @Override
        protected Boolean initialValue() {
            return false;
        }

        public boolean hasVisited() {
            return get();
        }

        public void markAsVisited() {
            set(true);
        }

        public void clear() {
            super.remove();
        }
    }

}
