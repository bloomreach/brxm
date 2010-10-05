/*
 *  Copyright 2010 Hippo.
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

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.easymock.EasyMock;
import org.hippoecm.hst.configuration.hosting.SiteMount;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerConfig;
import org.hippoecm.hst.core.container.Pipeline;
import org.hippoecm.hst.core.container.Pipelines;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.request.ResolvedSiteMount;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.jaxrs.services.AbstractJaxrsSpringTestCase;
import org.hippoecm.hst.site.HstServices;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

/**
 * TestContentService
 * 
 * @version $Id$
 **/
public class TestNodeResource extends AbstractJaxrsSpringTestCase {
    
    private static final String SITE_MOUNT_POINT = "/hst:hst/hst:sites/testproject-preview";
    
    protected Pipelines pipelines;
    protected Pipeline jaxrsPipeline;
    protected ServletConfig servletConfig;
    protected ServletContext servletContext;
    protected HstContainerConfig hstContainerConfig;
    protected ResolvedVirtualHost resolvedVirtualHost;
    protected SiteMount siteMount;
    protected ResolvedSiteMount resolvedSiteMount;
    protected HstMutableRequestContext requestContext;
    
    protected String[] getConfigurations() {
        return new String[] { "org/hippoecm/hst/jaxrs/services/content/TestContentServices.xml" };
    }
    
    @Before
    public void setUp() throws Exception {
        super.setUp();

        HstServices.setComponentManager(getComponentManager());
        
        pipelines = (Pipelines) getComponent(Pipelines.class.getName());
        jaxrsPipeline = this.pipelines.getPipeline("JaxrsPipeline");
        
        servletConfig = new MockServletConfig(new MockServletContext());
        servletContext = servletConfig.getServletContext();
        
        hstContainerConfig = new HstContainerConfig() {
            public ClassLoader getContextClassLoader() {
                return TestNodeResource.class.getClassLoader();
            }
            public ServletContext getServletContext() {
                return servletContext;
            }
        };
        
        resolvedVirtualHost = EasyMock.createNiceMock(ResolvedVirtualHost.class);
        EasyMock.expect(resolvedVirtualHost.getResolvedHostName()).andReturn("localhost").anyTimes();
        EasyMock.expect(resolvedVirtualHost.getPortNumber()).andReturn(8085).anyTimes();

        siteMount = EasyMock.createNiceMock(SiteMount.class);
        EasyMock.expect(siteMount.getMountPoint()).andReturn(SITE_MOUNT_POINT).anyTimes();
        
        resolvedSiteMount = EasyMock.createNiceMock(ResolvedSiteMount.class);
        EasyMock.expect(resolvedSiteMount.getResolvedVirtualHost()).andReturn(resolvedVirtualHost).anyTimes();
        EasyMock.expect(resolvedSiteMount.getSiteMount()).andReturn(siteMount).anyTimes();
        EasyMock.expect(resolvedSiteMount.getResolvedMountPath()).andReturn("/preview/services").anyTimes();
        
        EasyMock.replay(resolvedVirtualHost);
        EasyMock.replay(siteMount);
        EasyMock.replay(resolvedSiteMount);
        
        requestContext = ((HstRequestContextComponent)getComponent(HstRequestContextComponent.class.getName())).create(false);
        requestContext.setServletContext(servletContext);
        requestContext.setResolvedSiteMount(resolvedSiteMount);
    }
    
    @Test
    public void testRepositoryNodeResource() throws Exception {
    }
    
    private void invokeJaxrsPipeline(HttpServletRequest request, HttpServletResponse response) throws ContainerException {
    	request.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, requestContext);
        jaxrsPipeline.beforeInvoke(hstContainerConfig, requestContext, request, response);
        
        try {
            jaxrsPipeline.invoke(hstContainerConfig, requestContext, request, response);
        } catch (Exception e) {
            throw new ContainerException(e);
        } finally {
            jaxrsPipeline.afterInvoke(hstContainerConfig, requestContext, request, response);
        }
    }
}
