/*
 *  Copyright 2011-2021 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentsConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.platform.HstModelProvider;
import org.hippoecm.hst.platform.api.model.InternalHstModel;
import org.hippoecm.hst.platform.configuration.components.HstComponentConfigurationService;
import org.hippoecm.hst.platform.configuration.components.HstComponentsConfigurationService;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.hippoecm.hst.util.JcrSessionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class HstComponentsConfigurationModelsIT extends AbstractTestConfigurations {

    private HstManager hstManager;
    private InternalHstModel hstModel;
    private Session session;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        hstManager = getComponent(HstManager.class.getName());
        final HstModelProvider provider = HstServices.getComponentManager().getComponent(HstModelProvider.class);
        hstModel = (InternalHstModel) provider.getHstModel();
        this.session = createSession();
        createHstConfigBackup(session);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        restoreHstConfigBackup(session);
        session.logout();
        super.tearDown();
    }


    /**
     * All the mounts that point to the exact same hst:configuration node MUST also share the same
     * {@link HstComponentsConfigurationService}. This does not
     * hold for the from inherited configuration: namely if a configuration inherits from another one, the
     *  org.hippoecm.hst.configuration.components.HstComponentConfiguration#isInherited() flag is typically differently .
     *  If the {@link HstComponentConfigurationService}
     * is not shared, memory footprint of the HST model will become very large and loading of the model
     * becomes slow. The model MUST always be shared. This integration test verifies that
     * <p/>
     * For the unittest content we have all the available hst:configuration inherit from the hst:configuration
     * 'unittestcommon' :
     * This is the configuration that contains the pages, components and templates for all the {@link Mount}s. Thus,
     * all
     * {@link HstComponentsConfigurationService}s for the unittest model should be the same, EXCEPT for
     * the Mounts that point to '/hst:hst/hst:sites/unittestsubproject' which in turn refers to
     * '/hst:hst/hst:configurations/unittestsubproject' : This unittestsubproject has its own hst:pages/homepage (which
     * is only meant for this unit test to validate the
     * correct working of inheriting {@link HstComponentsConfigurationService}s only
     * when all pages, components, templates and catalogs are the same)
     *
     * @throws Exception
     */
    @Test
    public void testSharedHstComponentsConfigurations() throws Exception {
        // since unittestproject contains its own 'hst:prototypepages' node, we first move this node away (otherwise
        // instance won't be shared)
        removePagePrototypeFromConfig(session);
        // wait for the jcr events to really have been all processed
        Thread.sleep(100);
        // make sure model is fully reloaded the first time
        hstModel.invalidate();

        ResolvedMount mount1 = hstManager.getVirtualHosts().matchMount("www.unit.test",  "/");
        ResolvedMount mount2 = hstManager.getVirtualHosts().matchMount("m.unit.test",  "/");
        ResolvedMount mount4 = hstManager.getVirtualHosts().matchMount("preview.unit.test",  "/");
        ResolvedMount mount5 = hstManager.getVirtualHosts().matchMount("www.unit.partial",  "/");
        ResolvedMount mount6 = hstManager.getVirtualHosts().matchMount("www.unit.partial",  "/sub1");
        ResolvedMount mount7 = hstManager.getVirtualHosts().matchMount("www.unit.partial",  "/sub1/subsub1");
        ResolvedMount mount8 = hstManager.getVirtualHosts().matchMount("www.unit.partial",  "/sub2");
        ResolvedMount mount9 = hstManager.getVirtualHosts().matchMount("localhost",  "/");
        ResolvedMount mount10 = hstManager.getVirtualHosts().matchMount("localhost:8081",  "/");
        ResolvedMount mount11 = hstManager.getVirtualHosts().matchMount("localhost",  "/preview");

        assertNotSame(mount1, mount2);
        assertNotSame(mount1, mount4);
        assertNotSame(mount1, mount5);

        // because sub.unit.test and localhost/subsite have hst:mountpoint equal to '/hst:hst/hst:sites/unittestsubproject'
        // which in turn points to '/hst:hst/hst:configurations/unittestsubproject' which inherits
        // all pages, components and catalogs from unittestcommon, *but* has its own hst:pages/homepage,
        // its model cannot be shared with the one from only the unittestcommon
        ResolvedMount mountWithDiffHstCompServ1 = hstManager.getVirtualHosts().matchMount("sub.unit.test",  "/");
        ResolvedMount mountWithDiffHstCompServ2 = hstManager.getVirtualHosts().matchMount("localhost",  "/subsite");

        final HstSite hstSite1 = mount1.getMount().getHstSite();
        final HstSite hstSite2 = mount2.getMount().getHstSite();
        final HstSite hstSite4 = mount4.getMount().getHstSite();
        final HstSite hstSite5 = mount5.getMount().getHstSite();

        assertNotSame(hstSite1, hstSite2);
        assertNotSame(hstSite1, hstSite4);
        assertNotSame(hstSite1, hstSite5);

        HstComponentsConfiguration service1 = hstSite1.getComponentsConfiguration();
        HstComponentsConfiguration service2 = hstSite2.getComponentsConfiguration();
        HstComponentsConfiguration service4 = hstSite4.getComponentsConfiguration();
        HstComponentsConfiguration service5 = hstSite5.getComponentsConfiguration();
        HstComponentsConfiguration service6 = mount6.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration service7 = mount7.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration service8 = mount8.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration service9 = mount9.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration service10 = mount10.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration service11 = mount11.getMount().getHstSite().getComponentsConfiguration();

        HstComponentsConfiguration WithDiffHstCompServ1 = mountWithDiffHstCompServ1.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration WithDiffHstCompServ2 = mountWithDiffHstCompServ2.getMount().getHstSite().getComponentsConfiguration();

        assertSame("Expected shared HstComponentsConfiguration objects failed", service1, service2);
        assertSame("Expected shared HstComponentsConfiguration objects failed", service1, service4);
        // service 5-8 have an hst config that inherits from service1 hence must have different inherit flag
        assertNotSame("Expected different HstComponentsConfiguration objects failed", service1, service5);
        assertNotSame("Expected different HstComponentsConfiguration objects failed", service1, service6);
        assertNotSame("Expected different HstComponentsConfiguration objects failed", service1, service7);
        assertNotSame("Expected different HstComponentsConfiguration objects failed", service1, service8);
        assertSame("Expected shared HstComponentsConfiguration objects failed", service1, service9);
        assertSame("Expected shared HstComponentsConfiguration objects failed", service1, service10);
        assertSame("Expected shared HstComponentsConfiguration objects failed", service1, service11);
        assertSame("Expected shared HstComponentsConfiguration objects failed", service9, service11);

        assertNotSame("Expected non shared HstComponentsConfiguration objects failed", service1, WithDiffHstCompServ1);
        assertNotSame("Expected non shared HstComponentsConfiguration objects failed", service1, WithDiffHstCompServ2);

    }

    /**
     * HstComponentsConfigurationService instance are reused between models after changes if the changes
     * could never have effect on them.
     *
     * @throws Exception
     */
    @Test
    public void testReloadOnlyChangedHstComponentsConfigurations() throws Exception {
        removePagePrototypeFromConfig(session);

        // wait for the jcr events to really have been all processed
        Thread.sleep(100);
        // make sure model is fully reloaded the first time
        hstModel.invalidate();

        final ResolvedMount mountBefore1 = hstManager.getVirtualHosts().matchMount("www.unit.test", "/");
        final ResolvedMount mountBefore2 = hstManager.getVirtualHosts().matchMount("www.unit.partial",  "/");

        final HstSite hstSiteBefore1 = mountBefore1.getMount().getHstSite();
        final HstSite hstSiteBefore2 = mountBefore2.getMount().getHstSite();

        assertNotSame(hstSiteBefore1, hstSiteBefore2);

        final HstComponentsConfiguration componenentConfigsBefore1 = hstSiteBefore1.getComponentsConfiguration();
        final HstComponentsConfiguration componenentConfigsBefore2 = hstSiteBefore2.getComponentsConfiguration();

        // there is inheritance from global to unittest site hence not the same
        assertNotSame(componenentConfigsBefore1, componenentConfigsBefore2);

        Node globalConfig = session.getNode("/hst:hst/hst:configurations/global");
        globalConfig.addNode("hst:pages", "hst:pages");

        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);

        session.save();
        invalidator.eventPaths(pathsToBeChanged);

        final ResolvedMount mountAfter1 = hstManager.getVirtualHosts().matchMount("www.unit.test",  "/");
        final ResolvedMount mountAfter2 = hstManager.getVirtualHosts().matchMount("www.unit.partial",  "/");

        final HstSite hstSiteAfter1 = mountAfter1.getMount().getHstSite();
        final HstSite hstSiteAfter2 = mountAfter2.getMount().getHstSite();

        assertNotSame(hstSiteAfter1, hstSiteAfter2);

        assertNotSame(hstSiteAfter1, hstSiteBefore1);
        assertNotSame(hstSiteAfter2, hstSiteBefore2);

        final HstComponentsConfiguration componenentConfigsAfter1 = hstSiteAfter1.getComponentsConfiguration();
        final HstComponentsConfiguration componenentConfigsAfter2 = hstSiteAfter2.getComponentsConfiguration();

        // the configuration for www.unit.test which uses /hst:hst/hst:configurations/unittestproject did not change
        // and should be reused!
        assertSame(componenentConfigsAfter1, componenentConfigsBefore1);

        // the componenentConfigsAfter2 should have been changed because
        // hst:hst/hst:configurations/global has a new component node added
        assertNotSame(componenentConfigsAfter2, componenentConfigsBefore2);

        // remove node again
        session.getNode("/hst:hst/hst:configurations/global").getNode("hst:pages").remove();
        pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);

        // now we expect the same instance again from cache
        final ResolvedMount mountSecondAfter1 = hstManager.getVirtualHosts().matchMount("www.unit.test",  "/");
        final ResolvedMount mountSecondAfter2 = hstManager.getVirtualHosts().matchMount("www.unit.partial",  "/");

        final HstSite hstSiteSecondAfter1 = mountSecondAfter1.getMount().getHstSite();
        final HstSite hstSiteSecondAfter2 = mountSecondAfter2.getMount().getHstSite();

        assertNotSame(hstSiteSecondAfter1, hstSiteSecondAfter2);

        assertNotSame(hstSiteSecondAfter1, hstSiteBefore1);
        assertNotSame(hstSiteSecondAfter2, hstSiteBefore2);

        final HstComponentsConfiguration componenentConfigsSecondAfter1 = hstSiteSecondAfter1.getComponentsConfiguration();
        final HstComponentsConfiguration componenentConfigsSecondAfter2 = hstSiteSecondAfter2.getComponentsConfiguration();

        assertNotSame(componenentConfigsSecondAfter1, componenentConfigsSecondAfter2);

        // for www.unit.test nothing changed
        assertSame(componenentConfigsSecondAfter1, componenentConfigsBefore1);

        // now we add a hst:sitemenus nodes : This node is not part of HstComponentsConfiguration and should thus not
        // influence the instances

        globalConfig = session.getNode("/hst:hst/hst:configurations/global");
        globalConfig.addNode("hst:sitemenus", "hst:sitemenus");
        pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);

        // there is still inheritance from global to unittest site hence not the same
        final ResolvedMount mountThirdAfter1 = hstManager.getVirtualHosts().matchMount("www.unit.test",  "/");
        final ResolvedMount mountThirdAfter2 = hstManager.getVirtualHosts().matchMount("www.unit.partial",  "/");

        final HstSite hstSiteThirdAfter1 = mountThirdAfter1.getMount().getHstSite();
        final HstSite hstSiteThirdAfter2 = mountThirdAfter2.getMount().getHstSite();

        assertNotSame(hstSiteThirdAfter1, hstSiteThirdAfter2);

        assertNotSame(hstSiteThirdAfter1, hstSiteBefore1);
        assertNotSame(hstSiteThirdAfter2, hstSiteBefore2);

        final HstComponentsConfiguration componenentConfigsThirdAfter1 = hstSiteThirdAfter1.getComponentsConfiguration();
        final HstComponentsConfiguration componenentConfigsThirdAfter2 = hstSiteThirdAfter2.getComponentsConfiguration();

        assertNotSame(componenentConfigsThirdAfter1, componenentConfigsThirdAfter2);
        // for www.unit.test nothing changed
        assertSame(componenentConfigsThirdAfter1, componenentConfigsBefore1);

    }


    @Test
    public void testNewUniqueNamedNodeInHstDefaultConfigurationTriggersReloadAll() throws Exception {
        ResolvedMount mount1 = hstManager.getVirtualHosts().matchMount("www.unit.test", "/");
        ResolvedMount mount2 = hstManager.getVirtualHosts().matchMount("m.unit.test", "/");
        ResolvedMount mount4 = hstManager.getVirtualHosts().matchMount("preview.unit.test",  "/");
        ResolvedMount mount5 = hstManager.getVirtualHosts().matchMount("www.unit.partial", "/");
        ResolvedMount mount6 = hstManager.getVirtualHosts().matchMount("www.unit.partial", "/sub1");
        ResolvedMount mount7 = hstManager.getVirtualHosts().matchMount("www.unit.partial",  "/sub1/subsub1");

        HstComponentsConfiguration service1 = mount1.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration service2 = mount2.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration service4 = mount4.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration service5 = mount5.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration service6 = mount6.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration service7 = mount7.getMount().getHstSite().getComponentsConfiguration();

        Node defaultComponents = session.getNode("/hst:hst/hst:configurations/hst:default/hst:components");
        defaultComponents.addNode("testNewUniqueNamedNodeInHstDefaultConfigurationTriggersReloadAll", "hst:component");

        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
        session.save();
        Thread.sleep(100);
        invalidator.eventPaths(pathsToBeChanged);

        ResolvedMount mountAfter1 = hstManager.getVirtualHosts().matchMount("www.unit.test",  "/");
        ResolvedMount mountAfter2 = hstManager.getVirtualHosts().matchMount("m.unit.test", "/");
        ResolvedMount mountAfter4 = hstManager.getVirtualHosts().matchMount("preview.unit.test",  "/");
        ResolvedMount mountAfter5 = hstManager.getVirtualHosts().matchMount("www.unit.partial", "/");
        ResolvedMount mountAfter6 = hstManager.getVirtualHosts().matchMount("www.unit.partial", "/sub1");
        ResolvedMount mountAfter7 = hstManager.getVirtualHosts().matchMount("www.unit.partial", "/sub1/subsub1");

        HstComponentsConfiguration serviceAfter1 = mountAfter1.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration serviceAfter2 = mountAfter2.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration serviceAfter4 = mountAfter4.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration serviceAfter5 = mountAfter5.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration serviceAfter6 = mountAfter6.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration serviceAfter7 = mountAfter7.getMount().getHstSite().getComponentsConfiguration();

        assertNotSame(service1, serviceAfter1);
        assertNotSame(service2, serviceAfter2);
        assertNotSame(service4, serviceAfter4);
        assertNotSame(service5, serviceAfter5);
        assertNotSame(service6, serviceAfter6);
        assertNotSame(service7, serviceAfter7);

        // now a change below hst:default/hst:sitemap : This should not influence the HstComponentsConfiguration objects

        Node defaultSitemap = session.getNode("/hst:hst/hst:configurations/hst:default/hst:sitemap");
        defaultSitemap.addNode("testNewUniqueNamedNodeInHstDefaultConfigurationTriggersReloadAll", "hst:sitemapitem");
        session.save();
        Thread.sleep(100);

        ResolvedMount mountAgain1 = hstManager.getVirtualHosts().matchMount("www.unit.test", "/");
        ResolvedMount mountAgain2 = hstManager.getVirtualHosts().matchMount("m.unit.test",  "/");
        ResolvedMount mountAgain4 = hstManager.getVirtualHosts().matchMount("preview.unit.test", "/");
        ResolvedMount mountAgain5 = hstManager.getVirtualHosts().matchMount("www.unit.partial", "/");
        ResolvedMount mountAgain6 = hstManager.getVirtualHosts().matchMount("www.unit.partial",  "/sub1");
        ResolvedMount mountAgain7 = hstManager.getVirtualHosts().matchMount("www.unit.partial", "/sub1/subsub1");

        HstComponentsConfiguration serviceAgain1 = mountAgain1.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration serviceAgain2 = mountAgain2.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration serviceAgain4 = mountAgain4.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration serviceAgain5 = mountAgain5.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration serviceAgain6 = mountAgain6.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration serviceAgain7 = mountAgain7.getMount().getHstSite().getComponentsConfiguration();

        assertSame(serviceAgain1, serviceAfter1);
        assertSame(serviceAgain2, serviceAfter2);
        assertSame(serviceAgain4, serviceAfter4);
        assertSame(serviceAgain5, serviceAfter5);
        assertSame(serviceAgain6, serviceAfter6);
        assertSame(serviceAgain7, serviceAfter7);

    }

    @Test
    public void testNewUniqueNamedNodeInCommonCatalogTriggersReloadAll() throws Exception {
        ResolvedMount mount1 = hstManager.getVirtualHosts().matchMount("www.unit.test",  "/");
        ResolvedMount mount2 = hstManager.getVirtualHosts().matchMount("m.unit.test", "/");
        ResolvedMount mount4 = hstManager.getVirtualHosts().matchMount("preview.unit.test",  "/");
        ResolvedMount mount5 = hstManager.getVirtualHosts().matchMount("www.unit.partial",  "/");
        ResolvedMount mount6 = hstManager.getVirtualHosts().matchMount("www.unit.partial",  "/sub1");
        ResolvedMount mount7 = hstManager.getVirtualHosts().matchMount("www.unit.partial", "/sub1/subsub1");

        HstComponentsConfiguration service1 = mount1.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration service2 = mount2.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration service4 = mount4.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration service5 = mount5.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration service6 = mount6.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration service7 = mount7.getMount().getHstSite().getComponentsConfiguration();

        Node configurationsNode = session.getNode("/hst:hst/hst:configurations");
        Node commonCatalog = configurationsNode.addNode("hst:catalog", "hst:catalog");
        commonCatalog.addNode("testNewUniqueNamedNodeInCommonCatalogTriggersReloadAll", "hst:containeritempackage");

        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);

        ResolvedMount mountAfter1 = hstManager.getVirtualHosts().matchMount("www.unit.test",  "/");
        ResolvedMount mountAfter2 = hstManager.getVirtualHosts().matchMount("m.unit.test",  "/");
        ResolvedMount mountAfter4 = hstManager.getVirtualHosts().matchMount("preview.unit.test",  "/");
        ResolvedMount mountAfter5 = hstManager.getVirtualHosts().matchMount("www.unit.partial",  "/");
        ResolvedMount mountAfter6 = hstManager.getVirtualHosts().matchMount("www.unit.partial",  "/sub1");
        ResolvedMount mountAfter7 = hstManager.getVirtualHosts().matchMount("www.unit.partial",  "/sub1/subsub1");

        HstComponentsConfiguration serviceAfter1 = mountAfter1.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration serviceAfter2 = mountAfter2.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration serviceAfter4 = mountAfter4.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration serviceAfter5 = mountAfter5.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration serviceAfter6 = mountAfter6.getMount().getHstSite().getComponentsConfiguration();
        HstComponentsConfiguration serviceAfter7 = mountAfter7.getMount().getHstSite().getComponentsConfiguration();

        assertNotSame(service1, serviceAfter1);
        assertNotSame(service2, serviceAfter2);
        assertNotSame(service4, serviceAfter4);
        assertNotSame(service5, serviceAfter5);
        assertNotSame(service6, serviceAfter6);
        assertNotSame(service7, serviceAfter7);

    }

    @Test
    public void testHstComponentConfigurationFromCommonAreInherited() throws Exception {
        // since all hst:pages and hst:components are inherited through hst:inheritsfrom = ../unittestcommon, we expect
        // all HstComponentConfiguration instances to be marked as 'inherited'
        // there is one explicit non inherited component, and that is
        // hst:hst/hst:configurations/unittestsubproject/hst:pages/homepage

        final String explicitNonInheritedComponent = "/hst:hst/hst:configurations/unittestsubproject/hst:pages/homepage";
        final VirtualHosts virtualHosts = hstManager.getVirtualHosts();
        final List<String> hostGroupNames = virtualHosts.getHostGroupNames();
        for (String hostGroupName : hostGroupNames) {
            final List<Mount> mountsByHostGroup = virtualHosts.getMountsByHostGroup(hostGroupName);
            for (Mount mount : mountsByHostGroup) {
                if (mount.getHstSite() == null) {
                    continue;
                }
                if (mount.getHstSite().getComponentsConfiguration() == null) {
                    continue;
                }
                for (HstComponentConfiguration hstComponentConfiguration : mount.getHstSite().getComponentsConfiguration().getComponentConfigurations().values()) {
                    assertAllComponentsAreInheritedExceptXPages(hstComponentConfiguration, explicitNonInheritedComponent, mount.getHstSite().getConfigurationPath());
                }
            }
        }

    }

    private void assertAllComponentsAreInheritedExceptXPages(final HstComponentConfiguration compConfig,
                                                             final String explicitNonInheritedComponent,
                                                             final String siteConfigurationPath) {

        if (explicitNonInheritedComponent.equals(compConfig.getCanonicalStoredLocation()) && siteConfigurationPath.equals("/hst:hst/hst:configurations/unittestsubproject")) {
            assertFalse(compConfig.isInherited());
        } else if (compConfig.getCanonicalStoredLocation().contains("/hst:xpages/")) {
            assertFalse("hst:xpages should never be inherited", compConfig.isInherited());
        } else {
            assertTrue(compConfig.isInherited());
        }

        for (HstComponentConfiguration child : compConfig.getChildren().values()) {
            assertAllComponentsAreInheritedExceptXPages(child, explicitNonInheritedComponent, siteConfigurationPath);
        }
    }

    private void removePagePrototypeFromConfig(final Session session) throws RepositoryException {
        session.getNode("/hst:hst/hst:configurations/unittestproject/hst:prototypepages").remove();
        session.save();
    }

}
