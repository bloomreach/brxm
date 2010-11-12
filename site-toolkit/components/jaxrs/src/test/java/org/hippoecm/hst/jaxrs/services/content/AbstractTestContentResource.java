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

import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.easymock.EasyMock;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.container.HstContainerRequest;
import org.hippoecm.hst.container.HstContainerRequestImpl;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerConfig;
import org.hippoecm.hst.core.container.HstContainerURLProvider;
import org.hippoecm.hst.core.container.Pipeline;
import org.hippoecm.hst.core.container.Pipelines;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.hippoecm.hst.jaxrs.services.AbstractJaxrsSpringTestCase;
import org.hippoecm.hst.site.HstServices;
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
    private static final String MOUNT_CONTENTPATH = "/hst:hst/hst:sites/testproject-preview/hst:content";
    private static final String MOUNT_CANONICAL_CONTENTPATH = "/documents/testproject";
    
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
    protected HstMutableRequestContext requestContext;
    protected HstURLFactory urlFactory;
    protected HstContainerURLProvider urlProvider;
    
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
        EasyMock.expect(resolvedVirtualHost.getResolvedHostName()).andReturn("localhost").anyTimes();
        EasyMock.expect(resolvedVirtualHost.getPortNumber()).andReturn(8085).anyTimes();

        virtualHosts = EasyMock.createNiceMock(VirtualHosts.class);
        
        virtualHost = EasyMock.createNiceMock(VirtualHost.class);
        EasyMock.expect(virtualHost.getHostName()).andReturn("localhost").anyTimes();
        EasyMock.expect(virtualHost.getHostGroupName()).andReturn("dev").anyTimes();
        EasyMock.expect(virtualHost.getVirtualHosts()).andReturn(virtualHosts).anyTimes();

        mount = EasyMock.createNiceMock(Mount.class);
        EasyMock.expect(mount.getMountPoint()).andReturn(MOUNT_POINT).anyTimes();
        EasyMock.expect(mount.getContentPath()).andReturn(MOUNT_CONTENTPATH).anyTimes();
        EasyMock.expect(mount.getCanonicalContentPath()).andReturn(MOUNT_CANONICAL_CONTENTPATH).anyTimes();
        EasyMock.expect(mount.isMapped()).andReturn(true).anyTimes();
        EasyMock.expect(mount.getVirtualHost()).andReturn(virtualHost).anyTimes();
        EasyMock.expect(mount.getTypes()).andReturn(new ArrayList<String>()).anyTimes();
        EasyMock.expect(mount.getMountProperties()).andReturn(new HashMap<String,String>()).anyTimes();
        
        resolvedMount = EasyMock.createNiceMock(ResolvedMount.class);
        EasyMock.expect(resolvedMount.getResolvedVirtualHost()).andReturn(resolvedVirtualHost).anyTimes();
        EasyMock.expect(resolvedMount.getMount()).andReturn(mount).anyTimes();
        EasyMock.expect(resolvedMount.getResolvedMountPath()).andReturn("/preview/services").anyTimes();

        EasyMock.replay(resolvedVirtualHost);
        EasyMock.replay(virtualHosts);
        EasyMock.replay(virtualHost);
        EasyMock.replay(mount);
        EasyMock.replay(resolvedMount);
        
        requestContext = ((HstRequestContextComponent)getComponent(HstRequestContextComponent.class.getName())).create(false);
        requestContext.setServletContext(servletContext);
        requestContext.setResolvedMount(resolvedMount);
        
        urlFactory = getComponent(HstURLFactory.class.getName());
        urlProvider = this.urlFactory.getContainerURLProvider();        
    }
    
    protected void invokeJaxrsPipeline(HttpServletRequest request, HttpServletResponse response) throws ContainerException {
    	request.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, requestContext);
    	HstContainerRequest cr = new HstContainerRequestImpl(request, "./");
    	requestContext.setPathSuffix(cr.getPathSuffix());
    	requestContext.setBaseURL(urlProvider.parseURL(cr, response, requestContext.getResolvedMount()));
        jaxrsPipeline.beforeInvoke(hstContainerConfig, requestContext, cr, response);
        
        try {
            jaxrsPipeline.invoke(hstContainerConfig, requestContext, cr, response);
        } catch (Exception e) {
            throw new ContainerException(e);
        } finally {
            jaxrsPipeline.afterInvoke(hstContainerConfig, requestContext, cr, response);
        }
    }
}
