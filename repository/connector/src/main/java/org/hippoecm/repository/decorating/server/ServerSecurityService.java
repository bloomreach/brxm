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

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.rmi.server.RemoteAdapterFactory;
import org.apache.jackrabbit.rmi.server.ServerObject;
import org.hippoecm.repository.decorating.remote.RemoteGroup;
import org.hippoecm.repository.decorating.remote.RemoteSecurityService;
import org.hippoecm.repository.decorating.remote.RemoteUser;
import org.onehippo.repository.security.SecurityService;

public class ServerSecurityService extends ServerObject implements RemoteSecurityService {

    private final SecurityService securityService;

    protected ServerSecurityService(final SecurityService securityService, final RemoteAdapterFactory factory) throws RemoteException {
        super(factory);
        this.securityService = securityService;
    }

    @Override
    public boolean hasUser(final String userId) throws RepositoryException, RemoteException {
        return securityService.hasUser(userId);
    }

    @Override
    public boolean hasGroup(final String groupId) throws RepositoryException, RemoteException {
        return securityService.hasGroup(groupId);
    }

    @Override
    public RemoteUser getUser(final String userId) throws RepositoryException, RemoteException {
        return new ServerUser(userId, securityService, getFactory());
    }

    @Override
    public RemoteGroup getGroup(final String groupId) throws RepositoryException, RemoteException {
        return new ServerGroup(groupId, securityService, getFactory());
    }

}
