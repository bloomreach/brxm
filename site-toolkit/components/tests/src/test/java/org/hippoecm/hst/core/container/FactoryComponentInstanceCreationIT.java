/*
 *  Copyright 2012-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import java.util.HashSet;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.core.component.HstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.platform.container.components.HstComponentRegistryImpl;
import org.hippoecm.hst.test.AbstractSpringTestCase;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FactoryComponentInstanceCreationIT extends AbstractSpringTestCase {

    private VirtualHost virtualHost;
    private VirtualHosts virtualHosts;


    /**
     * addAnnotatedClassesConfigurationParam must be added before super setUpClass, hence redefine same setUpClass method
     * to hide the super.setUpClass and invoke that explicitly
     */
    @BeforeClass
    public static void setUpClass() throws Exception {
        String classXmlFileName = FactoryComponentInstanceCreationIT.class.getName().replace(".", "/") + ".xml";
        AbstractSpringTestCase.addAnnotatedClassesConfigurationParam(classXmlFileName);
        AbstractSpringTestCase.setUpClass();
    }

    @AfterClass
    public static void afterClass() throws RepositoryException {
        String classXmlFileName = FactoryComponentInstanceCreationIT.class.getName().replace(".", "/") + ".xml";
        AbstractSpringTestCase.removeAnnotatedClassesConfigurationParam(classXmlFileName);
        AbstractSpringTestCase.afterClass();
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        virtualHost = createNiceMock(VirtualHost.class);
        virtualHosts = createNiceMock(VirtualHosts.class);
        expect(virtualHost.getVirtualHosts()).andStubReturn(virtualHosts);
        expect(virtualHosts.getComponentRegistry()).andStubReturn(new HstComponentRegistryImpl());
        replay(virtualHost, virtualHosts);
    }

    @Test
    public void testComponentInstances() {

        ResolvedSiteMapItem resolvedSiteMapItem = createNiceMock(ResolvedSiteMapItem.class);
        expect(resolvedSiteMapItem.isExperiencePage()).andReturn(false).anyTimes();

        Mount mount1 = createNiceMock(Mount.class);
        expect(mount1.getIdentifier()).andReturn("cafe-babe").anyTimes();
        expect(mount1.getVirtualHost()).andStubReturn(virtualHost);

        Mount mount2 = createNiceMock(Mount.class);
        // same identifier as mount1 on purpose!
        expect(mount2.getIdentifier()).andReturn("cafe-babe").anyTimes();
        expect(mount2.getVirtualHost()).andStubReturn(virtualHost);

        ResolvedMount resolvedMount = createNiceMock(ResolvedMount.class);
        expect(resolvedMount.getMount()).andReturn(mount1).anyTimes();

        HstRequestContext requestContext = createNiceMock(HstRequestContext.class);
        expect(requestContext.getResolvedMount()).andReturn(resolvedMount).anyTimes();
        expect(requestContext.getComponentFilterTags()).andReturn(new HashSet<String>()).anyTimes();
        expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem).anyTimes();

        try {
            ModifiableRequestContextProvider.set(requestContext);
            // mock environment
            HstContainerConfig mockHstContainerConfig = createNiceMock(HstContainerConfig.class);
            HstComponentConfiguration compConfig = createNiceMock(HstComponentConfiguration.class);
            //expect(compConfig.getReferenceName()).andReturn("refName").anyTimes();
            expect(compConfig.getId()).andReturn("some//path").anyTimes();

            replay(mockHstContainerConfig, compConfig, mount1, mount2, resolvedMount, resolvedSiteMapItem, requestContext);

            HstComponentFactory componentFactory = getComponent(HstComponentFactory.class.getName());
            HstComponent componentInstance1 = componentFactory.getComponentInstance(mockHstContainerConfig, compConfig, mount1);
            HstComponent componentInstance2 = componentFactory.getComponentInstance(mockHstContainerConfig, compConfig, mount1);

            assertTrue("Component instances should be same for same mount and compConfig instance", componentInstance1 == componentInstance2);

            // now create an instance with mount2. Even though mount2 has the same identifier as mount1, still
            // a different component instance should be created this time, because the hashCode is also accounted for by
            // the componentFactory. If the hashCode by coincidence is the same, the component instance would be the same
            // hence also check for the hashCode to be sure
            HstComponent componentInstance3 = componentFactory.getComponentInstance(mockHstContainerConfig, compConfig, mount2);

            if (mount2.hashCode() != mount1.hashCode()) {
                assertTrue("Component instances should not be the same because different mount instances", componentInstance1 != componentInstance3);
            }

            // now for an XPage component: XPage components created for an XPage request should not end in the registry, hence
            // uniquely created every time

            reset(resolvedSiteMapItem);
            expect(resolvedSiteMapItem.isExperiencePage()).andReturn(true).anyTimes();
            replay(resolvedSiteMapItem);

            HstComponent componentInstance4 = componentFactory.getComponentInstance(mockHstContainerConfig, compConfig, mount1);
            HstComponent componentInstance5 = componentFactory.getComponentInstance(mockHstContainerConfig, compConfig, mount1);

            assertFalse("Component instances should NEVER be same for EXPERIENCE PAGE", componentInstance4 == componentInstance5);


        } finally {
            ModifiableRequestContextProvider.clear();
        }

    }

    @Test
    public void testComponentCreationFails() {

        ResolvedSiteMapItem resolvedSiteMapItem = createNiceMock(ResolvedSiteMapItem.class);
        expect(resolvedSiteMapItem.isExperiencePage()).andReturn(false).anyTimes();

        Mount mount1 = createNiceMock(Mount.class);
        expect(mount1.getIdentifier()).andReturn("cafe-babe").anyTimes();
        expect(mount1.getVirtualHost()).andStubReturn(virtualHost);

        ResolvedMount resolvedMount = createNiceMock(ResolvedMount.class);
        expect(resolvedMount.getMount()).andReturn(mount1).anyTimes();

        HstRequestContext requestContext = createNiceMock(HstRequestContext.class);
        expect(requestContext.getResolvedMount()).andReturn(resolvedMount).anyTimes();
        expect(requestContext.getComponentFilterTags()).andReturn(new HashSet<>()).anyTimes();
        expect(requestContext.getResolvedSiteMapItem()).andReturn(resolvedSiteMapItem).anyTimes();

        try {
            ModifiableRequestContextProvider.set(requestContext);
            HstContainerConfig mockHstContainerConfig = createNiceMock(HstContainerConfig.class);
            HstComponentConfiguration compConfig = createNiceMock(HstComponentConfiguration.class);
            expect(compConfig.getId()).andReturn("some//path").anyTimes();
            expect(compConfig.getComponentClassName()).andReturn("org.hippoecm.hst.core.container.NotExistingClass").anyTimes();

            replay(mockHstContainerConfig, compConfig, mount1, resolvedMount, resolvedSiteMapItem, requestContext);

            HstComponentFactory componentFactory = getComponent(HstComponentFactory.class.getName());
            try {
                componentFactory.getComponentInstance(mockHstContainerConfig, compConfig, mount1);
                fail("Exception should had been thrown");
            } catch (HstComponentException e) {
                try {
                    componentFactory.getComponentInstance(mockHstContainerConfig, compConfig, mount1);
                    fail("Exception should had been thrown");
                } catch (HstComponentException e2) {
                    // second failure should result in exact same exception instance
                    assertTrue(e == e2);
                }
            }
        } finally {
            ModifiableRequestContextProvider.clear();
        }

    }

}
