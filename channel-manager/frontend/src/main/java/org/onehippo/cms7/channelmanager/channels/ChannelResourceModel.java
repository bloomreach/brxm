package org.onehippo.cms7.channelmanager.channels;

import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.hst.configuration.channel.Channel;

class ChannelResourceModel extends LoadableDetachableModel<String> {

    private static final long serialVersionUID = 1L;

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
        if (StringUtils.isNotEmpty(key)) {
            Properties bundleProperties = channelStore.getChannelResourceValues(channel);
            if (bundleProperties != null && bundleProperties.containsKey(key)) {
                return bundleProperties.getProperty(key);
            }
        }

        return null;
    }

    static String getChannelResourceValue(Channel channel, String key) {
        return null;
    }

}