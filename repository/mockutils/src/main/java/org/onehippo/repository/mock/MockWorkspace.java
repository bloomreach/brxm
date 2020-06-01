/*
 * Copyright 2014-2017 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Optional;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.lock.LockManager;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.observation.ObservationManager;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;

import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;
import org.onehippo.repository.security.SecurityService;
import org.xml.sax.ContentHandler;

/**
 * Mock version of {@link Workspace}.
 */
public class MockWorkspace implements HippoWorkspace {

    private final MockSession session;
    private final QueryManager queryManager;
    private WorkflowManager workflowManager;

    MockWorkspace(MockSession session, QueryManager queryManager) {
        this.session = session;
        this.queryManager = queryManager;
        workflowManager = new MockWorkflowManager(session);
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
    public MockVersionManager getVersionManager() throws RepositoryException {
        return new MockVersionManager(session);
    }

    @Override
    public QueryManager getQueryManager() throws RepositoryException {
        return Optional.ofNullable(queryManager).orElseThrow(UnsupportedOperationException::new);
    }

    @Override
    public WorkflowManager getWorkflowManager() throws RepositoryException {
        return workflowManager;
    }

    /**
     * Sets a custom workflow manager (e.g. a custom mock)
     * @param workflowManager the workflow manager to use from now on
     */
    public void setWorkflowManager(final WorkflowManager workflowManager) {
        this.workflowManager = workflowManager;
    }

    // REMAINING METHODS ARE NOT IMPLEMENTED

    @Override
    public void copy(final String srcAbsPath, final String destAbsPath) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copy(final String srcWorkspace, final String srcAbsPath, final String destAbsPath) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clone(final String srcWorkspace, final String srcAbsPath, final String destAbsPath, final boolean removeExisting) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void move(final String srcAbsPath, final String destAbsPath) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void restore(final Version[] versions, final boolean removeExisting) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public LockManager getLockManager() throws RepositoryException {
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
    public ObservationManager getObservationManager() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getAccessibleWorkspaceNames() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ContentHandler getImportContentHandler(final String parentAbsPath, final int uuidBehavior) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void importXML(final String parentAbsPath, final InputStream in, final int uuidBehavior) throws IOException, RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createWorkspace(final String name) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createWorkspace(final String name, final String srcWorkspace) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteWorkspace(final String name) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public HierarchyResolver getHierarchyResolver() throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SecurityService getSecurityService() throws RepositoryException {
        throw new UnsupportedOperationException();
    }
}
