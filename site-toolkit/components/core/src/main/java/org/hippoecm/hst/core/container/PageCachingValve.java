/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.io.Serializable;
import java.util.concurrent.Callable;

import org.hippoecm.hst.cache.CacheElement;
import org.hippoecm.hst.cache.HstCache;
import org.hippoecm.hst.cache.HstCacheException;
import org.hippoecm.hst.cache.HstPageInfo;
import org.hippoecm.hst.cache.UncacheableHstPageInfo;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.diagnosis.HDC;
import org.hippoecm.hst.diagnosis.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageCachingValve extends AbstractBaseOrderableValve {

    private static final String REQUEST_INFO_CACHE_KEY_ATTR_NAME = PageCachingValve.class.getName() + ".reqInfo";

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

    private RequestInfoCacheKeyFragmentCreator requestInfoCacheKeyFragmentCreator;

    public PageCachingValve() {
        this(new DefaultRequestInfoCacheKeyFragmentCreator());
    }

    public PageCachingValve(RequestInfoCacheKeyFragmentCreator requestInfoCacheKeyFragmentCreator) {
        this.requestInfoCacheKeyFragmentCreator = requestInfoCacheKeyFragmentCreator;
    }

    public void setPageCache(HstCache pageCache) {
        this.pageCache = pageCache;
    }

    @Override
    public void invoke(ValveContext context) throws ContainerException {

        HstRequestContext requestContext = context.getRequestContext();

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

            HstPageInfo pageInfo = getPageInfoFromCacheOrBuild(context);

            if (pageInfo == null) {
                throw new ContainerException("PageInfo null. ");
            }

            requestContext.getServletRequest().setAttribute(PageInfoRenderingValve.PAGE_INFO, pageInfo);
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

    private void appendRequestInfoToCacheKey(final ValveContext context) {
        Serializable requestInfo = requestInfoCacheKeyFragmentCreator.create(context.getRequestContext());
        final PageCacheKey pageCacheKey = context.getPageCacheContext().getPageCacheKey();
        pageCacheKey.setAttribute(REQUEST_INFO_CACHE_KEY_ATTR_NAME, requestInfo);
    }

    /**
     * Build page info either using the cache or building the page directly.
     */
    protected HstPageInfo getPageInfoFromCacheOrBuild(final ValveContext context) throws Exception {
        final PageCacheKey keyPage = context.getPageCacheContext().getPageCacheKey();
        CacheElement element = pageCache.get(keyPage, new Callable<CacheElement>() {
            @Override
            public CacheElement call() throws Exception {
                HstPageInfo pageInfo = createHstPageInfoByInvokingNextValve(context, pageCache.getTimeToLiveSeconds());

                if (pageInfo.isOk()) {
                    if (pageInfo.isNoCachePresentOrExpiresImmediately()) {
                        log.debug("Creating uncacheable element for page '{}' with keyPage '{}' because it contains no cache header or expires immediately.",
                                context.getServletRequest().getRequestURI(), keyPage);
                        return pageCache.createUncacheableElement(keyPage, pageInfo);
                    } else if (pageInfo instanceof UncacheableHstPageInfo) {
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

        return (HstPageInfo) element.getContent();
    }

}
