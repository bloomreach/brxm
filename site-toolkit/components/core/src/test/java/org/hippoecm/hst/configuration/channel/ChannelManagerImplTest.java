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

import java.security.PrivilegedAction;
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
import static junit.framework.Assert.assertTrue;

public class ChannelManagerImplTest extends AbstractHstTestCase {

    @Override
    @After
    public void tearDown() throws Exception {
        Node internalHostGroup = getSession().getNode("/hst:hst/hst:hosts/dev-local");
        if (internalHostGroup.hasNode("myhost")) {
            internalHostGroup.getNode("myhost").remove();
        }
        for (NodeIterator ni = getSession().getNode("/hst:hst/hst:unittestsites").getNodes("channel-*"); ni.hasNext(); ) {
            ni.nextNode().remove();
        }
        for (NodeIterator ni = getSession().getNode("/hst:hst/hst:configurations").getNodes("channel-*"); ni.hasNext(); ) {
            ni.nextNode().remove();
        }
        for (NodeIterator ni = getSession().getNode("/hst:hst/hst:channels").getNodes("cmit-*"); ni.hasNext(); ) {
            ni.nextNode().remove();
        }
        internalHostGroup.getSession().save();
        super.tearDown();
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
    }

    @Test
    public void channelIsCreatedFromBlueprint() throws ChannelException, RepositoryException {
        final ChannelManagerImpl manager = createManager();
        int numberOfChannels = manager.getChannels().size();

        List<Blueprint> bluePrints = manager.getBlueprints();
        assertEquals(1, bluePrints.size());
        final Blueprint blueprint = bluePrints.get(0);

        final Channel channel = blueprint.createChannel("cmit-channel");
        channel.setUrl("http://myhost");
        channel.setContentRoot("/content/documents");
        asAdmin(new PrivilegedAction<ChannelException>() {
            @Override
            public ChannelException run() {
                try {
                    manager.persist(blueprint.getId(), channel);
                } catch (ChannelException ce) {
                    return ce;
                }
                return null;
            }
        });
        Node node = getSession().getNode("/hst:hst/hst:hosts/dev-local");

        assertTrue(node.hasNode("myhost/hst:root"));

        Map<String, Channel> channels = manager.getChannels();
        assertEquals(numberOfChannels + 1, channels.size());
        assertTrue(channels.containsKey(channel.getId()));
        assertEquals("http://myhost", channels.get(channel.getId()).getUrl());
    }

    @Test(expected = MountNotFoundException.class)
    public void ancestorMountsMustExist() throws ChannelException, RepositoryException {
        final ChannelManagerImpl manager = createManager();

        List<Blueprint> bluePrints = manager.getBlueprints();
        assertEquals(1, bluePrints.size());
        final Blueprint blueprint = bluePrints.get(0);

        final Channel channel = blueprint.createChannel("cmit-channel");
        channel.setUrl("http://myhost/newmount");
        asAdmin(new PrivilegedAction<ChannelException>() {
            @Override
            public ChannelException run() {
                try {
                    manager.persist(blueprint.getId(), channel);
                } catch (ChannelException ce) {
                    return ce;
                }
                return null;
            }
        });
    }

    public static interface TestInfoClass extends ChannelInfo {
        @Parameter(name="getme", defaultValue = "aap")
        String getGetme();
    }

    @Test
    public void blueprintDefaultValuesAreCopied() throws RepositoryException, ChannelException {
        Node testNode = getSession().getRootNode().addNode("test", "hst:hst");
        testNode.addNode("hst:hosts").addNode("dev-local", HstNodeTypes.NODETYPE_HST_VIRTUALHOSTGROUP);

        Node bpFolder = testNode.addNode(HstNodeTypes.NODENAME_HST_BLUEPRINTS, "hst:blueprints");
        Node bp = bpFolder.addNode("test-bp", "hst:blueprint");
        Node channelBlueprint = bp.addNode(HstNodeTypes.NODENAME_HST_CHANNEL, HstNodeTypes.NODETYPE_HST_CHANNEL);
        channelBlueprint.setProperty(HstNodeTypes.CHANNEL_PROPERTY_CHANNELINFO_CLASS, TestInfoClass.class.getName());
        Node defaultChannelInfo = channelBlueprint.addNode(HstNodeTypes.NODENAME_HST_CHANNELINFO, HstNodeTypes.NODETYPE_HST_CHANNELINFO);
        defaultChannelInfo.setProperty("getme", "noot");
        getSession().save();

        final ChannelManagerImpl manager = createManager();
        manager.setRootPath(testNode.getPath());

        final Channel channel = manager.getBlueprint("test-bp").createChannel("cmit-channel");
        channel.setUrl("http://localhost");
        Map<String, Object> properties = channel.getProperties();
        assertTrue(properties.containsKey("getme"));
        assertEquals("noot", properties.get("getme"));

        asAdmin(new PrivilegedAction<ChannelException>() {
            @Override
            public ChannelException run() {
                try {
                    manager.persist("test-bp", channel);
                } catch (ChannelException ce) {
                    return ce;
                }
                return null;
            }
        });
        TestInfoClass channelInfo = manager.getChannelInfo(channel.getId());
        assertEquals("noot", channelInfo.getGetme());
    }

    private <T extends Exception> void asAdmin(PrivilegedAction<T> action) throws T {
        T ce = HstSubject.doAs(createSubject(), action);
        HstSubject.clearSubject();
        if (ce != null) {
            throw ce;
        }
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
        manager.setHostGroup("dev-local");
        manager.setSites("hst:unittestsites");
        return manager;
    }

}
