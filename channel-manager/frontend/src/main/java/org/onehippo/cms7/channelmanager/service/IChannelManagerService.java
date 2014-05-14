/*
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.service;

import org.apache.wicket.util.io.IClusterable;

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
     * @param cmsPreviewPrefix the prefix for viewing the preview
     */
    void viewChannel(String channelId, String pathInfo, String contextPath, String cmsPreviewPrefix);

}
