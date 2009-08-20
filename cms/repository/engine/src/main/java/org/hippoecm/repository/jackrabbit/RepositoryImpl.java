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
package org.hippoecm.repository.jackrabbit;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.SearchManager;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.security.authentication.AuthContext;
import org.apache.jackrabbit.core.state.ISMLocking;
import org.apache.jackrabbit.core.state.ItemStateCacheFactory;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.SharedItemStateManager;

import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.FacetedNavigationEngineFirstImpl;
import org.hippoecm.repository.FacetedNavigationEngineWrapperImpl;

public class RepositoryImpl extends org.apache.jackrabbit.core.RepositoryImpl {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static Logger log = LoggerFactory.getLogger(RepositoryImpl.class);

    protected RepositoryImpl(RepositoryConfig repConfig) throws RepositoryException {
        super(repConfig);
    }

    private FacetedNavigationEngine<FacetedNavigationEngine.Query,FacetedNavigationEngine.Context> facetedEngine;

    public FacetedNavigationEngine<FacetedNavigationEngine.Query,FacetedNavigationEngine.Context> getFacetedNavigationEngine() {
        if (facetedEngine == null) {
            log.warn("Please configure your facetedEngine correctly. Application will fall back to default faceted engine, but this is a very inefficient one. In your repository.xml (or workspace.xml if you have started the repository already at least once) configure the correct class for SearchIndex. See Hippo ECM documentation 'SearchIndex configuration' for further information.");
            facetedEngine = new FacetedNavigationEngineWrapperImpl(new FacetedNavigationEngineFirstImpl());
        }
        return facetedEngine;
    }

    protected boolean isStarted = false;

    final boolean isStarted() {
        return isStarted;
    }

    public void setFacetedNavigationEngine(FacetedNavigationEngine engine) {
        facetedEngine = engine;
    }

    void initializeLocalItemStateManager(HippoLocalItemStateManager stateMgr,
            org.apache.jackrabbit.core.SessionImpl session, Subject subject) throws RepositoryException {
        FacetedNavigationEngine<FacetedNavigationEngine.Query,FacetedNavigationEngine.Context> facetedEngine = getFacetedNavigationEngine();
        FacetedNavigationEngine.Context facetedContext;
        facetedContext = facetedEngine.prepare(session.getUserID(), subject, null, session);
        stateMgr.initialize(session, facetedEngine, facetedContext);
    }

    @Override
    protected SharedItemStateManager createItemStateManager(PersistenceManager persistMgr, NodeId rootNodeId,
            NodeTypeRegistry ntReg, boolean usesReferences, ItemStateCacheFactory cacheFactory, ISMLocking locking)
            throws ItemStateException {
        return new HippoSharedItemStateManager(this, persistMgr, rootNodeId, ntReg, true, cacheFactory, locking);
    }

    @Override
    protected org.apache.jackrabbit.core.SessionImpl createSessionInstance(AuthContext loginContext,
                                                WorkspaceConfig wspConfig)
            throws AccessDeniedException, RepositoryException {

        return new XASessionImpl(this, loginContext, wspConfig);
    }

    @Override
    protected org.apache.jackrabbit.core.SessionImpl createSessionInstance(Subject subject,
                                                WorkspaceConfig wspConfig)
            throws AccessDeniedException, RepositoryException {

        return new XASessionImpl(this, subject, wspConfig);
    }

    @Override
    protected NodeTypeRegistry getNodeTypeRegistry() {
        return super.getNodeTypeRegistry();
    }

    @Override
    protected NodeId getRootNodeId() {
        return super.getRootNodeId();
    }

    @Override
    protected FileSystem getFileSystem() {
        return super.getFileSystem();
    }

    @Override
    protected NamespaceRegistryImpl getNamespaceRegistry() {
        return super.getNamespaceRegistry();
    }

    public SearchManager getSearchManager(String workspaceName) throws NoSuchWorkspaceException, RepositoryException {
        return ((WorkspaceInfo) getWorkspaceInfo(workspaceName)).getSearchManager();
    }

    /**
     * Get the root/system session for a workspace
     * @param workspaceName if the workspaceName equals null the default namespace is taken
     * @return Session the rootSession
     * @throws RepositoryException
     */
    protected Session getRootSession(String workspaceName) throws RepositoryException {
        if (workspaceName == null) {
            workspaceName = super.repConfig.getDefaultWorkspaceName();
        }
        return ((WorkspaceInfo) getWorkspaceInfo(workspaceName)).getRootSession();
    }

    @Override
    protected WorkspaceInfo createWorkspaceInfo(WorkspaceConfig wspConfig) {
        return new WorkspaceInfo(wspConfig);
    }

    protected class WorkspaceInfo extends org.apache.jackrabbit.core.RepositoryImpl.WorkspaceInfo {

        protected WorkspaceInfo(WorkspaceConfig config) {
            super(config);
        }

        @Override
        protected SearchManager getSearchManager() throws RepositoryException {
            return super.getSearchManager();
        }

        /**
         * Returns the system session for this workspace.
         *
         * @return the system session for this workspace
         * @throws RepositoryException if the system session could not be created
         */
        protected Session getRootSession() throws RepositoryException {
            return super.getSystemSession();
        }
    }

    /**
     * Wrapper for login, adds rootSession to credentials if credentials are of SimpleCredentials.
     * @return session the authenticated session
     */
    @Override
    public Session login(Credentials credentials, String workspaceName) throws LoginException,
            NoSuchWorkspaceException, RepositoryException {
        char[] empty = {};
        SimpleCredentials sc = new SimpleCredentials(null, empty);
        Session rootSession = getRootSession(workspaceName);
        if (rootSession == null) {
            throw new RepositoryException("Unable to get the rootSession for workspace: " + workspaceName);
        }

        // non anonymous logins
        if (credentials != null) {
            if (credentials instanceof SimpleCredentials) {
                sc = (SimpleCredentials) credentials;
            }
        }

        sc.setAttribute("rootSession", rootSession);
        Session session = super.login(sc, workspaceName);

        return session;
    }

    /**
     * Calls <code>login(credentials, null)</code>.
     *
     * @return session
     * @see #login(Credentials, String)
     */
    @Override
    public Session login(Credentials credentials) throws LoginException, RepositoryException {
        return login(credentials, null);
    }

    /**
     * Calls <code>login(null, workspaceName)</code>.
     *
     * @return session
     * @see #login(Credentials, String)
     */
    @Override
    public Session login(String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return login(null, workspaceName);
    }

    /**
     * Calls <code>login(null, null)</code>.
     *
     * @return session
     * @see #login(Credentials, String)
     */
    @Override
    public Session login() throws LoginException, RepositoryException {
        return login(null, null);
    }
}
