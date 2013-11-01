/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNotSame;
import org.hippoecm.hst.configuration.components.HstComponentConfigurationService;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfigurationService;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.core.beans.AbstractBeanTestCase;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestSharedHstComponentsConfigurationModels extends AbstractTestConfigurations {

    private HstManager hstManager;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.hstManager = getComponent(HstManager.class.getName());
    }
    
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        
    }
    
    /**
     * All the mounts that share the same items for:
     * <ol>
     *  <li>hst:pages</li>
     *  <li>hst:components</li>
     *  <li>hst:templates</li>
     *  <li>hst:catalog</li>
     *  <li>hst:workspace</li>
     * </ol>
     * 
     * MUST also share the same {@link HstComponentsConfigurationService}. This also 
     * holds of course of the items above are coming from inherited configuration. If the {@link HstComponentConfigurationService}
     * is not shared, memory footprint of the HST model will become very large and loading of the model 
     * becomes slow. The model MUST always be shared. This unit test verifies that
     * 
     * For the unittest content we have all the available hst:configuration inherit from the hst:configuration 'unittestcommon' :
     * This is the configuration that contains the pages, components and templates for all the {@link Mount}s. Thus, all 
     * {@link HstComponentsConfigurationService}s for the unittest model should be the same, EXCEPT for 
     * the Mounts that point to '/hst:hst/hst:sites/unittestsubproject' which in turn refers to   
     * '/hst:hst/hst:configurations/unittestsubproject' : This unittestsubproject has its own hst:pages/homepage (which is only meant for this unit test to validate the 
     * correct working of inheriting {@link HstComponentsConfigurationService}s only
     * when all pages, components, templates and catalogs are the same)
     * 
     * @throws Exception
     */
    @Test
    public void testSharedHstComponentsConfigurationServices() throws Exception {

        ResolvedMount mount1 = hstManager.getVirtualHosts().matchMount("www.unit.test", "", "/");
        ResolvedMount mount2 = hstManager.getVirtualHosts().matchMount("m.unit.test", "", "/");
        ResolvedMount mount4 = hstManager.getVirtualHosts().matchMount("preview.unit.test", "", "/");
        ResolvedMount mount5 = hstManager.getVirtualHosts().matchMount("www.unit.partial", "", "/");
        ResolvedMount mount6 = hstManager.getVirtualHosts().matchMount("www.unit.partial", "", "/sub1");
        ResolvedMount mount7 = hstManager.getVirtualHosts().matchMount("www.unit.partial", "", "/sub1/subsub1");
        ResolvedMount mount8 = hstManager.getVirtualHosts().matchMount("www.unit.partial", "", "/sub2");
        ResolvedMount mount9 = hstManager.getVirtualHosts().matchMount("localhost", "", "/");
        ResolvedMount mount10 = hstManager.getVirtualHosts().matchMount("localhost:8081", "", "/");
        ResolvedMount mount11 = hstManager.getVirtualHosts().matchMount("localhost", "", "/preview");
        
        // because sub.unit.test and localhost/subsite have hst:mountpoint equal to '/hst:hst/hst:sites/unittestsubproject'
        // which in turn points to '/hst:hst/hst:configurations/unittestsubproject' which inherits
        // all pages, components and catalogs from unittestcommon, *but* has its own hst:pages/homepage,
        // its model cannot be shared with the one from only the unittestcommon
        ResolvedMount mountWithDiffHstCompServ1 = hstManager.getVirtualHosts().matchMount("sub.unit.test", "", "/");
        ResolvedMount mountWithDiffHstCompServ2 = hstManager.getVirtualHosts().matchMount("localhost", "", "/subsite");
        
        HstComponentsConfiguration service1 = mount1.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration service2 = mount2.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration service4 = mount4.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration service5 = mount5.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration service6 = mount6.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration service7 = mount7.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration service8 = mount8.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration service9 = mount9.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration service10 = mount10.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration service11 = mount11.getMount().getHstSite().getComponentsConfiguration();
        
        HstComponentsConfiguration WithDiffHstCompServ1 = mountWithDiffHstCompServ1.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration WithDiffHstCompServ2 = mountWithDiffHstCompServ2.getMount().getHstSite().getComponentsConfiguration();

        assertSame("Expected shared HstComponentsConfiguration objects failed", service1 , service2);
        assertSame("Expected shared HstComponentsConfiguration objects failed", service1 , service4);
        assertSame("Expected shared HstComponentsConfiguration objects failed", service1 , service5);
        assertSame("Expected shared HstComponentsConfiguration objects failed", service1 , service6);
        assertSame("Expected shared HstComponentsConfiguration objects failed", service1 , service7);
        assertSame("Expected shared HstComponentsConfiguration objects failed", service1 , service8);
        assertSame("Expected shared HstComponentsConfiguration objects failed", service1 , service9);
        assertSame("Expected shared HstComponentsConfiguration objects failed", service1 , service10);
        assertSame("Expected shared HstComponentsConfiguration objects failed", service1 , service11);

        assertNotSame("Expected non shared HstComponentsConfiguration objects failed", service1 , WithDiffHstCompServ1);
        assertNotSame("Expected non shared HstComponentsConfiguration objects failed", service1 , WithDiffHstCompServ2);
    }
    
    
}
