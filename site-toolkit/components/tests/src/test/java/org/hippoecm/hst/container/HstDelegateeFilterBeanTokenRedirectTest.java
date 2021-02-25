/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.container;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.SimpleCredentials;

import org.apache.commons.lang3.StringUtils;
import org.easymock.EasyMock;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.mock.core.request.MockCmsSessionContext;
import org.hippoecm.hst.platform.security.NimbusJwtTokenServiceImpl;
import org.hippoecm.hst.util.HstRequestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HstDelegateeFilterBeanTokenRedirectTest {

    private HstDelegateeFilterBean hstDelegateeFilterBean;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockCmsSessionContext cmsSessionContext;
    private NimbusJwtTokenServiceImpl jwtTokenService;
    private Mount mount;
    private VirtualHost virtualHost;

    @Before
    public void setUp() {

        hstDelegateeFilterBean = new HstDelegateeFilterBean();
        hstDelegateeFilterBean.setClusterNodeAffinityCookieName("serverid");
        hstDelegateeFilterBean.setClusterNodeAffinityHeaderName("Server-Id");
        hstDelegateeFilterBean.setClusterNodeAffinityQueryParam("server-id-param");
        hstDelegateeFilterBean.setJwtTokenParam("token");
        hstDelegateeFilterBean.setEndpointParam("endpoint");

        response = new MockHttpServletResponse();

        cmsSessionContext = new MockCmsSessionContext(new SimpleCredentials("dummy", "dummy".toCharArray()));
        cmsSessionContext.setId("dummy");

        jwtTokenService = new NimbusJwtTokenServiceImpl();
        jwtTokenService.init();

        virtualHost = EasyMock.createNiceMock(VirtualHost.class);
        expect(virtualHost.getHostName()).andStubReturn("localhost");

        mount = EasyMock.createNiceMock(Mount.class);

        expect(mount.getVirtualHost()).andStubReturn(virtualHost);
        expect(mount.getPageModelApi()).andStubReturn("resourceapi");
        expect(mount.isContextPathInUrl()).andStubReturn(true);
        expect(mount.getContextPath()).andStubReturn("/site");
        expect(mount.getScheme()).andStubReturn("http");
        expect(mount.getMountPath()).andStubReturn("");

        replay(mount, virtualHost);
    }


    private MockHttpServletRequest createRequest() {
        request = new MockHttpServletRequest();
        request.getSession().setAttribute(CmsSessionContext.SESSION_KEY, cmsSessionContext);
        request.setContextPath("/site");
        request.setRequestURI("/site");
        return request;
    }

    @After
    public void tearDown() {
        jwtTokenService.destroy();
    }

    @Test
    public void plain_redirect_UR() throws Exception {

        request = createRequest();

        hstDelegateeFilterBean.doRedirectPreviewURL(request, response, null, new HashMap<>(),
                "https://spa.example.com", mount);


        {
            final String redirectedUrl = response.getRedirectedUrl();
            final URI uri = new URI(redirectedUrl);
            assertEquals("spa.example.com", uri.getHost());
            assertEquals("", uri.getPath());
            final Map<String, String[]> parameters = HstRequestUtils.parseQueryString(uri, "UTF-8");
            assertEquals(2L, parameters.size());
            assertTrue(parameters.containsKey("token"));
            assertEquals("http://localhost/site/resourceapi", parameters.get("endpoint")[0]);
        }

    }

    @Test
    public void sub_mount_preview_URL() throws Exception {

        request = createRequest();
        request.setRequestURI("/site/submount");

        reset(mount);
        expect(mount.getVirtualHost()).andStubReturn(virtualHost);
        expect(mount.getPageModelApi()).andStubReturn("resourceapi");
        expect(mount.isContextPathInUrl()).andStubReturn(true);
        expect(mount.getContextPath()).andStubReturn("/site");
        expect(mount.getScheme()).andStubReturn("http");
        expect(mount.getMountPath()).andStubReturn("/submount");
        replay(mount);

        hstDelegateeFilterBean.doRedirectPreviewURL(request, response, null, new HashMap<>(),
                "https://spa.example.com", mount);


        {
            final String redirectedUrl = response.getRedirectedUrl();
            final URI uri = new URI(redirectedUrl);
            assertEquals("spa.example.com", uri.getHost());
            assertEquals("", uri.getPath());
            final Map<String, String[]> parameters = HstRequestUtils.parseQueryString(uri, "UTF-8");
            assertEquals(2L, parameters.size());
            assertTrue(parameters.containsKey("token"));
            assertEquals("http://localhost/site/submount/resourceapi", parameters.get("endpoint")[0]);
        }

    }

    @Test
    public void invalid_redirect_UR() throws Exception {
        request = createRequest();

        hstDelegateeFilterBean.doRedirectPreviewURL(request, response, null, new HashMap<>(),
                "12spa:example&com", mount);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }


    @Test
    public void preview_URL_with_cluster_node_affinity() throws Exception {

        request = createRequest();
        request.addHeader("Server-Id", "my-server");
        hstDelegateeFilterBean.doRedirectPreviewURL(request, response, null, new HashMap<>(),
                "https://spa.example.com", mount);


        {
            final String redirectedUrl = response.getRedirectedUrl();
            final URI uri = new URI(redirectedUrl);
            assertEquals("spa.example.com", uri.getHost());
            assertEquals("", uri.getPath());
            final Map<String, String[]> parameters = HstRequestUtils.parseQueryString(uri, "UTF-8");
            assertEquals(3L, parameters.size());
            assertTrue(parameters.containsKey("token"));
            assertEquals("my-server", parameters.get("server-id-param")[0]);
            assertEquals("http://localhost/site/resourceapi", parameters.get("endpoint")[0]);
        }

    }

    @Test
    public void redirect_URL_with_cluster_node_affinity() throws Exception {

        request = createRequest();
        request.addHeader("Server-Id", "my-server");

        hstDelegateeFilterBean.doRedirectPreviewURL(request, response, "/my/path", new HashMap<>(),
                "https://spa.example.com", mount);
        {
            final String redirectedUrl = response.getRedirectedUrl();
            final URI uri = new URI(redirectedUrl);
            assertEquals("spa.example.com", uri.getHost());
            assertEquals("/my/path", uri.getPath());

            final Map<String, String[]> parameters = HstRequestUtils.parseQueryString(uri, "UTF-8");

            assertEquals(3L, parameters.size());
            assertTrue(parameters.containsKey("token"));
            assertEquals("my-server", parameters.get("server-id-param")[0]);
            assertEquals("http://localhost/site/resourceapi", parameters.get("endpoint")[0]);
        }

        request = createRequest();
        response = new MockHttpServletResponse();
        hstDelegateeFilterBean.doRedirectPreviewURL(request, response, "/my/path", new HashMap<>(),
                "https://spa.example.com", mount);

        {
            final String redirectedUrl = response.getRedirectedUrl();
            final URI uri = new URI(redirectedUrl);
            assertEquals("spa.example.com", uri.getHost());
            assertEquals("/my/path", uri.getPath());
            final Map<String, String[]> parameters = HstRequestUtils.parseQueryString(uri, "UTF-8");
            assertEquals(2L, parameters.size());
            assertEquals("http://localhost/site/resourceapi", parameters.get("endpoint")[0]);
        }
    }

    @Test
    public void redirect_URL_with_queryString_with_cluster_node_affinity_with_pathInfo() throws Exception {

        request = createRequest();
        request.addHeader("Server-Id", "my-server");

        hstDelegateeFilterBean.doRedirectPreviewURL(request, response, "/my/path", new HashMap<>(),
                "https://spa.example.com?foo=bar", mount);

        {
            final String redirectedUrl = response.getRedirectedUrl();
            final URI uri = new URI(redirectedUrl);
            assertEquals("spa.example.com", uri.getHost());
            assertEquals("/my/path", uri.getPath());
            final Map<String, String[]> parameters = HstRequestUtils.parseQueryString(uri, "UTF-8");
            assertEquals(4L, parameters.size());
            assertTrue(parameters.containsKey("token"));
            assertEquals("my-server", parameters.get("server-id-param")[0]);
            assertEquals("bar", parameters.get("foo")[0]);
            assertEquals("http://localhost/site/resourceapi", parameters.get("endpoint")[0]);
        }

        request = createRequest();
        response = new MockHttpServletResponse();
        hstDelegateeFilterBean.doRedirectPreviewURL(request, response, "/my/path", new HashMap<>(),
                "https://spa.example.com?foo=bar", mount);

        {
            final String redirectedUrl = response.getRedirectedUrl();
            final URI uri = new URI(redirectedUrl);
            assertEquals("spa.example.com", uri.getHost());
            assertEquals("/my/path", uri.getPath());
            final Map<String, String[]> parameters = HstRequestUtils.parseQueryString(uri, "UTF-8");
            assertEquals(3L, parameters.size());
            assertEquals("bar", parameters.get("foo")[0]);
            assertEquals("http://localhost/site/resourceapi", parameters.get("endpoint")[0]);
        }
    }

    @Test
    public void redirect_URL_with_port_queryString_with_cluster_node_affinity_with_pathInfo() throws Exception {

        request = createRequest();
        request.addHeader("Server-Id", "my-server");

        hstDelegateeFilterBean.doRedirectPreviewURL(request, response, "/my/path", new HashMap<>(),
                "https://spa.example.com:3000?foo=bar", mount);

        final String redirectedUrl = response.getRedirectedUrl();

        assertEquals("https://spa.example.com:3000/my/path", StringUtils.substringBefore(redirectedUrl, "?"));

    }

    @Test
    public void redirect_URL_with_port_queryString_with_cluster_node_affinity_with_pathInfo_with_queryParameters() throws Exception {

        final HashMap<String, String[]> queryParameters = new HashMap<>();
        queryParameters.put("foo", new String[]{"bar"});
        queryParameters.put("lux", new String[]{"flux", "crux"});

        request = createRequest();
        request.addHeader("Server-Id", "my-server");

        hstDelegateeFilterBean.doRedirectPreviewURL(request, response, "/my/path", queryParameters,
                "https://spa.example.com:3000?test=test1", mount);

        {
            final String redirectedUrl = response.getRedirectedUrl();
            assertEquals("https://spa.example.com:3000/my/path", StringUtils.substringBefore(redirectedUrl, "?"));
            final URI uri = new URI(redirectedUrl);
            assertEquals("spa.example.com", uri.getHost());
            assertEquals("/my/path", uri.getPath());
            final Map<String, String[]> parameters = HstRequestUtils.parseQueryString(uri, "UTF-8");
            assertEquals("Expect also 'foo' and 'lux'", 6L, parameters.size());
            assertTrue(parameters.containsKey("token"));
            assertEquals("my-server", parameters.get("server-id-param")[0]);
            assertEquals("test1", parameters.get("test")[0]);
            assertEquals("bar", parameters.get("foo")[0]);
            assertEquals("flux", parameters.get("lux")[0]);
            assertEquals("crux", parameters.get("lux")[1]);
            assertEquals("http://localhost/site/resourceapi", parameters.get("endpoint")[0]);
            assertTrue(redirectedUrl.contains("lux=flux&lux=crux"));
        }

        request = createRequest();
        response = new MockHttpServletResponse();
        hstDelegateeFilterBean.doRedirectPreviewURL(request, response, "/my/path", queryParameters,
                "https://spa.example.com:3000?test=test1", mount);

        {
            final String redirectedUrl = response.getRedirectedUrl();
            assertEquals("https://spa.example.com:3000/my/path", StringUtils.substringBefore(redirectedUrl, "?"));
            final URI uri = new URI(redirectedUrl);
            assertEquals("spa.example.com", uri.getHost());
            assertEquals("/my/path", uri.getPath());
            final Map<String, String[]> parameters = HstRequestUtils.parseQueryString(uri, "UTF-8");
            assertEquals("Expect also 'foo' and 'lux'", 5L, parameters.size());
            assertEquals("test1", parameters.get("test")[0]);
            assertEquals("bar", parameters.get("foo")[0]);
            assertEquals("flux", parameters.get("lux")[0]);
            assertEquals("crux", parameters.get("lux")[1]);
            assertEquals("http://localhost/site/resourceapi", parameters.get("endpoint")[0]);
            assertTrue(redirectedUrl.contains("lux=flux&lux=crux"));
        }
    }

    @Test
    public void redirect_fails_without_valid_cms_session_context() throws Exception {
        request = createRequest();
        request.getSession().invalidate();
        hstDelegateeFilterBean.doRedirectPreviewURL(request, response,"", new HashMap<>(),
                "https://spa.example.com", mount);
        assertEquals(SC_FORBIDDEN, response.getStatus());
    }

    @Test
    public void redirect_URL_explicit_PMA_ENDPOINT_overrides_automatic_endpoint_injection() throws Exception {

        final HashMap<String, String[]> queryParameters = new HashMap<>();
        queryParameters.put("foo", new String[]{"bar"});
        queryParameters.put("lux", new String[]{"flux", "crux"});

        request = createRequest();
        request.addHeader("Server-Id", "my-server");

        hstDelegateeFilterBean.doRedirectPreviewURL(request, response, "/my/path", queryParameters,
                "https://spa.example.com:3000?test=test1&endpoint=https://brx.example.com/api/resourceapi", mount);

        {
            final String redirectedUrl = response.getRedirectedUrl();
            assertEquals("https://spa.example.com:3000/my/path", StringUtils.substringBefore(redirectedUrl, "?"));
            final URI uri = new URI(redirectedUrl);
            assertEquals("spa.example.com", uri.getHost());
            assertEquals("/my/path", uri.getPath());
            final Map<String, String[]> parameters = HstRequestUtils.parseQueryString(uri, "UTF-8");
            assertEquals("Expect also 'foo' and 'lux'", 6L, parameters.size());
            assertTrue(parameters.containsKey("token"));
            assertEquals("my-server", parameters.get("server-id-param")[0]);
            assertEquals("test1", parameters.get("test")[0]);
            assertEquals("bar", parameters.get("foo")[0]);
            assertEquals("flux", parameters.get("lux")[0]);
            assertEquals("crux", parameters.get("lux")[1]);
            assertEquals("https://brx.example.com/api/resourceapi", parameters.get("endpoint")[0]);
            assertTrue(redirectedUrl.contains("lux=flux&lux=crux"));
        }

        request = createRequest();
        response = new MockHttpServletResponse();
        hstDelegateeFilterBean.doRedirectPreviewURL(request, response, "/my/path", queryParameters,
                "https://spa.example.com:3000?test=test1&endpoint=https://brx.example.com/api/resourceapi", mount);

        {
            final String redirectedUrl = response.getRedirectedUrl();
            assertEquals("https://spa.example.com:3000/my/path", StringUtils.substringBefore(redirectedUrl, "?"));
            final URI uri = new URI(redirectedUrl);
            assertEquals("spa.example.com", uri.getHost());
            assertEquals("/my/path", uri.getPath());
            final Map<String, String[]> parameters = HstRequestUtils.parseQueryString(uri, "UTF-8");
            assertEquals("Expect also 'foo' and 'lux'", 5L, parameters.size());
            assertEquals("test1", parameters.get("test")[0]);
            assertEquals("bar", parameters.get("foo")[0]);
            assertEquals("flux", parameters.get("lux")[0]);
            assertEquals("crux", parameters.get("lux")[1]);
            assertEquals("https://brx.example.com/api/resourceapi", parameters.get("endpoint")[0]);
            assertTrue(redirectedUrl.contains("lux=flux&lux=crux"));
        }
    }
}
