/*
 * Copyright 2011-2017 Hippo B.V. (http://www.onehippo.com)
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
     * Opens the channel with the given id. The path can point to any location in a channel,
     * e.g. the home page or a specific detail page.
     *
     * @param channelId   The identifier of the channel
     * @param channelPath The location inside the channel
     */
    void viewChannel(String channelId, String channelPath);

    /**
     * Opens the channel with the given id and then selects the given branch of that channel. The path can point to
     * any location in a channel, e.g. the home page or a specific detail page.
     *
     * @param channelId   The identifier of the channel
     * @param channelPath The location inside the channel
     * @param branchId    The branchId of the channel to select
     */
    void viewChannel(String channelId, String channelPath, String branchId);

}
