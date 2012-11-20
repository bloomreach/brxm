package org.onehippo.cms7.channelmanager.channels;

import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ChannelResourceModel extends LoadableDetachableModel<String> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ChannelResourceModel.class);

    private final Channel channel;
    private final String key;
    private final ChannelStore channelStore;

    ChannelResourceModel(String key, Channel channel, ChannelStore channelStore) {
        this.channel = channel;
        this.key = key;
        this.channelStore = channelStore;
    }

    @Override
    protected String load() {
        try {
            if (StringUtils.isNotEmpty(key)) {
                Properties bundleProperties = channelStore.getChannelResourceValues(channel);
                if (bundleProperties != null && bundleProperties.containsKey(key)) {
                    return bundleProperties.getProperty(key);
                }
            }
        } catch (ChannelException ce) {
            if (log.isDebugEnabled()) {
                log.warn("Could not resource values for channel with id '" + channel.getId() + "'", ce);
            } else {
                log.warn("Could not resource values for channel with id '{}' - {}", channel.getId(), ce.toString());
            }
        }

        return null;
    }

}