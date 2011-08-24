package org.onehippo.cms7.channelmanager.channels;

import java.util.ResourceBundle;

import org.apache.wicket.Session;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ChannelUtil {

    private static Logger log = LoggerFactory.getLogger(ChannelUtil.class);

    private ChannelUtil() {
        // prevent instantiation
    }

    static ChannelManager getChannelManager() {
        ChannelManager channelManager = HstServices.getComponentManager().getComponent(ChannelManager.class.getName());
        if (channelManager != null) {
            return channelManager;
        } else {
            throw new IllegalStateException("Unable to get the Channel Manager: HST component " + ChannelManager.class.getName() + " cannot be loaded");
        }
    }

    static <T extends ChannelInfo> T getChannelInfo(Channel channel) {
        try {
            return getChannelManager().getChannelInfo(channel);
        } catch (ChannelException e) {
            log.debug("No ChannelInfo found for channel '" + channel.getId() + "', returning null", e);
        }
        return null;
    }

    static Class<? extends ChannelInfo> getChannelInfoClass(Channel channel) {
        try {
            return getChannelManager().getChannelInfoClass(channel);
        } catch (ChannelException e) {
            log.debug("No ChannelInfo class found for channel '" + channel.getId() + "', returning null", e);
        }
        return null;
    }

    static ResourceBundle getResourceBundle(Channel channel) {
        return ChannelUtil.getChannelManager().getResourceBundle(channel, Session.get().getLocale());
    }

}
