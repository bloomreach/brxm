/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.updater;

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

import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoWorkspace;
import org.xml.sax.ContentHandler;

public class UpdaterWorkspace implements Workspace {

    Workspace upstream;
    UpdaterSession session;

    UpdaterWorkspace(UpdaterSession session, Workspace upstream) {
        this.session = session;
        this.upstream = upstream;
    }

    HierarchyResolver getHierarchyResolver() throws RepositoryException {
        return ((HippoWorkspace) upstream).getHierarchyResolver();
    }

    public Session getSession() {
        return session;
    }

    public String getName() {
        return upstream.getName();
    }

    public void copy(String srcAbsPath, String destAbsPath)
            throws ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException,
                   ItemExistsException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public void copy(String srcWorkspace, String srcAbsPath, String destAbsPath)
            throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException,
                   PathNotFoundException, ItemExistsException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public void clone(String srcWorkspace, String srcAbsPath, String destAbsPath, boolean removeExisting)
            throws NoSuchWorkspaceException, ConstraintViolationException, VersionException, AccessDeniedException,
                   PathNotFoundException, ItemExistsException, LockException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public void move(String srcAbsPath, String destAbsPath)
            throws ConstraintViolationException, VersionException, AccessDeniedException, PathNotFoundException,
                   ItemExistsException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public void restore(Version[] versions, boolean removeExisting)
            throws ItemExistsException, UnsupportedRepositoryOperationException, VersionException, LockException,
                   InvalidItemStateException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public QueryManager getQueryManager()
            throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public NamespaceRegistry getNamespaceRegistry()
            throws RepositoryException {
        return upstream.getNamespaceRegistry();
    }

    public NodeTypeManager getNodeTypeManager()
            throws RepositoryException {
        return upstream.getNodeTypeManager();
    }

    public ObservationManager getObservationManager()
            throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public String[] getAccessibleWorkspaceNames()
            throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public ContentHandler getImportContentHandler(String parentAbsPath, int uuidBehavior)
            throws PathNotFoundException, ConstraintViolationException, VersionException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public void importXML(String parentAbsPath, InputStream in, int uuidBehavior)
            throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException,
                   InvalidSerializedDataException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public LockManager getLockManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public VersionManager getVersionManager() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UpdaterException("illegal method");
   }

    public void createWorkspace(String name) throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public void createWorkspace(String name, String srcWorkspace) throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public void deleteWorkspace(String name) throws AccessDeniedException, UnsupportedRepositoryOperationException, NoSuchWorkspaceException, RepositoryException {
        throw new UpdaterException("illegal method");
    }
}
