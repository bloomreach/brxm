/*
 *  Copyright 2010-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.jaxrs.services.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.easymock.EasyMock;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.container.GenericRequestContextWrapper;
import org.hippoecm.hst.container.HstContainerRequest;
import org.hippoecm.hst.container.HstContainerRequestImpl;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerConfig;
import org.hippoecm.hst.core.container.HstContainerURLProvider;
import org.hippoecm.hst.core.container.Pipeline;
import org.hippoecm.hst.core.container.Pipelines;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.jaxrs.services.AbstractJaxrsSpringTestCase;
import org.hippoecm.hst.platform.container.components.HstComponentRegistryImpl;
import org.hippoecm.hst.util.HstRequestUtils;
import org.junit.Before;
import org.springframework.mock.web.MockServletConfig;

import static org.junit.Assert.assertTrue;

/**
 * AbstractTestContentResource
 * 
 * @version $Id$
 **/
public abstract class AbstractTestContentResource extends AbstractJaxrsSpringTestCase {

    private static final String MOUNT_POINT = "/hst:hst/hst:sites/testproject-preview";
    private static final String MOUNT_CONTENTPATH = "/testcontent/documents/testproject";
    private static final String MOUNT_PATH = "/preview/services";

    public static final String BEANS_ANNOTATED_CLASSES_CONF_PARAM = "hst-beans-annotated-classes";

    protected Pipelines pipelines;
    protected Pipeline jaxrsPipeline;
    protected MockServletConfig servletConfig;
    protected HstContainerConfig hstContainerConfig;
    protected ResolvedVirtualHost resolvedVirtualHost;
    protected VirtualHost virtualHost;
    protected VirtualHosts virtualHosts;
    protected Mount mount;
    protected ResolvedMount resolvedMount;
    protected HstURLFactory urlFactory;
    protected HstContainerURLProvider urlProvider;
    protected HstLinkCreator linkCreator;

    @Before
    public void setUp() throws Exception {
        
        pipelines = getComponent(Pipelines.class.getName());
        jaxrsPipeline = this.pipelines.getPipeline("JaxrsRestContentPipeline");

        servletContext.addInitParameter(BEANS_ANNOTATED_CLASSES_CONF_PARAM,
                "classpath*:org/hippoecm/hst/jaxrs/model/beans/**/*.class");
        servletConfig = new MockServletConfig(servletContext);
        
        hstContainerConfig = new HstContainerConfig() {
            public ClassLoader getContextClassLoader() {
                return AbstractTestContentResource.class.getClassLoader();
            }
            public ServletContext getServletContext() {
                return servletContext;
            }
        };
        
        resolvedVirtualHost = EasyMock.createNiceMock(ResolvedVirtualHost.class);

        String hostGroupName = "dev";
        List<Mount> mountsForHostGroup = Collections.emptyList();
        virtualHosts = EasyMock.createNiceMock(VirtualHosts.class);
        EasyMock.expect(virtualHosts.getMountsByHostGroup(hostGroupName)).andReturn(mountsForHostGroup).anyTimes();
        EasyMock.expect(virtualHosts.getComponentRegistry()).andStubReturn(new HstComponentRegistryImpl());

        virtualHost = EasyMock.createNiceMock(VirtualHost.class);
        EasyMock.expect(virtualHost.getHostName()).andReturn("localhost").anyTimes();
        EasyMock.expect(virtualHost.getHostGroupName()).andReturn(hostGroupName).anyTimes();
        EasyMock.expect(virtualHost.getVirtualHosts()).andReturn(virtualHosts).anyTimes();

        mount = EasyMock.createNiceMock(Mount.class);
        EasyMock.expect(mount.getMountPoint()).andReturn(MOUNT_POINT).anyTimes();
        EasyMock.expect(mount.getContentPath()).andReturn(MOUNT_CONTENTPATH).anyTimes();
        EasyMock.expect(mount.isMapped()).andReturn(true).anyTimes();
        EasyMock.expect(mount.getVirtualHost()).andReturn(virtualHost).anyTimes();
        EasyMock.expect(mount.getTypes()).andReturn(new ArrayList<String>()).anyTimes();
        EasyMock.expect(mount.getMountProperties()).andReturn(new HashMap<String,String>()).anyTimes();
        
        resolvedMount = EasyMock.createNiceMock(ResolvedMount.class);
        EasyMock.expect(resolvedMount.getMount()).andReturn(mount).anyTimes();
        EasyMock.expect(resolvedMount.getResolvedMountPath()).andReturn(MOUNT_PATH).anyTimes();
        
        linkCreator = EasyMock.createNiceMock(HstLinkCreator.class);
 
        EasyMock.replay(resolvedVirtualHost);
        EasyMock.replay(virtualHosts);
        EasyMock.replay(virtualHost);
        EasyMock.replay(mount);
        EasyMock.replay(resolvedMount);
        EasyMock.replay(linkCreator);
       
        urlFactory = getComponent(HstURLFactory.class.getName());
        urlProvider = this.urlFactory.getContainerURLProvider();        
    }
    
    public HstMutableRequestContext createRequestContext(String siteMapItemRelativeContentPath) {
        HstSiteMapItem hstSiteMapItem = EasyMock.createNiceMock(HstSiteMapItem.class);
        EasyMock.expect(hstSiteMapItem.isCacheable()).andReturn(false).anyTimes();
        EasyMock.expect(hstSiteMapItem.getId()).andReturn("42").anyTimes();

        ResolvedSiteMapItem resolvedSiteMapItem = EasyMock.createNiceMock(ResolvedSiteMapItem.class);
        EasyMock.expect(resolvedSiteMapItem.getResolvedMount()).andReturn(resolvedMount).anyTimes();
        EasyMock.expect(resolvedSiteMapItem.getRelativeContentPath()).andReturn(siteMapItemRelativeContentPath).anyTimes();
        EasyMock.expect(resolvedSiteMapItem.getHstSiteMapItem()).andReturn(hstSiteMapItem).anyTimes();
        
        EasyMock.replay(hstSiteMapItem);
        EasyMock.replay(resolvedSiteMapItem);

        HstMutableRequestContext requestContext = ((HstRequestContextComponent)getComponent(HstRequestContextComponent.class.getName())).create();
        
        requestContext.setServletContext(servletContext);
        requestContext.setResolvedMount(resolvedMount);
        requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
        requestContext.setLinkCreator(linkCreator);
        return requestContext;
    }
    
    protected void invokeJaxrsPipeline(HttpServletRequest request, HttpServletResponse response) throws ContainerException {
        // every time a jaxrs pipeline is invoked, we also need to create a new request context

        HstContainerRequest cr = new HstContainerRequestImpl(request, "./");
        String requestPath = HstRequestUtils.getRequestPath(cr);
        String pathInfo = requestPath.substring(MOUNT_PATH.length() + 1);
        
        HstMutableRequestContext requestContext = createRequestContext(pathInfo);
        requestContext.setServletRequest(request);
        requestContext.setServletResponse(response);
        requestContext.matchingFinished();
        ModifiableRequestContextProvider.set(requestContext);

    	request.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, requestContext);
    	requestContext.setPathSuffix(cr.getPathSuffix());
    	requestContext.setBaseURL(urlProvider.parseURL(cr, response, requestContext.getResolvedMount()));
        
        try {
            jaxrsPipeline.invoke(hstContainerConfig, requestContext, cr, response);
        } catch (Exception e) {
            throw new ContainerException(e);
        } finally {
            jaxrsPipeline.cleanup(hstContainerConfig, requestContext, cr, response);
            ModifiableRequestContextProvider.clear();
        }
    }

    protected void invokeJaxrsPipelineAsAdmin(HttpServletRequest request, HttpServletResponse response) throws ContainerException, RepositoryException {
        // every time a jaxrs pipeline is invoked, we also need to create a new request context

        HstContainerRequest cr = new HstContainerRequestImpl(request, "./");
        String requestPath = HstRequestUtils.getRequestPath(cr);
        String pathInfo = requestPath.substring(MOUNT_PATH.length() + 1);

        final Session admin;
        final HstMutableRequestContext requestContextWrapper;
        {
            HstMutableRequestContext requestContext = createRequestContext(pathInfo);
            requestContext.setServletRequest(request);
            requestContext.setServletResponse(response);
            requestContext.matchingFinished();
            Repository repository = componentManager.getComponent(Repository.class.getName() + ".delegating");
            admin = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));

            requestContext.setSession(admin);
            requestContextWrapper = new GenericRequestContextWrapper(requestContext) {
                @Override
                public void setSession(final Session session) {
                    // do nothing to avoid the Initialization valve reset the session to null
                }
            };
        }

        ModifiableRequestContextProvider.set(requestContextWrapper);
        request.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, requestContextWrapper);
        requestContextWrapper.setPathSuffix(cr.getPathSuffix());
        requestContextWrapper.setBaseURL(urlProvider.parseURL(cr, response, requestContextWrapper.getResolvedMount()));

        try {
            jaxrsPipeline.invoke(hstContainerConfig, requestContextWrapper, cr, response);
        } catch (Exception e) {
            throw new ContainerException(e);
        } finally {
            jaxrsPipeline.cleanup(hstContainerConfig, requestContextWrapper, cr, response);
            ModifiableRequestContextProvider.clear();
            if (admin != null) {
                admin.logout();
            }
        }
    }


    protected void assertWithPreviewUserContentContains(final String absPath, final String property, final String mustContain) throws RepositoryException {
        Session previewSession = null;
        try {
            Credentials previewCreds = componentManager.getComponent(Credentials.class.getName() + ".preview.delegating");
            final Repository repository = componentManager.getComponent(Repository.class.getName() + ".delegating");
            previewSession = repository.login(previewCreds);
            final String previewContent = previewSession.getNode(absPath)
                    .getProperty(property).getString();

            assertTrue(previewContent.contains(mustContain));
        } finally {
            if (previewSession != null) {
                previewSession.logout();
            }
        }
    }
}
