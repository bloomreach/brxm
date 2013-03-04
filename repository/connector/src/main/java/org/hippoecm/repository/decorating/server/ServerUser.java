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
import org.onehippo.repository.security.Group;
import org.onehippo.repository.security.SecurityService;

public class ServerUser extends ServerObject implements RemoteUser {

    private final String userId;
    private final SecurityService securityService;

    public ServerUser(final String userId, final SecurityService securityService, final RemoteAdapterFactory factory) throws RemoteException {
        super(factory);
        this.userId = userId;
        this.securityService = securityService;
    }

    @Override
    public String getId() throws RemoteException {
        return userId;
    }

    @Override
    public boolean isSystemUser() throws RepositoryException, RemoteException {
        return securityService.getUser(userId).isSystemUser();
    }

    @Override
    public boolean isActive() throws RepositoryException, RemoteException {
        return securityService.getUser(userId).isActive();
    }

    @Override
    public Set<RemoteGroup> getMemberships() throws RepositoryException, RemoteException {
        final Set<RemoteGroup> memberships = new HashSet<RemoteGroup>();
        for (Group group : securityService.getUser(userId).getMemberships()) {
            memberships.add(new ServerGroup(group.getId(), securityService, getFactory()));
        }
        return Collections.unmodifiableSet(memberships);
    }

}
