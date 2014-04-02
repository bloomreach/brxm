/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.easymock.EasyMock;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
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
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.util.HstRequestUtils;
import org.junit.Before;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

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
    protected MockServletContext servletContext;
    protected HstContainerConfig hstContainerConfig;
    protected ResolvedVirtualHost resolvedVirtualHost;
    protected VirtualHost virtualHost;
    protected VirtualHosts virtualHosts;
    protected Mount mount;
    protected ResolvedMount resolvedMount;
    protected HstURLFactory urlFactory;
    protected HstContainerURLProvider urlProvider;
    protected HstLinkCreator linkCreator;
    
    protected String[] getConfigurations() {
        return new String[] { "org/hippoecm/hst/jaxrs/services/content/TestContentServices.xml" };
    }
    
    @Before
    public void setUp() throws Exception {
        super.setUp();

        HstServices.setComponentManager(getComponentManager());
        
        pipelines = (Pipelines) getComponent(Pipelines.class.getName());
        jaxrsPipeline = this.pipelines.getPipeline("JaxrsRestContentPipeline");
        
        servletContext = new MockServletContext() { public String getRealPath(String path) { return null; } };
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
        ResolvedSiteMapItem resolvedSiteMapItem = EasyMock.createNiceMock(ResolvedSiteMapItem.class);
        EasyMock.expect(resolvedSiteMapItem.getResolvedMount()).andReturn(resolvedMount).anyTimes();
        EasyMock.expect(resolvedSiteMapItem.getRelativeContentPath()).andReturn(siteMapItemRelativeContentPath).anyTimes();
        
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
}
