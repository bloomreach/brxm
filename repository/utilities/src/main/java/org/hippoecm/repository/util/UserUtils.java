/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 */
package org.hippoecm.repository.util;

import java.util.Optional;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoWorkspace;
import org.onehippo.cms7.services.HippoServiceRegistry;
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
     */
    public static Optional<String> getUserName(final String userId, final Session session) {
        try {
            final HippoWorkspace workspace = (HippoWorkspace) session.getWorkspace();
            final User user = workspace.getSecurityService().getUser(userId);
            return getUserName(user);
        } catch (RepositoryException e) {
            log.debug("Unable to determine displayName of user '{}'.", userId, e);
        }
        return Optional.empty();
    }

    public static Optional<String> getUserName(User user) throws RepositoryException {
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
        return Optional.of(username.isEmpty() ? user.getId() : username);
    }


}
