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
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.security.HstSubject;
import org.hippoecm.hst.test.AbstractHstTestCase;
import org.junit.After;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class ChannelManagerImplTest extends AbstractHstTestCase {

    @Override
    @After
    public void tearDown() throws Exception {
        Node internalHostGroup = getSession().getNode("/hst:hst/hst:hosts/dev-localhost");
        if (internalHostGroup.hasNode("myhost")) {
            internalHostGroup.getNode("myhost").remove();
        }
        for (NodeIterator ni = getSession().getNode("/hst:hst/hst:sites").getNodes("channel-*"); ni.hasNext(); ) {
            ni.nextNode().remove();
        }
        for (NodeIterator ni = getSession().getNode("/hst:hst/hst:sites").getNodes("cmit-*"); ni.hasNext(); ) {
            ni.nextNode().remove();
        }
        for (NodeIterator ni = getSession().getNode("/hst:hst/hst:configurations").getNodes("channel-*"); ni.hasNext(); ) {
            ni.nextNode().remove();
        }
        for (NodeIterator ni = getSession().getNode("/hst:hst/hst:configurations").getNodes("cmit-*"); ni.hasNext(); ) {
            ni.nextNode().remove();
        }
        for (NodeIterator ni = getSession().getNode("/hst:hst/hst:channels").getNodes("cmit-*"); ni.hasNext(); ) {
            ni.nextNode().remove();
        }
        getSession().save();
        super.tearDown();
    }

    @Test
    public void createUniqueChannelId() throws RepositoryException, ChannelException {
        ChannelManagerImpl manager = createManager();

        assertEquals("test", manager.createUniqueChannelId("test"));
        assertEquals("name-with-spaces", manager.createUniqueChannelId("Name with Spaces"));
        assertEquals("special-characters--and---and", manager.createUniqueChannelId("Special Characters: % and / and []"));
        assertEquals("'testchannel' already exists in the default unit test content, so the new channel ID should get a suffix",
                "testchannel-1", manager.createUniqueChannelId("testchannel"));
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
        int numberOfChannels = manager.getChannels().size();

        List<Blueprint> bluePrints = manager.getBlueprints();
        assertEquals(1, bluePrints.size());
        final Blueprint blueprint = bluePrints.get(0);

        final Channel channel = blueprint.createChannel();
        channel.setName("CMIT Test Channel: with special and/or specific characters");
        channel.setUrl("http://myhost");
        channel.setContentRoot("/unittestcontent/documents");
        channel.setLocale("nl_NL");

        String channelId = HstSubject.doAsPrivileged(createSubject(), new PrivilegedExceptionAction<String>() {
            @Override
            public String run() throws ChannelException {
                return manager.persist(blueprint.getId(), channel);
            }
        }, null);
        assertEquals("cmit-test-channel-with-special-and-or-specific-characters", channelId);
        Node node = getSession().getNode("/hst:hst/hst:hosts/dev-localhost");

        assertTrue(node.hasNode("myhost/hst:root"));

        Map<String, Channel> channels = manager.getChannels();
        assertEquals(numberOfChannels + 1, channels.size());
        assertTrue(channels.containsKey("cmit-test-channel-with-special-and-or-specific-characters"));

        Channel created = channels.get("cmit-test-channel-with-special-and-or-specific-characters");
        assertNotNull(created);
        assertEquals("cmit-test-channel-with-special-and-or-specific-characters", created.getId());
        assertEquals("CMIT Test Channel: with special and/or specific characters", created.getName());
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

        final Channel channel = blueprint.createChannel();
        channel.setName("cmit-channel");
        channel.setUrl("http://myhost/newmount");

        try {
            HstSubject.doAsPrivileged(createSubject(), new PrivilegedExceptionAction<String>() {
                @Override
                public String run() throws ChannelException {
                    return manager.persist(blueprint.getId(), channel);
                }
            }, null);
            fail("Expected a " + MountNotFoundException.class.getName());
        } catch (PrivilegedActionException e) {
            assertEquals(MountNotFoundException.class, e.getCause().getClass());
        }
    }

    public static interface TestInfoClass extends ChannelInfo {
        @Parameter(name="getme", defaultValue = "aap")
        String getGetme();
    }

    @Test
    public void blueprintDefaultValuesAreCopied() throws RepositoryException, ChannelException, PrivilegedActionException {
        Node testNode = getSession().getRootNode().addNode("test", "hst:hst");
        testNode.addNode("hst:hosts").addNode("dev-localhost", HstNodeTypes.NODETYPE_HST_VIRTUALHOSTGROUP);
        testNode.addNode(HstNodeTypes.NODENAME_HST_CHANNELS, HstNodeTypes.NODETYPE_HST_CHANNELS);

        Node bpFolder = testNode.addNode(HstNodeTypes.NODENAME_HST_BLUEPRINTS, HstNodeTypes.NODETYPE_HST_BLUEPRINTS);
        Node bp = bpFolder.addNode("test-bp", HstNodeTypes.NODETYPE_HST_BLUEPRINT);
        Node channelBlueprint = bp.addNode(HstNodeTypes.NODENAME_HST_CHANNEL, HstNodeTypes.NODETYPE_HST_CHANNEL);
        channelBlueprint.setProperty(HstNodeTypes.CHANNEL_PROPERTY_CHANNELINFO_CLASS, TestInfoClass.class.getName());
        Node defaultChannelInfo = channelBlueprint.addNode(HstNodeTypes.NODENAME_HST_CHANNELINFO, HstNodeTypes.NODETYPE_HST_CHANNELINFO);
        defaultChannelInfo.setProperty("getme", "noot");
        getSession().save();

        final ChannelManagerImpl manager = createManager();
        manager.setRootPath(testNode.getPath());

        final Channel channel = manager.getBlueprint("test-bp").createChannel();
        channel.setName("cmit-channel");
        channel.setUrl("http://localhost");
        Map<String, Object> properties = channel.getProperties();
        assertTrue(properties.containsKey("getme"));
        assertEquals("noot", properties.get("getme"));

        HstSubject.doAsPrivileged(createSubject(), new PrivilegedExceptionAction<String>() {
            @Override
            public String run() throws ChannelException {
                return manager.persist("test-bp", channel);
            }
        }, null);
        TestInfoClass channelInfo = manager.getChannelInfo(channel);
        assertEquals("noot", channelInfo.getGetme());
    }

    private Subject createSubject() {
        Subject subject = new Subject();
        subject.getPrivateCredentials().add(new SimpleCredentials("admin", "admin".toCharArray()));
        subject.setReadOnly();
        return subject;
    }

    private ChannelManagerImpl createManager() throws RepositoryException {
        ChannelManagerImpl manager = new ChannelManagerImpl();
        manager.setRepository(getRepository());
        // FIXME: use readonly credentials
        manager.setCredentials(new SimpleCredentials("admin", "admin".toCharArray()));
        manager.setHostGroup("dev-localhost");
        manager.setSites("hst:sites");
        return manager;
    }

}
