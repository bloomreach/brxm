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
import java.util.List;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.hippoecm.hst.container.HstFilter;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.fullrequestcycle.ConfigurationLockedTest;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.addon.module.model.ModuleDefinition;
import org.hippoecm.hst.site.container.ModuleDescriptorUtils;
import org.hippoecm.hst.site.container.SpringComponentManager;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.onehippo.cms7.services.context.HippoWebappContext;
import org.onehippo.cms7.services.context.HippoWebappContextRegistry;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;

import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static org.hippoecm.hst.core.container.ContainerConstants.CMS_REQUEST_RENDERING_MOUNT_ID;
import static org.junit.Assert.assertTrue;
import static org.onehippo.cms7.services.context.HippoWebappContext.Type.SITE;

public class AbstractFullRequestCycleTest {

    protected SpringComponentManager componentManager;
    protected HippoWebappContext webappContext = new HippoWebappContext(SITE, new MockServletContext() {
        public String getContextPath() {
            return "/site";
        }
    });
    protected static ObjectMapper mapper = new ObjectMapper();

    protected Filter filter;

    @BeforeClass
    public static void setUpClass() throws Exception {
        //Enable legacy project structure mode (without extensions)
        System.setProperty("use.hcm.sites", "false");
    }

    @Before
    public void setUp() throws Exception {

        final PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.setProperty("hst.configuration.rootPath", "/hst:hst");
        componentManager = new SpringComponentManager(configuration);
        componentManager.setConfigurationResources(getConfigurations());

        HippoWebappContextRegistry.get().register(webappContext);
        componentManager.setServletContext(webappContext.getServletContext());

        List<ModuleDefinition> addonModuleDefinitions = ModuleDescriptorUtils.collectAllModuleDefinitions();
        if (addonModuleDefinitions != null && !addonModuleDefinitions.isEmpty()) {
            componentManager.setAddonModuleDefinitions(addonModuleDefinitions);
        }

        componentManager.initialize();
        componentManager.start();
        HstServices.setComponentManager(getComponentManager());
        filter = HstServices.getComponentManager().getComponent(HstFilter.class.getName());

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
    public void tearDown() throws Exception {
        this.componentManager.stop();
        this.componentManager.close();
        HippoWebappContextRegistry.get().unregister(webappContext);
        HstServices.setComponentManager(null);
        ModifiableRequestContextProvider.clear();

    }

    protected String[] getConfigurations() {
        String classXmlFileName = AbstractFullRequestCycleTest.class.getName().replace(".", "/") + ".xml";
        String classXmlFileName2 = AbstractFullRequestCycleTest.class.getName().replace(".", "/") + "-*.xml";
        return new String[]{classXmlFileName, classXmlFileName2};
    }

    protected ComponentManager getComponentManager() {
        return this.componentManager;
    }

    protected Session createSession(final String userName, final String password) throws RepositoryException {
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        return repository.login(new SimpleCredentials(userName, password.toCharArray()));
    }

    public String getNodeId(final String jcrPath) throws RepositoryException {
        final Session admin = createSession("admin", "admin");
        final String mountId = admin.getNode(jcrPath).getIdentifier();
        admin.logout();
        return mountId;
    }

    public MockHttpServletResponse render(final String mountId, final RequestResponseMock requestResponse, final Credentials authenticatedCmsUser) throws IOException, ServletException {
        MockHttpSession session = (MockHttpSession)requestResponse.getRequest().getSession(false);
        if (session == null) {
            session = new MockHttpSession();
            requestResponse.getRequest().setSession(session);
        }
        session.setAttribute(CMS_REQUEST_RENDERING_MOUNT_ID, mountId);
        MockHttpServletResponse response = render(requestResponse, authenticatedCmsUser);

        if (response.getStatus() == SC_FORBIDDEN && this.getClass().getName().equals(ConfigurationLockedTest.class.getName())) {
            // in ConfigurationLockedTest we want to short-circuit by an exception
            throw new ForbiddenException(response);
        }
        return response;
    }

    public MockHttpServletResponse render(final RequestResponseMock requestResponse, final Credentials authenticatedCmsUser) throws IOException, ServletException {
        final MockHttpServletRequest request = requestResponse.getRequest();

        final MockHttpSession mockHttpSession;
        if (request.getSession(false) == null) {
            mockHttpSession = new MockHttpSession();
            request.setSession(mockHttpSession);
        } else {
            mockHttpSession = (MockHttpSession)request.getSession();
        }

        mockHttpSession.setAttribute(CmsSessionContext.SESSION_KEY, new CmsSessionContextMock(authenticatedCmsUser));

        final MockHttpServletResponse response = requestResponse.getResponse();

        filter.doFilter(request, response, new MockFilterChain(new HttpServlet() {
            @Override
            protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
                super.doGet(req, resp);
            }
        }, filter));
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
        request.addHeader("Host", hostAndPort);
        request.setPathInfo(pathInfo);
        request.setContextPath("/site");
        request.setRequestURI("/site" + pathInfo);
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

    }

    public static class CmsSessionContextMock implements CmsSessionContext {

        private SimpleCredentials credentials;

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
    }

    protected void setPrivilegePropsForSecurityModel() throws RepositoryException {
        final Session admin = createSession("admin", "admin");
        final Node mount = admin.getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root");
        // make sure that users that have 'hippo:admin' role on /hst:hst can publish other ones their changes
        mount.setProperty("manage.changes.privileges","hippo:admin");
        mount.setProperty("manage.changes.privileges.path","/hst:hst");
        admin.save();
        admin.logout();
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


