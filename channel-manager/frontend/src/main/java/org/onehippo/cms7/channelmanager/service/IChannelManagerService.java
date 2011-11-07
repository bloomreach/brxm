package org.onehippo.cms7.channelmanager.service;

import org.apache.wicket.IClusterable;

/**
 * Provides channel manager functionality to other CMS plugins.
 */
public interface IChannelManagerService extends IClusterable {

    /**
     * Opens the channel with the given id. The mount path can point to any location in a channel,
     * e.g. the home page or a specific detail page.
     *
     * @param channelId The Identifier of the channel
     * @param pathInfo the location inside the channel (without a context path but including the path of the mount + the path of the document)
     * @param contextPath the contextPath of the channel to load
     * @param templateComposerContextPath the context path for the template composer
     */
    public void viewChannel(String channelId, String pathInfo, String contextPath, String templateComposerContextPath);

}
