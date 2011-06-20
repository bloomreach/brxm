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

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.test.AbstractHstTestCase;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class ChannelManagerImplTest extends AbstractHstTestCase {

    @Test
    public void ChannelsAreReadCorrectly() throws ChannelException, RepositoryException {
        ChannelManagerImpl manager = createManager();

        Map<String,Channel> channels = manager.getChannels();
        assertEquals(1, channels.size());
        assertEquals("testchannel", channels.keySet().iterator().next());

        Channel channel = channels.values().iterator().next();
        assertEquals("testchannel", channel.getId());
        assertEquals(Channel.UNKNOWN_BLUEPRINT, channel.getBluePrintId());
    }

    private ChannelManagerImpl createManager() throws RepositoryException {
        Session session = getRepository().login(new SimpleCredentials("admin", "admin".toCharArray()));
        ChannelManagerImpl manager = new ChannelManagerImpl(session.getNode("/hst:hst"));
        return manager;
    }

}
