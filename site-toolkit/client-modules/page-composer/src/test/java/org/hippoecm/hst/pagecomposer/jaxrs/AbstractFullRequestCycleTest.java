/*
 *  Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Arrays;
import java.util.Map;

import javax.jcr.Credentials;
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
import org.hippoecm.hst.core.internal.BranchSelectionService;
import org.hippoecm.hst.mock.core.request.MockCmsSessionContext;
import org.hippoecm.hst.pagecomposer.jaxrs.cxf.PrivilegesAllowedInvokerPreprocessor;
import org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.fullrequestcycle.ConfigurationLockedTest;
import org.junit.After;
import org.junit.Before;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.hippoecm.hst.core.container.ContainerConstants.CMS_REQUEST_RENDERING_MOUNT_ID;
import static org.junit.Assert.assertTrue;

public class AbstractFullRequestCycleTest extends AbstractComponentManagerTest {


    protected static final SimpleCredentials ADMIN_CREDENTIALS = new SimpleCredentials("admin", "admin".toCharArray());
    protected static final SimpleCredentials EDITOR_CREDENTIALS = new SimpleCredentials("editor", "editor".toCharArray());
    protected static final SimpleCredentials AUTHOR_CREDENTIALS = new SimpleCredentials("author", "author".toCharArray());

    private final static Logger log = LoggerFactory.getLogger(AbstractFullRequestCycleTest.class);

    public static final String TEST_BRANCH_ID_PAYLOAD_NAME = "testBranchId";


    protected static ObjectMapper mapper = new ObjectMapper();

    protected Filter filter;

    protected BranchSelectionService testBranchSelectionService = contextPayload -> (String)contextPayload.get(TEST_BRANCH_ID_PAYLOAD_NAME);


    @Before
    public void setUp() throws Exception {

        super.setUp();

        HippoServiceRegistry.register(testBranchSelectionService, BranchSelectionService.class);

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


    @After
    @Override
    public void tearDown() throws Exception {
        HippoServiceRegistry.unregister(testBranchSelectionService, BranchSelectionService.class);
        super.tearDown();
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
        return createSession(new SimpleCredentials(userName, password.toCharArray()));
    }
    protected Session createSession(final Credentials creds) throws RepositoryException {
        Repository repository = platformComponentManager.getComponent(Repository.class.getName() + ".delegating");
        return repository.login(creds);
    }

    /**
     * the identifier of the node at jcrPath. If possible use {@link #getNodeId(Session, String)} since this one first
     * needs to login a session
     */
    public String getNodeId(final String jcrPath) throws RepositoryException {
        final Session admin = createSession("admin", "admin");
        final String identifier = admin.getNode(jcrPath).getIdentifier();
        admin.logout();
        return identifier;
    }

    public String getNodeId(final Session session, final String jcrPath) throws RepositoryException {
        try {
            return session.getNode(jcrPath).getIdentifier();
        } catch (RepositoryException e) {
            log.error("Cannot find jcr node '{}' with session '{}'", jcrPath, session.getUserID());
            throw e;
        }
    }

    public MockHttpServletResponse render(final String mountId, final RequestResponseMock requestResponse, final Credentials authenticatedCmsUser) throws IOException, ServletException {
        return render(mountId, requestResponse, authenticatedCmsUser, null);
    }

    public MockHttpServletResponse render(final String mountId, final RequestResponseMock requestResponse,
                                          final Credentials authenticatedCmsUser, final String branchId) throws IOException, ServletException {
        final MockHttpServletRequest request = requestResponse.getRequest();

        final MockHttpSession mockHttpSession;
        if (request.getSession(false) == null) {
            mockHttpSession = new MockHttpSession();
            request.setSession(mockHttpSession);
        } else {
            mockHttpSession = (MockHttpSession)request.getSession();
        }

        final MockCmsSessionContext cmsSessionContext = new MockCmsSessionContext(authenticatedCmsUser);
        mockHttpSession.setAttribute(CmsSessionContext.SESSION_KEY, cmsSessionContext);
        if (mountId != null) {
            cmsSessionContext.getContextPayload().put(CMS_REQUEST_RENDERING_MOUNT_ID, mountId);
        }
        if (branchId == null) {
            cmsSessionContext.getContextPayload().remove(TEST_BRANCH_ID_PAYLOAD_NAME);
        } else {
            cmsSessionContext.getContextPayload().put(TEST_BRANCH_ID_PAYLOAD_NAME, branchId);
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
        // for full request tests, we mimic the HstFilter: for a servlet filter, the servlet path is equal to the pathInfo
        // and the pathInfo is null : HstDelegateeFilterBean later on sets these values correct
        request.setServletPath(pathInfo);
        request.setPathInfo(null);
        request.setContextPath(PLATFORM_CONTEXT_PATH);
        request.setRequestURI(PLATFORM_CONTEXT_PATH + pathInfo);
        request.setMethod(method);
        if (queryString != null) {
            request.setQueryString(queryString);

            // for some reason queryString does not end up as parameters so set those explicitly
            Arrays.stream(queryString.split("&"))
                    .forEach(paramKeyVal -> request.setParameter(substringBefore(paramKeyVal, "="), substringAfter(paramKeyVal, "=")));
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

    public static class ForbiddenException extends RuntimeException {
        private MockHttpServletResponse response;

        public ForbiddenException(final MockHttpServletResponse response) {

            this.response = response;
        }

        public MockHttpServletResponse getResponse() {
            return response;
        }
    }

    public class SuppressPrivilegesAllowedPreProcessor implements AutoCloseable {

        private final PrivilegesAllowedInvokerPreprocessor privilegesAllowedInvokerPreprocessor;

        public SuppressPrivilegesAllowedPreProcessor() {
            privilegesAllowedInvokerPreprocessor =
                    platformComponentManager.getComponent(PrivilegesAllowedInvokerPreprocessor.class, "org.hippoecm.hst.pagecomposer");
            privilegesAllowedInvokerPreprocessor.setEnabled(false);
        }

        @Override
        public void close() throws Exception {
            privilegesAllowedInvokerPreprocessor.setEnabled(true);
        }
    }

    // returns the jcr session containing the changes, do more changes and save this session when needed (and logout)
    protected Session backupHstAndCreateWorkspace() throws RepositoryException {
        final Session session = createSession("admin", "admin");
        createHstConfigBackup(session);
        // move the hst:sitemap and hst:pages below the 'workspace' because since HSTTWO-3959 only the workspace
        // gets copied to preview configuration
        if (!session.nodeExists("/hst:hst/hst:configurations/unittestproject/hst:workspace")) {
            session.getNode("/hst:hst/hst:configurations/unittestproject").addNode("hst:workspace", "hst:workspace");
        }
        if (!session.nodeExists("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:sitemap")) {
            session.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace").addNode("hst:sitemap", "hst:sitemap");
        }
        if (!session.nodeExists("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages")) {
            session.getNode("/hst:hst/hst:configurations/unittestproject/hst:workspace").addNode("hst:pages", "hst:pages");
        }
        return session;
    }

    protected Map<String, Object> startEdit(final Credentials creds) throws RepositoryException, IOException, ServletException {
        final String mountId = getNodeId("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");
        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", "/_rp/" + mountId + "./edit", null, "POST");

        final MockHttpServletResponse response = render(mountId, requestResponse, creds);
        final String restResponse = response.getContentAsString();
        return mapper.readerFor(Map.class).readValue(restResponse);
    }


}


