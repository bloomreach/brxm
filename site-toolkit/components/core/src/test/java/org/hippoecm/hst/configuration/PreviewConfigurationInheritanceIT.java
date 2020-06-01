/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.MountService;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.EventPathsInvalidator;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.hippoecm.hst.util.JcrSessionUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_WORKSPACE;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_WORKSPACE;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class PreviewConfigurationInheritanceIT extends AbstractTestConfigurations {

    private HstManager hstManager;
    private Session session;
    private EventPathsInvalidator invalidator;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.hstManager = getComponent(HstManager.class.getName());
        this.invalidator = HstServices.getComponentManager().getComponent(EventPathsInvalidator.class.getName());

        this.session = createSession();
        createHstConfigBackup(session);
        movePagesToWorkspace("/hst:hst/hst:configurations/unittestcommon", session);
        createPreview("/hst:hst/hst:configurations/unittestproject", session);
        session.save();
    }

    private void movePagesToWorkspace(final String configRoot, final Session session) throws RepositoryException {
        Node rootConfig = session.getNode(configRoot);
        if (!rootConfig.hasNode(NODENAME_HST_WORKSPACE)) {
            rootConfig.addNode(NODENAME_HST_WORKSPACE, NODETYPE_HST_WORKSPACE);
        }
        session.move("/hst:hst/hst:configurations/unittestcommon/hst:pages",
                "/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:pages");
    }

    private void createPreview(final String liveConfigRoot, final Session session) throws RepositoryException {
        Node preview = JcrUtils.copy(session, liveConfigRoot, liveConfigRoot + "-preview");
        preview.setProperty(GENERAL_PROPERTY_INHERITS_FROM, new String[]{"../unittestproject"});
    }

    @Override
    @After
    public void tearDown() throws Exception {
        restoreHstConfigBackup(session);
        session.logout();
        super.tearDown();
    }

    @Test
    public void assert_preview_configuration_inherits_everything_live_configuration_inherits() throws Exception {
        // only the live configuration will inherit from the 'unittestcommon/hst:workspace'
        session.getNode("/hst:hst/hst:configurations/unittestproject").setProperty(GENERAL_PROPERTY_INHERITS_FROM,
                new String[]{"../unittestcommon", "../unittestcommon/hst:workspace"});
        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);
        inheritanceAndInvalidationAssertions();
    }
    @Test
    public void assert_preview_configuration_inherits_everything_live_configuration_inherits_2() throws Exception {
        // only the live configuration will inherit from the 'unittestcommon/hst:workspace/hst:pages'
        session.getNode("/hst:hst/hst:configurations/unittestproject").setProperty(GENERAL_PROPERTY_INHERITS_FROM,
                new String[]{"../unittestcommon", "../unittestcommon/hst:workspace/hst:pages"});
        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);
        inheritanceAndInvalidationAssertions();
    }

    private void inheritanceAndInvalidationAssertions() throws Exception {
        {
            VirtualHosts vhosts = hstManager.getVirtualHosts();
            final Mount mount = vhosts.getMountByIdentifier(session.getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root").getIdentifier());
            final HstComponentConfiguration livePageComponent = mount.getHstSite().getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage");
            assertNotNull("live configuration should had inherited 'hst:pages/homepage' from unittestcommon/hst:workspace", livePageComponent);

            final HstComponentConfiguration previewPageComponent = ((MountService)mount).getPreviewHstSite().getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage");
            assertNotNull("preview configuration should had inherited 'hst:pages/homepage' via live that in turns inherits from unittestcommon/hst:workspace", previewPageComponent);
        }
        session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:workspace/hst:pages/homepage").remove();
        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);
        {
            VirtualHosts vhosts = hstManager.getVirtualHosts();
            final Mount mount = vhosts.getMountByIdentifier(session.getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root").getIdentifier());
            final HstComponentConfiguration livePageComponent = mount.getHstSite().getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage");
            assertNull("live configuration should not have 'hst:pages/homepage' since removed from inherited", livePageComponent);

            final HstComponentConfiguration previewPageComponent = ((MountService)mount).getPreviewHstSite().getComponentsConfiguration().getComponentConfiguration("hst:pages/homepage");
            assertNull("preview configuration should not have 'hst:pages/homepage' since live inherits from common from which is it removed", previewPageComponent);
        }
    }

}
