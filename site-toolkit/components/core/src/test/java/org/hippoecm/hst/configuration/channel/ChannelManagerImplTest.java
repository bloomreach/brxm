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
package org.hippoecm.hst.configuration.channel;

import java.security.PrivilegedActionException;
import java.util.List;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.channel.ChannelManagerEventListenerException.Status;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.model.EventPathsInvalidator;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.core.container.CmsJcrSessionThreadLocal;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.hippoecm.hst.util.JcrSessionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNull;

public class ChannelManagerImplTest extends AbstractTestConfigurations {

    public static interface TestChannelInfo extends ChannelInfo {

        @Parameter(name = "title", defaultValue = "default")
        String getTitle();

    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        propagateJcrSession(getCredentials());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        HstServices.setComponentManager(null);

        Session session = getSession();
        for (NodeIterator ni = session.getNode("/hst:hst/hst:hosts/dev-localhost").getNodes("cmit-*"); ni.hasNext(); ) {
            ni.nextNode().remove();
        }
        for (NodeIterator ni = session.getNode("/hst:hst/hst:sites").getNodes("channel-*"); ni.hasNext(); ) {
            ni.nextNode().remove();
        }
        for (NodeIterator ni = session.getNode("/hst:hst/hst:sites").getNodes("cmit-*"); ni.hasNext(); ) {
            ni.nextNode().remove();
        }
        for (NodeIterator ni = session.getNode("/hst:hst/hst:configurations").getNodes("channel-*"); ni.hasNext(); ) {
            ni.nextNode().remove();
        }
        for (NodeIterator ni = session.getNode("/hst:hst/hst:configurations").getNodes("cmit-*"); ni.hasNext(); ) {
            ni.nextNode().remove();
        }
        for (NodeIterator ni = session.getNode("/hst:hst/hst:channels").getNodes("cmit-*"); ni.hasNext(); ) {
            ni.nextNode().remove();
        }
        for (NodeIterator ni = session.getNode("/hst:hst/hst:blueprints").getNodes("cmit-*"); ni.hasNext(); ) {
            ni.nextNode().remove();
        }
        session.save();
        dePropagateSession();

        super.tearDown();
    }

    @Test
    public void createUniqueChannelId() throws RepositoryException, ChannelException {
        final ChannelManagerImpl manager = HstServices.getComponentManager().getComponent(ChannelManager.class.getName());

        assertEquals("test", manager.createUniqueChannelId("test", getSession()));
        assertEquals("name-with-spaces", manager.createUniqueChannelId("Name with Spaces", getSession()));
        assertEquals("special-characters--and---and", manager.createUniqueChannelId("Special Characters: % and / and []", getSession()));
        assertEquals("'testchannel' already exists in the default unit test content, so the new channel ID should get a suffix",
                "testchannel-1", manager.createUniqueChannelId("testchannel", getSession()));
        assertEquals("'unittestproject' already exists as an hst:site node in the default unit test content, so the new channel ID should get a suffix",
                "unittestproject-1", manager.createUniqueChannelId("unittestproject", getSession()));
        assertEquals("'unittestcommon' already exists as an hst:configuration node in the default unit test content, so the new channel ID should get a suffix",
                "unittestcommon-1", manager.createUniqueChannelId("unittestcommon", getSession()));
    }

    @Test
    public void channelsAreReadCorrectly() throws Exception {
        final HstManager manager = HstServices.getComponentManager().getComponent(HstManager.class.getName());

        Map<String, Channel> channels = manager.getVirtualHosts().getChannels();
        assertEquals(1, channels.size());
        assertEquals("testchannel", channels.keySet().iterator().next());

        Channel channel = channels.values().iterator().next();
        assertEquals("testchannel", channel.getId());
        assertEquals("Test Channel", channel.getName());
        assertEquals("en_EN", channel.getLocale());
    }

    @Test
    public void channelPropertiesSaved() throws Exception {
        final ChannelManagerImpl channelMngr= HstServices.getComponentManager().getComponent(ChannelManager.class.getName());
        final HstManager hstManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());
        Map<String, Channel> channels = hstManager.getVirtualHosts().getChannels();
        try (ClosableMockHstRequestContext cmrc = new ClosableMockHstRequestContext(hstManager)) {
            assertEquals(1, channels.size());
            final Channel channel = channels.values().iterator().next();
            channel.setChannelInfoClassName(getClass().getCanonicalName() + "$" + TestChannelInfo.class.getSimpleName());
            channel.getProperties().put("title", "test title");
            channelMngr.save(channel);
            // give jcr events time to invalidate the model
            Thread.sleep(200);

            channels =  hstManager.getVirtualHosts().getChannels();

            assertEquals(1, channels.size());
            Channel savedChannel = channels.values().iterator().next();

            Map<String, Object> savedProperties = savedChannel.getProperties();
            assertTrue(savedProperties.containsKey("title"));
            assertEquals("test title", savedProperties.get("title"));

        }

    }


    @Test
    public void channelsAreNotClonedWhenRetrieved() throws Exception {
        final HstManager hstManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());
        Map<String, Channel> channels = hstManager.getVirtualHosts().getChannels();
        Map<String, Channel> channelsAgain = hstManager.getVirtualHosts().getChannels();
        assertTrue(channelsAgain == channels);
    }

    @Test
    public void channelsMapIsNewInstanceWhenReloadedAfterChange() throws Exception {
        final ChannelManagerImpl channelMngr= HstServices.getComponentManager().getComponent(ChannelManager.class.getName());
        final HstManager hstManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());
        Map<String, Channel> channels = hstManager.getVirtualHosts().getChannels();

        try (ClosableMockHstRequestContext cmrc = new ClosableMockHstRequestContext(hstManager)) {
            final Channel channel = channels.values().iterator().next();
            channel.setChannelInfoClassName(getClass().getCanonicalName() + "$" + TestChannelInfo.class.getSimpleName());
            channel.getProperties().put("title", "test title");
            channelMngr.save(channel);
            // give jcr events time to invalidate the model
            Thread.sleep(200);

            Map<String, Channel> channelsAgain = hstManager.getVirtualHosts().getChannels();
            assertTrue("After a change, getChannels should return different instance for the Map",channelsAgain != channels);
        }
    }


    @Test
    public void channelIsCreatedFromBlueprint() throws Exception {
        final ChannelManagerImpl channelMngr= HstServices.getComponentManager().getComponent(ChannelManager.class.getName());
        final HstManager hstManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());

        List<Blueprint> bluePrints = hstManager.getVirtualHosts().getBlueprints();
        assertEquals(1, bluePrints.size());
        final Blueprint blueprint = bluePrints.get(0);

        final Channel channel = blueprint.getPrototypeChannel();
        channel.setName("CMIT Test Channel: with special and/or specific characters");
        channel.setUrl("http://cmit-myhost");
        channel.setContentRoot("/unittestcontent/documents");
        channel.setLocale("nl_NL");

        try (ClosableMockHstRequestContext cmrc = new ClosableMockHstRequestContext(hstManager)) {
            String channelId = channelMngr.persist(blueprint.getId(), channel);
            final String encodedChannelName = "cmit-test-channel-with-special-and-or-specific-characters";
            assertEquals(encodedChannelName, channelId);

            Node channelNode = getSession().getNode("/hst:hst/hst:channels/" + channelId);
            assertEquals("CMIT Test Channel: with special and/or specific characters", channelNode.getProperty("hst:name").getString());

            Node hostNode = getSession().getNode("/hst:hst/hst:hosts/dev-localhost");
            assertTrue(hostNode.hasNode("cmit-myhost/hst:root"));

            Node mountNode = hostNode.getNode("cmit-myhost/hst:root");
            assertEquals("nl_NL", mountNode.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCALE).getString());
            String sitePath = "/hst:hst/hst:sites/" + channelId;
            assertEquals(sitePath, mountNode.getProperty(HstNodeTypes.MOUNT_PROPERTY_MOUNTPOINT).getString());
            assertTrue(getSession().itemExists(sitePath));

            Node siteNode = getSession().getNode(sitePath);
            assertEquals("/unittestcontent/documents", siteNode.getProperty(HstNodeTypes.SITE_CONTENT).getString());
        }
    }

    @Test
    public void channelsAreReloadedAfterAddingOne() throws Exception {
        final HstManager hstManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());
        Map<String, Channel> channels = hstManager.getVirtualHosts().getChannels();
        int numberOfChannels = channels.size();

        Node channelsNode = getSession().getNode("/hst:hst/hst:channels");
        Node newChannel = channelsNode.addNode("cmit-test-channel", "hst:channel");
        newChannel.setProperty("hst:name", "CMIT Test Channel");

        // channels must have a mount pointing to them otherwise they are skipped, hence point to this channel from
        // subsite mount
        Node mountForNewChannel = getSession().getNode("/hst:hst/hst:hosts/dev-localhost/localhost/hst:root/subsite");
        mountForNewChannel.setProperty("hst:channelpath", newChannel.getPath());

        // for direct jcr node changes, we need to trigger an invalidation event ourselves
        EventPathsInvalidator invalidator = HstServices.getComponentManager().getComponent(EventPathsInvalidator.class.getName());
        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(getSession(), getSession().getNode("/hst:hst"), false);

        getSession().save();
        invalidator.eventPaths(pathsToBeChanged);

        // manager should reload
        channels =  hstManager.getVirtualHosts().getChannels();
        assertEquals(numberOfChannels + 1, channels.size());
        assertTrue(channels.containsKey("cmit-test-channel"));

        Channel created = channels.get("cmit-test-channel");
        assertNotNull(created);
        assertEquals("cmit-test-channel", created.getId());
        assertEquals("CMIT Test Channel", created.getName());
        assertEquals("http://localhost/site/subsite", created.getUrl());
        assertEquals("/unittestcontent/documents/unittestsubproject", created.getContentRoot());
        assertEquals("en_EN", created.getLocale());

        // clean up only the added channelpath for subsite
        mountForNewChannel.getProperty("hst:channelpath").remove();
        getSession().save();
    }

    @Test
    public void channelsThatAreNotReferencedByAMountAreDiscarded() throws ChannelException, RepositoryException, PrivilegedActionException, ContainerException {
        final HstManager hstManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());
        Map<String, Channel> channels = hstManager.getVirtualHosts().getChannels();
        int numberOfChannerBeforeAddingAnOrphanOne = channels.size();

        Node channelsNode = getSession().getNode("/hst:hst/hst:channels");
        Node newChannel = channelsNode.addNode("cmit-test-channel", "hst:channel");
        newChannel.setProperty("hst:name", "CMIT Test Channel");
        getSession().save();

        // for direct jcr node changes, we need to trigger an invalidation event ourselves
        EventPathsInvalidator invalidator = HstServices.getComponentManager().getComponent(EventPathsInvalidator.class.getName());
        String[] pathsToBeChanged = JcrSessionUtils.getPendingChangePaths(getSession(), getSession().getNode("/hst:hst"), false);

        getSession().save();
        invalidator.eventPaths(pathsToBeChanged);

        channels = hstManager.getVirtualHosts().getChannels();

        assertEquals(numberOfChannerBeforeAddingAnOrphanOne , channels.size());
        assertFalse(channels.containsKey("cmit-test-channel"));

    }

    @Test(expected = ChannelException.class)
    public void ancestorMountsMustExist() throws Exception {
        final ChannelManagerImpl channelMngr= HstServices.getComponentManager().getComponent(ChannelManager.class.getName());
        final HstManager hstManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());

        List<Blueprint> bluePrints = hstManager.getVirtualHosts().getBlueprints();
        assertEquals(1, bluePrints.size());
        final Blueprint blueprint = bluePrints.get(0);

        final Channel channel = blueprint.getPrototypeChannel();
        channel.setName("cmit-channel");
        channel.setUrl("http://cmit-myhost/newmount");

        try (ClosableMockHstRequestContext cmrc = new ClosableMockHstRequestContext(hstManager)) {
            channelMngr.persist(blueprint.getId(), channel);
        }
    }

    public static interface TestInfoClass extends ChannelInfo {
        @Parameter(name="getme", defaultValue = "aap")
        String getGetme();
    }

    @Test
    public void blueprintDefaultValuesAreCopied() throws Exception {
        final HstManager hstManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());

        Node configNode = getSession().getRootNode().getNode("hst:hst");
        Node bpFolder = configNode.getNode(HstNodeTypes.NODENAME_HST_BLUEPRINTS);

        Node bp = bpFolder.addNode("cmit-test-bp", HstNodeTypes.NODETYPE_HST_BLUEPRINT);
        bp.addNode(HstNodeTypes.NODENAME_HST_CONFIGURATION, HstNodeTypes.NODETYPE_HST_CONFIGURATION);
        Node channelBlueprint = bp.addNode(HstNodeTypes.NODENAME_HST_CHANNEL, HstNodeTypes.NODETYPE_HST_CHANNEL);
        channelBlueprint.setProperty(HstNodeTypes.CHANNEL_PROPERTY_CHANNELINFO_CLASS, TestInfoClass.class.getName());
        Node defaultChannelInfo = channelBlueprint.addNode(HstNodeTypes.NODENAME_HST_CHANNELINFO, HstNodeTypes.NODETYPE_HST_CHANNELINFO);
        defaultChannelInfo.setProperty("getme", "noot");
        getSession().save();

        final ChannelManagerImpl manager = HstServices.getComponentManager().getComponent(ChannelManager.class.getName());

        final Channel channel = hstManager.getVirtualHosts().getBlueprint("cmit-test-bp").getPrototypeChannel();
        channel.setName("cmit-channel");
        channel.setUrl("http://cmit-myhost");
        channel.setContentRoot("/");
        Map<String, Object> properties = channel.getProperties();
        assertTrue(properties.containsKey("getme"));
        assertEquals("noot", properties.get("getme"));

        try (ClosableMockHstRequestContext cmrc = new ClosableMockHstRequestContext(hstManager)) {
            manager.persist("cmit-test-bp", channel);
            Thread.sleep(200);
            TestInfoClass channelInfo = hstManager.getVirtualHosts().getChannelInfo(channel);
            assertEquals("noot", channelInfo.getGetme());
        }
    }

    @Test
    public void testChannelManagerEventListeners() throws Exception {
        final HstManager hstManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());
        final ChannelManagerImpl channelMngr = HstServices.getComponentManager().getComponent(ChannelManager.class.getName());

        Node configNode = getSession().getRootNode().getNode("hst:hst");
        Node bpFolder = configNode.getNode(HstNodeTypes.NODENAME_HST_BLUEPRINTS);

        Node bp = bpFolder.addNode("cmit-test-bp2", HstNodeTypes.NODETYPE_HST_BLUEPRINT);
        Node hstConfigNode = bp.addNode(HstNodeTypes.NODENAME_HST_CONFIGURATION, HstNodeTypes.NODETYPE_HST_CONFIGURATION);
        hstConfigNode.addNode("hst:sitemap","hst:sitemap");
        hstConfigNode.setProperty("hst:inheritsfrom", new String[] {"../unittestcommon"});
        Node channelBlueprint = bp.addNode(HstNodeTypes.NODENAME_HST_CHANNEL, HstNodeTypes.NODETYPE_HST_CHANNEL);
        channelBlueprint.setProperty(HstNodeTypes.CHANNEL_PROPERTY_CHANNELINFO_CLASS, TestInfoClass.class.getName());
        Node defaultChannelInfo = channelBlueprint.addNode(HstNodeTypes.NODENAME_HST_CHANNELINFO, HstNodeTypes.NODETYPE_HST_CHANNELINFO);
        defaultChannelInfo.setProperty("getme", "noot");
        getSession().save();

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
        try (ClosableMockHstRequestContext cmrc = new ClosableMockHstRequestContext(hstManager)) {
            channelId =  channelMngr.persist("cmit-test-bp2", channelToPersist);
        }
        assertEquals(1, listener1.getCreatedCount());
        assertEquals(1, listener2.getCreatedCount());
        assertEquals(1, listener3.getCreatedCount());
        assertEquals(0, listener1.getUpdatedCount());
        assertEquals(0, listener2.getUpdatedCount());
        assertEquals(0, listener3.getUpdatedCount());

        // give jcr events time to invalidate the model
        Thread.sleep(200);
        channel = hstManager.getVirtualHosts().getChannels().get(channelId);
        channel.setName("cmit-channel2");
        channel.setUrl("http://cmit-myhost2");
        channel.setContentRoot("/");
        final Channel channelToSave = channel;

        try (ClosableMockHstRequestContext cmrc = new ClosableMockHstRequestContext(hstManager)) {
            channelMngr.save(channelToSave);
        }
        assertEquals(1, listener1.getCreatedCount());
        assertEquals(1, listener2.getCreatedCount());
        assertEquals(1, listener3.getCreatedCount());
        assertEquals(1, listener1.getUpdatedCount());
        assertEquals(1, listener2.getUpdatedCount());
        assertEquals(1, listener3.getUpdatedCount());

        channelMngr.removeChannelManagerEventListeners(listener1, listener2, listener3);

        channel = hstManager.getVirtualHosts().getChannels().get(channelId);
        channel.setName("cmit-channel2");
        channel.setUrl("http://cmit-myhost2");
        channel.setContentRoot("/");
        final Channel channelToSave2 = channel;
        try (ClosableMockHstRequestContext cmrc = new ClosableMockHstRequestContext(hstManager)) {
            channelMngr.save(channelToSave2);
        }
        assertEquals(1, listener1.getCreatedCount());
        assertEquals(1, listener2.getCreatedCount());
        assertEquals(1, listener3.getCreatedCount());
        assertEquals(1, listener1.getUpdatedCount());
        assertEquals(1, listener2.getUpdatedCount());
        assertEquals(1, listener3.getUpdatedCount());

    }

    @Test(expected = ChannelException.class)
    public void testChannelManagerShortCircuitingEventListeners() throws Exception {
        Node configNode = getSession().getRootNode().getNode("hst:hst");
        Node bpFolder = configNode.getNode(HstNodeTypes.NODENAME_HST_BLUEPRINTS);
    
        Node bp = bpFolder.addNode("cmit-test-bp2", HstNodeTypes.NODETYPE_HST_BLUEPRINT);
        bp.addNode(HstNodeTypes.NODENAME_HST_CONFIGURATION, HstNodeTypes.NODETYPE_HST_CONFIGURATION);
        Node channelBlueprint = bp.addNode(HstNodeTypes.NODENAME_HST_CHANNEL, HstNodeTypes.NODETYPE_HST_CHANNEL);
        channelBlueprint.setProperty(HstNodeTypes.CHANNEL_PROPERTY_CHANNELINFO_CLASS, TestInfoClass.class.getName());
        Node defaultChannelInfo = channelBlueprint.addNode(HstNodeTypes.NODENAME_HST_CHANNELINFO, HstNodeTypes.NODETYPE_HST_CHANNELINFO);
        defaultChannelInfo.setProperty("getme", "noot");
        getSession().save();

        final ChannelManagerImpl channelMngr = HstServices.getComponentManager().getComponent(ChannelManager.class.getName());
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

        final Channel channelToPersist = channel;
        try (ClosableMockHstRequestContext cmrc = new ClosableMockHstRequestContext(hstManager)) {
            channelMngr.persist("cmit-test-bp2", channelToPersist);
        }
    }

    private void propagateJcrSession(Credentials credentials) throws LoginException, RepositoryException {
        Session session;
        Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        session = repository.login(credentials);
        CmsJcrSessionThreadLocal.setJcrSession(session);
    }

    private void dePropagateSession() {
        CmsJcrSessionThreadLocal.clearJcrSession();
    }

    private Credentials getCredentials() {
        return new SimpleCredentials("admin", "admin".toCharArray());
    }

    public Session getSession() {
        return CmsJcrSessionThreadLocal.getJcrSession();
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

    private static class ClosableMockHstRequestContext implements AutoCloseable {

        ClosableMockHstRequestContext(HstManager manager) throws ContainerException {
            MockHstRequestContext ctx = new MockHstRequestContext();
            final VirtualHost dummyHost = manager.getVirtualHosts().getMountsByHostGroup("dev-localhost").get(0).getVirtualHost();
            ctx.setVirtualHost(dummyHost);
            ModifiableRequestContextProvider.set(ctx);
        }

        @Override
        public void close() throws Exception {
            ModifiableRequestContextProvider.clear();
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
