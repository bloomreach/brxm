/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.restapi;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

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

import org.apache.commons.configuration.PropertiesConfiguration;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.content.tool.DefaultContentBeansTool;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.platform.model.HstModelRegistry;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.addon.module.model.ModuleDefinition;
import org.hippoecm.hst.site.container.ModuleDescriptorUtils;
import org.hippoecm.hst.site.container.SpringComponentManager;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.context.HippoWebappContext;
import org.onehippo.cms7.services.context.HippoWebappContextRegistry;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;


public class AbstractRestApiIT {

    protected static SpringComponentManager componentManager;
    protected static final MockServletContext servletContext = new MockServletContext();
    protected static HippoWebappContext webappContext = new HippoWebappContext(HippoWebappContext.Type.SITE, servletContext);
    protected static Filter filter;

    @BeforeClass
    public static void setUpClass() throws Exception {
        //Enable legacy project structure mode (without extensions)
        System.setProperty("use.hcm.sites", "false");

        final PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.addProperty("hst.configuration.rootPath", "/hst:hst");
        componentManager = new SpringComponentManager(configuration);
        componentManager.setConfigurationResources(getConfigurations());

        servletContext.addInitParameter(DefaultContentBeansTool.BEANS_ANNOTATED_CLASSES_CONF_PARAM, "classpath*:org/onehippo/**/*.class");
        servletContext.setContextPath("/site");
        HippoWebappContextRegistry.get().register(webappContext);

        componentManager.setServletContext(servletContext);

        List<ModuleDefinition> addonModuleDefinitions = ModuleDescriptorUtils.collectAllModuleDefinitions();
        if (addonModuleDefinitions != null && !addonModuleDefinitions.isEmpty()) {
            componentManager.setAddonModuleDefinitions(addonModuleDefinitions);
        }

        componentManager.initialize();
        componentManager.start();
        HstServices.setComponentManager(componentManager);
        filter = HstServices.getComponentManager().getComponent("org.hippoecm.hst.container.HstFilter");

        final HstModelRegistry modelRegistry = HippoServiceRegistry.getService(HstModelRegistry.class);
        modelRegistry.registerHstModel(servletContext, componentManager, true);
    }

    @AfterClass
    public static void tearDown() {

        ModifiableRequestContextProvider.clear();
        componentManager.stop();
        componentManager.close();
        HippoWebappContextRegistry.get().unregister(webappContext);
        HstServices.setComponentManager(null);

    }

    protected static String[] getConfigurations() {
        String classXmlFileName = AbstractRestApiIT.class.getName().replace(".", "/") + ".xml";
        String classXmlFileName2 = AbstractRestApiIT.class.getName().replace(".", "/") + "-*.xml";
        String classXmlFileNamePlatform = "org/hippoecm/hst/test/platform-context.xml";
        return new String[] { classXmlFileName, classXmlFileName2, classXmlFileNamePlatform };
    }

    protected Session createLiveUserSession() throws RepositoryException {
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        Credentials credentials= HstServices.getComponentManager().getComponent(Credentials.class.getName() + ".default.delegating");
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
        request.setMethod("GET");
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

    }

}
