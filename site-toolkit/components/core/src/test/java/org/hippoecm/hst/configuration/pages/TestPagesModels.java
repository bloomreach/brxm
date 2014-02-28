/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.configuration.pages;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import junit.framework.Assert;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.model.EventPathsInvalidator;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestPagesModels extends AbstractTestConfigurations {

    private HstManager hstManager;
    private HippoSession session;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        session = createSession();
        createHstConfigBackup(session);
        hstManager = getComponent(HstManager.class.getName());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        restoreHstConfigBackup(session);
        session.logout();
        super.tearDown();
    }


    protected HippoSession createSession() throws RepositoryException {
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        return (HippoSession)repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
    }

    @Test
    public void test_unittestcommon_pages_without_workspace_pages() throws Exception {
        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        for (HstComponentConfiguration hstComponentConfiguration : hstSite.getComponentsConfiguration().getComponentConfigurations().values()) {
            assertTrue(hstComponentConfiguration.isInherited());
            assertTrue(hstComponentConfiguration.getCanonicalStoredLocation().startsWith("/hst:hst/hst:configurations/unittestcommon"));
        }
    }

    @Test
    public void test_unittestproject_pages_without_workspace_pages() throws Exception {
        session.move("/hst:hst/hst:configurations/unittestcommon/hst:pages",
                "/hst:hst/hst:configurations/unittestproject/hst:pages");
        session.save();
        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        for (HstComponentConfiguration hstComponentConfiguration : hstSite.getComponentsConfiguration().getComponentConfigurations().values()) {
            if (hstComponentConfiguration.getCanonicalStoredLocation().startsWith("/hst:hst/hst:configurations/unittestcommon/")) {
                // components are still from /unittestcommon
                assertTrue(hstComponentConfiguration.isInherited());
            } else {
                assertFalse(hstComponentConfiguration.isInherited());
            }
        }
    }

    @Test
    public void test_pages_completely_in_workspace() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestproject").addNode("hst:workspace");
        session.move("/hst:hst/hst:configurations/unittestcommon/hst:pages",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages");
        session.save();
        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        for (HstComponentConfiguration hstComponentConfiguration : hstSite.getComponentsConfiguration().getComponentConfigurations().values()) {
            if (hstComponentConfiguration.getCanonicalStoredLocation().startsWith("/hst:hst/hst:configurations/unittestcommon/")) {
                // components are still from /unittestcommon
                assertTrue(hstComponentConfiguration.isInherited());
            } else {
                assertFalse(hstComponentConfiguration.isInherited());
            }
        }
    }

    @Test
    public void test_page_partially_in_workspace() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestproject").addNode("hst:workspace");
        session.move("/hst:hst/hst:configurations/unittestcommon/hst:pages",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages");

        session.getNode("/hst:hst/hst:configurations/unittestproject").addNode("hst:pages");
        session.move("/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/standarddetail",
                "/hst:hst/hst:configurations/unittestproject/hst:pages/standarddetail");

        session.save();

        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        for (HstComponentConfiguration hstComponentConfiguration : hstSite.getComponentsConfiguration().getComponentConfigurations().values()) {
            if (hstComponentConfiguration.getCanonicalStoredLocation().startsWith("/hst:hst/hst:configurations/unittestcommon/")) {
                // components are still from /unittestcommon
                assertTrue(hstComponentConfiguration.isInherited());
            } else {
                HstComponentConfiguration root = hstComponentConfiguration;
                while (root.getParent() != null) {
                    root = root.getParent();
                }
                if (root.getName().equals("standarddetail")) {
                    assertFalse(hstComponentConfiguration.isInherited());
                    assertTrue(hstComponentConfiguration.getCanonicalStoredLocation().startsWith("/hst:hst/hst:configurations/unittestproject/hst:pages/standarddetail"));
                } else {
                    assertFalse(hstComponentConfiguration.isInherited());
                    assertTrue(hstComponentConfiguration.getCanonicalStoredLocation().startsWith("/hst:hst/hst:configurations/unittestproject/hst:workspace"));
                }
            }
        }
    }

    @Test
    public void test_page_duplicate_node_in_workspace_is_skipped() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestproject").addNode("hst:workspace");
        session.move("/hst:hst/hst:configurations/unittestcommon/hst:pages",
                "/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages");

        session.getNode("/hst:hst/hst:configurations/unittestproject").addNode("hst:pages");

        JcrUtils.copy(session,"/hst:hst/hst:configurations/unittestproject/hst:workspace/hst:pages/standarddetail",
                "/hst:hst/hst:configurations/unittestproject/hst:pages/standarddetail");

        session.save();

        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        for (HstComponentConfiguration hstComponentConfiguration : hstSite.getComponentsConfiguration().getComponentConfigurations().values()) {
            if (hstComponentConfiguration.getCanonicalStoredLocation().startsWith("/hst:hst/hst:configurations/unittestcommon/")) {
                // components are still from /unittestcommon
                assertTrue(hstComponentConfiguration.isInherited());
            } else {
                HstComponentConfiguration root = hstComponentConfiguration;
                while (root.getParent() != null) {
                    root = root.getParent();
                }
                if (root.getName().equals("standarddetail")) {
                    assertFalse(hstComponentConfiguration.isInherited());
                    assertTrue(hstComponentConfiguration.getCanonicalStoredLocation().startsWith("/hst:hst/hst:configurations/unittestproject/hst:pages/standarddetail"));
                } else {
                    assertFalse(hstComponentConfiguration.isInherited());
                    assertTrue(hstComponentConfiguration.getCanonicalStoredLocation().startsWith("/hst:hst/hst:configurations/unittestproject/hst:workspace"));
                }
            }
        }
    }

    @Test
    public void test_workspace_in_inherited_pages_is_ignored() throws Exception {
        session.getNode("/hst:hst/hst:configurations/unittestcommon").addNode("hst:workspace");
        session.move("/hst:hst/hst:configurations/unittestcommon/hst:pages",
                "/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:pages");
        session.save();
        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        for (HstComponentConfiguration hstComponentConfiguration : hstSite.getComponentsConfiguration().getComponentConfigurations().values()) {
            assertTrue(hstComponentConfiguration.isInherited());
            assertTrue(hstComponentConfiguration.getCanonicalStoredLocation().startsWith("/hst:hst/hst:configurations/unittestcommon/"));
        }
        assertNull(hstSite.getComponentsConfiguration().getComponentConfigurations().get("standarddetail"));
    }

    @Test
    public void test_marked_deleted_nodes_are_ignored()  throws Exception {
        Node homePage = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages/homepage");
        homePage.addMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
        homePage.setProperty(HstNodeTypes.EDITABLE_PROPERTY_STATE, "deleted");
        final Node standardBody = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages/standardoverview/body");
        standardBody.addMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
        standardBody.setProperty(HstNodeTypes.EDITABLE_PROPERTY_STATE, "deleted");
        session.save();

        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        assertNull(hstSite.getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage"));
        assertNull(hstSite.getComponentsConfiguration().getComponentConfiguration("hst:pages/standardoverview/body"));
        assertNull(hstSite.getComponentsConfiguration().getComponentConfiguration("hst:pages/standardoverview").getChildByName("body"));
    }
}
