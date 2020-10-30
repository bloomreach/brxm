/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagemodelapi.common;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.content.tool.DefaultContentBeansTool;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.mock.core.request.MockCmsSessionContext;
import org.hippoecm.hst.platform.HstModelProvider;
import org.hippoecm.hst.platform.api.model.EventPathsInvalidator;
import org.hippoecm.hst.platform.api.model.InternalHstModel;
import org.hippoecm.hst.platform.model.HstModelRegistry;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.addon.module.model.ModuleDefinition;
import org.hippoecm.hst.site.container.ModuleDescriptorUtils;
import org.hippoecm.hst.site.container.SpringComponentManager;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.onehippo.cms7.services.context.HippoWebappContext;
import org.onehippo.cms7.services.context.HippoWebappContextRegistry;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;

import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.CACHE_CONTROL;
import static org.springframework.http.HttpHeaders.PRAGMA;

/**
 * This class exposes common functionality for testing against the Page Model API. It is built after
 * org.hippoecm.hst.restapi.AbstractRestApiIT and is identical to that.
 */
public abstract class AbstractPageModelApiITCases {

    public static final String LOCALHOST_JCR_PATH = "/hst:hst/hst:hosts/dev-localhost/localhost";
    public static final String SPA_MOUNT_JCR_PATH = LOCALHOST_JCR_PATH + "/hst:root/spa";
    public static final String ANNOTATED_CLASSES_CONFIGURATION_PARAM = "classpath*:org/hippoecm/hst/pagemodelapi/common/**/*.class";

    protected static final SimpleCredentials EDITOR_CREDS = new SimpleCredentials("editor", "editor".toCharArray());
    protected SpringComponentManager componentManager;
    protected final MockServletContext servletContext = new MockServletContext();
    protected HippoWebappContext webappContext = new HippoWebappContext(HippoWebappContext.Type.SITE, servletContext);
    protected Filter filter;
    protected static ObjectMapper mapper = new ObjectMapper();
    protected EventPathsInvalidator eventPathsInvalidator;

    @BeforeClass
    public static void setUpClass() throws Exception {
        //Enable legacy project structure mode (without extensions)
        System.setProperty("use.hcm.sites", "false");
    }

    @Before
    public void setUp() throws Exception {
        final PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.addProperty("hst.configuration.rootPath", "/hst:hst");
        // below is handy such that during integration tests, the PMA response is nicely formatted (if you do a
        // sysout)
        configuration.addProperty("pagemodelapi.v10.pretty.print", true);

        configuration.addProperty("cms.default.cmspreviewprefix", "_cmsinternal");

        componentManager = new SpringComponentManager(configuration);
        componentManager.setConfigurationResources(getConfigurations());

        servletContext.addInitParameter(DefaultContentBeansTool.BEANS_ANNOTATED_CLASSES_CONF_PARAM, getAnnotatedClassesConfigurationParam());
        servletContext.setContextPath("/site");
        HippoWebappContextRegistry.get().register(webappContext);

        componentManager.setServletContext(servletContext);

        List<ModuleDefinition> addonModuleDefinitions = ModuleDescriptorUtils.collectAllModuleDefinitions();
        if (addonModuleDefinitions != null && !addonModuleDefinitions.isEmpty()) {
            componentManager.setAddonModuleDefinitions(addonModuleDefinitions);
        }

        componentManager.initialize();
        componentManager.start();
        HstServices.setComponentManager(getComponentManager());
        filter = HstServices.getComponentManager().getComponent("org.hippoecm.hst.container.HstFilter");

        final HstModelRegistry modelRegistry = HippoServiceRegistry.getService(HstModelRegistry.class);
        modelRegistry.registerHstModel(servletContext, componentManager, true);

        final HstModelProvider provider = HstServices.getComponentManager().getComponent(HstModelProvider.class);
        final InternalHstModel hstModel = (InternalHstModel) provider.getHstModel();
        eventPathsInvalidator = hstModel.getEventPathsInvalidator();
    }

    protected String getAnnotatedClassesConfigurationParam() {
        return ANNOTATED_CLASSES_CONFIGURATION_PARAM;
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
        String classXmlFileName = AbstractPageModelApiITCases.class.getName().replace(".", "/") + ".xml";
        String classXmlFileName2 = AbstractPageModelApiITCases.class.getName().replace(".", "/") + "-*.xml";
        String classXmlFileNamePlatform = "org/hippoecm/hst/test/platform-context.xml";
        return new String[]{classXmlFileName, classXmlFileName2, classXmlFileNamePlatform};
    }

    protected ComponentManager getComponentManager() {
        return this.componentManager;
    }

    protected Session createLiveUserSession() throws RepositoryException {
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        Credentials credentials = HstServices.getComponentManager().getComponent(Credentials.class.getName() + ".default.delegating");
        return repository.login(credentials);
    }

    protected Session createSession(final String userName, final String password) throws RepositoryException {
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        return repository.login(new SimpleCredentials(userName, password.toCharArray()));
    }


    protected MockHttpServletResponse render(final RequestResponseMock requestResponse) throws IOException, ServletException {
        final MockHttpServletRequest request = requestResponse.getRequest();
        final MockHttpServletResponse response = requestResponse.getResponse();

        filter.doFilter(request, response, new MockFilterChain(new HttpServlet() {
            @Override
            protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
                super.doGet(req, resp);
            }
        }, filter));
        return response;
    }

    public MockHttpServletResponse renderChannelMgrPreview(final RequestResponseMock requestResponse,
                                          final Credentials authenticatedCmsUser) throws IOException, ServletException {
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

        final MockHttpServletResponse response = requestResponse.getResponse();

        try {
            filter.doFilter(request, response, new MockFilterChain(new HttpServlet() {
                @Override
                protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
                    super.doGet(req, resp);
                }
            }, filter));
        } finally {
            mockHttpSession.invalidate();
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
                                                      final String queryString) {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest();

        request.setCharacterEncoding("utf-8");

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

        // for full request tests, we mimic the HstFilter: for a servlet filter, the servlet path is equal to the pathInfo
        // and the pathInfo is null : HstDelegateeFilterBean later on sets these values correct
        request.setServletPath(pathInfo);
        request.setPathInfo(null);
        request.setContextPath("/site");
        request.setRequestURI("/site" + pathInfo);
        request.setMethod("GET");
        if (queryString != null) {
            request.setQueryString(queryString);

            Arrays.stream(queryString.split("&"))
                    .forEach(paramKeyVal -> {
                        try {
                            request.setParameter(
                                    URLDecoder.decode(substringBefore(paramKeyVal, "="), "utf-8"), substringAfter(paramKeyVal, "="));
                        } catch (UnsupportedEncodingException e) {
                            throw new IllegalStateException(e);
                        }
                    });

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

    public String getActualJson(final String pathInfo) throws IOException, ServletException {
        return getActualJson(pathInfo, "0.9");
    }

    public String getActualJson(final String pathInfo, final String apiVersion) throws IOException, ServletException {
        return getActualJson(pathInfo, apiVersion, null, null);
    }

    public String getActualJson(final String pathInfo, final String apiVersion, final String queryString) throws IOException, ServletException {
        return getActualJson(pathInfo, apiVersion, queryString, null);
    }

    /**
     *
     * @param pathInfo
     * @param apiVersion
     * @param queryString
     * @param authenticatedUser if not null, a preview Channel Manager PMA request will be done on behalf of these
     *                          credentials
     * @return
     * @throws IOException
     * @throws ServletException
     */
    public String getActualJson(final String pathInfo, final String apiVersion, final String queryString,
                                final Credentials authenticatedUser) throws IOException, ServletException {
        final RequestResponseMock requestResponse = mockGetRequestResponse(
                "http", "localhost", pathInfo, queryString);

        requestResponse.getRequest().addHeader(ContainerConstants.PAGE_MODEL_ACCEPT_VERSION, apiVersion);
        final MockHttpServletResponse response;
        if (authenticatedUser != null) {
            response = renderChannelMgrPreview(requestResponse, authenticatedUser);
            assertThat(response.containsHeader(CACHE_CONTROL));
            assertThat(response.getHeader(CACHE_CONTROL))
                    .as("Channel Manager preview requests should be always private")
                    .isEqualTo("private, max-age=0, no-store");
            assertThat(response.containsHeader(PRAGMA));
            assertThat(response.getHeader(PRAGMA))
                    .as("Channel Manager preview requests should be always private")
                    .isEqualTo("no-cache");
        } else {
            response = render(requestResponse);
        }

        return response.getContentAsString();
    }

}
