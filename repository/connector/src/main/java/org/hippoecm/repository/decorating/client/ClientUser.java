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
package org.hippoecm.repository.decorating.client;

import java.rmi.RemoteException;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.rmi.client.RemoteRepositoryException;
import org.hippoecm.repository.decorating.remote.RemoteUser;
import org.onehippo.repository.security.User;

public class ClientUser implements User {

    private final RemoteUser remote;

    public ClientUser(final RemoteUser remote) {
        this.remote = remote;
    }

    @Override
    public String getId() throws RepositoryException {
        try {
            return remote.getId();
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    @Override
    public boolean isSystemUser() throws RepositoryException {
        try {
            return remote.isSystemUser();
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    @Override
    public boolean isActive() throws RepositoryException {
        try {
            return remote.isActive();
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    @Override
    public Set<String> getMemberships() throws RepositoryException {
        try {
            return remote.getMemberships();
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }
}
