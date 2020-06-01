/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.core.container;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.google.common.cache.CacheBuilder;

import org.easymock.EasyMock;
import org.hippoecm.hst.cache.CacheElement;
import org.hippoecm.hst.cache.HstCache;
import org.hippoecm.hst.cache.webfiles.CacheableWebFile;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.webfiles.WhitelistReader;
import org.hippoecm.hst.mock.core.component.MockHstRequest;
import org.hippoecm.hst.mock.core.component.MockHstResponse;
import org.hippoecm.hst.mock.core.component.MockValveContext;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.hippoecm.hst.mock.core.request.MockResolvedSiteMapItem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.webfiles.Binary;
import org.onehippo.cms7.services.webfiles.WebFile;
import org.onehippo.cms7.services.webfiles.WebFileBundle;
import org.onehippo.cms7.services.webfiles.WebFileException;
import org.onehippo.cms7.services.webfiles.WebFileNotFoundException;
import org.onehippo.cms7.services.webfiles.WebFilesService;
import org.onehippo.repository.mock.MockNode;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hippoecm.hst.core.container.WebFileValve.WHITE_LIST_CONTENT_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestWebFileValve {

    public static final String STYLE_CSS_CONTENTS = "/* example css */";
    private MockHstRequest request;
    private MockHstRequestContext requestContext;
    private MockHstResponse response;
    private MockValveContext valveContext;
    private WebFileValve valve;
    private HstCache cache;
    private WebFilesService webFilesService;
    private WebFileBundle webFileBundle;
    private WebFile webFile;

    @Before
    public void setUp() throws Exception {
        request = new MockHstRequest();

        requestContext = new MockHstRequestContext();
        final Session session = MockNode.root().getSession();
        requestContext.setSession(session);
        mockContextPath("site", requestContext);
        request.setRequestContext(requestContext);

        response = new MockHstResponse();
        valveContext = new MockValveContext(request, response);
        valve = new WebFileValve();

        cache = EasyMock.createNiceMock(HstCache.class);
        valve.setWebFileCache(cache);
        valve.setNegativeWebFileCacheBuilder(CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).expireAfterWrite(1, TimeUnit.HOURS));

        webFilesService = EasyMock.createMock(WebFilesService.class);
        webFileBundle = EasyMock.createMock(WebFileBundle.class);
        webFile = EasyMock.createMock(WebFile.class);
        expect(webFilesService.getJcrWebFileBundle(eq(session), eq("site"))).andReturn(webFileBundle).anyTimes();
        HippoServiceRegistry.register(webFilesService, WebFilesService.class);
    }

    private void replayMocks() {
        replay(cache, webFilesService, webFileBundle, webFile);
    }

    private static void mockContextPath(final String contextPath, final MockHstRequestContext requestContext) throws RepositoryException {
        final ResolvedMount resolvedMount = EasyMock.createMock(ResolvedMount.class);
        final Mount mount = EasyMock.createMock(Mount.class);
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();
        expect(mount.getContextPath()).andReturn(contextPath).anyTimes();
        replay(resolvedMount, mount);
        requestContext.setResolvedMount(resolvedMount);
    }

    private static void mockResolvedSiteMapItem(final String relativeContentPath, final String version, final MockHstRequestContext requestContext) throws RepositoryException {
        final MockResolvedSiteMapItem resolvedSiteMapItem = new MockResolvedSiteMapItem();
        resolvedSiteMapItem.setRelativeContentPath(relativeContentPath);
        resolvedSiteMapItem.addParameter("version", version);
        requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
    }

    private static WebFile styleCss() {
        final WebFile styleCss = EasyMock.createMock(WebFile.class);
        expect(styleCss.getPath()).andReturn("/css/style.css");
        expect(styleCss.getName()).andReturn("style.css");
        expect(styleCss.getMimeType()).andReturn("text/css");
        expect(styleCss.getEncoding()).andReturn("UTF-8");
        expect(styleCss.getLastModified()).andReturn(Calendar.getInstance());

        final Binary binary = EasyMock.createNiceMock(Binary.class);
        byte[] data = STYLE_CSS_CONTENTS.getBytes();
        expect(binary.getSize()).andReturn((long)data.length);
        expect(binary.getStream()).andReturn(new ByteArrayInputStream(data));
        expect(styleCss.getBinary()).andReturn(binary);

        replay(styleCss, binary);

        return styleCss;
    }

    private static WebFile fooCss() {
        final WebFile fooCss = EasyMock.createMock(WebFile.class);
        expect(fooCss.getPath()).andReturn("/foo.css");
        expect(fooCss.getName()).andReturn("foo.css");
        expect(fooCss.getMimeType()).andReturn("text/css");
        expect(fooCss.getEncoding()).andReturn("UTF-8");
        expect(fooCss.getLastModified()).andReturn(Calendar.getInstance());

        final Binary binary = EasyMock.createNiceMock(Binary.class);
        byte[] data = STYLE_CSS_CONTENTS.getBytes();
        expect(binary.getSize()).andReturn((long)data.length);
        expect(binary.getStream()).andReturn(new ByteArrayInputStream(data));
        expect(fooCss.getBinary()).andReturn(binary);

        replay(fooCss, binary);

        return fooCss;
    }

    private static WebFile whitelist() {
        final WebFile whitelist = EasyMock.createMock(WebFile.class);
        expect(whitelist.getPath()).andReturn("/hst-whitelist.txt");
        expect(whitelist.getName()).andReturn("hst-whitelist.txt");
        expect(whitelist.getMimeType()).andReturn("text/plain");
        expect(whitelist.getEncoding()).andReturn("UTF-8");
        expect(whitelist.getLastModified()).andReturn(Calendar.getInstance());

        final Binary binary = new TestBinary(TestWebFileValve.class.getResourceAsStream("TestWebFileValveWhitelist.txt"));
        expect(whitelist.getBinary()).andReturn(binary);
        replay(whitelist);
        return whitelist;
    }

    private static WebFile emptyWhitelist() {
        final WebFile whitelist = EasyMock.createMock(WebFile.class);
        expect(whitelist.getPath()).andReturn("/hst-whitelist.txt");
        expect(whitelist.getName()).andReturn("hst-whitelist.txt");
        expect(whitelist.getMimeType()).andReturn("text/plain");
        expect(whitelist.getEncoding()).andReturn("UTF-8");
        expect(whitelist.getLastModified()).andReturn(Calendar.getInstance());

        final Binary binary = new TestBinary(TestWebFileValve.class.getResourceAsStream("TestWebFileValveEmptyWhitelist.txt"));
        expect(whitelist.getBinary()).andReturn(binary);
        replay(whitelist);
        return whitelist;
    }


    @After
    public void tearDown() throws Exception {
        HippoServiceRegistry.unregister(webFilesService, WebFilesService.class);
    }

    @Test
    public void uncached_web_resource_from_workspace_is_cached() throws Exception {

        mockResolvedSiteMapItem("css/style.css", "bundleVersion", requestContext);
        expect(webFileBundle.getAntiCacheValue()).andReturn("bundleVersion").anyTimes();
        expect(webFileBundle.get("/hst-whitelist.txt")).andReturn(whitelist());
        expect(webFileBundle.get("/css/style.css")).andReturn(styleCss());
        expect(cache.createElement(anyObject(), anyObject())).andReturn(EasyMock.createMock(CacheElement.class));
        replayMocks();

        valve.invoke(valveContext);

        verify(cache);
        assertCssIsWritten(styleCss(), true);
        assertTrue("Next valve should have been invoked", valveContext.isNextValveInvoked());
    }

    @Test
    public void request_for_version_other_than_anti_cache_value_returns_404() throws Exception {
        mockResolvedSiteMapItem("css/style.css", "bundleVersion", requestContext);
        expect(webFileBundle.getAntiCacheValue()).andReturn("antiCacheValueThatIsNotEqualToTheBundleVersion").anyTimes();
        // make sure /hst-whitelist.txt can be read
        expect(webFileBundle.get("/hst-whitelist.txt", "antiCacheValueThatIsNotEqualToTheBundleVersion")).andReturn(whitelist());
        expect(webFileBundle.get("/css/style.css", "bundleVersion")).andReturn(styleCss());
        expect(cache.createElement(anyObject(), anyObject())).andReturn(EasyMock.createMock(CacheElement.class));
        replayMocks();

        valve.invoke(valveContext);

        verify(cache);

        assertEquals("nothing should be written to the response", "", response.getContentAsString());
        assertFalse("Next valve should not have been invoked", valveContext.isNextValveInvoked());
        assertEquals("response code", 404, response.getStatusCode());
    }

    @Test
    public void cached_web_resource_is_served_from_cache() throws Exception {
        mockResolvedSiteMapItem("css/style.css", "bundleVersion", requestContext);
        expect(webFileBundle.getAntiCacheValue()).andReturn("bundleVersion").anyTimes();
        expect(webFileBundle.get("/hst-whitelist.txt")).andReturn(whitelist());

        final CacheElement cacheElement = EasyMock.createMock(CacheElement.class);
        expect(cacheElement.getContent()).andReturn(new CacheableWebFile(styleCss(), "bundleVersion"));
        replay(cacheElement);
        final String cssCacheKey = "/webfiles/site/css/style.css";
        expect(cache.get(cssCacheKey)).andReturn(cacheElement);
        replayMocks();

        valve.invoke(valveContext);

        verify(cache);
        assertCssIsWritten(styleCss(), true);
        assertTrue("Next valve should have been invoked", valveContext.isNextValveInvoked());
    }

    @Test
    public void get_webfile_without_anti_cache_value_works_and_gets_cached_but_no_cache_headers() throws Exception {
        mockResolvedSiteMapItem("css/style.css", null, requestContext);
        expect(webFileBundle.get("/hst-whitelist.txt")).andReturn(whitelist());

        final CacheElement cacheElement = EasyMock.createMock(CacheElement.class);
        expect(cacheElement.getContent()).andReturn(new CacheableWebFile(styleCss(), null));
        replay(cacheElement);
        final String cssCacheKey = "/webfiles/site/css/style.css";
        expect(cache.get(cssCacheKey)).andReturn(cacheElement);
        replayMocks();

        valve.invoke(valveContext);

        verify(cache);
        assertCssIsWritten(styleCss(), false);
        assertTrue("Next valve should have been invoked", valveContext.isNextValveInvoked());
    }

    @Test
    public void error_while_caching_clears_lock_and_stops_valve_invocation() throws Exception {
        mockResolvedSiteMapItem("css/style.css", "bundleVersion", requestContext);
        expect(webFileBundle.getAntiCacheValue()).andReturn("bundleVersion").anyTimes();
        expect(webFileBundle.get("/css/style.css")).andReturn(styleCss());
        final String whitelistKey = "/webfiles/site" + WHITE_LIST_CONTENT_PATH;
        WhitelistReader whitelistReader = new WhitelistReader(TestWebFileValve.class.getResourceAsStream("TestWebFileValveWhitelist.txt"));
        CacheElement cacheElement = new CacheElement(){
            @Override
            public Object getKey() {
                return whitelistKey;
            }
            @Override
            public Object getContent() {
                return whitelistReader;
            }
            @Override
            public int getTimeToLiveSeconds() {
                return 0;
            }
            @Override
            public void setTimeToLiveSeconds(final int timeToLive) {
            }
            @Override
            public int getTimeToIdleSeconds() {
                return 0;
            }
            @Override
            public void setTimeToIdleSeconds(final int timeToIdle) {
            }
            @Override
            public boolean isEternal() {
                return false;
            }
            @Override
            public void setEternal(final boolean eternal) {
            }
            @Override
            public boolean isCacheable() {
                return false;
            }
        };
        expect(cache.get(eq(whitelistKey))).andReturn(cacheElement);
        expect(cache.createElement(anyObject(), anyObject())).andThrow(new RuntimeException("simulate error while caching"));
        expect(cache.createElement(anyObject(), eq(null))).andReturn(null);
        replayMocks();

        try {
            valve.invoke(valveContext);
        } catch (ContainerException expected) {
            verify(cache);
            assertEquals("nothing should be written to the response", "", response.getContentAsString());
            assertFalse("Next valve should not have been invoked", valveContext.isNextValveInvoked());
            return;
        }
        fail("expected a ContainerException");
    }

    @Test
    public void unknown_web_resource_clears_lock_and_sets_not_found_status() throws Exception {
        mockResolvedSiteMapItem("css/style.css", "bundleVersion", requestContext);
        expect(webFileBundle.getAntiCacheValue()).andReturn("bundleVersion").anyTimes();
        expect(webFileBundle.get("/hst-whitelist.txt")).andReturn(whitelist());
        expect(webFileBundle.get("/css/style.css")).andThrow(new WebFileException("simulate unknown web file"));
        expect(cache.createElement(anyObject(), eq(null))).andReturn(null);
        replayMocks();

        valve.invoke(valveContext);

        verify(cache);
        assertEquals("nothing should be written to the response", "", response.getContentAsString());
        assertFalse("Next valve should not have been invoked", valveContext.isNextValveInvoked());
        assertEquals("response code", 404, response.getStatusCode());
    }

    @Test
    public void non_whitelisted_web_resource_does_not_get_served() throws Exception {
        mockResolvedSiteMapItem("css/style.css", "bundleVersion", requestContext);
        expect(webFileBundle.getAntiCacheValue()).andReturn("bundleVersion").anyTimes();
        expect(webFileBundle.get("/hst-whitelist.txt")).andReturn(emptyWhitelist());
        expect(webFileBundle.get("/css/style.css")).andReturn(styleCss());
        expect(cache.createElement(anyObject(), anyObject())).andReturn(EasyMock.createMock(CacheElement.class)).times(1);
        replayMocks();
        valve.invoke(valveContext);
        verify(cache);
        assertEquals("nothing should be written to the response", "", response.getContentAsString());
        assertFalse("Next valve should not have been invoked", valveContext.isNextValveInvoked());
        assertEquals("response code", 404, response.getStatusCode());
    }

    @Test
    public void whitelist_gets_cached() throws Exception {
        mockResolvedSiteMapItem("css/style.css", "bundleVersion", requestContext);
        expect(webFileBundle.getAntiCacheValue()).andReturn("bundleVersion").anyTimes();
        expect(webFileBundle.get("/hst-whitelist.txt")).andReturn(whitelist());
        expect(webFileBundle.get("/css/style.css")).andReturn(styleCss());

        final String whitelistKey = "/webfiles/site" + WHITE_LIST_CONTENT_PATH;
        final String cssStyleKey =  "/webfiles/site/css/style.css";
        expect(cache.createElement(eq(whitelistKey), anyObject())).andReturn(EasyMock.createMock(CacheElement.class));
        expect(cache.createElement(eq(cssStyleKey), anyObject())).andReturn(EasyMock.createMock(CacheElement.class));
        replayMocks();
        valve.invoke(valveContext);
        verify(webFileBundle, cache);
        assertCssIsWritten(styleCss(), true);
        assertTrue("Next valve should have been invoked", valveContext.isNextValveInvoked());
    }

    @Test
    public void whitelist_web_resource_file_instead_of_folder() throws Exception {
        mockResolvedSiteMapItem("foo.css", "bundleVersion", requestContext);
        expect(webFileBundle.getAntiCacheValue()).andReturn("bundleVersion").anyTimes();
        expect(webFileBundle.get("/hst-whitelist.txt")).andReturn(whitelist());
        expect(webFileBundle.get("/foo.css")).andReturn(fooCss());
        replayMocks();
        valve.invoke(valveContext);
        verify(webFileBundle, cache);
        assertCssIsWritten(fooCss(), true);
        assertTrue("Next valve should have been invoked", valveContext.isNextValveInvoked());
    }

    @Test
    public void whitelist_not_present_results_in_web_resource_not_being_served() throws Exception {
        mockResolvedSiteMapItem("css/style.css", "bundleVersion", requestContext);
        expect(webFileBundle.getAntiCacheValue()).andReturn("bundleVersion").anyTimes();
        expect(webFileBundle.get("/hst-whitelist.txt")).andThrow(new WebFileNotFoundException());
        expect(webFileBundle.get("/css/style.css")).andReturn(styleCss());
        expect(cache.createElement(anyObject(), anyObject())).andReturn(EasyMock.createMock(CacheElement.class)).times(1);
        replayMocks();
        valve.invoke(valveContext);
        verify(cache);
        assertEquals("nothing should be written to the response", "", response.getContentAsString());
        assertFalse("Next valve should not have been invoked", valveContext.isNextValveInvoked());
        assertEquals("response code", 404, response.getStatusCode());
    }

    @Test
    public void non_exising_webfile_ends_up_in_negative_cache() throws Exception {
        mockResolvedSiteMapItem("css/style.css", "bundleVersion", requestContext);
        expect(webFileBundle.getAntiCacheValue()).andReturn("bundleVersion").anyTimes();
        expect(webFileBundle.get("/hst-whitelist.txt")).andReturn(whitelist());
        expect(webFileBundle.get("/css/style.css")).andThrow(new WebFileNotFoundException("Could not find file"));
        replayMocks();

        valve.invoke(valveContext);

        assertEquals("nothing should be written to the response", "", response.getContentAsString());
        assertFalse("Next valve should not have been invoked", valveContext.isNextValveInvoked());
        assertEquals("response code", 404, response.getStatusCode());

        // assert negative cache has the entry
        final String cacheKey =  "/webfiles/site/css/style.css";
        assertNotNull(valve.negativeWebFileCache.getIfPresent(cacheKey));
    }

    private void assertCssIsWritten(final WebFile styleCss, final boolean withCacheHeaders) throws UnsupportedEncodingException {
        final Map<String, List<Object>> headers = response.getHeaders();
        assertEquals("Content-Length header", String.valueOf(styleCss.getBinary().getSize()), headers.get("Content-Length").get(0));
        assertEquals("Content type", styleCss.getMimeType(), response.getContentType());
        if (withCacheHeaders) {
            assertTrue("Expires in the future", ((Date)headers.get("Expires").get(0)).after(Calendar.getInstance().getTime()));
            assertEquals("Cache-Control header", "max-age=31536000", headers.get("Cache-Control").get(0));
        }
        assertEquals("written web file", STYLE_CSS_CONTENTS, response.getContentAsString());
    }

    private static class TestBinary implements Binary {

        final InputStream stream;

        public TestBinary(final InputStream stream) {
            this.stream = stream;
        }

        @Override
        public InputStream getStream() {
            return stream;
        }

        @Override
        public long getSize() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void dispose() {
            throw new UnsupportedOperationException();
        }
    }
}