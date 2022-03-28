/*
 *  Copyright 2014-2022 Bloomreach
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
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.cache.CacheElement;
import org.hippoecm.hst.cache.HstCache;
import org.hippoecm.hst.cache.webfiles.CacheableWebFile;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.webfiles.AllowlistReader;
import org.hippoecm.hst.util.WebFileUtils;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.webfiles.Binary;
import org.onehippo.cms7.services.webfiles.WebFile;
import org.onehippo.cms7.services.webfiles.WebFileBundle;
import org.onehippo.cms7.services.webfiles.WebFileException;
import org.onehippo.cms7.services.webfiles.WebFileNotFoundException;
import org.onehippo.cms7.services.webfiles.WebFileTagNotFoundException;
import org.onehippo.cms7.services.webfiles.WebFilesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.cms7.utilities.servlet.ResourceServlet.CACHE_CONTROL_PUBLIC;
import static org.onehippo.cms7.utilities.servlet.ResourceServlet.CACHE_CONTROL_PRIVATE;
import static org.onehippo.cms7.utilities.servlet.ResourceServlet.HTTP_CACHE_CONTROL_HEADER;
import static org.onehippo.cms7.utilities.servlet.ResourceServlet.HTTP_EXPIRES_HEADER;

public class WebFileValve extends AbstractBaseOrderableValve {

    private static final Logger log = LoggerFactory.getLogger(WebFileValve.class);
    private static final long ONE_YEAR_SECONDS = TimeUnit.SECONDS.convert(365L, TimeUnit.DAYS);
    private static final long ONE_YEAR_MILLISECONDS = TimeUnit.MILLISECONDS.convert(ONE_YEAR_SECONDS, TimeUnit.SECONDS);

    static final String ALLOWLIST_CONTENT_PATH = "/hst-allowlist.txt";

    private HstCache webFileCache;

    Cache negativeWebFileCache;

    public void setWebFileCache(final HstCache webFileCache) {
        this.webFileCache = webFileCache;
    }

    public void setNegativeWebFileCacheBuilder(final CacheBuilder negativeWebFileCacheBuilder) {
        this.negativeWebFileCache = negativeWebFileCacheBuilder.build();
    }

    @Override
    public void invoke(final ValveContext context) throws ContainerException {
        final HstRequestContext requestContext = context.getRequestContext();
        final HttpServletResponse response = context.getServletResponse();

        try {
            final CacheableWebFile webFile = getWebFile(requestContext);
            // only set client cache headers in case version was present in request
            final boolean includeCacheHeaders = webFile.getVersion() != null;
            setHeaders(response, webFile, includeCacheHeaders);
            writeWebFile(response, webFile);
        } catch (WebFileException e) {
            final HttpServletRequest request = context.getServletRequest();
            if (log.isDebugEnabled()) {
                log.info("Cannot serve binary '{}'", request.getPathInfo(), e);
            } else {
                log.info("Cannot serve binary '{}', cause: {}", request.getPathInfo(), e.toString());
            }
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        } catch (Exception e) {
            throw new ContainerException(e);
        }

        context.invokeNext();
    }

    private CacheableWebFile getWebFile(final HstRequestContext requestContext) throws RepositoryException, ContainerException, IOException, WebFileException {
        final WebFilesService service = getWebFilesService();

        final Session session = requestContext.getSession();
        final String bundleName = WebFileUtils.getBundleName(requestContext);
        log.debug("Trying to get web file bundle '{}' with session user '{}'", bundleName, session.getUserID());
        final WebFileBundle webFileBundle = service.getJcrWebFileBundle(session, bundleName);

        final String relativeContentPath = requestContext.getResolvedSiteMapItem().getRelativeContentPath();

        if (StringUtils.isEmpty(relativeContentPath)) {
            throw new WebFileException("Cannot serve web file for empty relative content path.");
        }

        boolean isAllowed = false;
        try {
            final Set<String> allowlist = getAllowlistReader(requestContext).getAllowlist();
            for (String allowedPath : allowlist) {
                if (relativeContentPath.startsWith(allowedPath) || relativeContentPath.equals(allowedPath)) {
                    isAllowed = true;
                    break;
                }
            }
        } catch (WebFileNotFoundException e) {
            isAllowed = false;
        }

        if (!isAllowed) {
            final String msg = String.format("Web file for relative content path '%s' is not allowed in '%s' for '%s' " +
                            "hence won't be served publicly. If it is required to be served publicly, add it to the file '%s'",
                    relativeContentPath, ALLOWLIST_CONTENT_PATH, bundleName, ALLOWLIST_CONTENT_PATH);
            throw new WebFileException(msg);
        }

        final String contentPath = "/" + relativeContentPath;
        final String version = getVersion(requestContext);

        final String cacheKey = WebFilesService.JCR_ROOT_PATH + "/" + bundleName + contentPath;

        if (negativeWebFileCache.getIfPresent(cacheKey) != null) {
            throw new WebFileNotFoundException("Negative cache contains requested web file.");
        }

        final CacheElement cacheElement = webFileCache.get(cacheKey);
        if (cacheElement == null) {
            return cacheWebFile(webFileBundle, contentPath, version, cacheKey);
        } else {
            return (CacheableWebFile)cacheElement.getContent();
        }
    }

    private AllowlistReader getAllowlistReader(final HstRequestContext requestContext)
            throws ContainerException, RepositoryException, WebFileException, IOException {
        final String bundleName = WebFileUtils.getBundleName(requestContext);

        final String version = getVersion(requestContext);
        final String cacheKey= WebFilesService.JCR_ROOT_PATH + "/" + bundleName + ALLOWLIST_CONTENT_PATH;

        try {
            final CacheElement cacheElement = webFileCache.get(cacheKey);
            if (cacheElement != null && cacheElement.getContent() instanceof AllowlistReader) {
                return (AllowlistReader)cacheElement.getContent();
            }
            final WebFilesService service = getWebFilesService();
            final Session session = requestContext.getSession();
            final WebFileBundle webFileBundle = service.getJcrWebFileBundle(session, bundleName);

            final WebFile webFile = getWebFileFromBundle(webFileBundle, ALLOWLIST_CONTENT_PATH, version);
            final AllowlistReader allowlistReader = new AllowlistReader(webFile.getBinary().getStream());
            final CacheElement allowlistReaderElement = webFileCache.createElement(cacheKey, allowlistReader);
            webFileCache.put(allowlistReaderElement);
            return allowlistReader;
        } catch (Exception e) {
            if (e instanceof WebFileNotFoundException) {
                log.info("No '{}' configured in web files for '{}'. All web files will be allowed. In the next PATCH version " +
                                "(HST 3.1.1) all web files will be blocked if there is no '{}' configured in web files.",
                        ALLOWLIST_CONTENT_PATH, bundleName, ALLOWLIST_CONTENT_PATH);
            }
            clearBlockingLock(cacheKey);
            throw e;
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

    private String getVersion(final HstRequestContext requestContext) throws WebFileException {
        final String version = requestContext.getResolvedSiteMapItem().getParameter("version");
        if (version == null) {
            log.info("Version is null. Serving latest WebFile version and don't set any cache headers on the response");
        }
        return version;
    }

    private CacheableWebFile cacheWebFile(final WebFileBundle webFileBundle, final String contentPath, final String version, final String cacheKey) throws IOException {
        try {
            final WebFile webFile = getWebFileFromBundle(webFileBundle, contentPath, version);
            final CacheableWebFile cacheableWebFile = new CacheableWebFile(webFile, version);
            final CacheElement element = webFileCache.createElement(cacheKey, cacheableWebFile);
            webFileCache.put(element);
            return cacheableWebFile;
        } catch (WebFileException e) {
            if (log.isDebugEnabled()) {
                log.info("Cannot serve binary for '{}'.", contentPath, e);
            } else {
                log.info("Cannot serve binary for '{}' : ", contentPath, e.toString());
            }
            negativeWebFileCache.put(cacheKey, Boolean.TRUE);
            clearBlockingLock(cacheKey);
            throw e;
         } catch (Exception e) {
            clearBlockingLock(cacheKey);
            throw e;
        }
    }

    private WebFile getWebFileFromBundle(final WebFileBundle webFileBundle, final String contentPath, final String version) throws IOException {
        if (version == null || version.equals(webFileBundle.getAntiCacheValue())) {
            return webFileBundle.get(contentPath);
        } else {
            throw new WebFileTagNotFoundException("Currently only version == null or version equal to anti cache value " +
                    "is supported");
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

    private static void setHeaders(final HttpServletResponse response, final WebFile webFile,
                                   final boolean includeCacheHeaders) throws RepositoryException {
        // no need for ETag since expires 1 year
        response.setHeader("Content-Length", Long.toString(webFile.getBinary().getSize()));
        response.setContentType(webFile.getMimeType());
        if (includeCacheHeaders) {
            // one year ahead max, see http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.21
            response.setDateHeader(HTTP_EXPIRES_HEADER, ONE_YEAR_MILLISECONDS + System.currentTimeMillis());
            HstRequestContext requestContext = RequestContextProvider.get();
            boolean isPreview = requestContext != null && requestContext.isPreview();
            response.setHeader(HTTP_CACHE_CONTROL_HEADER,
                    (isPreview ? CACHE_CONTROL_PRIVATE : CACHE_CONTROL_PUBLIC) +
                            ", max-age=" + ONE_YEAR_SECONDS);
        }
    }

    private static void writeWebFile(final HttpServletResponse response, final WebFile webFile) throws IOException {
        final Binary binary = webFile.getBinary();
        try (ServletOutputStream outputStream = response.getOutputStream()) {
            IOUtils.copy(binary.getStream(), outputStream);
            outputStream.flush();
        }
    }

    public void onEvent(final Event event) throws RepositoryException {
        String path = event.getPath();
        if (path != null) {
            // if the path is from either jcr:content child node or property of jcr:content child node
            // of the web resource node, then remove the descendant path to use the web resource node path
            // as the cache key by the event.
            final int offset = path.indexOf("/jcr:content");
            if (offset != -1) {
                path = path.substring(0, offset);
            }
            final boolean remove = webFileCache.remove(path);
            if (remove) {
                log.debug("Removed '{}' from webFileCache", path);
            }
            negativeWebFileCache.invalidate(path);
        }
    }
}
