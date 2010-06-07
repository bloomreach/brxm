/*
 *  Copyright 2008 Hippo.
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
import org.hippoecm.hst.configuration.hosting.SiteMount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.hosting.VirtualHostsManager;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.container.RepositoryNotAvailableException;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.internal.HstRequestContextComponent;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedSiteMount;
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
        this.componentManager = new SpringComponentManager(getContainerConfiguration());
        ((SpringComponentManager) this.componentManager).setConfigurationResources(getConfigurations());
        
        this.componentManager.initialize();
        this.componentManager.start();
    }

    @After
    public void tearDown() throws Exception {
        this.componentManager.stop();
        this.componentManager.close();
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
        return getComponentManager().<T>getComponent(name);
    }
    
    protected Configuration getContainerConfiguration() {
        PropertiesConfiguration propConf = new PropertiesConfiguration();
        return propConf;
    }
    
    protected void setResolvedSiteMount(HstMutableRequestContext requestContext) {
        
        ResolvedSiteMount resolvedSiteMount = createNiceMock(ResolvedSiteMount.class);
        SiteMount siteMount = createNiceMock(SiteMount.class);
        VirtualHost virtualHost = createNiceMock(VirtualHost.class);
        
        expect(resolvedSiteMount.getResolvedMountPath()).andReturn("").anyTimes();
        expect(resolvedSiteMount.getSiteMount()).andReturn(siteMount).anyTimes();
        expect(siteMount.getVirtualHost()).andReturn(virtualHost).anyTimes();
        expect(virtualHost.isContextPathInUrl()).andReturn(true).anyTimes();

        replay(resolvedSiteMount);
        replay(siteMount);
        replay(virtualHost);
        
        // to parse a url, there must be a ResolvedSiteMount on the HstRequestContext
        requestContext.setResolvedSiteMount(resolvedSiteMount);
    }
    

    protected HstRequestContext resolveRequest(HttpServletRequest request, HttpServletResponse response) throws RepositoryNotAvailableException {
        VirtualHostsManager virtualHostManager = HstServices.getComponentManager().getComponent(VirtualHostsManager.class.getName());
        VirtualHosts vHosts = virtualHostManager.getVirtualHosts();
        HstMutableRequestContext requestContext = ((HstRequestContextComponent)HstServices.getComponentManager().getComponent(HstRequestContextComponent.class.getName())).create(false);
        request.setAttribute(ContainerConstants.HST_REQUEST_CONTEXT, requestContext);
        ResolvedSiteMount mount = vHosts.matchSiteMount(HstRequestUtils.getFarthestRequestHost(request), request.getContextPath() , HstRequestUtils.getRequestPath(request));     
        requestContext.setResolvedSiteMount(mount);
        // now we can parse the url *with* a RESOLVED_SITEMOUNT which is needed!        
        HstURLFactory factory = (HstURLFactory)HstServices.getComponentManager().getComponent(HstURLFactory.class.getName());
        HstContainerURL hstContainerURL = factory.getContainerURLProvider().parseURL(request, response, mount);
        ResolvedSiteMapItem resolvedSiteMapItem = mount.matchSiteMapItem(hstContainerURL.getPathInfo());
        requestContext.setBaseURL(hstContainerURL);
        requestContext.setResolvedSiteMapItem(resolvedSiteMapItem);
        return requestContext;
    }
}
