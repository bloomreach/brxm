/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.cmsrest.filter;


import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.google.common.base.Predicate;

import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.container.RequestContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * only channels that the authenticated cms user is allowed to read the content from will pass the {@link Predicate}
 */
public class ContentBasedChannelFilter implements Predicate<Channel> {

    private static final Logger log = LoggerFactory.getLogger(ContentBasedChannelFilter.class);

    @Override
    public boolean apply(final Channel channel) {
        try {
            Session cmsSession = RequestContextProvider.get().getSession();
            if (cmsSession.nodeExists(channel.getContentRoot())) {
                log.debug("Predicate passed for channel '{}' because user has read access on '{}'",
                        new String[]{channel.toString(), cmsSession.getUserID(), channel.getContentRoot()});
                return true;
            }
            log.info("Skipping channel '{}' for user '{}' because she has no read access on '{}'",
                    new String[]{channel.toString(), cmsSession.getUserID(), channel.getContentRoot()});
            return false;
        } catch (RepositoryException e) {
            log.warn("Exception while trying to check channel {}. Skip that channel:", channel.getContentRoot(), e);
            return false;
        }
    }
}
