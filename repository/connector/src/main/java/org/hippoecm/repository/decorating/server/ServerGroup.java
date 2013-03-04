/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.decorating.server;

import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.rmi.server.RemoteAdapterFactory;
import org.apache.jackrabbit.rmi.server.ServerObject;
import org.hippoecm.repository.decorating.remote.RemoteGroup;
import org.hippoecm.repository.decorating.remote.RemoteUser;
import org.onehippo.repository.security.SecurityService;
import org.onehippo.repository.security.User;

public class ServerGroup extends ServerObject implements RemoteGroup {

    private final String groupId;
    private final SecurityService securityService;

    public ServerGroup(final String groupId, final SecurityService securityService, final RemoteAdapterFactory factory) throws RemoteException {
        super(factory);
        this.groupId = groupId;
        this.securityService = securityService;
    }

    @Override
    public String getId() throws RemoteException {
        return groupId;
    }

    @Override
    public Set<RemoteUser> getMembers() throws RepositoryException, RemoteException {
        final Set<RemoteUser> members = new HashSet<RemoteUser>();
        for (User user : securityService.getGroup(groupId).getMembers()) {
            members.add(new ServerUser(user.getId(), securityService, getFactory()));
        }
        return Collections.unmodifiableSet(members);
    }

}
