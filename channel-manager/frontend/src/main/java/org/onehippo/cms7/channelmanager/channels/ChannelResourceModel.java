package org.onehippo.cms7.channelmanager.channels;

import java.util.ResourceBundle;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.hst.configuration.channel.Channel;

class ChannelResourceModel extends LoadableDetachableModel<String> {

    private final Channel channel;
    private final String key;

    public ChannelResourceModel(Channel channel, String key) {
        this.channel = channel;
        this.key = key;
    }

    @Override
    protected String load() {
        return getChannelResourceValue(channel, key);
    }

    static String getChannelResourceValue(Channel channel, String key) {
        if (StringUtils.isNotEmpty(key)) {
            ResourceBundle bundle = ChannelUtil.getResourceBundle(channel);
            if (bundle != null) {
                return bundle.getString(key);
            }
        }
        return null;
    }

}