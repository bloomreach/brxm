/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.jackrabbit;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;

import org.apache.jackrabbit.core.NamespaceRegistryImpl;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.SearchManager;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.persistence.PersistenceManager;
import org.apache.jackrabbit.core.security.AuthContext;
import org.apache.jackrabbit.core.state.ItemStateCacheFactory;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.hippoecm.repository.FacetedNavigationEngine;
import org.hippoecm.repository.FacetedNavigationEngineFirstImpl;
import org.hippoecm.repository.FacetedNavigationEngineWrapperImpl;

public class RepositoryImpl extends org.apache.jackrabbit.core.RepositoryImpl
{
    protected RepositoryImpl(RepositoryConfig repConfig) throws RepositoryException {
        super(repConfig);
    }

    private FacetedNavigationEngine facetedEngine;

    public FacetedNavigationEngine getFacetedNavigationEngine() {
        if(facetedEngine == null) {
          facetedEngine = new FacetedNavigationEngineWrapperImpl(new FacetedNavigationEngineFirstImpl());
        }
        return facetedEngine;
    }

    public void setFacetedNavigationEngine(FacetedNavigationEngine engine) {
        facetedEngine = engine;
    }

    void initializeLocalItemStateManager(HippoLocalItemStateManager stateMgr,
                                         org.apache.jackrabbit.core.SessionImpl session)
    {
        stateMgr.initialize(session.getNamePathResolver(), session.getHierarchyManager(),
                            getFacetedNavigationEngine(),
                            session instanceof XASessionImpl ? ((XASessionImpl)session).getFacetedNavigationContext()
                                                             : ((SessionImpl)session).getFacetedNavigationContext());
    }

    public static RepositoryImpl create(RepositoryConfig config)
        throws RepositoryException {
        return new RepositoryImpl(config);
    }

    @Override
    protected SharedItemStateManager createItemStateManager(PersistenceManager persistMgr,
                                                            NodeId rootNodeId,
                                                            NodeTypeRegistry ntReg,
                                                            boolean usesReferences,
                                                            ItemStateCacheFactory cacheFactory)
            throws ItemStateException {
        return new HippoSharedItemStateManager(this, persistMgr, rootNodeId, ntReg, true, cacheFactory);
    }

    @Override
    protected org.apache.jackrabbit.core.SessionImpl createSessionInstance(AuthContext loginContext,
                                                                           WorkspaceConfig wspConfig)
        throws AccessDeniedException, RepositoryException {
        return new XASessionImpl(this, loginContext, wspConfig);
    }
  
    @Override
    protected org.apache.jackrabbit.core.SessionImpl createSessionInstance(Subject subject, WorkspaceConfig wspConfig)
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

    @Override
    public Session login(Credentials credentials, String workspaceName) throws LoginException, NoSuchWorkspaceException, RepositoryException {
        Session session = super.login(credentials, workspaceName);

        /*
         * TODO/FIXME: Below facet authorizationQuery must be fetched. The authorizationQuery looks like below.
         * These authorizations needs to be fetched from the repository.
         * Map<String,String[]> authorizationQuery = new HashMap<String,String[]>();
         * authorizationQuery.put("x", new String[]{"x1","x2"});
         * authorizationQuery.put("y", new String[]{"y1","y2"});
         * authorizationQuery.put("z", new String[]{"z1","z2"});
         * FacetedNavigationEngine.Context context = getFacetedNavigationEngine().prepare("abc", authorizationQuery, null, servicingSession);
         */
        FacetedNavigationEngine.Context context = getFacetedNavigationEngine().prepare(null, null, null, session);
        if(session instanceof HippoSession)
            ((HippoSession)session).setFacetedNavigationContext(context);
        else
            throw new RepositoryException("programming fault");

        return session;
    }

    public SearchManager getSearchManager(String workspaceName) throws NoSuchWorkspaceException, RepositoryException {
        return ((WorkspaceInfo)getWorkspaceInfo(workspaceName)).getSearchManager();
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
        return ((WorkspaceInfo)getWorkspaceInfo(workspaceName)).getRootSession();
    }
    
    protected WorkspaceInfo createWorkspaceInfo(WorkspaceConfig wspConfig) {
        return new WorkspaceInfo(wspConfig);
    }

    protected class WorkspaceInfo extends org.apache.jackrabbit.core.RepositoryImpl.WorkspaceInfo {

        protected WorkspaceInfo(WorkspaceConfig config) {
            super(config);
        }

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
    
}
