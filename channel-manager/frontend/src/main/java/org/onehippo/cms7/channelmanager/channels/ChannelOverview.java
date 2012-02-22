package org.onehippo.cms7.channelmanager.channels;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
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

    public ChannelOverview(IPluginConfig channelListConfig, ExtStoreFuture channelStoreFuture, boolean blueprintsAvailable) {
        this.store = (ChannelStore) channelStoreFuture.getStore();

        canModifyChannels = store.canModifyChannels();
        log.info("Current user is allowed to modify channels: {}", canModifyChannels);

        this.blueprintsAvailable = blueprintsAvailable;
        log.info("Blueprints for new channels are available: {}", blueprintsAvailable);

        final ChannelIconPanel channelIconCard = new ChannelIconPanel(channelListConfig, channelStoreFuture);
        channelIconCard.setRegion(BorderLayout.Region.CENTER);
        add(channelIconCard);

        final ChannelGridPanel channelListCard = new ChannelGridPanel(channelListConfig, channelStoreFuture);
        channelListCard.setRegion(BorderLayout.Region.CENTER);
        add(channelListCard);
    }

}
