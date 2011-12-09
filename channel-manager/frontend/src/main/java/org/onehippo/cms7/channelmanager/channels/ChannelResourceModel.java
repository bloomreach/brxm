package org.onehippo.cms7.channelmanager.channels;

import java.util.ResourceBundle;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.LoadableDetachableModel;
import org.hippoecm.hst.configuration.channel.Channel;

class ChannelResourceModel extends LoadableDetachableModel<String> {

    private static final long serialVersionUID = 1L;

    private final Channel channel;
    private final String key;

    ChannelResourceModel(Channel channel, String key) {
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
            if (bundle != null && bundle.containsKey(key)) {
                return bundle.getString(key);
            }
        }
        return null;
    }

}