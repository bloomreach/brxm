/*
 *  Copyright 2011 Hippo.
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
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;

import org.easymock.IAnswer;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.channel.ChannelException.Type;
import org.hippoecm.hst.configuration.channel.ChannelManagerEventListenerException.Status;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.MutableMount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.container.CmsJcrSessionThreadLocal;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.RepositoryNotAvailableException;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.security.HstSubject;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractHstTestCase;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNull;

public class ChannelManagerImplTest extends AbstractHstTestCase {

    public static interface TestChannelInfo extends ChannelInfo {

        @Parameter(name = "title", defaultValue = "default")
        String getTitle();

    }

    private List<Mount> mounts;
    private VirtualHost testHost;
    private MutableMount testMount;

    public void setComponentManager(ComponentManager componentManager) {
        HstServices.setComponentManager(componentManager);
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
        ChannelManagerImpl manager = createManager();

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
    public void channelsAreReadCorrectly() throws ChannelException, RepositoryException {
        ChannelManagerImpl manager = createManager();

        Map<String, Channel> channels = manager.getChannels();
        assertEquals(1, channels.size());
        assertEquals("testchannel", channels.keySet().iterator().next());

        Channel channel = channels.values().iterator().next();
        assertEquals("testchannel", channel.getId());
        assertEquals("Test Channel", channel.getName());
        assertEquals("en_EN", channel.getLocale());
    }

    @Test
    public void channelWithContextPathIsSavedCorrectly() throws ChannelException, RepositoryException, PrivilegedActionException {
        final ChannelManagerImpl manager = createManager();

        Map<String, Channel> channels = manager.getChannels();
        assertEquals(1, channels.size());

        final Channel channel = channels.values().iterator().next();
        assertNull("Initial channel should not have a context path", channel.getContextPath());
        channel.setUrl("http://localhost:8080/site");
        channel.setContextPath("/site");
        channel.setMountPath("");
        channel.setChannelInfoClassName(getClass().getCanonicalName() + "$" + TestChannelInfo.class.getSimpleName());
        channel.getProperties().put("title", "test title");

        HstSubject.doAsPrivileged(createSubject(), new PrivilegedExceptionAction<Void>() {
            @Override
            public Void run() throws ChannelException {
                manager.save(channel);
                return null;
            }
        }, null);


        channels = createManager().getChannels();
        assertEquals(1, channels.size());
        Channel savedChannel = channels.values().iterator().next();

        Map<String, Object> savedProperties = savedChannel.getProperties();
        assertTrue(savedProperties.containsKey("title"));
        assertEquals("test title", savedProperties.get("title"));
    }

    @Test
    public void channelsAreClonedWhenRetrieved() throws ChannelException, RepositoryException {
        ChannelManagerImpl manager = createManager();

        Map<String, Channel> channels = manager.getChannels();
        Channel channel = channels.values().iterator().next();
        channel.setName("aap");

        Map<String, Channel> moreChannels = manager.getChannels();
        assertFalse(moreChannels == channels);
        assertEquals("Test Channel", moreChannels.values().iterator().next().getName());
    }

    @Test
    public void channelIsCreatedFromBlueprint() throws ChannelException, RepositoryException, PrivilegedActionException {
        final ChannelManagerImpl manager = createManager();

        List<Blueprint> bluePrints = manager.getBlueprints();
        assertEquals(1, bluePrints.size());
        final Blueprint blueprint = bluePrints.get(0);

        final Channel channel = blueprint.getPrototypeChannel();
        channel.setName("CMIT Test Channel: with special and/or specific characters");
        channel.setUrl("http://cmit-myhost");
        channel.setContentRoot("/unittestcontent/documents");
        channel.setLocale("nl_NL");

        String channelId = HstSubject.doAsPrivileged(createSubject(), new PrivilegedExceptionAction<String>() {
            @Override
            public String run() throws ChannelException {
                return manager.persist(blueprint.getId(), channel);
            }
        }, null);
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
        assertEquals(getSession().getNode("/unittestcontent/documents").getIdentifier(),
                siteNode.getNode(HstNodeTypes.NODENAME_HST_CONTENTNODE).getProperty(HippoNodeType.HIPPO_DOCBASE).getString());
    }

    @Test
    public void channelsAreReloaded() throws ChannelException, RepositoryException, PrivilegedActionException, RepositoryNotAvailableException {
        final ChannelManagerImpl manager = createManager();
        int numberOfChannels = manager.getChannels().size();

        Node channelsNode = getSession().getNode("/hst:hst/hst:channels");
        Node channel = channelsNode.addNode("cmit-test-channel", "hst:channel");
        channel.setProperty("hst:name", "CMIT Test Channel");
        getSession().save();

        manager.invalidate();

        // manager should reload

        reset(testHost, testMount);
        

        VirtualHosts vhosts = HstServices.getComponentManager().getComponent(VirtualHosts.class.getName());
        expectMountLoad(testHost, testMount, vhosts);

        MutableMount newMount = createNiceMock(MutableMount.class);
        mounts.add(newMount);
        
        VirtualHost newHost = createNiceMock(VirtualHost.class);
        
        expect(newMount.getChannelPath()).andReturn("/hst:hst/hst:channels/cmit-test-channel").anyTimes();
        expect(newMount.getMountPoint()).andReturn("mountpoint").anyTimes();
        expect(newMount.getHstSite()).andReturn(createNiceMock(HstSite.class)).anyTimes();
        expect(newMount.getCanonicalContentPath()).andReturn("/unittestcontent/documents");
        expect(newMount.getVirtualHost()).andReturn(newHost).anyTimes();
        expect(newHost.getHostName()).andReturn("myhost").anyTimes();
        expect(newHost.getVirtualHosts()).andReturn(vhosts).anyTimes();
        expect(newMount.getLocale()).andReturn("nl_NL").anyTimes();
        expect(newMount.getScheme()).andReturn("http").anyTimes();
        expect(newMount.isPreview()).andReturn(false);
        expect(newMount.isMapped()).andReturn(true);
        expect(newMount.getMountPath()).andReturn("").anyTimes();
        expect(newMount.getPort()).andReturn(0).anyTimes();

        replay(testHost, testMount, newHost, newMount);
      
        Map<String, Channel> channels = manager.getChannels();

        // verify reload
        verify(testHost, testMount, newHost, newMount);

        assertEquals(numberOfChannels + 1, channels.size());
        assertTrue(channels.containsKey("cmit-test-channel"));

        Channel created = channels.get("cmit-test-channel");
        assertNotNull(created);
        assertEquals("cmit-test-channel", created.getId());
        assertEquals("CMIT Test Channel", created.getName());
        assertEquals("http://myhost", created.getUrl());
        assertEquals("/unittestcontent/documents", created.getContentRoot());
        assertEquals("nl_NL", created.getLocale());
    }

    @Test
    public void ancestorMountsMustExist() throws ChannelException, RepositoryException, PrivilegedActionException {
        final ChannelManagerImpl manager = createManager();

        List<Blueprint> bluePrints = manager.getBlueprints();
        assertEquals(1, bluePrints.size());
        final Blueprint blueprint = bluePrints.get(0);

        final Channel channel = blueprint.getPrototypeChannel();
        channel.setName("cmit-channel");
        channel.setUrl("http://cmit-myhost/newmount");

        try {
            HstSubject.doAsPrivileged(createSubject(), new PrivilegedExceptionAction<String>() {
                @Override
                public String run() throws ChannelException {
                    return manager.persist(blueprint.getId(), channel);
                }
            }, null);
            fail("Expected a ChannelException with type " + ChannelException.Type.MOUNT_NOT_FOUND);
        } catch (PrivilegedActionException e) {
            assertEquals(ChannelException.Type.MOUNT_NOT_FOUND, ((ChannelException)e.getCause()).getType());
        }
    }

    public static interface TestInfoClass extends ChannelInfo {
        @Parameter(name="getme", defaultValue = "aap")
        String getGetme();
    }

    @Test
    public void blueprintDefaultValuesAreCopied() throws RepositoryException, ChannelException, PrivilegedActionException {
        Node configNode = getSession().getRootNode().getNode("hst:hst");
        Node bpFolder = configNode.getNode(HstNodeTypes.NODENAME_HST_BLUEPRINTS);

        Node bp = bpFolder.addNode("cmit-test-bp", HstNodeTypes.NODETYPE_HST_BLUEPRINT);
        bp.addNode(HstNodeTypes.NODENAME_HST_CONFIGURATION, HstNodeTypes.NODETYPE_HST_CONFIGURATION);
        Node channelBlueprint = bp.addNode(HstNodeTypes.NODENAME_HST_CHANNEL, HstNodeTypes.NODETYPE_HST_CHANNEL);
        channelBlueprint.setProperty(HstNodeTypes.CHANNEL_PROPERTY_CHANNELINFO_CLASS, TestInfoClass.class.getName());
        Node defaultChannelInfo = channelBlueprint.addNode(HstNodeTypes.NODENAME_HST_CHANNELINFO, HstNodeTypes.NODETYPE_HST_CHANNELINFO);
        defaultChannelInfo.setProperty("getme", "noot");
        getSession().save();

        final ChannelManagerImpl manager = createManager();

        final Channel channel = manager.getBlueprint("cmit-test-bp").getPrototypeChannel();
        channel.setName("cmit-channel");
        channel.setUrl("http://cmit-myhost");
        channel.setContentRoot("/");
        Map<String, Object> properties = channel.getProperties();
        assertTrue(properties.containsKey("getme"));
        assertEquals("noot", properties.get("getme"));

        HstSubject.doAsPrivileged(createSubject(), new PrivilegedExceptionAction<String>() {
            @Override
            public String run() throws ChannelException {
                return manager.persist("cmit-test-bp", channel);
            }
        }, null);
        TestInfoClass channelInfo = manager.getChannelInfo(channel);
        assertEquals("noot", channelInfo.getGetme());
    }

    @Test
    public void testChannelManagerEventListeners() throws RepositoryException, ChannelException, PrivilegedActionException {
        Node configNode = getSession().getRootNode().getNode("hst:hst");
        Node bpFolder = configNode.getNode(HstNodeTypes.NODENAME_HST_BLUEPRINTS);

        Node bp = bpFolder.addNode("cmit-test-bp2", HstNodeTypes.NODETYPE_HST_BLUEPRINT);
        bp.addNode(HstNodeTypes.NODENAME_HST_CONFIGURATION, HstNodeTypes.NODETYPE_HST_CONFIGURATION);
        Node channelBlueprint = bp.addNode(HstNodeTypes.NODENAME_HST_CHANNEL, HstNodeTypes.NODETYPE_HST_CHANNEL);
        channelBlueprint.setProperty(HstNodeTypes.CHANNEL_PROPERTY_CHANNELINFO_CLASS, TestInfoClass.class.getName());
        Node defaultChannelInfo = channelBlueprint.addNode(HstNodeTypes.NODENAME_HST_CHANNELINFO, HstNodeTypes.NODETYPE_HST_CHANNELINFO);
        defaultChannelInfo.setProperty("getme", "noot");
        getSession().save();

        final ChannelManagerImpl manager = createManager();

        Channel channel = manager.getBlueprint("cmit-test-bp2").getPrototypeChannel();
        channel.setName("cmit-channel2");
        channel.setUrl("http://cmit-myhost2");
        channel.setContentRoot("/");
        Map<String, Object> properties = channel.getProperties();
        assertTrue(properties.containsKey("getme"));
        assertEquals("noot", properties.get("getme"));
        
        MyChannelManagerEventListener listener1 = new MyChannelManagerEventListener();
        MyChannelManagerEventListener listener2 = new MyChannelManagerEventListener();
        MyChannelManagerEventListener listener3 = new MyChannelManagerEventListener();
        
        manager.addChannelManagerEventListeners(listener1, listener2, listener3);

        Subject subject = createSubject();
        final Channel channelToPersist = channel;
        
        String channelId = HstSubject.doAsPrivileged(subject, new PrivilegedExceptionAction<String>() {
            @Override
            public String run() throws ChannelException {
                return manager.persist("cmit-test-bp2", channelToPersist);
            }
        }, null);
        
        assertEquals(1, listener1.getCreatedCount());
        assertEquals(1, listener2.getCreatedCount());
        assertEquals(1, listener3.getCreatedCount());
        assertEquals(0, listener1.getUpdatedCount());
        assertEquals(0, listener2.getUpdatedCount());
        assertEquals(0, listener3.getUpdatedCount());
        
        channel = manager.getChannels().get(channelId);
        channel.setName("cmit-channel2");
        channel.setUrl("http://cmit-myhost2");
        channel.setContentRoot("/");
        final Channel channelToSave = channel;
        
        HstSubject.doAsPrivileged(subject, new PrivilegedExceptionAction<Object>() {
            @Override
            public Object run() throws ChannelException {
                manager.save(channelToSave);
                return null;
            }
        }, null);

        assertEquals(1, listener1.getCreatedCount());
        assertEquals(1, listener2.getCreatedCount());
        assertEquals(1, listener3.getCreatedCount());
        assertEquals(1, listener1.getUpdatedCount());
        assertEquals(1, listener2.getUpdatedCount());
        assertEquals(1, listener3.getUpdatedCount());

        manager.removeChannelManagerEventListeners(listener1, listener2, listener3);
        
        channel = manager.getChannels().get(channelId);
        channel.setName("cmit-channel2");
        channel.setUrl("http://cmit-myhost2");
        channel.setContentRoot("/");
        final Channel channelToSave2 = channel;
        
        HstSubject.doAsPrivileged(subject, new PrivilegedExceptionAction<Object>() {
            @Override
            public Object run() throws ChannelException {
                manager.save(channelToSave2);
                return null;
            }
        }, null);

        assertEquals(1, listener1.getCreatedCount());
        assertEquals(1, listener2.getCreatedCount());
        assertEquals(1, listener3.getCreatedCount());
        assertEquals(1, listener1.getUpdatedCount());
        assertEquals(1, listener2.getUpdatedCount());
        assertEquals(1, listener3.getUpdatedCount());
    }

    @Test
    public void testChannelManagerShortCircuitingEventListeners() throws RepositoryException, ChannelException, PrivilegedActionException {
        Node configNode = getSession().getRootNode().getNode("hst:hst");
        Node bpFolder = configNode.getNode(HstNodeTypes.NODENAME_HST_BLUEPRINTS);
    
        Node bp = bpFolder.addNode("cmit-test-bp2", HstNodeTypes.NODETYPE_HST_BLUEPRINT);
        bp.addNode(HstNodeTypes.NODENAME_HST_CONFIGURATION, HstNodeTypes.NODETYPE_HST_CONFIGURATION);
        Node channelBlueprint = bp.addNode(HstNodeTypes.NODENAME_HST_CHANNEL, HstNodeTypes.NODETYPE_HST_CHANNEL);
        channelBlueprint.setProperty(HstNodeTypes.CHANNEL_PROPERTY_CHANNELINFO_CLASS, TestInfoClass.class.getName());
        Node defaultChannelInfo = channelBlueprint.addNode(HstNodeTypes.NODENAME_HST_CHANNELINFO, HstNodeTypes.NODETYPE_HST_CHANNELINFO);
        defaultChannelInfo.setProperty("getme", "noot");
        getSession().save();

        final ChannelManagerImpl manager = createManager();

        Channel channel = manager.getBlueprint("cmit-test-bp2").getPrototypeChannel();
        channel.setName("cmit-channel2");
        channel.setUrl("http://cmit-myhost2");
        channel.setContentRoot("/");
        Map<String, Object> properties = channel.getProperties();
        assertTrue(properties.containsKey("getme"));
        assertEquals("noot", properties.get("getme"));
        
        ChannelManagerEventListener shortCircuitingListener = new MyShortCircuitingEventListener();
        
        manager.addChannelManagerEventListeners(shortCircuitingListener);

        Subject subject = createSubject();
        final Channel channelToPersist = channel;
        
        String channelId = HstSubject.doAsPrivileged(subject, new PrivilegedExceptionAction<String>() {
            @Override
            public String run() {
                try {
                    String s = manager.persist("cmit-test-bp2", channelToPersist);
                    fail("The persist should fail");
                    return s;
                } catch (ChannelException e) {
                    // we should get here because of MyShortCircuitingEventListener
                    assertTrue("Type of the ChannelException should be  STOPPED_BY_LISTENER ",e.getType() == Type.STOPPED_BY_LISTENER);
                    return null;
                }
            }
        }, null);
       
        assertTrue("channelId should be null ",channelId == null);
    }
    
    private Subject createSubject() {
        Subject subject = new Subject();
        subject.getPrivateCredentials().add(new SimpleCredentials("admin", "admin".toCharArray()));
        subject.setReadOnly();
        return subject;
    }

    private ChannelManagerImpl createManager() throws ChannelException {

        final ChannelManagerImpl manager = new ChannelManagerImpl();
        manager.setRepository(getRepository());
        
        ComponentManager cm = createMock(ComponentManager.class);
        setComponentManager(cm);

        final VirtualHosts vhosts = createNiceMock(VirtualHosts.class);
        expect(vhosts.getCmsPreviewPrefix()).andReturn("_cmsinternal").anyTimes();
        HstManager hstMgr = createNiceMock(HstManager.class);
        try {
            expect(hstMgr.getVirtualHosts()).andAnswer(new IAnswer<VirtualHosts>() {
                @Override
                public VirtualHosts answer() throws Throwable {
                    manager.load(vhosts, isA(Session.class));
                    return vhosts;
                }
            }).anyTimes();
        } catch (RepositoryNotAvailableException e) {
            // mock impl doesn't throw
        }
        expect(cm.getComponent(HstManager.class.getName())).andReturn(hstMgr).anyTimes();
        expect(cm.getComponent(VirtualHosts.class.getName())).andReturn(vhosts).anyTimes();

        mounts = new LinkedList<Mount>();
        expect(vhosts.getMountsByHostGroup("dev-localhost")).andReturn(mounts).anyTimes();
        expect(vhosts.getHostGroupNames()).andReturn(Arrays.asList("dev-localhost")).anyTimes();
        expect(vhosts.getChannelManagerHostGroupName()).andReturn("dev-localhost").anyTimes();
        expect(vhosts.getChannelManagerSitesName()).andReturn("hst:sites").anyTimes();
        
        testHost = createNiceMock(VirtualHost.class);
        
        testMount = createNiceMock(MutableMount.class);
        mounts.add(testMount);

        expectMountLoad(testHost, testMount, vhosts);

        replay(vhosts, cm, hstMgr, testHost, testMount);

        manager.load();

        verify(testHost, testMount);

        return manager;
    }

    private void propagateJcrSession(Credentials credentials) throws LoginException, RepositoryException {
        Session session;

        if (credentials == null) {
            session = getRepository().login();
        } else {
            session = getRepository().login(credentials);
        }

        CmsJcrSessionThreadLocal.setJcrSession(session);
    }

    private void propagateJcrSession() throws LoginException, RepositoryException {
        propagateJcrSession(null);
    }

    private void dePropagateSession() {
        CmsJcrSessionThreadLocal.clearJcrSession();
    }

    private Credentials getCredentials() {
        return new SimpleCredentials("admin", "admin".toCharArray());
    }

    @Override
    public Session getSession() {
        return CmsJcrSessionThreadLocal.getJcrSession();
    }

    private static void expectMountLoad(final VirtualHost host, final MutableMount mount, VirtualHosts vhosts) {
        expect(host.getHostName()).andReturn("localhost").anyTimes();
        expect(host.getVirtualHosts()).andReturn(vhosts).anyTimes();
        
        expect(mount.getMountPoint()).andReturn("mountpoint");
        expect(mount.getHstSite()).andReturn(createNiceMock(HstSite.class));
        expect(mount.getCanonicalContentPath()).andReturn("/content/documents");

        expect(mount.getChannelPath()).andReturn("/hst:hst/hst:channels/testchannel");
        expect(mount.getVirtualHost()).andReturn(host).anyTimes();
        expect(mount.getLocale()).andReturn("en_EN");
        expect(mount.getScheme()).andReturn("http");
        expect(mount.getMountPath()).andReturn("");
        expect(mount.isPreview()).andReturn(false);
        expect(mount.isMapped()).andReturn(true);
        expect(mount.isPortInUrl()).andReturn(false);
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
