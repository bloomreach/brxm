package org.onehippo.cms7.channelmanager.channels;

import java.util.ResourceBundle;

import org.apache.wicket.Session;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ChannelUtil {

    private static Logger log = LoggerFactory.getLogger(ChannelUtil.class);

    private ChannelUtil() {
        // prevent instantiation
    }

    /**
     * @return the Channel Manager, or <code><null/code> if the channel manager cannot be loaded or is unavailable
     * (e.g. when the site is down).
     */
    static ChannelManager getChannelManager() {
        ComponentManager componentManager = HstServices.getComponentManager();
        if (componentManager == null) {
            return null;
        }

        return HstServices.getComponentManager().getComponent(ChannelManager.class.getName());
    }

    /**
     * @param channel the channel to get the ChannelInfo class for
     * @return the ChannelInfo class for the given channel, or <code>null</code> if the channel does not have a
     * custom ChannelInfo class or the channel manager could not be loaded (e.g. because the site is down).
     */
    static Class<? extends ChannelInfo> getChannelInfoClass(Channel channel) {
        ChannelManager channelManager = getChannelManager();
        try {
            return channelManager == null ? null : getChannelManager().getChannelInfoClass(channel);
        } catch (ChannelException e) {
            log.debug("No ChannelInfo class found for channel '" + channel.getId() + "', returning null", e);
        }
        return null;
    }

    /**
     * @param channel the channel to return the resource bundle for
     * @return the resource bundle for the given channel, or <code>null</code> if the channel does not have a resource
     * bundle or the channel manager could not be loaded (e.g. because the site is down).
     */
    static ResourceBundle getResourceBundle(Channel channel) {
        ChannelManager channelManager = getChannelManager();
        return channelManager == null ? null : channelManager.getResourceBundle(channel, Session.get().getLocale());
    }

}
