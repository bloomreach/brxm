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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.rmi.client.RemoteRuntimeException;
import org.hippoecm.repository.decorating.remote.RemoteGroup;
import org.hippoecm.repository.decorating.remote.RemoteUser;
import org.onehippo.repository.security.Group;
import org.onehippo.repository.security.User;

public class ClientGroup implements Group {

    private final RemoteGroup remote;

    public ClientGroup(final RemoteGroup remote) {
        this.remote = remote;
    }

    @Override
    public String getId() throws RepositoryException {
        try {
            return remote.getId();
        } catch (RemoteException e) {
            throw new RemoteRuntimeException(e);
        }
    }

    @Override
    public Set<User> getMembers() throws RepositoryException {
        try {
            final Set<User> members = new HashSet<User>();
            for (RemoteUser remoteUser : remote.getMembers()) {
                members.add(new ClientUser(remoteUser));
            }
            return Collections.unmodifiableSet(members);
        } catch (RemoteException e) {
            throw new RemoteRuntimeException(e);
        }
    }

}
