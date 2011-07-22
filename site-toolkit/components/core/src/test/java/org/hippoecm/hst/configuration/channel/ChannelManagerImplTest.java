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
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

public class ChannelManagerImplTest extends AbstractHstTestCase {

    @Override
    @After
    public void tearDown() throws Exception {
        Node internalHostGroup = getSession().getNode("/hst:hst/hst:hosts/dev-internal");
        if (internalHostGroup.hasNode("myhost")) {
            internalHostGroup.getNode("myhost").remove();
        }
        for (NodeIterator ni = getSession().getNode("/hst:hst/hst:unittestsites").getNodes("channel-*"); ni.hasNext(); ) {
            ni.nextNode().remove();
        }
        for (NodeIterator ni = getSession().getNode("/hst:hst/hst:configurations").getNodes("channel-*"); ni.hasNext(); ) {
            ni.nextNode().remove();
        }
        internalHostGroup.getSession().save();
        super.tearDown();
    }

    @Test
    public void ChannelsAreReadCorrectly() throws ChannelException, RepositoryException {
        ChannelManagerImpl manager = createManager();

        Map<String, Channel> channels = manager.getChannels();
        assertEquals(1, channels.size());
        assertEquals("testchannel", channels.keySet().iterator().next());

        Channel channel = channels.values().iterator().next();
        assertEquals("testchannel", channel.getId());
        assertEquals("Test Channel", channel.getName());
        assertEquals(Channel.UNKNOWN_BLUEPRINT, channel.getBlueprintId());
    }

    @Test
    public void ChannelIsCreatedFromBlueprint() throws ChannelException, RepositoryException {
        final ChannelManagerImpl manager = createManager();
        int numberOfChannels = manager.getChannels().size();

        final List<Blueprint> bluePrints = manager.getBlueprints();
        assertEquals(1, bluePrints.size());

        final Channel channel = manager.createChannel(bluePrints.get(0).getId());
        channel.setUrl("http://myhost/mychannel");
        channel.setContentRoot("/content/documents");
        asAdmin(new PrivilegedAction<ChannelException>() {
            @Override
            public ChannelException run() {
                try {
                    manager.save(channel);
                } catch (ChannelException ce) {
                    return ce;
                }
                return null;
            }
        });
        Node node = getSession().getNode("/hst:hst/hst:hosts/dev-internal");

        assertTrue(node.hasNode("myhost/hst:root/mychannel"));

        Map<String, Channel> channels = manager.getChannels();
        assertEquals(numberOfChannels + 1, channels.size());
        assertTrue(channels.containsKey(channel.getId()));
        assertNull(channel.getName());
    }

    public static interface TestInfoClass {
        @Parameter(name="getme", defaultValue = "aap")
        String getGetme();
    }

    @Test
    public void blueprintDefaultValuesAreCopied() throws RepositoryException, ChannelException {
        Node testNode = getSession().getRootNode().addNode("test", "hst:hst");
        testNode.addNode("hst:hosts").addNode("dev-internal", "hst:virtualhostgroup");

        Node bpFolder = testNode.addNode(HstNodeTypes.NODENAME_HST_BLUEPRINTS, "hst:blueprints");
        Node bp = bpFolder.addNode("test-bp", "hst:blueprint");
        bp.setProperty("hst:channelinfoclass", TestInfoClass.class.getName());
        Node defaultChannelInfo = bp.addNode("hst:defaultchannelinfo", "hst:channelinfo");
        defaultChannelInfo.setProperty("getme", "noot");
        getSession().save();

        final ChannelManagerImpl manager = createManager();
        manager.setRootPath(testNode.getPath());

        final Channel channel = manager.createChannel("test-bp");
        channel.setUrl("http://localhost/test");
        Map<String, Object> properties = channel.getProperties();
        assertTrue(properties.containsKey("getme"));
        assertEquals("noot", properties.get("getme"));

        asAdmin(new PrivilegedAction<ChannelException>() {
            @Override
            public ChannelException run() {
                try {
                    manager.save(channel);
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
        manager.setHostGroup("dev-internal");
        manager.setSites("hst:unittestsites");
        return manager;
    }

}
