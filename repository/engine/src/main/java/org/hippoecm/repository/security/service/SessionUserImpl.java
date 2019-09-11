/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.security.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.security.group.GroupManager;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.onehippo.repository.security.SessionUser;
import org.onehippo.repository.security.User;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_USERROLES;

/**
 * Implementation of a {@link SessionUser} representing a logged in user, with its <em>effective</em>
 * {@link SessionUser#getUserRoles() user roles} resolved at login time, as well as its
 * {@link User#getMemberships() group memberships} (already needed during the login process).
 */
public class SessionUserImpl extends UserImpl implements SessionUser {

    public SessionUserImpl(final Node userNode, final GroupManager groupManager,
                           final Function<Set<String>, Set<String>> userRolesResolver) throws RepositoryException {
        super(userNode, groupManager, userRolesResolver);
    }


    protected void collectUserRolesAndGroups(final Node userNode, final GroupManager groupManager,
                                             final HashSet<String> userRoles, final HashSet<String> groups)
            throws RepositoryException {
        userRoles.addAll(collectUserRoles(userNode));
        for (Node groupNode : new NodeIterable(groupManager.getMemberships(getId()))) {
            userRoles.addAll(JcrUtils.getStringListProperty(groupNode, HIPPO_USERROLES, Collections.emptyList()));
            groups.add(NodeNameCodec.decode(groupNode.getName()));
        }
    }

}
