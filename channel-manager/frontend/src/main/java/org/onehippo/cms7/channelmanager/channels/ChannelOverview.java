/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.channelmanager.channels;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.cms7.channelmanager.ChannelManagerHeaderItem;
import org.onehippo.cms7.channelmanager.ExtStoreFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.layout.BorderLayout;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.ExtProperty;

@ExtClass("Hippo.ChannelManager.ChannelOverview")
public class ChannelOverview extends ExtPanel {

    public static final String CHANNEL_OVERVIEW_PANEL_JS = "ChannelOverview.js";

    private static final Logger log = LoggerFactory.getLogger(ChannelOverview.class);

    private ChannelStore store;

    @ExtProperty
    private boolean canModifyChannels;

    @ExtProperty
    @SuppressWarnings("unused")
    private boolean blueprintsAvailable;

    public ChannelOverview(final IPluginConfig channelListConfig,
                           final String composerRestMountPath,
                           final ExtStoreFuture channelStoreFuture, boolean blueprintsAvailable) {
        this.store = (ChannelStore) channelStoreFuture.getStore();

        canModifyChannels = store.canModifyChannels();
        log.info("Current user is allowed to modify channels: {}", canModifyChannels);

        this.blueprintsAvailable = blueprintsAvailable;
        log.info("Blueprints for new channels are available: {}", blueprintsAvailable);

        final ChannelIconPanel channelIconCard = new ChannelIconPanel(channelListConfig, channelStoreFuture);
        channelIconCard.setRegion(BorderLayout.Region.CENTER);
        add(channelIconCard);

        final ChannelGridPanel channelListCard = new ChannelGridPanel(channelListConfig, composerRestMountPath, channelStoreFuture);
        channelListCard.setRegion(BorderLayout.Region.CENTER);
        add(channelListCard);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        response.render(ChannelManagerHeaderItem.get());
    }

}
