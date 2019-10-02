/*
 *  Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.util;

import java.util.Optional;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.security.SecurityService;
import org.onehippo.repository.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserUtils {

    private static final Logger log = LoggerFactory.getLogger(UserUtils.class);

    /**
     * Look up the real user name pertaining to a user ID
     *
     * @param userId  ID of some user
     * @param session current user's JCR session
     * @return name of the user or nothing, wrapped in an Optional
     * @deprecated since 14.0.0, use {@link #getUserName(String)} instead (session parameter is no longer needed)
     */
    public static Optional<String> getUserName(final String userId, final Session session) {
        return getUserName(userId);
    }

    /**
     * Look up the real user name pertaining to a user ID
     *
     * @param userId  ID of some user
     * @return name of the user or nothing, wrapped in an Optional
     */
    public static Optional<String> getUserName(final String userId) {
        try {
            final SecurityService securityService = HippoServiceRegistry.getService(SecurityService.class);
            if (securityService != null) {
                final User user = securityService.getUser(userId);
                if (user != null) {
                    return getUserName(user);
                }
            }
        } catch (RepositoryException e) {
            log.debug("Unable to determine displayName of user '{}'.", userId, e);
        }
        return Optional.empty();
    }

    public static Optional<String> getUserName(User user) throws RepositoryException {
        return Optional.of(getDisplayName(user));
    }

    public static String getDisplayName(User user) {
        final String firstName = user.getFirstName();
        final String lastName = user.getLastName();

        StringBuilder sb = new StringBuilder();
        if (firstName != null) {
            sb.append(firstName.trim());
            sb.append(" ");
        }
        if (lastName != null) {
            sb.append(lastName.trim());
        }
        final String username = sb.toString().trim();
        return username.isEmpty() ? user.getId() : username;
    }
}
