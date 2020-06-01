/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.utilities.servlet;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class ProxyFilterTest {


    private static MockFilterChain chain;

    static {
        // Retrieve test resources from file-system using 'classpath' protocol
        URL.setURLStreamHandlerFactory(protocol -> protocol.equals("classpath") ? ProxyFilterTest.ClassPathUrlHandler.INSTANCE : null);
    }

    @Test
    public void testProxy() throws Exception {
        final ProxyFilterTest.ProxyServlet proxyServlet = new ProxyFilterTest.ProxyServlet(
                "servlet/path@classpath:org/onehippo/cms7/utilities/servlet/test");
        final HttpServletResponse response = proxyServlet.get("/path/index.js");

        assertEquals(200, response.getStatus());
    }

    @Test
    public void testProxies() throws Exception {
        final ProxyFilterTest.ProxyServlet proxyServlet = new ProxyFilterTest.ProxyServlet(
                "servlet/path@classpath:org/onehippo/cms7/utilities/servlet/test," +
                        "servlet/path2@classpath:org/onehippo/cms7/utilities/servlet/test2");

        final HttpServletResponse response1 = proxyServlet.get("/path/index.js");
        assertEquals(200, response1.getStatus());

        final HttpServletResponse response2 = proxyServlet.get("/path2/onehippo.gif");
        assertEquals(200, response2.getStatus());
    }

    @Test
    public void testProxyFileNotFound() throws Exception {
        final ProxyFilterTest.ProxyServlet proxyServlet = new ProxyFilterTest.ProxyServlet(
                "servlet/path@classpath:org/onehippo/cms7/utilities/servlet/test");
        proxyServlet.get("/path/non-existing.js");
        assertNotNull(chain.getRequest());
    }

    @Test
    public void testProxyDisabled() throws Exception {
        final ProxyFilterTest.ProxyServlet proxyServlet = new ProxyFilterTest.ProxyServlet("");
        proxyServlet.get("/sub/index.js");
        assertNotNull(chain.getRequest());
    }

    private static class ClassPathUrlHandler extends URLStreamHandler {

        static final ProxyFilterTest.ClassPathUrlHandler INSTANCE = new ProxyFilterTest.ClassPathUrlHandler();

        @Override
        protected URLConnection openConnection(URL u) throws IOException {
            final ClassLoader classLoader = getClass().getClassLoader();
            final URL resourceUrl = classLoader.getResource(u.getPath());
            if (resourceUrl != null) {
                return resourceUrl.openConnection();
            }
            return null;
        }
    }

    private static class ProxyServlet {

        final ProxyFilter filter;

        ProxyServlet(final String resourceProxies) throws ServletException {
            System.setProperty("resource.proxies", resourceProxies);

            filter = new ProxyFilter();

            final MockFilterConfig config = new MockFilterConfig();

            config.addInitParameter("jarPathPrefix", "/servlet");
            filter.init(config);
        }

        HttpServletResponse get(final String path) throws ServletException, IOException {
            final MockHttpServletRequest request = new MockHttpServletRequest();
            final MockHttpServletResponse response = new MockHttpServletResponse();

            request.setMethod("GET");
            request.setPathInfo(path);
            chain = new MockFilterChain();
            filter.doFilter(request, response, chain);

            return response;
        }
    }

}