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
import java.util.Iterator;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.rmi.client.RemoteRepositoryException;
import org.apache.jackrabbit.rmi.client.RemoteRuntimeException;
import org.hippoecm.repository.decorating.remote.RemoteSecurityService;
import org.hippoecm.repository.decorating.remote.RemoteUser;
import org.hippoecm.repository.decorating.server.ServerUser;
import org.onehippo.repository.security.Group;
import org.onehippo.repository.security.SecurityService;
import org.onehippo.repository.security.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ClientSecurityService implements SecurityService {

    private static final Logger log = LoggerFactory.getLogger(ClientSecurityService.class);

    private final RemoteSecurityService remote;

    public ClientSecurityService(final Session session, final RemoteSecurityService remote, final ClientServicesAdapterFactory clientServicesAdapterFactory) {
        this.remote = remote;
    }

    @Override
    public boolean hasUser(final String userId) throws RepositoryException {
        try {
            return remote.hasUser(userId);
        } catch (RemoteException e) {
            throw new RemoteRuntimeException(e);
        }
    }

    @Override
    public User getUser(final String userId) throws ItemNotFoundException, RepositoryException {
        try {
            return new ClientUser(remote.getUser(userId));
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    @Override
    public Iterable<User> listUsers() throws RepositoryException {
        final Iterator<String> iterator;
        try {
            iterator = remote.listUsers().iterator();
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
        return new Iterable<User>() {
            @Override
            public Iterator<User> iterator() {
                return new Iterator<User>() {

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public User next() {
                        try {
                            return getUser(iterator.next());
                        } catch (RepositoryException e) {
                            log.error("Failed to get next user: " + e.toString());
                        }
                        return null;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    @Override
    public boolean hasGroup(final String groupId) throws RepositoryException {
        try {
            return remote.hasGroup(groupId);
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

    @Override
    public Group getGroup(final String groupId) throws ItemNotFoundException, RepositoryException {
        try {
            return new ClientGroup(remote.getGroup(groupId));
        } catch (RemoteException e) {
            throw new RemoteRepositoryException(e);
        }
    }

}
