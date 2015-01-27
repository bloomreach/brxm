/*
 *  Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.hippoecm.hst.cache.CacheElement;
import org.hippoecm.hst.cache.HstCache;
import org.hippoecm.hst.cache.webfiles.CacheableWebFile;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.WebFileUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.webfiles.Binary;
import org.onehippo.cms7.services.webfiles.WebFile;
import org.onehippo.cms7.services.webfiles.WebFileBundle;
import org.onehippo.cms7.services.webfiles.WebFileException;
import org.onehippo.cms7.services.webfiles.WebFilesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebFileValve extends AbstractBaseOrderableValve {

    private static final Logger log = LoggerFactory.getLogger(WebFileValve.class);
    private static final long ONE_YEAR_SECONDS = TimeUnit.SECONDS.convert(365L, TimeUnit.DAYS);
    private static final long ONE_YEAR_MILLISECONDS = TimeUnit.MILLISECONDS.convert(ONE_YEAR_SECONDS, TimeUnit.SECONDS);

    private HstCache webFileCache;

    public void setWebFileCache(final HstCache webFileCache) {
        this.webFileCache = webFileCache;
    }

    @Override
    public void invoke(final ValveContext context) throws ContainerException {
        final HstRequestContext requestContext = context.getRequestContext();
        final HttpServletResponse response = context.getServletResponse();

        try {
            final WebFile webFile = getWebFile(requestContext);
            setHeaders(response, webFile);
            writeWebFile(response, webFile);
        } catch (WebFileException e) {
            final HttpServletRequest request = context.getServletRequest();
            if (log.isDebugEnabled()) {
                log.info("Cannot serve binary '{}'", request.getPathInfo(), e);
            } else {
                log.info("Cannot serve binary '{}', cause: '{}'", request.getPathInfo(), e.toString());
            }
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        } catch (Exception e) {
            throw new ContainerException(e);
        }

        context.invokeNext();
    }

    private WebFile getWebFile(final HstRequestContext requestContext) throws RepositoryException, ContainerException, IOException, WebFileException {
        final WebFilesService service = getWebFilesService();

        final Session session = requestContext.getSession();
        final String bundleName = WebFileUtils.getBundleName(requestContext);
        log.debug("Trying to get web file bundle '{}' with session user '{}'", bundleName, session.getUserID());
        final WebFileBundle webFileBundle = service.getJcrWebFileBundle(session, bundleName);

        final String contentPath = "/" + requestContext.getResolvedSiteMapItem().getRelativeContentPath();
        final String version = getVersion(requestContext, contentPath);
        final String cacheKey = createCacheKey(bundleName, contentPath, version);

        final CacheElement cacheElement = webFileCache.get(cacheKey);

        if (cacheElement == null) {
            return cacheWebFile(webFileBundle, contentPath, version, cacheKey);
        } else {
            return (CacheableWebFile) cacheElement.getContent();
        }
    }

    private WebFilesService getWebFilesService() throws ContainerException {
        WebFilesService service = HippoServiceRegistry.getService(WebFilesService.class);
        if (service == null) {
            log.error("Missing service for '{}'. Cannot serve web file.", WebFilesService.class.getName());
            throw new ContainerException("Missing service for '" + WebFilesService.class.getName() + "'. Cannot serve web file.");
        }
        return service;
    }

    private String getVersion(final HstRequestContext requestContext, final String contentPath) throws WebFileException {
        final String version = requestContext.getResolvedSiteMapItem().getParameter("version");
        if (version == null) {
            String msg = String.format("Cannot serve web file '%s' for mount '%s' because sitemap item" +
                            "'%s' does not contain version param.", contentPath,
                    requestContext.getResolvedMount().getMount(),
                    requestContext.getResolvedSiteMapItem().getHstSiteMapItem().getQualifiedId());
            throw new WebFileException(msg);
        }
        return version;
    }

    private String createCacheKey(final String bundleName, final String contentPath, final String version) {
        final StringBuilder cacheKeyBuilder = new StringBuilder(bundleName).append('\uFFFF');
        cacheKeyBuilder.append(version).append(contentPath);
        return cacheKeyBuilder.toString();
    }

    private WebFile cacheWebFile(final WebFileBundle webFileBundle, final String contentPath, final String version, final String cacheKey) throws IOException {
        try {
            final WebFile webFile = getWebFileFromBundle(webFileBundle, contentPath, version);
            final CacheableWebFile cacheableWebFile = new CacheableWebFile(webFile);
            final CacheElement element = webFileCache.createElement(cacheKey, cacheableWebFile);
            webFileCache.put(element);
            return cacheableWebFile;
        } catch (Exception e) {
            clearBlockingLock(cacheKey);
            throw e;
        }
    }

    private WebFile getWebFileFromBundle(final WebFileBundle webFileBundle, final String contentPath, final String version) throws IOException {
        if (version.equals(webFileBundle.getAntiCacheValue())) {
            return webFileBundle.get(contentPath);
        } else {
            return webFileBundle.get(contentPath, version);
        }
    }

    /**
     * Blocking EhCache creates a lock during a #get that returns null. Hence if after the get the creation for the web
     * resource fails, we need to clear the lock for the cacheKey
     */
    private void clearBlockingLock(final String cacheKey) {
        log.debug("Clear lock for {}", cacheKey);
        final CacheElement element = webFileCache.createElement(cacheKey, null);
        webFileCache.put(element);
    }

    private static void setHeaders(final HttpServletResponse response, final WebFile webFile) throws RepositoryException {
        // no need for ETag since expires 1 year
        response.setHeader("Content-Length", Long.toString(webFile.getBinary().getSize()));
        response.setContentType(webFile.getMimeType());
        // one year ahead max, see http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.21
        response.setDateHeader("Expires", ONE_YEAR_MILLISECONDS + System.currentTimeMillis());
        response.setHeader("Cache-Control", "max-age=" + ONE_YEAR_SECONDS);
    }

    private static void writeWebFile(final HttpServletResponse response, final WebFile webFile) throws IOException {
        final Binary binary = webFile.getBinary();
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            IOUtils.copy(binary.getStream(), outputStream);
            outputStream.flush();
        }
    }

}
