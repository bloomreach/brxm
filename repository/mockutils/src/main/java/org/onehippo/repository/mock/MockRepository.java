/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.mock;

import java.util.UUID;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

public class MockRepository implements Repository {

    private final String clusterId = UUID.randomUUID().toString();

    @Override
    public String[] getDescriptorKeys() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isStandardDescriptor(final String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSingleValueDescriptor(final String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value getDescriptorValue(final String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Value[] getDescriptorValues(final String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDescriptor(final String key) {
        if ("jackrabbit.cluster.id".equals(key)) {
            return clusterId;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public Session login(final Credentials credentials, final String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Session login(final Credentials credentials) throws LoginException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Session login(final String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Session login() throws LoginException, RepositoryException {
        throw new UnsupportedOperationException();
    }
}
