/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.services;

import java.util.List;
import java.util.stream.Collectors;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.core.beans.AbstractBeanTestCase;
import org.hippoecm.hst.platform.api.PlatformServices;
import org.hippoecm.hst.site.HstServices;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.hst.Channel;

import static org.assertj.core.api.Assertions.assertThat;

public class ChannelServiceTest extends AbstractBeanTestCase {

    @Test
    public void get_channels_without_user_session_returns_all() throws Exception {

        final PlatformServices platformServices = HippoServiceRegistry.getService(PlatformServices.class);

        final List<Channel> liveChannels = platformServices.getChannelService().getLiveChannels("dev-localhost");

        assertThat(liveChannels)
                .as("Expected 4 live channels (combined from /hst:hst and /hst:site2")
                .hasSize(4);

        assertThat(liveChannels.stream().map(channel -> channel.getId()).collect(Collectors.toSet()))
                .as("Unexpected channel ids")
                .containsExactlyInAnyOrder("unittestproject", "extraproject", "intranettestproject", "unittestsubproject");

    }

    @Test
    public void get_channels_with_liveuser_session_are_filtered_on_privilege() throws Exception {

        final PlatformServices platformServices = HippoServiceRegistry.getService(PlatformServices.class);

        final Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName());
        final SimpleCredentials creds = getComponent(Credentials.class.getName() + ".default");
        Session liveuserSession = repository.login(creds);

        // assert liveuser can read the content of the channels, thus /unittestcontent and /extracontent
        // however since the liveuser does not have privilege 'hippo:channel-webviewer' on the channel configurations,
        // all channels should be filtered

        assertThat(liveuserSession.nodeExists("/unittestcontent"));
        assertThat(liveuserSession.nodeExists("/extracontent"));

        final List<Channel> liveChannels = platformServices.getChannelService().getLiveChannels(liveuserSession, "dev-localhost");

        assertThat(liveChannels)
                .as("'liveuser' does not have privilege 'hippo:channel-webviewer' hence the channels should " +
                        "not be returned")
                .hasSize(0);

    }
}
