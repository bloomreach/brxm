/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.services.channel;

import java.util.function.BiPredicate;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.Privilege;

import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.platform.services.channel.ChannelManagerPrivileges.CHANNEL_VIEWER_PRIVILEGE_NAME;

public class PrivilegeBasedChannelFilter implements BiPredicate<Session, Channel> {

    private static final Logger log = LoggerFactory.getLogger(ContentReadChannelFilter.class);

    @Override
    public boolean test(final Session userSession, final Channel channel) {

        try {
            final Privilege privilege = userSession.getAccessControlManager().privilegeFromName(CHANNEL_VIEWER_PRIVILEGE_NAME);
            return userSession.getAccessControlManager().hasPrivileges(channel.getHstConfigPath(), new Privilege[]{privilege});
        } catch (RepositoryException e) {
            log.warn("Exception while checking privilege '{}' for channel '{}'. Skip that channel:",
                    CHANNEL_VIEWER_PRIVILEGE_NAME, channel.getHstConfigPath(), e);
            return false;
        }

    }
}
