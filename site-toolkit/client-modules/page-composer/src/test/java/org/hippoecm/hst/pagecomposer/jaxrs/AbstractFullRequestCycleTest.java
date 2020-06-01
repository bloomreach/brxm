/*
 *  Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.Filter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.hippoecm.hst.container.HstFilter;
import org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.fullrequestcycle.ConfigurationLockedTest;
import org.junit.Before;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static org.hippoecm.hst.core.container.ContainerConstants.CMS_REQUEST_RENDERING_MOUNT_ID;
import static org.junit.Assert.assertTrue;

public class AbstractFullRequestCycleTest extends AbstractComponentManagerTest {


    protected static ObjectMapper mapper = new ObjectMapper();

    protected Filter filter;


    @Before
    public void setUp() throws Exception {

        super.setUp();

        filter = platformComponentManager.getComponent(HstFilter.class.getName());

        // assert admin has hippo:admin privilege
        Session admin = createSession("admin", "admin");
        assertTrue(admin.hasPermission("/hst:hst", "jcr:write"));
        assertTrue(admin.hasPermission("/hst:hst", "hippo:admin"));
        admin.logout();

        // assert editor is part of webmaster group
        final Session editor = createSession("editor", "editor");
        assertTrue(editor.hasPermission("/hst:hst", "jcr:write"));
        editor.logout();
    }


    protected String[] getConfigurations(final boolean platform) {
        String classXmlFileName = AbstractFullRequestCycleTest.class.getName().replace(".", "/") + ".xml";
        String classXmlFileName2 = AbstractFullRequestCycleTest.class.getName().replace(".", "/") + "-*.xml";
        if (!platform) {
            return new String[]{classXmlFileName, classXmlFileName2};
        }

        String classXmlFileNamePlatform = "org/hippoecm/hst/test/platform-context.xml";
        return new String[] { classXmlFileName, classXmlFileName2, classXmlFileNamePlatform };
    }

    protected Session createSession(final String userName, final String password) throws RepositoryException {
        Repository repository = platformComponentManager.getComponent(Repository.class.getName() + ".delegating");
        return repository.login(new SimpleCredentials(userName, password.toCharArray()));
    }

    public String getNodeId(final String jcrPath) throws RepositoryException {
        final Session admin = createSession("admin", "admin");
        final String mountId = admin.getNode(jcrPath).getIdentifier();
        admin.logout();
        return mountId;
    }

    public MockHttpServletResponse render(final String mountId, final RequestResponseMock requestResponse, final Credentials authenticatedCmsUser) throws IOException, ServletException {
        final MockHttpServletRequest request = requestResponse.getRequest();

        final MockHttpSession mockHttpSession;
        if (request.getSession(false) == null) {
            mockHttpSession = new MockHttpSession();
            request.setSession(mockHttpSession);
        } else {
            mockHttpSession = (MockHttpSession)request.getSession();
        }

        final CmsSessionContextMock cmsSessionContext = new CmsSessionContextMock(authenticatedCmsUser);
        mockHttpSession.setAttribute(CmsSessionContext.SESSION_KEY, cmsSessionContext);
        if (mountId != null) {
            cmsSessionContext.getContextPayload().put(CMS_REQUEST_RENDERING_MOUNT_ID, mountId);
        }

        final MockHttpServletResponse response = requestResponse.getResponse();

        filter.doFilter(request, response, new MockFilterChain(new HttpServlet() {
            @Override
            protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
                super.doGet(req, resp);
            }
        }, filter));

        if (response.getStatus() == SC_FORBIDDEN && this.getClass().getName().equals(ConfigurationLockedTest.class.getName())) {
            // in ConfigurationLockedTest we want to short-circuit by an exception
            throw new ForbiddenException(response);
        }
        return response;
    }

    /**
     * @param scheme      http or https
     * @param hostAndPort eg localhost:8080 or www.example.com
     * @param pathInfo    the request pathInfo, starting with a slash
     * @param queryString optional query string
     * @return RequestResponseMock containing {@link MockHttpServletRequest} and {@link MockHttpServletResponse}
     * @throws Exception
     */
    public RequestResponseMock mockGetRequestResponse(final String scheme,
                                                      final String hostAndPort,
                                                      final String pathInfo,
                                                      final String queryString,
                                                      final String method) {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();

        String host = hostAndPort.split(":")[0];
        if (hostAndPort.split(":").length > 1) {
            int port = Integer.parseInt(hostAndPort.split(":")[1]);
            request.setLocalPort(port);
            request.setServerPort(port);
        }
        if (scheme == null) {
            request.setScheme("http");
        } else {
            request.setScheme(scheme);
        }
        request.setServerName(host);

        request.addHeader("hostGroup", "dev-localhost");
        // the context path of the site that is being edited
        request.addHeader("contextPath", "/site");
        request.setPathInfo(pathInfo);
        request.setContextPath(PLATFORM_CONTEXT_PATH);
        request.setRequestURI(PLATFORM_CONTEXT_PATH + pathInfo);
        request.setMethod(method);
        if (queryString != null) {
            request.setQueryString(queryString);
        }

        return new RequestResponseMock(request, response);
    }

    public static class RequestResponseMock {
        MockHttpServletRequest request;
        MockHttpServletResponse response;

        public RequestResponseMock(final MockHttpServletRequest request, final MockHttpServletResponse response) {
            this.request = request;
            this.response = response;

        }

        public MockHttpServletRequest getRequest() {
            return request;
        }

        public MockHttpServletResponse getResponse() {
            return response;
        }

        public CmsSessionContext getCmsSessionContext() {
            final HttpSession session = request.getSession();
            if (session == null) {
                return null;
            }

            return (CmsSessionContext)session.getAttribute(CmsSessionContext.SESSION_KEY);
        }

    }

    public static class CmsSessionContextMock implements CmsSessionContext {

        private SimpleCredentials credentials;
        private Map<String, Serializable> contextPayload = new HashMap<>();

        public CmsSessionContextMock(Credentials credentials) {
            this.credentials = (SimpleCredentials)credentials;
        }

        @Override
        public String getId() {
            return null;
        }

        @Override
        public String getCmsContextServiceId() {
            return null;
        }

        @Override
        public Object get(final String key) {
            return CmsSessionContext.REPOSITORY_CREDENTIALS.equals(key) ? credentials : null;
        }

        @Override
        public Map<String, Serializable> getContextPayload() {
            return contextPayload;
        }
    }

    public static class ForbiddenException extends RuntimeException {
        private MockHttpServletResponse response;

        public ForbiddenException(final MockHttpServletResponse response) {

            this.response = response;
        }

        public MockHttpServletResponse getResponse() {
            return response;
        }
    }
}


