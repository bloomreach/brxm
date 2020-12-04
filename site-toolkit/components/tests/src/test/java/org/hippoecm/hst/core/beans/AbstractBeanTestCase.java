/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.beans;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.commons.configuration.Configuration;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.container.HstContainerRequestImpl;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.content.beans.ContentTypesProvider;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.manager.VersionedObjectConverterProxy;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.platform.model.HstModel;
import org.hippoecm.hst.platform.model.HstModelRegistry;
import org.hippoecm.hst.site.container.SpringComponentManager;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.hippoecm.hst.util.GenericHttpServletRequestWrapper;
import org.hippoecm.hst.util.HstRequestUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.contenttype.ContentTypeService;
import org.onehippo.cms7.services.context.HippoWebappContext;
import org.onehippo.cms7.services.context.HippoWebappContextRegistry;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import static org.onehippo.cms7.services.context.HippoWebappContext.Type.SITE;

/**
 * <p>
 * AbstractBeanSpringTestCase
 * </p>
 */
public abstract class AbstractBeanTestCase extends AbstractTestConfigurations {
    protected static MockServletContext servletContext2;
    protected static HippoWebappContext webappContext2;
    protected static SpringComponentManager componentManager2;
    protected static HstModel hstModelSite2;

    private HstManager hstManager;
    private HstURLFactory hstURLFactory;

    private final static String HTTP_SCHEME = "http";
    private final static String HTTPS_SCHEME = "https";

    /**
     * addAnnotatedClassesConfigurationParam must be added before super setUpClass, hence redefine same setUpClass method
     * to hide the super.setUpClass and invoke that explicitly
     */
    @BeforeClass
    public static void setUpClass() throws Exception {

        AbstractTestConfigurations.setUpClass();

        servletContext2 = new MockServletContext() {
            public String getContextPath() {
                return "/site2";
            }

            public ClassLoader getClassLoader() {
                return AbstractBeanTestCase.class.getClassLoader();
            }
        };
        webappContext2 = new HippoWebappContext(SITE, servletContext2);
        HippoWebappContextRegistry.get().register(webappContext2);
        servletContext2.setContextPath("/site2");

        final Configuration containerConfiguration = getContainerConfiguration();
        containerConfiguration.addProperty("hst.configuration.rootPath", "/hst:site2");
        componentManager2 = new SpringComponentManager(containerConfiguration);
        componentManager2.setConfigurationResources(getConfigurations(false));
        componentManager2.setServletContext(servletContext2);
        componentManager2.initialize();
        componentManager2.start();

        final HstModelRegistry modelRegistry = HippoServiceRegistry.getService(HstModelRegistry.class);
        hstModelSite2 = modelRegistry.registerHstModel(servletContext2, componentManager2, true);
    }

    @AfterClass
    public static void afterClass() throws RepositoryException {
        final HstModelRegistry modelRegistry = HippoServiceRegistry.getService(HstModelRegistry.class);
        modelRegistry.unregisterHstModel(webappContext2.getServletContext().getContextPath());
        if (componentManager2 != null) {
            componentManager2.stop();
            componentManager2.close();
        }
        HippoWebappContextRegistry.get().unregister(webappContext2);
        AbstractTestConfigurations.afterClass();
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.hstManager = getComponent(HstManager.class.getName());
        this.hstURLFactory = getComponent(HstURLFactory.class.getName());

    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    protected ObjectConverter getObjectConverter() {
        return new VersionedObjectConverterProxy(getAnnotatedClasses(), Collections.emptyList(),
                new ContentTypesProvider(HippoServiceRegistry.getService(ContentTypeService.class)), true);
    }

    protected Collection<Class<? extends HippoBean>> getAnnotatedClasses() {
        List<Class<? extends HippoBean>> annotatedClasses = new ArrayList<Class<? extends HippoBean>>();
        annotatedClasses.add(TextBean.class);
        annotatedClasses.add(NewsBean.class);
        return annotatedClasses;
    }

    protected HstRequestContext getRequestContextWithResolvedSiteMapItemAndContainerURL(final String hostAndPort,
                                                                                        final String pathInfo) throws Exception {
        return getRequestContextWithResolvedSiteMapItemAndContainerURL(null, hostAndPort, pathInfo, null);
    }

    protected HstRequestContext getRequestContextWithResolvedSiteMapItemAndContainerURL(final String scheme,
                                                                                        final String hostAndPort,
                                                                                        final String pathInfo,
                                                                                        final String queryString) throws Exception {
        final HstModel hstModel = HippoServiceRegistry.getService(HstModelRegistry.class).getHstModel("/site");
        return getRequestContextWithResolvedSiteMapItemAndContainerURL(scheme, hostAndPort, "/site", pathInfo, queryString, hstModel);
    }


    protected HstRequestContext getRequestContextWithResolvedSiteMapItemAndContainerURL(final String scheme,
                                                                                        final String hostAndPort,
                                                                                        final String contextPath,
                                                                                        final String pathInfo,
                                                                                        final String queryString,
                                                                                        final HstModel hstModel) throws Exception {
        HstRequestContextComponent rcc = getComponent(HstRequestContextComponent.class.getName());
        HstMutableRequestContext requestContext = rcc.create();
        ModifiableRequestContextProvider.set(requestContext);
        HstContainerURL containerUrl = createContainerUrl(scheme, hostAndPort, pathInfo, contextPath, hstModel, requestContext, queryString);
        requestContext.setBaseURL(containerUrl);
        ResolvedSiteMapItem resolvedSiteMapItem = getResolvedSiteMapItem(containerUrl, hstModel);
        requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
        requestContext.setResolvedMount(resolvedSiteMapItem.getResolvedMount());
        requestContext.matchingFinished();
        HstURLFactory hstURLFactory = getComponent(HstURLFactory.class.getName());
        requestContext.setURLFactory(hstURLFactory);
        //TODO SS: re-evaluate this
//        requestContext.setSiteMapMatcher(requestContext.getSiteMapMatcher());
        return requestContext;
    }

    protected HstContainerURL createContainerUrl(final String scheme,
                                                 final String hostAndPort,
                                                 final String pathInfo,
                                                 final String contextPath,
                                                 final HstModel hstModel,
                                                 final HstMutableRequestContext requestContext,
                                                 final String queryString) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        GenericHttpServletRequestWrapper containerRequest;
        {
            MockHttpServletRequest request = new MockHttpServletRequest();
            String host = hostAndPort.split(":")[0];
            if (hostAndPort.split(":").length > 1) {
                int port = Integer.parseInt(hostAndPort.split(":")[1]);
                request.setLocalPort(port);
                request.setServerPort(port);
            }
            request.setScheme(scheme == null ? HTTP_SCHEME : scheme);
            request.setServerName(host);
            request.addHeader("Host", hostAndPort);
            setRequestInfo(request, contextPath, pathInfo);
            if (queryString != null) {
                request.setQueryString(queryString);
            }
            containerRequest = new HstContainerRequestImpl(request, hstManager.getPathSuffixDelimiter());
        }

        requestContext.setServletRequest(containerRequest);
        requestContext.setServletResponse(response);

        VirtualHosts vhosts = hstModel.getVirtualHosts();
        ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(containerRequest),
                HstRequestUtils.getRequestPath(containerRequest));

        setHstServletPath(containerRequest, mount);
        return hstURLFactory.getContainerURLProvider().parseURL(containerRequest, response, mount);
    }

    protected ResolvedSiteMapItem getResolvedSiteMapItem(HstContainerURL url, HstModel hstModel) throws ContainerException {
        VirtualHosts vhosts = hstModel.getVirtualHosts();
        final ResolvedMount resolvedMount = vhosts.matchMount(url.getHostName(), url.getRequestPath());
        return resolvedMount.matchSiteMapItem(url.getPathInfo());
    }

    protected ResolvedMount getResolvedMount(HstContainerURL url, HstModel hstModel) throws ContainerException {
        VirtualHosts vhosts = hstModel.getVirtualHosts();
        return vhosts.matchMount(url.getHostName(), url.getRequestPath());
    }


    protected ResolvedSiteMapItem getResolvedSiteMapItem(HstContainerURL url) throws ContainerException {
        VirtualHosts vhosts = hstManager.getVirtualHosts();
        final ResolvedMount resolvedMount = vhosts.matchMount(url.getHostName(), url.getRequestPath());
        return resolvedMount.matchSiteMapItem(url.getPathInfo());
    }


}
