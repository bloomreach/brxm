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
package org.hippoecm.repository.jackrabbit;

import java.io.IOException;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.NamespaceException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;

import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.core.SearchManager;
import org.apache.jackrabbit.core.cluster.ClusterNode;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.journal.ExternalRepositorySyncRevisionServiceImpl;
import org.apache.jackrabbit.core.lock.LockManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.observation.ObservationDispatcher;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.query.QueryHandler;
import org.apache.jackrabbit.core.query.lucene.ConsistencyCheck;
import org.apache.jackrabbit.core.query.lucene.ConsistencyCheckError;
import org.apache.jackrabbit.core.security.JackrabbitSecurityManager;
import org.apache.jackrabbit.core.security.authentication.AuthContext;
import org.apache.jackrabbit.core.state.ISMLocking;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.query.lucene.HippoQueryHandler;
import org.hippoecm.repository.query.lucene.ServicingSearchIndex;
import org.onehippo.repository.InternalHippoRepository;
import org.onehippo.repository.journal.ExternalRepositorySyncRevisionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryImpl extends org.apache.jackrabbit.core.RepositoryImpl implements InternalHippoRepository {

    private static Logger log = LoggerFactory.getLogger(RepositoryImpl.class);
    
    private FacetedNavigationEngine<FacetedNavigationEngine.Query, FacetedNavigationEngine.Context> facetedEngine;

    protected boolean isStarted = false;

    private ExternalRepositorySyncRevisionService externalRepositorySyncRevisionService;

    protected RepositoryImpl(RepositoryConfig repConfig) throws RepositoryException {
        super(repConfig);
        searchIndexConsistencyCheck();
    }

    private void searchIndexConsistencyCheck() throws RepositoryException {
        try {
            final ServicingSearchIndex searchIndex = (ServicingSearchIndex) getSearchManager("default").getQueryHandler();
            if (searchIndex.getServicingConsistencyCheckEnabled()) {
                final ConsistencyCheck consistencyCheck = searchIndex.runConsistencyCheck();
                List<ConsistencyCheckError> errors = consistencyCheck.getErrors();
                if (!errors.isEmpty()) {
                    consistencyCheck.doubleCheckErrors();
                    errors = consistencyCheck.getErrors();
                    if (!errors.isEmpty()) {
                        if (searchIndex.getAutoRepair()) {
                            consistencyCheck.repair(true);
                        } else {
                            for (ConsistencyCheckError error : errors) {
                                log.warn(error.toString());
                            }
                        }
                    } else {
                        log.info("No errors detected");
                    }
                } else {
                    log.info("No errors detected");
                }
            }
        } catch (IOException e) {
            log.error("Search index consistency check failed", e);
        }
    }

    @Override
    protected NamespaceRegistryImpl createNamespaceRegistry() throws RepositoryException {
        NamespaceRegistryImpl nsReg = super.createNamespaceRegistry();
        log.info("Initializing hippo namespace");
        safeRegisterNamespace(nsReg, "hippo", "http://www.onehippo.org/jcr/hippo/nt/2.0.4");
        log.info("Initializing hipposys namespace");
        safeRegisterNamespace(nsReg, "hipposys", "http://www.onehippo.org/jcr/hipposys/nt/1.0");
        log.info("Initializing hipposysedit namespace");
        safeRegisterNamespace(nsReg, "hipposysedit", "http://www.onehippo.org/jcr/hipposysedit/nt/1.2");

        // TODO HREPTWO-3571 remove the hippofacnav registration here to its own subproject
        log.info("Initializing hippofacnav namespace: this needs to move to its own subproject, see HREPTWO-3571: ");
        safeRegisterNamespace(nsReg, "hippofacnav", "http://www.onehippo.org/jcr/hippofacnav/nt/1.0.1");

        log.info("Initializing hipposched namespace");
        safeRegisterNamespace(nsReg, "hipposched", "http://www.hippoecm.org/hipposched/nt/1.3");
        return nsReg;
    }

    private void safeRegisterNamespace(NamespaceRegistryImpl nsreg, String prefix, String uri)
            throws NamespaceException, RepositoryException {
        try {
            nsreg.getURI(prefix);
        } catch (NamespaceException ex) {
            nsreg.registerNamespace(prefix, uri);
        }
    }


    public FacetedNavigationEngine<FacetedNavigationEngine.Query, FacetedNavigationEngine.Context> getFacetedNavigationEngine() {
        if (facetedEngine == null) {
            log.warn("Please configure your facetedEngine correctly. Application will fall back to default "
                            + "faceted engine, but this is a very inefficient one. In your repository.xml (or workspace.xml if you have "
                            + "started the repository already at least once) configure the correct class for SearchIndex. See Hippo ECM "
                            + "documentation 'SearchIndex configuration' for further information.");
        }
        return facetedEngine;
    }

    final boolean isStarted() {
        return isStarted;
    }

    public void setFacetedNavigationEngine(FacetedNavigationEngine engine) {
        facetedEngine = engine;
    }

    public HippoQueryHandler getHippoQueryHandler(String workspaceName) throws RepositoryException {
        final SearchManager searchManager = ((HippoWorkspaceInfo) getWorkspaceInfo(workspaceName)).getSearchManager();
        if (searchManager != null) {
            final QueryHandler queryHandler = searchManager.getQueryHandler();
            if (queryHandler instanceof  HippoQueryHandler) {
                return (HippoQueryHandler) queryHandler;
            }
        }
        return null;
    }

    void initializeLocalItemStateManager(HippoLocalItemStateManager stateMgr,
            org.apache.jackrabbit.core.SessionImpl session, Subject subject) throws RepositoryException {
        FacetedNavigationEngine.Context facetedContext = null;
        if (facetedEngine != null) {
            facetedContext = facetedEngine.prepare(session.getUserID(), subject, null, session);
        }
        stateMgr.initialize(session, facetedEngine, facetedContext);
    }

    @Override
    protected SharedItemStateManager createItemStateManager(
            PersistenceManager persistMgr, boolean usesReferences,
            ISMLocking locking) throws ItemStateException {
        return new HippoSharedItemStateManager(this, persistMgr, context.getRootNodeId(),
                context.getNodeTypeRegistry(), true, context.getItemStateCacheFactory(),
                locking, context.getNodeIdFactory());
    }
               

    @Override
    protected org.apache.jackrabbit.core.SessionImpl createSessionInstance(AuthContext loginContext,
            WorkspaceConfig wspConfig) throws AccessDeniedException, RepositoryException {

        return new XASessionImpl(context, loginContext, wspConfig);
    }

    @Override
    protected org.apache.jackrabbit.core.SessionImpl createSessionInstance(Subject subject, WorkspaceConfig wspConfig)
            throws AccessDeniedException, RepositoryException {

        return new XASessionImpl(context, subject, wspConfig);
    }

    protected RepositoryConfig getRepositoryConfig() {
        return super.getConfig();
    }

    protected NodeTypeRegistry getNodeTypeRegistry() {
        return context.getNodeTypeRegistry();
    }

    @Override
    protected NodeTypeRegistry createNodeTypeRegistry() throws RepositoryException {
        return new HippoNodeTypeRegistry(context.getNamespaceRegistry(), context.getFileSystem());
    }

    protected NodeId getRootNodeId() {
        return context.getRootNodeId();
    }

    protected FileSystem getFileSystem() {
        return context.getFileSystem();
    }

    public NamespaceRegistryImpl getNamespaceRegistry() {
        return context.getNamespaceRegistry();
    }

    public JackrabbitSecurityManager getSecurityManager() throws RepositoryException {
        return context.getSecurityManager();
    }

    public SearchManager getSearchManager(String workspaceName) throws NoSuchWorkspaceException, RepositoryException {
        return getWorkspaceInfo(workspaceName).getSearchManager();
    }

    public PersistenceManager getPersistenceManager(String workspaceName) throws RepositoryException {
        return getWorkspaceInfo(workspaceName).getPersistenceManager();
    }

    /**
     * Get the root/system session for a workspace
     * @param workspaceName if the workspaceName equals null the default namespace is taken
     * @return Session the rootSession
     * @throws RepositoryException
     */
    public Session getRootSession(String workspaceName) throws RepositoryException {
        if (workspaceName == null) {
            workspaceName = super.repConfig.getDefaultWorkspaceName();
        }
        return (getWorkspaceInfo(workspaceName)).getRootSession();
    }

    public synchronized ExternalRepositorySyncRevisionService getExternalRepositorySyncRevisionService() throws RepositoryException {
        sanityCheck();
        if (externalRepositorySyncRevisionService == null) {
            ClusterNode clusterNode = context.getClusterNode();
            externalRepositorySyncRevisionService = new ExternalRepositorySyncRevisionServiceImpl(clusterNode != null ? clusterNode.getJournal() : null);
        }
        return externalRepositorySyncRevisionService;
    }

    @Override
    protected HippoWorkspaceInfo getWorkspaceInfo(final String workspaceName) throws RepositoryException {
        return (HippoWorkspaceInfo) super.getWorkspaceInfo(workspaceName);
    }

    @Override
    protected WorkspaceInfo createWorkspaceInfo(WorkspaceConfig wspConfig) {
        return new HippoWorkspaceInfo(wspConfig);
    }

    protected class HippoWorkspaceInfo extends org.apache.jackrabbit.core.RepositoryImpl.WorkspaceInfo {

        private SearchManager searchMgr;

        protected HippoWorkspaceInfo(WorkspaceConfig config) {
            super(config);
        }

        @Override
        protected SearchManager getSearchManager() throws RepositoryException {
            if (!isInitialized()) {
                throw new IllegalStateException("workspace '" + getName()
                                                        + "' not initialized");
            }

            synchronized (this) {
                if (searchMgr == null && getConfig().isSearchEnabled()) {
                    // search manager is lazily instantiated in order to avoid
                    // 'chicken & egg' bootstrap problems
                    searchMgr = new HippoSearchManager(getName(),
                        context, HippoWorkspaceInfo.this.getConfig(), HippoWorkspaceInfo.this.getItemStateProvider(),
                              HippoWorkspaceInfo.this.getPersistenceManager(), context.getRootNodeId(),
                              getSystemSearchManager(HippoWorkspaceInfo.this.getName()),
                              org.apache.jackrabbit.core.RepositoryImpl.SYSTEM_ROOT_NODE_ID);
                }
                return searchMgr;
             }
        }

        @Override
        protected void doDispose() {
            super.doDispose();
            synchronized (this) {
                if (searchMgr != null) {
                    searchMgr.close();
                    searchMgr = null;
                }
            }
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

        @Override
        protected ObservationDispatcher getObservationDispatcher() {
            return super.getObservationDispatcher();
        }

        @Override
        protected LockManagerImpl createLockManager() throws RepositoryException {
            return new HippoLockManager(getSystemSession(), getFileSystem(), context.getExecutor());
        }
    }

}
