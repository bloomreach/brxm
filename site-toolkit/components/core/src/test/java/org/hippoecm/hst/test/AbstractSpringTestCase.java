/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.test;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.model.MutableHstManager;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.site.container.SpringComponentManager;
import org.hippoecm.hst.util.HstRequestUtils;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * AbstractSpringTestCase
 * </p>
 * <p>
 * 
 * </p>
 * 
 * @version $Id$
 *  
 */
public abstract class AbstractSpringTestCase
{

    protected final static Logger log = LoggerFactory.getLogger(AbstractSpringTestCase.class);
    protected ComponentManager componentManager;

    @Before
    public void setUp() throws Exception {
        componentManager = new SpringComponentManager(getContainerConfiguration());
        componentManager.setConfigurationResources(getConfigurations());
        
        componentManager.initialize();
        componentManager.start();
        HstServices.setComponentManager(getComponentManager());

        final HstManager hstManager = componentManager.getComponent(HstManager.class.getName());
        if (hstManager != null) {
            ((MutableHstManager) hstManager).setContextPath("/site");
        }
    }

    @After
    public void tearDown() throws Exception {
        final HstManager hstManager = componentManager.getComponent(HstManager.class.getName());
        this.componentManager.stop();
        this.componentManager.close();
        HstServices.setComponentManager(null);

        // always clear HstRequestContext in case it is set on a thread local
        ModifiableRequestContextProvider.clear();
    }

    /**
     * required specification of spring configurations
     * the derived class can override this.
     */
    protected String[] getConfigurations() {
        String classXmlFileName = getClass().getName().replace(".", "/") + ".xml";
        String classXmlFileName2 = getClass().getName().replace(".", "/") + "-*.xml";
        return new String[] { classXmlFileName, classXmlFileName2 };
    }
    
    protected ComponentManager getComponentManager() {
        return this.componentManager;
    }

    protected <T> T getComponent(String name) {
        return getComponentManager().getComponent(name);
    }
    
    protected Configuration getContainerConfiguration() {
        return new PropertiesConfiguration();
    }
    
    protected void setResolvedMount(HstMutableRequestContext requestContext) {
        
        ResolvedMount resolvedMount = createNiceMock(ResolvedMount.class);
        Mount mount = createNiceMock(Mount.class);
        VirtualHost virtualHost = createNiceMock(VirtualHost.class);
        VirtualHosts virtualHosts = createNiceMock(VirtualHosts.class);
        HstManager hstManager = createNiceMock(HstManager.class);
        
        expect(resolvedMount.getResolvedMountPath()).andReturn("").anyTimes();
        expect(resolvedMount.getMount()).andReturn(mount).anyTimes();
        expect(mount.isContextPathInUrl()).andReturn(true).anyTimes();
        expect(mount.getVirtualHost()).andReturn(virtualHost).anyTimes();
        expect(virtualHost.isContextPathInUrl()).andReturn(true).anyTimes();

        expect(virtualHost.getVirtualHosts()).andReturn(virtualHosts).anyTimes();
        expect(virtualHosts.getHstManager()).andReturn(hstManager).anyTimes();
        expect(hstManager.getPathSuffixDelimiter()).andReturn("./").anyTimes();
        
        replay(resolvedMount);
        replay(mount);
        replay(virtualHost);
        replay(virtualHosts);
        replay(hstManager);
        
        // to parse a url, there must be a ResolvedMount on the HstRequestContext
        requestContext.setResolvedMount(resolvedMount);
    }
    

    protected HstRequestContext resolveRequest(HttpServletRequest request, HttpServletResponse response) throws ContainerException {
        HstManager hstSitesManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());
        VirtualHosts vHosts = hstSitesManager.getVirtualHosts();
        HstMutableRequestContext requestContext = ((HstRequestContextComponent)HstServices.getComponentManager().getComponent(HstRequestContextComponent.class.getName())).create();
        request.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, requestContext);
        ResolvedMount mount = vHosts.matchMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath() , HstRequestUtils.getRequestPath(request));     
        requestContext.setResolvedMount(mount);
        // now we can parse the url *with* a RESOLVED_MOUNT which is needed!        
        HstURLFactory factory = HstServices.getComponentManager().getComponent(HstURLFactory.class.getName());
        HstContainerURL hstContainerURL = factory.getContainerURLProvider().parseURL(request, response, mount);
        ResolvedSiteMapItem resolvedSiteMapItem = mount.matchSiteMapItem(hstContainerURL.getPathInfo());
        requestContext.setBaseURL(hstContainerURL);
        requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
        return requestContext;
    }
}
