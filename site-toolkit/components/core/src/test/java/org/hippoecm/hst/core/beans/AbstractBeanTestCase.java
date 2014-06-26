/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;

import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.container.HstContainerRequestImpl;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.HstSiteMapMatcher;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.hippoecm.hst.util.GenericHttpServletRequestWrapper;
import org.hippoecm.hst.util.HstRequestUtils;
import org.hippoecm.hst.util.ObjectConverterUtils;
import org.junit.Before;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * <p>
 * AbstractBeanSpringTestCase
 * </p> 
 * 
 */ 
public abstract class AbstractBeanTestCase extends AbstractTestConfigurations {

    private HstManager hstManager;
    private HstURLFactory hstURLFactory;
    private HstSiteMapMatcher siteMapMatcher;

    private final static String HTTP_SCHEME = "http";
    private final static String HTTPS_SCHEME = "https";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.hstManager = getComponent(HstManager.class.getName());
        this.siteMapMatcher = getComponent(HstSiteMapMatcher.class.getName());
        this.hstURLFactory = getComponent(HstURLFactory.class.getName());
    }

    protected ObjectConverter getObjectConverter() {
        return ObjectConverterUtils.createObjectConverter(getAnnotatedClasses(), true);
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
        HstRequestContextComponent rcc = getComponent(HstRequestContextComponent.class.getName());
        HstMutableRequestContext requestContext = rcc.create();
        HstContainerURL containerUrl = createContainerUrl(scheme, hostAndPort, pathInfo, requestContext, queryString);
        requestContext.setBaseURL(containerUrl);
        ResolvedSiteMapItem resolvedSiteMapItem = getResolvedSiteMapItem(containerUrl);
        requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
        requestContext.setResolvedMount(resolvedSiteMapItem.getResolvedMount());
        HstURLFactory hstURLFactory = getComponent(HstURLFactory.class.getName());
        requestContext.setURLFactory(hstURLFactory);
        requestContext.setSiteMapMatcher(siteMapMatcher);
        return requestContext;
    }

    protected HstContainerURL createContainerUrl(final String scheme,
                                                 final String hostAndPort,
                                                 final String pathInfo,
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
            if (scheme == null) {
                request.setScheme(HTTP_SCHEME);
            } else {
                request.setScheme(scheme);
            }
            request.setServerName(host);
            request.addHeader("Host", hostAndPort);
            setRequestInfo(request, "/site", pathInfo);
            if (queryString != null) {
                request.setQueryString(queryString);
            }
            containerRequest = new HstContainerRequestImpl(request, hstManager.getPathSuffixDelimiter());
        }

        requestContext.setServletRequest(containerRequest);
        requestContext.setServletResponse(response);

        VirtualHosts vhosts = hstManager.getVirtualHosts();
        ResolvedMount mount = vhosts.matchMount(HstRequestUtils.getFarthestRequestHost(containerRequest),
                containerRequest.getContextPath(), HstRequestUtils.getRequestPath(containerRequest));

        setHstServletPath(containerRequest, mount);
        return hstURLFactory.getContainerURLProvider().parseURL(containerRequest, response, mount);
    }

    protected ResolvedSiteMapItem getResolvedSiteMapItem(HstContainerURL url) throws ContainerException {
        VirtualHosts vhosts = hstManager.getVirtualHosts();
        return vhosts.matchSiteMapItem(url);
    }



}
