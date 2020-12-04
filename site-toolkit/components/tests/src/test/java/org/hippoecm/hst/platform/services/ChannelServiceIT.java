/*
 *  Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;

import org.hippoecm.hst.core.beans.AbstractBeanTestCase;
import org.hippoecm.hst.platform.api.PlatformServices;
import org.hippoecm.hst.site.HstServices;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.hst.Channel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.CHANNEL_VIEWER_PRIVILEGE_NAME;
import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_VALUE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PRIVILEGES;

public class ChannelServiceIT extends AbstractBeanTestCase {

    private PlatformServices platformServices;
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        platformServices = HippoServiceRegistry.getService(PlatformServices.class);
    }

    @Test
    public void get_channels_without_user_session_returns_all() throws Exception {

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

        final Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName());
        final SimpleCredentials creds = getComponent(Credentials.class.getName() + ".default");
        Session liveuserSession = repository.login(creds);

        // assert liveuser can read the content of the channels, thus /unittestcontent and /extracontent
        // however since the liveuser does not have privilege 'hippo:channel-viewer' on the channel configurations,
        // all channels should be filtered

        assertThat(liveuserSession.nodeExists("/unittestcontent")).isTrue();
        assertThat(liveuserSession.nodeExists("/extracontent")).isTrue();

        final List<Channel> liveChannels = platformServices.getChannelService().getLiveChannels(liveuserSession, "dev-localhost");

        assertThat(liveChannels)
                .as("'liveuser' does not have privilege 'hippo:channel-viewer' hence the channels should " +
                        "not be returned")
                .hasSize(0);

        liveuserSession.logout();
    }


    @Test
    public void get_channels_with_author_session_are_filtered_on_privilege() throws Exception {

        final Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        final Session author = repository.login(new SimpleCredentials("author", "author".toCharArray()));

        // assert author can read the content of the channels, thus /unittestcontent and /extracontent
        // however since the liveuser does not have privilege 'hippo:channel-viewer' on the channel configurations,
        // all channels should be filtered

        assertThat(author.nodeExists("/unittestcontent")).isTrue();
        assertThat(author.nodeExists("/extracontent")).isTrue();

        final List<Channel> authorChannels = platformServices.getChannelService().getLiveChannels(author, "dev-localhost");

        assertThat(authorChannels.stream().map(channel -> channel.getId()).collect(Collectors.toSet()))
                .as("Author is expected to have privilege '%s' on all channel configurations and read access " +
                        "on the content hence should 'see' them all", CHANNEL_VIEWER_PRIVILEGE_NAME)
                .containsExactlyInAnyOrder("unittestproject", "extraproject", "intranettestproject", "unittestsubproject");

        author.logout();

        // remove the privilege 'hippo:channel-viewer' from 'channel-viewer' role : After this, the channels
        // should be filtered for the cms users while the author can still read the content

        final Session admin = createSession();
        final Node viewerRoleNode = admin.getNode("/hippo:configuration/hippo:roles/channel-viewer");
        Value[] originalValues = null;
        try {
            final Property privileges = viewerRoleNode.getProperty(HIPPO_PRIVILEGES);
            originalValues = privileges.getValues();
            privileges.remove();
            admin.save();

            final Session newAuthor = repository.login(new SimpleCredentials("author", "author".toCharArray()));
            assertThat(newAuthor.nodeExists("/unittestcontent")).isTrue();
            assertThat(newAuthor.nodeExists("/extracontent")).isTrue();

            final List<Channel> newAuthorChannels = platformServices.getChannelService().getLiveChannels(newAuthor, "dev-localhost");

            assertThat(newAuthorChannels)
                    .as("Authors do not have privilege '%s' any more hence channels should be filtered", CHANNEL_VIEWER_PRIVILEGE_NAME)
                    .hasSize(0);
            newAuthor.logout();

        } finally {
            viewerRoleNode.setProperty(HIPPO_PRIVILEGES, originalValues);
            admin.save();
            admin.logout();
        }
    }

    @Test
    public void get_channels_with_author_session_are_filtered_on_content_read_access() throws Exception {

        final Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");

        final Session admin = createSession();

        final Node extraContentFacetRule =
                admin.getNode("/hippo:configuration/hippo:domains/content/extracontent-domain/extracontent-and-descendants");

        final String originalValue = extraContentFacetRule.getProperty(HIPPOSYS_VALUE).getString();

        try {

            // after the change below, 'cms-users' should not have read-access to '/extracontent' any more
            extraContentFacetRule.setProperty(HIPPOSYS_VALUE, "/non-existing");
            admin.save();

            final Session author = repository.login(new SimpleCredentials("author", "author".toCharArray()));
            assertThat(author.nodeExists("/unittestcontent"))
                    .as("/unittestcontent should still be readable")
                    .isTrue();
            assertThat(author.nodeExists("/extracontent"))
                    .as("/extracontent should NOT be readable any more")
                    .isFalse();

            final List<Channel> authorChannels = platformServices.getChannelService().getLiveChannels(author, "dev-localhost");

            assertThat(authorChannels.stream().map(channel -> channel.getId()).collect(Collectors.toSet()))
                    .as("Author is expected to have privilege '%s' on all channel configurations but only " +
                            "read access to the content for some channels, hence should not see them all", CHANNEL_VIEWER_PRIVILEGE_NAME)
                    .containsExactlyInAnyOrder("unittestproject", "intranettestproject", "unittestsubproject");


            author.logout();
        } finally {
            extraContentFacetRule.setProperty(HIPPOSYS_VALUE, originalValue);
            admin.save();
            admin.logout();
        }
    }
}
