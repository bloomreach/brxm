/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.mock;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionManager;

import org.xml.sax.ContentHandler;

/**
 * Mock version of {@link Workspace}.
 */
public class MockWorkspace implements Workspace {

    private final MockSession session;

    MockWorkspace(MockSession session) {
        this.session = session;
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public String getName() {
        return "mock";
    }

    @Override
    public MockVersionManager getVersionManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        return new MockVersionManager(session);
    }

    // REMAINING METHODS ARE NOT IMPLEMENTED

    @Override
    public void copy(final String srcAbsPath, final String destAbsPath) throws ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copy(final String srcWorkspace, final String srcAbsPath, final String destAbsPath) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clone(final String srcWorkspace, final String srcAbsPath, final String destAbsPath, final boolean removeExisting) throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void move(final String srcAbsPath, final String destAbsPath) throws ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restore(final Version[] versions, final boolean removeExisting) throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public LockManager getLockManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public QueryManager getQueryManager() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NamespaceRegistry getNamespaceRegistry() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public NodeTypeManager getNodeTypeManager() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ObservationManager getObservationManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getAccessibleWorkspaceNames() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ContentHandler getImportContentHandler(final String parentAbsPath, final int uuidBehavior) throws PathNotFoundException, ConstraintViolationException, VersionException, LockException, AccessDeniedException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void importXML(final String parentAbsPath, final InputStream in, final int uuidBehavior) throws IOException, VersionException, PathNotFoundException, ItemExistsException, ConstraintViolationException, InvalidSerializedDataException, LockException, AccessDeniedException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createWorkspace(final String name) throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createWorkspace(final String name, final String srcWorkspace) throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteWorkspace(final String name) throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException {
        throw new UnsupportedOperationException();
    }
}
