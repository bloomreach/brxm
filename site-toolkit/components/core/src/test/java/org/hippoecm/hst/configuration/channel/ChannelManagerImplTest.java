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

import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.test.AbstractHstTestCase;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class ChannelManagerImplTest extends AbstractHstTestCase {

    @Test
    public void ChannelsAreReadCorrectly() throws ChannelException, RepositoryException {
        ChannelManagerImpl manager = createManager();

        Map<String,Channel> channels = manager.getChannels();
        assertEquals(1, channels.size());
        assertEquals("testchannel", channels.keySet().iterator().next());

        Channel channel = channels.values().iterator().next();
        assertEquals("testchannel", channel.getId());
        assertEquals(Channel.UNKNOWN_BLUEPRINT, channel.getBlueprintId());
    }

    @Test
    public void ChannelIsCreatedFromBlueprint() throws ChannelException, RepositoryException {
        ChannelManagerImpl manager = createManager();

        List<Blueprint> bluePrints = manager.getBlueprints();
        assertEquals(1, bluePrints.size());

        Channel channel = manager.createChannel(bluePrints.get(0).getId());
        channel.setUrl("http://myhost/mychannel");
        channel.setContentRoot("/content/documents");
        manager.save(channel);

        Node node = getSession().getNode("/hst:hst/hst:hosts/dev-internal");
        assertTrue(node.hasNode("myhost/hst:root/mychannel"));
    }

    private ChannelManagerImpl createManager() throws RepositoryException {
        ChannelManagerImpl manager = new ChannelManagerImpl();
        manager.setCredentials(new SimpleCredentials("admin", "admin".toCharArray()));
        manager.setRepository(getRepository());
        manager.setHostGroup("dev-internal");
        manager.setSites("hst:unittestsites");
        return manager;
    }

}
