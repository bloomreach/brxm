/*
 *  Copyright 2011-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration.channel;

import java.security.PrivilegedActionException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.channel.Blueprint;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.channel.ChannelManagerEvent;
import org.hippoecm.hst.configuration.channel.ChannelManagerEventListener;
import org.hippoecm.hst.configuration.channel.ChannelManagerEventListenerException;
import org.hippoecm.hst.configuration.channel.ChannelManagerEventListenerException.Status;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.EventPathsInvalidator;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.hippoecm.hst.platform.HstModelProvider;
import org.hippoecm.hst.platform.api.model.PlatformHstModel;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.hippoecm.hst.util.JcrSessionUtils;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.hst.Channel;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static org.hippoecm.hst.configuration.HstNodeTypes.CHANNEL_PROPERTY_CHANNELINFO_CLASS;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_BLUEPRINTS;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_CHANNEL;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_CHANNELINFO;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_CONFIGURATION;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_WORKSPACE;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_BLUEPRINT;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CHANNEL;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CHANNELINFO;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONFIGURATION;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_WORKSPACE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class ChannelManagerImplIT extends AbstractTestConfigurations {

    private ChannelManagerImpl channelMngr;
    private HstManager hstManager;
    private EventPathsInvalidator invalidator;
    private Session session;

    public interface TestChannelInfo extends ChannelInfo {

        @Parameter(name = "title", defaultValue = "default")
        String getTitle();

    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        Credentials cred = new SimpleCredentials("admin", "admin".toCharArray());
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        session = repository.login(cred);
        createHstConfigBackup(session);
        final MockHstRequestContext requestContext = new MockHstRequestContext();
        requestContext.setAttribute("HOST_GROUP_NAME_FOR_CMS_HOST", "dev-localhost");
        requestContext.setSession(session);
        ModifiableRequestContextProvider.set(requestContext);

        final HstModelProvider provider = HstServices.getComponentManager().getComponent(HstModelProvider.class);
        final PlatformHstModel hstModel = (PlatformHstModel) provider.getHstModel();
        invalidator = hstModel.getEventPathsInvalidator();

        channelMngr = (ChannelManagerImpl) hstModel.getChannelManager();

        hstManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());
        final VirtualHosts virtualHosts = hstManager.getVirtualHosts();
        final VirtualHost dummyHost = virtualHosts.getMountsByHostGroup("dev-localhost").get(0).getVirtualHost();
        ((MockHstRequestContext) RequestContextProvider.get()).setVirtualHost(dummyHost);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        try {
            restoreHstConfigBackup(session);
            session.logout();
        } finally {
            super.tearDown();
        }
    }

    @Test
    public void createUniqueChannelName() throws RepositoryException, ChannelException {
        assertEquals("test", channelMngr.createUniqueHstConfigurationName("test", session));
        assertEquals("name-with-spaces", channelMngr.createUniqueHstConfigurationName("Name with Spaces", session));
        assertEquals("special-characters--and---and", channelMngr.createUniqueHstConfigurationName("Special Characters: % and / and []", session));
        assertEquals("'unittestproject' already exists as an hst:site node in the default unit test content, so the new channel ID should get a suffix",
                "unittestproject-1", channelMngr.createUniqueHstConfigurationName("unittestproject", session));
        assertEquals("'unittestcommon' already exists as an hst:configuration node in the default unit test content, so the new channel name should get a suffix",
                "unittestcommon-1", channelMngr.createUniqueHstConfigurationName("unittestcommon", session));
    }

    @Test
    public void channelsAreReadCorrectly() throws Exception {
        final HstManager manager = HstServices.getComponentManager().getComponent(HstManager.class.getName());

        Map<String, Channel> channels = manager.getVirtualHosts().getChannels("dev-localhost");
        assertEquals(2, channels.size());

        Channel channel = channels.get("unittestproject");
        assertEquals("unittestproject", channel.getId());
        assertEquals("Test Channel", channel.getName());
        assertEquals("en_EN", channel.getLocale());
    }

    @Test
    public void previews_add_channels_with_preview_id() throws Exception {
        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestproject", "/hst:hst/hst:configurations/unittestproject-preview");

        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);

        final HstManager manager = HstServices.getComponentManager().getComponent(HstManager.class.getName());
        Map<String, Channel> channels = manager.getVirtualHosts().getChannels("dev-localhost");
        assertEquals(3, channels.size());

        Channel channel = channels.get("unittestproject-preview");
        assertEquals("unittestproject-preview", channel.getId());
        assertEquals("Test Channel", channel.getName());
        assertEquals("en_EN", channel.getLocale());
    }

    @Test
    public void channelPropertiesSaved() throws Exception {

        Map<String, Channel> channels = hstManager.getVirtualHosts().getChannels("dev-localhost");
        assertEquals(2, channels.size());
        final Channel channel = channels.get("unittestproject");
        channel.setChannelInfoClassName(getClass().getCanonicalName() + "$" + TestChannelInfo.class.getSimpleName());
        channel.getProperties().put("title", "test title");
        // channel manager save triggers event path invalidation hence no explicit invalidation needed now
        channelMngr.save("dev-localhost", channel);
        resetDummyHostOnRequestContext();

        channels = hstManager.getVirtualHosts().getChannels("dev-localhost");

        assertEquals(2, channels.size());
        Channel savedChannel = channels.get("unittestproject");

        Map<String, Object> savedProperties = savedChannel.getProperties();
        assertTrue(savedProperties.containsKey("title"));
        assertEquals("test title", savedProperties.get("title"));
    }


    @Test
    public void channelsAreNotClonedWhenRetrieved() throws Exception {
        final HstManager hstManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());
        Map<String, Channel> channels = hstManager.getVirtualHosts().getChannels("dev-localhost");
        Map<String, Channel> channelsAgain = hstManager.getVirtualHosts().getChannels("dev-localhost");
        assertTrue(channelsAgain == channels);
        Iterator<Channel> iterator1 = channels.values().iterator();
        Iterator<Channel> iterator2 = channelsAgain.values().iterator();
        while (iterator1.hasNext()) {
            assertSame(iterator1.next(), iterator2.next());
        }
    }

    @Test
    public void channelsMapIsNewInstanceWhenReloadedAfterChange() throws Exception {
        Map<String, Channel> channels = hstManager.getVirtualHosts().getChannels("dev-localhost");
        final Channel channel = channels.values().iterator().next();
        channel.setChannelInfoClassName(getClass().getCanonicalName() + "$" + TestChannelInfo.class.getSimpleName());
        channel.getProperties().put("title", "test title");
        // channel manager save triggers event path invalidation hence no explicit invalidation needed now
        channelMngr.save("dev-localhost", channel);
        resetDummyHostOnRequestContext();
        Map<String, Channel> channelsAgain = hstManager.getVirtualHosts().getChannels("dev-localhost");
        assertTrue("After a change, getChannels should return different instance for the Map", channelsAgain != channels);
    }

    @Test
    public void channel_caching_assertions() throws Exception {
        final Map<String, Channel> channels = hstManager.getVirtualHosts().getChannels("dev-localhost");
        final Channel unittestproject = channels.get("unittestproject");
        final Channel unittestsubproject = channels.get("unittestsubproject");
        session.getNode(unittestproject.getChannelPath()).setProperty(HstNodeTypes.CHANNEL_PROPERTY_NAME, "new value");
        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);
        final Map<String, Channel> channelsAgain = hstManager.getVirtualHosts().getChannels("dev-localhost");
        assertNotSame(unittestproject, channelsAgain.get("unittestproject"));
        assertFalse(unittestproject.getName().equals(channelsAgain.get("unittestproject").getName()));

        // although 'unittestsubproject' is not changed, still a new object is expected because
        // org.hippoecm.hst.configuration.cache.HstConfigurationLoadingCache.loadChannel() invoke clone(channel)
        assertNotSame(unittestsubproject, channelsAgain.get("unittestsubproject"));
        assertTrue(unittestsubproject.getName().equals(channelsAgain.get("unittestsubproject").getName()));

        // very dirty check (but important to prove caching : the Channel#viewportMap is during #clone(Channel) not cloned
        // hence if org.hippoecm.hst.configuration.cache.HstConfigurationLoadingCache.loadChannel() returns a cached
        // channel, we expect the same object instance for Channel#viewportMap (making a bit use of a very tiny bug in this unit test)
        // since 'unittestproject' we expect a new Channel#viewportMap object. For 'unittestsubproject' we expect the same object
        // as we have before : If not, caching is broken!

        assertNotSame(unittestproject.getViewportMap(), channelsAgain.get("unittestproject").getViewportMap());
        assertSame(unittestsubproject.getViewportMap(), channelsAgain.get("unittestsubproject").getViewportMap());
    }

    @Test
    public void channel_caching_assertions_with_preview_that_inherits_channel_node_from_live() throws Exception {
        // test with non-workspace channel node
        Node previewConfig = session.getNode("/hst:hst/hst:configurations").addNode("unittestproject-preview");
        previewConfig.setProperty(GENERAL_PROPERTY_INHERITS_FROM, new String[]{"../unittestproject"});
        session.save();
        final Map<String, Channel> channels = hstManager.getVirtualHosts().getChannels("dev-localhost");

        assertFalse("Because there is no preview channel node, the channels only contain the 'live' channel id",
                channels.containsKey("unittestproject-preview"));

        Node workspace = session.getNode("/hst:hst/hst:configurations/unittestproject-preview").addNode("hst:workspace");
        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestproject/hst:channel", workspace.getPath()  + "/hst:channel");
        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);
        final Map<String, Channel> channelsAgain = hstManager.getVirtualHosts().getChannels("dev-localhost");
        // now the preview channel should be loaded
        assertTrue("There should be a preview channel since there is a node now in workspace",
                channelsAgain.containsKey("unittestproject-preview"));
    }


    @Test
    public void channel_is_created_from_blueprint_without_content_prototype() throws Exception {
        List<Blueprint> bluePrints = hstManager.getVirtualHosts().getBlueprints();
        assertEquals(1, bluePrints.size());
        final Blueprint blueprint = bluePrints.get(0);

        final Channel channel = blueprint.getPrototypeChannel();
        channel.setName("CMIT Test Channel: with special and/or specific characters");
        channel.setUrl("http://cmit-myhost");
        channel.setContentRoot("/unittestcontent/documents");
        channel.setLocale("nl_NL");

        String channelId = channelMngr.persist(blueprint.getId(), channel);
        resetDummyHostOnRequestContext();

        final String encodedChannelName = "cmit-test-channel-with-special-and-or-specific-characters";
        assertEquals(encodedChannelName, channelId);

        Node channelNode = session.getNode("/hst:hst/hst:configurations/" + channelId + "/hst:workspace/hst:channel");
        assertEquals("CMIT Test Channel: with special and/or specific characters", channelNode.getProperty("hst:name").getString());

        Node hostNode = session.getNode("/hst:hst/hst:hosts/dev-localhost");
        assertTrue(hostNode.hasNode("cmit-myhost/hst:root"));

        Node mountNode = hostNode.getNode("cmit-myhost/hst:root");
        assertEquals("nl_NL", mountNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCALE).getString());
        String sitePath = "/hst:hst/hst:sites/" + channelId;
        assertEquals(sitePath, mountNode.getProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT).getString());
        assertTrue(session.itemExists(sitePath));

        Node siteNode = session.getNode(sitePath);
        assertEquals("/unittestcontent/documents", siteNode.getProperty(HstNodeTypes.SITE_CONTENT).getString());

    }


    @Test
    public void channel_is_created_from_blueprint_with_content_prototype() throws Exception {
        // first create prototype content
        final String[] prototypeBootstrap = new String[] {
                "/hippo:configuration/hippo:queries/hippo:templates/new-subsite/hippostd:templates/testblueprint", "hippostd:folder",
                "jcr:mixinTypes", "mix:referenceable",
                "hippostd:foldertype", "new-document",
                "hippostd:foldertype", "new-folder"
        };

        try {
            RepositoryTestCase.build(prototypeBootstrap, session);
            // for direct jcr node changes, we need to trigger an invalidation event ourselves
            session.save();

            List<Blueprint> bluePrints = hstManager.getVirtualHosts().getBlueprints();
            assertEquals(1, bluePrints.size());
            final Blueprint blueprint = bluePrints.get(0);
            assertTrue(blueprint.getHasContentPrototype());


            final Channel channel = blueprint.getPrototypeChannel();
            channel.setName("newchannel");
            channel.setUrl("http://cmit-myhost");
            channel.setLocale("nl_NL");

            String channelId = channelMngr.persist(blueprint.getId(), channel);
            assertEquals("newchannel", channelId);

            // assert content created from prototype
            assertTrue(session.nodeExists("/unittestcontent/documents/newchannel"));

        } finally {
            if (session.nodeExists("/hippo:configuration/hippo:queries/hippo:templates/new-subsite/hippostd:templates/testblueprint")) {
                session.getNode("/hippo:configuration/hippo:queries/hippo:templates/new-subsite/hippostd:templates/testblueprint").remove();
            }
            if (session.nodeExists("/unittestcontent/documents/newchannel")) {
                session.getNode("/unittestcontent/documents/newchannel").remove();
            }
            session.save();
        }

    }


    @Test
    public void no_created_root_content_after_ChannelManagerEventListenerException_STOP_CHANNEL_PROCESSING() throws Exception {
        // make sure a ChannelException is forced *after* the workflow creates the root content. We can force this by
        // decorating the channel and force a ChannelException on channel#setContentRoot which is invoked after the content
        // creation
        // first create prototype content
        final String[] prototypeBootstrap = new String[] {
                "/hippo:configuration/hippo:queries/hippo:templates/new-subsite/hippostd:templates/testblueprint", "hippostd:folder",
                "jcr:mixinTypes", "mix:referenceable",
                "hippostd:foldertype", "new-document",
                "hippostd:foldertype", "new-folder"
        };

        try {
            RepositoryTestCase.build(prototypeBootstrap, session);
            session.save();

            ChannelManagerEventListener shortCircuitingListener = new MyShortCircuitingEventListener();
            channelMngr.addChannelManagerEventListeners(shortCircuitingListener);

            List<Blueprint> bluePrints = hstManager.getVirtualHosts().getBlueprints();
            assertEquals(1, bluePrints.size());
            final Blueprint blueprint = bluePrints.get(0);
            assertTrue(blueprint.getHasContentPrototype());


            final Channel channel = blueprint.getPrototypeChannel();

            channel.setName("newchannel");
            channel.setUrl("http://cmit-myhost");
            channel.setLocale("nl_NL");

            try {
                channelMngr.persist(blueprint.getId(), channel);
                fail("ChannelException was expected");
            } catch (ChannelException e) {
                //expected
            }
            // assert content *not* created because of STOP_CHANNEL_PROCESSING exception
            junit.framework.Assert.assertFalse(session.nodeExists("/unittestcontent/documents/newchannel"));

        } finally {
            if (session.nodeExists("/hippo:configuration/hippo:queries/hippo:templates/new-subsite/hippostd:templates/testblueprint")) {
                session.getNode("/hippo:configuration/hippo:queries/hippo:templates/new-subsite/hippostd:templates/testblueprint").remove();
            }
            if (session.nodeExists("/unittestcontent/documents/newchannel")) {
                session.getNode("/unittestcontent/documents/newchannel").remove();
            }
            session.save();
        }


    }

    @Test
    public void channelsAreReloadedAfterAddingOne() throws Exception {
        Map<String, Channel> channels = hstManager.getVirtualHosts().getChannels("dev-localhost");
        int numberOfChannelsBefore = channels.size();


        List<Blueprint> bluePrints = hstManager.getVirtualHosts().getBlueprints();
        assertEquals(1, bluePrints.size());
        final Blueprint blueprint = bluePrints.get(0);

        final Channel channel = blueprint.getPrototypeChannel();
        channel.setName("CMIT Test Channel");
        channel.setUrl("http://cmit-myhost");
        channel.setContentRoot("/unittestcontent/documents");
        channel.setLocale("nl_NL");

        String channelId = channelMngr.persist(blueprint.getId(), channel);
        resetDummyHostOnRequestContext();

        // for direct jcr node changes, we need to trigger an invalidation event ourselves
        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);
        resetDummyHostOnRequestContext();

        // manager should reload
        channels = hstManager.getVirtualHosts().getChannels("dev-localhost");
        assertEquals(numberOfChannelsBefore + 1, channels.size());
        assertTrue(channels.containsKey(channelId));

        Channel created = channels.get(channelId);
        assertNotNull(created);
        assertEquals(channelId, created.getId());
        assertEquals("CMIT Test Channel", created.getName());
        assertEquals("http://cmit-myhost/site", created.getUrl());
        assertEquals("/unittestcontent/documents", created.getContentRoot());
        assertEquals("nl_NL", created.getLocale());

    }

    @Test
    public void channels_not_referenced_by_mount_in_hostgroup_are_not_in_hostgroup() throws ChannelException, RepositoryException, PrivilegedActionException, ContainerException {
        Map<String, Channel> channels = hstManager.getVirtualHosts().getChannels("dev-localhost");
        int numberOfChannerBeforeAddingAnOrphanOne = channels.size();

        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestproject",
                "/hst:hst/hst:configurations/foo");

        Node newChannelNode = session.getNode("/hst:hst/hst:configurations/foo/hst:channel");
        newChannelNode.setProperty("hst:name", "CMIT Test Channel");

        // for direct jcr node changes, we need to trigger an invalidation event ourselves
        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);
        resetDummyHostOnRequestContext();

        channels = hstManager.getVirtualHosts().getChannels("dev-localhost");

        assertEquals(numberOfChannerBeforeAddingAnOrphanOne, channels.size());
        assertFalse(channels.containsKey("foo"));

    }

    @Test
    public void channels_for_current_contextpath_slashsite_only_are_loaded() throws Exception {
        // default context path is in superclass set to /site, hence for dev-localhost the mount 'intranet'
        // with contextpath '/site2' won't be part of dev-localhost channels when contextpath is /site
        Map<String, Channel> channels = hstManager.getVirtualHosts().getChannels("dev-localhost");
        assertTrue("unittestproject should be part of channels since has wrong contextpath",
                channels.containsKey("unittestproject"));
        junit.framework.Assert.assertFalse("intranettestproject should not be part of channels since has wrong contextpath",
                channels.containsKey("intranettestproject"));
    }

    @Test(expected = ChannelException.class)
    public void ancestorMountsMustExist() throws Exception {

        final ChannelManagerImpl channelMngr = this.channelMngr;
        final HstManager hstManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());

        List<Blueprint> bluePrints = hstManager.getVirtualHosts().getBlueprints();
        assertEquals(1, bluePrints.size());
        final Blueprint blueprint = bluePrints.get(0);

        final Channel channel = blueprint.getPrototypeChannel();
        channel.setName("cmit-channel");
        channel.setUrl("http://cmit-myhost/newmount");

        channelMngr.persist(blueprint.getId(), channel);
    }

    public static interface TestInfoClass extends ChannelInfo {
        @Parameter(name = "getme", defaultValue = "aap")
        String getGetme();
    }


    @Test
    public void missing_channel_node_in_blueprint_still_results_in_channel_node_created_project() throws Exception {
        Node configNode = session.getRootNode().getNode("hst:hst");
        Node bpFolder = configNode.getNode(NODENAME_HST_BLUEPRINTS);
        Node bp = bpFolder.addNode("cmit-test-bp", NODETYPE_HST_BLUEPRINT);
        bp.addNode(NODENAME_HST_CONFIGURATION, NODETYPE_HST_CONFIGURATION);
        // for direct jcr node changes, we need to trigger an invalidation event ourselves
        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);
        resetDummyHostOnRequestContext();
        final Channel blueprintChannel = hstManager.getVirtualHosts().getBlueprint("cmit-test-bp").getPrototypeChannel();

        blueprintChannel.setName("cmit-channel");
        blueprintChannel.setUrl("http://cmit-myhost");
        blueprintChannel.setContentRoot("/");

        channelMngr.persist("cmit-test-bp", blueprintChannel);
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/cmit-channel/hst:workspace/hst:channel"));
        resetDummyHostOnRequestContext();
        assertEquals("cmit-channel", hstManager.getVirtualHosts().getChannelById("dev-localhost","cmit-channel").getName());
    }

    @Test
    public void blueprintDefaultValuesAreCopied() throws Exception {
        createBlueprintWithChannel(true);

        // for direct jcr node changes, we need to trigger an invalidation event ourselves
        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);
        resetDummyHostOnRequestContext();
        final Channel channel = hstManager.getVirtualHosts().getBlueprint("cmit-test-bp").getPrototypeChannel();
        channel.setName("cmit-channel");
        channel.setUrl("http://cmit-myhost");
        channel.setContentRoot("/");
        Map<String, Object> properties = channel.getProperties();
        assertTrue(properties.containsKey("getme"));
        assertEquals("noot", properties.get("getme"));
        channelMngr.persist("cmit-test-bp", channel);
        resetDummyHostOnRequestContext();
        TestInfoClass channelInfo = hstManager.getVirtualHosts().getChannelInfo(channel);
        assertEquals("noot", channelInfo.getGetme());
    }

    private void createBlueprintWithChannel(final boolean belowWorkspace) throws RepositoryException {
        Node configNode = session.getRootNode().getNode("hst:hst");
        Node bpFolder = configNode.getNode(NODENAME_HST_BLUEPRINTS);
        Node bp = bpFolder.addNode("cmit-test-bp", NODETYPE_HST_BLUEPRINT);
        Node configuration = bp.addNode(NODENAME_HST_CONFIGURATION, NODETYPE_HST_CONFIGURATION);
        Node channelBlueprint;
        if (belowWorkspace) {
            Node workspace = configuration.addNode(NODENAME_HST_WORKSPACE, NODETYPE_HST_WORKSPACE);
            channelBlueprint = workspace.addNode(NODENAME_HST_CHANNEL, NODETYPE_HST_CHANNEL);
        } else {
            channelBlueprint = configuration.addNode(NODENAME_HST_CHANNEL, NODETYPE_HST_CHANNEL);
        }
        channelBlueprint.setProperty(CHANNEL_PROPERTY_CHANNELINFO_CLASS, TestInfoClass.class.getName());
        Node defaultChannelInfo = channelBlueprint.addNode(NODENAME_HST_CHANNELINFO, NODETYPE_HST_CHANNELINFO);
        defaultChannelInfo.setProperty("getme", "noot");
    }

    @Test
    public void channel_node_in_blueprint_not_below_workspace_still_ends_up_below_workspace_of_created_project() throws Exception {
        createBlueprintWithChannel(false);
        // for direct jcr node changes, we need to trigger an invalidation event ourselves
        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);
        resetDummyHostOnRequestContext();
        final Channel channel = hstManager.getVirtualHosts().getBlueprint("cmit-test-bp").getPrototypeChannel();
        channel.setName("cmit-channel");
        channel.setUrl("http://cmit-myhost");
        channel.setContentRoot("/");
        channelMngr.persist("cmit-test-bp", channel);
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/cmit-channel/hst:workspace/hst:channel"));
    }

    @Test
    public void channel_node_in_blueprint_below_workspace_still_ends_up_below_workspace_of_created_project() throws Exception {
        createBlueprintWithChannel(true);
        // for direct jcr node changes, we need to trigger an invalidation event ourselves
        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);
        resetDummyHostOnRequestContext();
        final Channel channel = hstManager.getVirtualHosts().getBlueprint("cmit-test-bp").getPrototypeChannel();
        channel.setName("cmit-channel");
        channel.setUrl("http://cmit-myhost");
        channel.setContentRoot("/");
        channelMngr.persist("cmit-test-bp", channel);
        assertTrue(session.nodeExists("/hst:hst/hst:configurations/cmit-channel/hst:workspace/hst:channel"));
    }

    @Test
    public void testChannelManagerEventListeners() throws Exception {

        Node configNode = session.getRootNode().getNode("hst:hst");
        Node bpFolder = configNode.getNode(NODENAME_HST_BLUEPRINTS);

        Node bp = bpFolder.addNode("cmit-test-bp2", NODETYPE_HST_BLUEPRINT);
        Node hstConfigNode = bp.addNode(NODENAME_HST_CONFIGURATION, NODETYPE_HST_CONFIGURATION);
        hstConfigNode.addNode("hst:sitemap", "hst:sitemap");
        hstConfigNode.setProperty("hst:inheritsfrom", new String[]{"../unittestcommon"});
        Node channelBlueprint = hstConfigNode.addNode(NODENAME_HST_CHANNEL, NODETYPE_HST_CHANNEL);
        channelBlueprint.setProperty(CHANNEL_PROPERTY_CHANNELINFO_CLASS, TestInfoClass.class.getName());
        Node defaultChannelInfo = channelBlueprint.addNode(NODENAME_HST_CHANNELINFO, NODETYPE_HST_CHANNELINFO);
        defaultChannelInfo.setProperty("getme", "noot");

        // for direct jcr node changes, we need to trigger an invalidation event ourselves
        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);
        resetDummyHostOnRequestContext();

        Channel channel = hstManager.getVirtualHosts().getBlueprint("cmit-test-bp2").getPrototypeChannel();
        channel.setName("cmit-channel2");
        channel.setUrl("http://cmit-myhost2");
        channel.setContentRoot("/");
        Map<String, Object> properties = channel.getProperties();
        assertTrue(properties.containsKey("getme"));
        assertEquals("noot", properties.get("getme"));

        MyChannelManagerEventListener listener1 = new MyChannelManagerEventListener();
        MyChannelManagerEventListener listener2 = new MyChannelManagerEventListener();
        MyChannelManagerEventListener listener3 = new MyChannelManagerEventListener();

        channelMngr.addChannelManagerEventListeners(listener1, listener2, listener3);

        final Channel channelToPersist = channel;

        String channelId;
        channelId = channelMngr.persist("cmit-test-bp2", channelToPersist);
        resetDummyHostOnRequestContext();

        assertEquals(1, listener1.getCreatedCount());
        assertEquals(1, listener2.getCreatedCount());
        assertEquals(1, listener3.getCreatedCount());
        assertEquals(0, listener1.getUpdatedCount());
        assertEquals(0, listener2.getUpdatedCount());
        assertEquals(0, listener3.getUpdatedCount());

        channel = hstManager.getVirtualHosts().getChannels("dev-localhost").get(channelId);
        channel.setName("cmit-channel2");
        channel.setUrl("http://cmit-myhost2");
        channel.setContentRoot("/");
        final Channel channelToSave = channel;

        channelMngr.save(channelToSave);
        resetDummyHostOnRequestContext();

        assertEquals(1, listener1.getCreatedCount());
        assertEquals(1, listener2.getCreatedCount());
        assertEquals(1, listener3.getCreatedCount());
        assertEquals(1, listener1.getUpdatedCount());
        assertEquals(1, listener2.getUpdatedCount());
        assertEquals(1, listener3.getUpdatedCount());

        channelMngr.removeChannelManagerEventListeners(listener1, listener2, listener3);

        channel = hstManager.getVirtualHosts().getChannels("dev-localhost").get(channelId);
        channel.setName("cmit-channel2");
        channel.setUrl("http://cmit-myhost2");
        channel.setContentRoot("/");
        final Channel channelToSave2 = channel;
        channelMngr.save(channelToSave2);
        resetDummyHostOnRequestContext();

        assertEquals(1, listener1.getCreatedCount());
        assertEquals(1, listener2.getCreatedCount());
        assertEquals(1, listener3.getCreatedCount());
        assertEquals(1, listener1.getUpdatedCount());
        assertEquals(1, listener2.getUpdatedCount());
        assertEquals(1, listener3.getUpdatedCount());

    }

    @Test(expected = ChannelException.class)
    public void testChannelManagerShortCircuitingEventListeners() throws Exception {
        Node configNode = session.getRootNode().getNode("hst:hst");
        Node bpFolder = configNode.getNode(NODENAME_HST_BLUEPRINTS);

        Node bp = bpFolder.addNode("cmit-test-bp2", NODETYPE_HST_BLUEPRINT);
        Node hstConfigNode = bp.addNode(NODENAME_HST_CONFIGURATION, NODETYPE_HST_CONFIGURATION);
        Node channelBlueprint = hstConfigNode.addNode(NODENAME_HST_CHANNEL, NODETYPE_HST_CHANNEL);
        channelBlueprint.setProperty(CHANNEL_PROPERTY_CHANNELINFO_CLASS, TestInfoClass.class.getName());
        Node defaultChannelInfo = channelBlueprint.addNode(NODENAME_HST_CHANNELINFO, NODETYPE_HST_CHANNELINFO);
        defaultChannelInfo.setProperty("getme", "noot");

        // for direct jcr node changes, we need to trigger an invalidation event ourselves
        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(session, session.getNode("/hst:hst"), false);
        session.save();
        invalidator.eventPaths(pathsToBeChanged);
        resetDummyHostOnRequestContext();

        final ChannelManagerImpl channelMngr = this.channelMngr;
        final HstManager hstManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());

        Channel channel = hstManager.getVirtualHosts().getBlueprint("cmit-test-bp2").getPrototypeChannel();
        channel.setName("cmit-channel2");
        channel.setUrl("http://cmit-myhost2");
        channel.setContentRoot("/");
        Map<String, Object> properties = channel.getProperties();
        assertTrue(properties.containsKey("getme"));
        assertEquals("noot", properties.get("getme"));

        ChannelManagerEventListener shortCircuitingListener = new MyShortCircuitingEventListener();

        channelMngr.addChannelManagerEventListeners(shortCircuitingListener);

        channelMngr.persist("cmit-test-bp2", channel);

    }

    private void resetDummyHostOnRequestContext() throws ContainerException {
        final VirtualHosts virtualHosts = hstManager.getVirtualHosts();
        final VirtualHost dummyHost = virtualHosts.getMountsByHostGroup("dev-localhost").get(0).getVirtualHost();
        ((MockHstRequestContext) RequestContextProvider.get()).setVirtualHost(dummyHost);
    }


    private static class MyChannelManagerEventListener implements ChannelManagerEventListener {

        private int createdCount;
        private int updatedCount;

        public void channelCreated(ChannelManagerEvent event) throws ChannelManagerEventListenerException {
            Blueprint blueprint = event.getBlueprint();
            Channel channel = event.getChannel();
            Node configRootNode = event.getConfigRootNode();
            assertNotNull(blueprint);
            assertNotNull(channel);
            assertNotNull(configRootNode);
            ++createdCount;
        }

        public void channelUpdated(ChannelManagerEvent event) throws ChannelManagerEventListenerException {
            Blueprint blueprint = event.getBlueprint();
            Channel channel = event.getChannel();
            Node configRootNode = event.getConfigRootNode();
            assertNull(blueprint);
            assertNotNull(channel);
            assertNotNull(configRootNode);
            ++updatedCount;
        }

        public int getCreatedCount() {
            return createdCount;
        }

        public int getUpdatedCount() {
            return updatedCount;
        }
    }


    private static class MyShortCircuitingEventListener implements ChannelManagerEventListener {

        @Override
        public void channelCreated(ChannelManagerEvent event) throws ChannelManagerEventListenerException {
            throw new ChannelManagerEventListenerException(Status.STOP_CHANNEL_PROCESSING);
        }

        @Override
        public void channelUpdated(ChannelManagerEvent event) throws ChannelManagerEventListenerException {
            throw new ChannelManagerEventListenerException(Status.STOP_CHANNEL_PROCESSING);
        }

    }
}
