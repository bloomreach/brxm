/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.repository.mock;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

/**
 * Mock version of a {@link Version}.
 * Current limitation: all Version specific methods throw an {@link UnsupportedOperationException}
 */
public class MockVersion extends MockNode implements Version {
    public MockVersion(final String name) {
        super(name);
    }

    public MockVersion(final String name, final String primaryTypeName) {
        super(name, primaryTypeName);
    }

    @Override
    public VersionHistory getContainingHistory() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Calendar getCreated() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Version getLinearSuccessor() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Version[] getSuccessors() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Version getLinearPredecessor() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Version[] getPredecessors() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Node getFrozenNode() throws RepositoryException {
        throw new UnsupportedOperationException();
    }
}
