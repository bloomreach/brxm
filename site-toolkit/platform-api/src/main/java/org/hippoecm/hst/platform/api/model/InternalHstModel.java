/*
 *  Copyright 2018-2022 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.api.model;

import java.util.function.BiPredicate;

import javax.jcr.Session;

import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.platform.model.HstModel;
import org.onehippo.cms7.services.hst.Channel;

public interface InternalHstModel extends HstModel {

    BiPredicate<Session, Channel> getChannelFilter() ;

    ChannelManager getChannelManager();

    void invalidate();

    /**
     * <p>
     *     Returns the same Object instance as long as the jcr nodes for the HST SiteMap belonging to the mount having
     *     configuration at {@code configurationPath} is backed by the same JCR Nodes. Note that also inherited HST
     *     configurations (like hst:default) must be taken into account
     * </p>
     * @param configurationPath path to an hst:configuration node
     * @return an object which can be used for caching purposes
     */

    Object getSiteMapConfigurationIdentity(final String configurationPath);
}
