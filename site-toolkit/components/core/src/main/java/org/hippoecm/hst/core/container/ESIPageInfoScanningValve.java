/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.cache.HstCache;
import org.hippoecm.hst.cache.HstPageInfo;
import org.hippoecm.hst.cache.esi.ESIFragmentInfo;
import org.hippoecm.hst.cache.esi.ESIHstPageInfo;
import org.hippoecm.hst.cache.esi.ESIPageScanner;
import org.hippoecm.hst.configuration.components.HstComponentInfo;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ESIPageInfoScanningValve extends AbstractBaseOrderableValve {

    private static final Logger log = LoggerFactory.getLogger(ESIPageInfoScanningValve.class);

    protected HstCache pageCache;

    private boolean esiFragmentsProcessing;

    private boolean esiFragmentsProcessingOnlyForAsyncComponents;

    private ESIPageScanner esiPageScanner;

    public ESIPageInfoScanningValve(ESIPageScanner esiPageScanner) {
        this.esiPageScanner = esiPageScanner;
    }

    public void setPageCache(HstCache pageCache) {
        this.pageCache = pageCache;
    }

    public boolean isEsiFragmentsProcessing() {
        return esiFragmentsProcessing;
    }

    public void setEsiFragmentsProcessing(boolean esiFragmentsProcessing) {
        this.esiFragmentsProcessing = esiFragmentsProcessing;
    }

    public boolean isEsiFragmentsProcessingOnlyForAsyncComponents() {
        return esiFragmentsProcessingOnlyForAsyncComponents;
    }

    public void setEsiFragmentsProcessingOnlyForAsyncComponents(boolean esiFragmentsProcessingOnlyForAsyncComponents) {
        this.esiFragmentsProcessingOnlyForAsyncComponents = esiFragmentsProcessingOnlyForAsyncComponents;
    }

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        if (!isEsiFragmentsProcessing()) {
            context.invokeNext();
            return;
        }

        if (isEsiFragmentsProcessingOnlyForAsyncComponents()) {
            HstComponentWindow rootRenderingWindow = context.getRootComponentRenderingWindow();

            if (rootRenderingWindow == null) {
                rootRenderingWindow = context.getRootComponentWindow();
            }

            if (!hasAnyEsiModeAsyncComponentWindow(rootRenderingWindow)) {
                context.invokeNext();
                return;
            }
        }

        HstRequestContext requestContext = context.getRequestContext();
        HttpServletRequest request = requestContext.getServletRequest();

        HstPageInfo pageInfo = null;

        try {
            boolean requestCacheable = Boolean.TRUE.equals(request.getAttribute(PageInfoRenderingValve.PAGE_INFO_CACHEABLE)) || isRequestCacheable(context);

            if (requestCacheable) {
                // pageInfo request attribute will be set in the PageCachingValve..
                context.invokeNext();
                pageInfo = (HstPageInfo) request.getAttribute(PageInfoRenderingValve.PAGE_INFO);

                if (pageInfo == null) {
                    log.warn("HstPageInfo was not found. It was supposed to be set by PageCachingValve.");
                }
            } else {
                // invoke next valve with response wrapper and create HstPageInfo
                pageInfo = createHstPageInfoByInvokingNextValve(context, pageCache.getTimeToLiveSeconds());
                request.setAttribute(PageInfoRenderingValve.PAGE_INFO, pageInfo);
            }

            if (pageInfo != null) {
                if (!(pageInfo instanceof ESIHstPageInfo)) {
                    String contentType = pageInfo.getContentType();

                    if (StringUtils.startsWith(contentType, "text/")) {
                        ESIHstPageInfo esiPageInfo = convertHstPageInfoToESIPageInfo(pageInfo);
                        request.setAttribute(PageInfoRenderingValve.PAGE_INFO, esiPageInfo);

                        // replace the current cache in the pageCache by converted ESIHstPageInfo.
                        if (requestCacheable) {
                            PageCacheKey pageCacheKey = context.getPageCacheContext().getPageCacheKey();

                            if (pageInfo.isNoCachePresentOrExpiresImmediately() || pageInfo instanceof UncacheableHstPageInfo) {
                                pageCache.put(pageCache.createUncacheableElement(pageCacheKey, esiPageInfo));
                            } else {
                                pageCache.put(pageCache.createElement(pageCacheKey, esiPageInfo));
                            }
                        }
                    } else {
                        log.debug("The cached pageInfo is not of text content ('{}'). So skipping to convert to ESIHstPageInfo.", contentType);
                    }
                }
            }
        } catch (Exception e) {
            throw new ContainerException(e);
        }
    }

    protected ESIHstPageInfo convertHstPageInfoToESIPageInfo(HstPageInfo pageInfo) throws Exception {
        String characterEncoding = StringUtils.defaultIfEmpty(pageInfo.getCharacterEncoding(), "UTF-8");
        String bodyContent = new String(pageInfo.getUngzippedBody(), characterEncoding);
        ESIHstPageInfo esiPageInfo = new ESIHstPageInfo(pageInfo.getStatusCode(), pageInfo.getContentType(),
                pageInfo.getSerializableCookies(), bodyContent, pageInfo.getCharacterEncoding(),
                pageInfo.getTimeToLiveSeconds(), pageInfo.getHeaders());

        List<ESIFragmentInfo> fragmentInfos = esiPageScanner.scanFragmentInfos(bodyContent);

        if (!fragmentInfos.isEmpty()) {
            esiPageInfo.addAllFragmentInfos(fragmentInfos);
        }

        return esiPageInfo;
    }


    protected boolean hasAnyEsiModeAsyncComponentWindow(final HstComponentWindow window) {
        if (window == null) {
            return false;
        }

        List<HstComponentWindow> asyncWindowList = new ArrayList<HstComponentWindow>();
        fillFirstEsiModeAsyncComponentWindow(window, asyncWindowList);

        return !asyncWindowList.isEmpty();
    }

    protected boolean isEsiModeAsyncComponentWindow(HstComponentWindow window) {
        HstComponentInfo compInfo = window.getComponentInfo();

        if (!compInfo.isAsync()) {
            return false;
        }

        String asyncMode = StringUtils.defaultIfEmpty(compInfo.getAsyncMode(), defaultAsynchronousComponentWindowRenderingMode);
        return StringUtils.equals("esi", asyncMode);
    }

    private void fillFirstEsiModeAsyncComponentWindow(final HstComponentWindow window, List<HstComponentWindow> asyncWindowsList) {
        if (isEsiModeAsyncComponentWindow(window)) {
            asyncWindowsList.add(window);
        } else {
            Map<String, HstComponentWindow> childWindowsMap = window.getChildWindowMap();

            if (childWindowsMap != null) {
                for (HstComponentWindow childWindow : childWindowsMap.values()) {
                    fillFirstEsiModeAsyncComponentWindow(childWindow, asyncWindowsList);

                    if (!asyncWindowsList.isEmpty()) {
                        break;
                    }
                }
            }
        }
    }

}
