/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cms7.services.hst.configuration;

import java.util.Map;

import org.hippoecm.hst.configuration.channel.Channel;

/**
 * <p>
 * This is not a {@link @SingletonService} nor a {@link @WhiteboardService} service since multiple HST webapp
 * can register a SharedChannelService implementation. Therefore, usage of this service should be done as follows:
 * <code>
 *     <pre>
 *     List<SharedChannelService> services = HippoServiceRegistry.getServices(SharedChannelService.class)
 *     </pre>
 * </code>
 * and then per service call the channels for a hostname. For example
 * <code>
 *     <pre>
 *         List<SharedChannelService> services = HippoServiceRegistry.getServices(SharedChannelService.class);
 *           for (SharedChannelService service : services) {
 *           Map<String, Channel> localhost = service.getChannels("localhost");
 *         }
 *     </pre>
 * </code>
 * </p>
 */
public interface SharedChannelService {

    /**
     * @param hostname The {@code hostname} that needs to be present in the HST host group for which the channels
     *                 are requested
     * @return The Map of channels for the HST host group that contains {@code hostname}. The keys are the
     * channel ids.
     * @throws IllegalStateException if the HST model could not be loaded
     * @throws IllegalArgumentException if {@code hostname} could not be matched in the HST model.
     */
    Map<String, Channel> getChannels(String hostname);

}
