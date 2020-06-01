/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.model.migrator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.hippoecm.repository.util.JcrUtils.getMultipleStringProperty;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HstChannelMigratorToV12Test extends RepositoryTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        // setup sets up a very basic standard pre-CMS-12 style hst configuration
        // per test, this setup can be enhanced
        RepositoryTestCase.build(new String[]{
                "/hst:test", "hst:hst",
                  "/hst:test/hst:channels", "hst:channels",
                      "/hst:test/hst:channels/myproject", "hst:channel",
                       "hst:name", "My Project",
                           "/hst:test/hst:channels/myproject/hst:channelinfo", "hst:channelinfo",
                  "/hst:test/hst:hosts", "hst:virtualhosts",
                     "/hst:test/hst:hosts/dev", "hst:virtualhostgroup",
                       "/hst:test/hst:hosts/dev/localhost", "hst:virtualhost",
                          "/hst:test/hst:hosts/dev/localhost/hst:root", "hst:mount",
                             "hst:channelpath", "/hst:test/hst:channels/myproject",
                             "hst:mountpoint", "/hst:test/hst:sites/myproject",
                     "/hst:test/hst:hosts/prod", "hst:virtualhostgroup",
                       "/hst:test/hst:hosts/prod/prodhost", "hst:virtualhost",
                          "/hst:test/hst:hosts/prod/prodhost/hst:root", "hst:mount",
                             "hst:channelpath", "/hst:test/hst:channels/myproject",
                             "hst:mountpoint", "/hst:test/hst:sites/myproject",
                  "/hst:test/hst:sites", "hst:sites",
                     "/hst:test/hst:sites/myproject", "hst:site",
                  "/hst:test/hst:configurations", "hst:configurations",
                     "/hst:test/hst:configurations/myproject", "hst:configuration",
                       "/hst:test/hst:configurations/myproject/hst:workspace", "hst:workspace",
        }, session);
    }

    @After
    public void tearDown() throws Exception {
        session.refresh(false);
        super.tearDown();

    }

    @Test
    public void assert_default_hst_bootstrap_version_12_is_not_changed_by_migrator() throws Exception {
        assertFalse(new HstChannelPreMigratorToV12().migrate(session, null, false));
        assertFalse(new HstChannelPreMigratorToV12("/hst:hst", false).migrate(session, null, false));
    }

    @Test
    public void migrate_common_setup_with_workspace_moves_channel_to_workspace() throws Exception {
        boolean changed = new HstChannelPreMigratorToV12("/hst:test", false).migrate(session, null, false);
        assertTrue("Expected the migrator to have resulted in changes", changed);
        Node hstRoot = session.getNode("/hst:test");
        assertMountsMigrated(hstRoot.getNode("hst:hosts"));
        assertFalse(hstRoot.hasNode("hst:channels"));
        assertTrue(hstRoot.hasNode("hst:configurations/myproject/hst:workspace/hst:channel"));
        assertTrue(hstRoot.hasNode("hst:configurations/myproject/hst:workspace/hst:channel/hst:channelinfo"));
    }

    @Test
    public void migrate_common_setup_without_workspace_moves_channel_to_created_workspace() throws Exception {

        session.getNode("/hst:test/hst:configurations/myproject/hst:workspace").remove();

        boolean changed = new HstChannelPreMigratorToV12("/hst:test", false).migrate(session, null,false);
        assertTrue("Expected the migrator to have resulted in changes", changed);
        Node hstRoot = session.getNode("/hst:test");
        assertMountsMigrated(hstRoot.getNode("hst:hosts"));
        assertFalse(hstRoot.hasNode("hst:channels"));
        assertTrue(hstRoot.hasNode("hst:configurations/myproject/hst:workspace"));
        assertTrue(hstRoot.hasNode("hst:configurations/myproject/hst:workspace/hst:channel"));
        assertTrue(hstRoot.hasNode("hst:configurations/myproject/hst:workspace/hst:channel/hst:channelinfo"));
    }

    @Test
    public void migrate_removes_preview_channels_and_preview_configurations() throws Exception {
        RepositoryTestCase.build(new String[]{
                  "/hst:test/hst:channels/myproject-preview", "hst:channel",
                     "hst:name", "My Project",
                     "/hst:test/hst:channels/myproject-preview/hst:channelinfo", "hst:channelinfo",
                   "/hst:test/hst:configurations/myproject-preview", "hst:configuration",
                     "/hst:test/hst:configurations/myproject-preview/hst:workspace", "hst:workspace",
        }, session);
        new HstChannelPreMigratorToV12("/hst:test", false).migrate(session, null,false);
        assertFalse(session.nodeExists("/hst:test/hst:channels/myproject-preview"));
        assertFalse(session.nodeExists("/hst:test/hst:configurations/myproject-preview"));
    }

    /**
     * An unmapped mount never uses a hst:configuration even if it uses the hst:mountpoint of the parent that does
     * have a hst:configuration
     */
    @Test
    public void migrate_existing_unmapped_rest_mount_does_not_result_in_nochannelinfo_property() throws Exception {
        RepositoryTestCase.build(new String[]{
                "/hst:test/hst:hosts/dev/localhost/hst:root/restapi", "hst:mount",
                "hst:ismapped", "false"
        }, session);
        assertTrue(new HstChannelPreMigratorToV12("/hst:test", false).migrate(session, null,false));

        Node hstRoot = session.getNode("/hst:test");
        assertTrue(hstRoot.hasNode("hst:hosts/dev/localhost/hst:root/restapi"));
        Node mount = hstRoot.getNode("hst:hosts/dev/localhost/hst:root/restapi");
        assertFalse(mount.hasProperty("hst:nochannelinfo"));
    }

    /**
     * This is a very subtle setup that needs correct migration: Child mounts do inherit the hst:mountpoint from a parent
     * mount. This means, that the child mount *GETS* it hst:configuration via the parent mount. However, *if* the child
     * mount did *NOT* have a hst:channel node in version CMS 11, it means that it now would get the channel node via
     * the hst:configuration where the channel node has migrated to. To solve this problem, we mark the mount with
     * 'hst:nochannelinfo = true'.
     */
    @Test
    public void migrate_mapped_rest_mount_does_result_in_nochannelinfo_property() throws Exception {
        // note 'hst:ismapped' is by default true when missing (and not inherited), but for clarity testing here, add it explicitly here
        RepositoryTestCase.build(new String[]{
                "/hst:test/hst:hosts/dev/localhost/hst:root/restapi", "hst:mount",
                  "hst:ismapped", "true",
                "/hst:test/hst:hosts/dev/localhost/hst:root/second", "hst:mount",
                   // hst:ismapped is implicitly true but since there is no mountpoint, we should get hst:nochannelinfo = true
                "/hst:test/hst:hosts/prod/prodhost/hst:root/restapi", "hst:mount",
                  "hst:ismapped", "true"
        }, session);
        assertTrue(new HstChannelPreMigratorToV12("/hst:test", false).migrate(session, null,false));

        Node hstRoot = session.getNode("/hst:test");
        for (String mountPath : new String[]{
                "hst:hosts/dev/localhost/hst:root/restapi",
                "hst:hosts/dev/localhost/hst:root/second",
                "hst:hosts/prod/prodhost/hst:root/restapi"}) {
            assertTrue(hstRoot.hasNode(mountPath));
            Node mount = hstRoot.getNode(mountPath);
            assertTrue(mount.hasProperty("hst:nochannelinfo"));
            assertEquals(true, mount.getProperty("hst:nochannelinfo").getBoolean());
        }
    }


    @Test
    public void migrate_child_mount_of_mapped_rest_mount_does_result_in_nochannelinfo_property() throws Exception {
         RepositoryTestCase.build(new String[]{
                "/hst:test/hst:hosts/dev/localhost/hst:root/restapi", "hst:mount",
                  "hst:ismapped", "true",
                "/hst:test/hst:hosts/dev/localhost/hst:root/restapi/child", "hst:mount"
        }, session);
        assertTrue(new HstChannelPreMigratorToV12("/hst:test", false).migrate(session, null,false));
        Node hstRoot = session.getNode("/hst:test");
        for (String mountPath : new String[]{"hst:hosts/dev/localhost/hst:root/restapi",
                "hst:hosts/dev/localhost/hst:root/restapi/child"}) {
            assertTrue(hstRoot.hasNode(mountPath));
            Node mount = hstRoot.getNode(mountPath);
            assertTrue(mount.hasProperty("hst:nochannelinfo"));
            assertEquals(true, mount.getProperty("hst:nochannelinfo").getBoolean());
        }
    }

    @Test
    public void migrate_existing_mapped_rest_mount_without_parent_having_mountpoint_does_not_result_in_nochannelinfo_property() throws Exception {
        RepositoryTestCase.build(new String[]{
                "/hst:test/hst:hosts/dev/newhost", "hst:virtualhost",
                  "/hst:test/hst:hosts/dev/newhost/hst:root", "hst:mount",
                     "/hst:test/hst:hosts/dev/newhost/hst:root/child", "hst:mount",
        }, session);
        assertTrue(new HstChannelPreMigratorToV12("/hst:test", false).migrate(session, null,false));
        assertFalse(session.nodeExists("/hst:hst/hst:channels"));
        Node hstRoot = session.getNode("/hst:test");
        for (String mountPath : new String[]{"hst:hosts/dev/newhost/hst:root",
                "hst:hosts/dev/newhost/hst:root/child"}) {
            assertTrue(hstRoot.hasNode(mountPath));
            Node mount = hstRoot.getNode(mountPath);
            assertFalse(mount.hasProperty("hst:nochannelinfo"));
        }
    }

    @Test
    public void migrate_mount_with_channelpath_that_does_not_exist_removes_channelpath() throws Exception {
        session.getNode("/hst:test/hst:hosts/dev/localhost/hst:root").setProperty("hst:channelpath", "/hst:hst/hst:channels/nonexisting");
        assertTrue(new HstChannelPreMigratorToV12("/hst:test", false).migrate(session, null,false));
        assertFalse(session.getNode("/hst:test/hst:hosts/dev/localhost/hst:root").hasProperty("hst:channelpath"));
    }

    /**
     * This is a very tricky old CMS 11 setup that used to be possible: A setup where you'd have two hst:mount nodes within *ONE* hostgroup that
     * have the *same* hst:mountpoint, but both point to a different hst:channel node. NOTE this was a DISCOURAGED setup since
     * both channels would modify the SAME hst configuration through the CM. In CMS 12, this is not possible any more. The only
     * way it works in CMS 12 is choosing one hst:configuration as 'leading' and have the other extend from it *BUT* having
     * its own hst:channel node. As a result, one configuration will be read-only.
     * If there are two sibling mount nodes that share the same hst configuration *BUT* have their on hst:channel, for
     * consistency, we'll make the hst:configuration node name that is alphabetically the first as the 'primary' configuration.
     */
    @Test
    public void migrate_mounts_sharing_same_hstconfig_but_both_have_own_channel_results_in_two_hstconfigs_with_own_channel() throws Exception {
        RepositoryTestCase.build(new String[]{
                  "/hst:test/hst:channels/sub1", "hst:channel",
                     "hst:name", "Sub 1",
                       "/hst:test/hst:channels/sub1/hst:channelinfo", "hst:channelinfo",
                  "/hst:test/hst:channels/sub2", "hst:channel",
                     "hst:name", "Sub 2",
                     "/hst:test/hst:channels/sub2/hst:channelinfo", "hst:channelinfo",
                  "/hst:test/hst:hosts/dev/localhost/hst:root/sub1", "hst:mount",
                     "hst:channelpath", "/hst:test/hst:channels/sub1",
                     "hst:mountpoint", "/hst:test/hst:sites/sub1",
                  "/hst:test/hst:hosts/dev/localhost/hst:root/sub2", "hst:mount",
                     "hst:channelpath", "/hst:test/hst:channels/sub2",
                     "hst:mountpoint", "/hst:test/hst:sites/sub2",
                  "/hst:test/hst:hosts/prod/prodhost/hst:root/sub1", "hst:mount",
                     "hst:channelpath", "/hst:test/hst:channels/sub1",
                     "hst:mountpoint", "/hst:test/hst:sites/sub1",
                  "/hst:test/hst:hosts/prod/prodhost/hst:root/sub2", "hst:mount",
                     "hst:channelpath", "/hst:test/hst:channels/sub2",
                     "hst:mountpoint", "/hst:test/hst:sites/sub2",
                  "/hst:test/hst:sites/sub1", "hst:site",
                     // both point to 'sub' configuration
                     "hst:configurationpath", "/hst:test/hst:configurations/sub",
                  "/hst:test/hst:sites/sub2", "hst:site",
                     // both point to 'sub' configuration
                     "hst:configurationpath", "/hst:test/hst:configurations/sub",
                  "/hst:test/hst:configurations/sub", "hst:configuration",
                     "/hst:test/hst:configurations/sub/hst:workspace", "hst:workspace",
        }, session);

        assertTrue(new HstChannelPreMigratorToV12("/hst:test", false).migrate(session, null,false));

        assertMountsMigrated(session.getNode("/hst:test/hst:hosts"));

        assertTrue("since hst:site 'sub1' pointed to '/hst:test/hst:configurations/sub' it should still have the hst:configuration path",
                session.getNode("/hst:test/hst:sites/sub1").hasProperty("hst:configurationpath"));

        assertFalse("Expected the hst:configurationpath to have been removed by the migrator",
                session.getNode("/hst:test/hst:sites/sub2").hasProperty("hst:configurationpath"));
        // expected a sub2 configuration to be created which inherits everything from '/hst:test/hst:configurations/sub'
        assertTrue(session.nodeExists("/hst:test/hst:configurations/sub2"));
        assertTrue(session.propertyExists("/hst:test/hst:configurations/sub2/hst:inheritsfrom"));
        assertTrue(session.nodeExists("/hst:test/hst:configurations/sub2/hst:workspace"));
        assertTrue(session.nodeExists("/hst:test/hst:configurations/sub2/hst:workspace/hst:channel"));
        assertEquals("Sub 2", session.getProperty("/hst:test/hst:configurations/sub2/hst:workspace/hst:channel/hst:name").getString());
        assertArrayEquals(new String[]{"../sub", "../sub/hst:workspace"},
                getMultipleStringProperty(session.getNode("/hst:test/hst:configurations/sub2"), "hst:inheritsfrom", null));
    }

    @Test
    public void migrate_blueprint_that_has_a_channel_node() throws Exception {
        RepositoryTestCase.build(new String[]{
                "/hst:test/hst:blueprints", "hst:blueprints",
                "/hst:test/hst:blueprints/website", "hst:blueprint",
                "/hst:test/hst:blueprints/website/hst:configuration", "hst:configuration",
                "/hst:test/hst:blueprints/website/hst:channel", "hst:channel",
                "/hst:test/hst:blueprints/website/hst:channel/hst:channelinfo", "hst:channelinfo",
        }, session);

        assertTrue(new HstChannelPreMigratorToV12("/hst:test", false).migrate(session, null,false));

        assertTrue(session.nodeExists("/hst:test/hst:blueprints/website/hst:configuration/hst:channel"));
        assertTrue(session.nodeExists("/hst:test/hst:blueprints/website/hst:configuration/hst:channel/hst:channelinfo"));
    }

    @Test
    public void migrate_blueprint_that_has_a_channel_node_but_no_configuration_creates_configuration() throws Exception {
        RepositoryTestCase.build(new String[]{
                "/hst:test/hst:blueprints", "hst:blueprints",
                "/hst:test/hst:blueprints/website", "hst:blueprint",
                "/hst:test/hst:blueprints/website/hst:channel", "hst:channel",
                "/hst:test/hst:blueprints/website/hst:channel/hst:channelinfo", "hst:channelinfo",
        }, session);

        assertTrue(new HstChannelPreMigratorToV12("/hst:test", false).migrate(session, null,false));

        assertTrue(session.nodeExists("/hst:test/hst:blueprints/website/hst:configuration"));
        assertTrue(session.nodeExists("/hst:test/hst:blueprints/website/hst:configuration/hst:channel"));
        assertTrue(session.nodeExists("/hst:test/hst:blueprints/website/hst:configuration/hst:channel/hst:channelinfo"));
    }

    @Test
    public void migrate_blueprints_that_do_not_have_a_channel_node() throws Exception {
        RepositoryTestCase.build(new String[]{
                  "/hst:test/hst:blueprints", "hst:blueprints",
                    "/hst:test/hst:blueprints/website", "hst:blueprint",
                      "/hst:test/hst:blueprints/website/hst:configuration", "hst:configuration",
        }, session);

        assertTrue(new HstChannelPreMigratorToV12("/hst:test", false).migrate(session, null,false));

        assertFalse(session.nodeExists("/hst:test/hst:blueprints/website/hst:configuration/hst:channel"));
    }

    private void assertMountsMigrated(final Node current) throws RepositoryException {
        for (Node child : new NodeIterable(current.getNodes())) {
            if (child.isNodeType("hst:mount")) {
                assertFalse(child.hasProperty("hst:channelpath"));
            }
            assertMountsMigrated(child);
        }
    }

}