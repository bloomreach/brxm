/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.decorating.checked;

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

import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.WorkflowManager;

/**
 * Simple workspace decorator.
 */
public class WorkspaceDecorator extends AbstractDecorator implements HippoWorkspace {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    /** The underlying workspace instance. */
    protected HippoWorkspace workspace;

    protected WorkspaceDecorator(DecoratorFactory factory, SessionDecorator session, HippoWorkspace workspace) {
        super(factory, session);
        this.workspace = workspace;
    }

    @Override
    protected void repair(Session upstreamSession) throws RepositoryException {
        workspace = (HippoWorkspace) upstreamSession.getWorkspace();
    }

    public Session getSession() {
        return session;
    }

    public String getName() {
        return workspace.getName();
    }

    public void copy(String srcAbsPath, String destAbsPath) throws ConstraintViolationException, VersionException,
            AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        check();
        workspace.copy(srcAbsPath, destAbsPath);
    }

    public void copy(String srcWorkspace, String srcAbsPath, String destAbsPath) throws NoSuchWorkspaceException,
            ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException,
            ItemExistsException, LockException, RepositoryException {
        check();
        workspace.copy(srcWorkspace, srcAbsPath, destAbsPath);
    }

    public void clone(String srcWorkspace, String srcAbsPath, String destAbsPath, boolean removeExisting)
            throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException,
            PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        check();
        workspace.clone(srcWorkspace, srcAbsPath, destAbsPath, removeExisting);
    }

    public void move(String srcAbsPath, String destAbsPath) throws ConstraintViolationException, VersionException,
            AccessDeniedException, PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        check();
        workspace.move(srcAbsPath, destAbsPath);
    }

    public void restore(Version[] versions, boolean removeExisting) throws ItemExistsException,
            UnsupportedRepositoryOperationException, VersionException, LockException, InvalidItemStateException,
            RepositoryException {
        check();
        Version[] tmp = new Version[versions.length];
        for (int i = 0; i < versions.length; i++) {
            ((VersionDecorator)versions[i]).check();
            tmp[i] = VersionDecorator.unwrap(versions[i]);
        }
        workspace.restore(tmp, removeExisting);
    }

    public QueryManager getQueryManager() throws RepositoryException {
        check();
        return factory.getQueryManagerDecorator(session, workspace.getQueryManager());
    }

    public DocumentManager getDocumentManager() throws RepositoryException {
        check();
        return factory.getDocumentManagerDecorator(session, workspace.getDocumentManager());
    }

    public WorkflowManager getWorkflowManager() throws RepositoryException {
        check();
        return factory.getWorkflowManagerDecorator(session, workspace.getWorkflowManager());
    }

    public HierarchyResolver getHierarchyResolver() throws RepositoryException {
        check();
        return factory.getHierarchyResolverDecorator(session, workspace.getHierarchyResolver());
    }

    public NamespaceRegistry getNamespaceRegistry() throws RepositoryException {
        check();
        return workspace.getNamespaceRegistry();
    }

    public NodeTypeManager getNodeTypeManager() throws RepositoryException {
        check();
        return workspace.getNodeTypeManager();
    }

    public ObservationManager getObservationManager() throws UnsupportedRepositoryOperationException,
            RepositoryException {
        check();
        return workspace.getObservationManager();
    }

    public String[] getAccessibleWorkspaceNames() throws RepositoryException {
        check();
        return workspace.getAccessibleWorkspaceNames();
    }

    public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehaviour)
            throws PathNotFoundException, ConstraintViolationException, VersionException, LockException,
            RepositoryException {
        check();
        return workspace.getImportContentHandler(parentAbsPath, uuidBehaviour);
    }

    public void importXML(String parentAbsPath, InputStream in, int uuidBehaviour) throws IOException,
            PathNotFoundException, ItemExistsException, ConstraintViolationException, InvalidSerializedDataException,
            LockException, RepositoryException {
        check();
        workspace.importXML(parentAbsPath, in, uuidBehaviour);
    }

    public LockManager getLockManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        check();
        return workspace.getLockManager();
    }

    public VersionManager getVersionManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        check();
        return workspace.getVersionManager();
    }

    public void createWorkspace(String name) throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void createWorkspace(String name, String srcWorkspace) throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void deleteWorkspace(String name) throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
