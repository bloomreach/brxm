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

import java.io.File;
import java.io.IOException;
import java.security.AccessControlException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import javax.jcr.AccessDeniedException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.security.auth.Subject;

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.RepositoryContext;
import org.apache.jackrabbit.core.WorkspaceImpl;
import org.apache.jackrabbit.core.config.AccessManagerConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.observation.EventStateCollectionFactory;
import org.apache.jackrabbit.core.observation.ObservationManagerImpl;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.authentication.AuthContext;
import org.apache.jackrabbit.core.state.ItemStateCacheFactory;
import org.apache.jackrabbit.core.state.ItemStateListener;
import org.apache.jackrabbit.core.state.LocalItemStateManager;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.hippoecm.repository.query.lucene.AuthorizationQuery;
import org.hippoecm.repository.security.AuthorizationFilterPrincipal;
import org.hippoecm.repository.security.HippoAMContext;
import org.onehippo.repository.security.domain.DomainRuleExtension;
import org.onehippo.repository.xml.DefaultContentHandler;
import org.onehippo.repository.xml.ImportContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;

public class XASessionImpl extends org.apache.jackrabbit.core.XASessionImpl implements InternalHippoSession {

    private static Logger log = LoggerFactory.getLogger(XASessionImpl.class);

    private final SessionImplHelper helper;

    protected XASessionImpl(RepositoryContext repositoryContext, AuthContext loginContext, WorkspaceConfig wspConfig)
            throws AccessDeniedException, RepositoryException {
        super(repositoryContext, loginContext, wspConfig);
        namePathResolver = new HippoNamePathResolver(this, true);
        helper = new SessionImplHelper(this, repositoryContext, context, subject) {
            @Override
            SessionItemStateManager getItemStateManager() {
                return context.getItemStateManager();
            }
        };
        helper.init();
    }

    protected XASessionImpl(RepositoryContext repositoryContext, Subject subject, WorkspaceConfig wspConfig) throws AccessDeniedException,
                                                                                                   RepositoryException {
        super(repositoryContext, subject, wspConfig);
        namePathResolver = new HippoNamePathResolver(this, true);
        helper = new SessionImplHelper(this, repositoryContext, context, subject) {
            @Override
            SessionItemStateManager getItemStateManager() {
                return context.getItemStateManager();
            }
        };
        helper.init();
    }

    @Override
    protected AccessManager createAccessManager(Subject subject) throws AccessDeniedException, RepositoryException {
        AccessManagerConfig amConfig = context.getRepository().getConfig().getAccessManagerConfig();
        try {
            HippoAMContext ctx = new HippoAMContext(
                    new File((context.getRepository()).getConfig().getHomeDir()),
                    context.getRepositoryContext().getFileSystem(),
                    this, subject, context.getHierarchyManager(), context.getPrivilegeManager(),
                    this, getWorkspace().getName(), context.getNodeTypeManager(), getItemStateManager());
            AccessManager accessMgr = amConfig.newInstance(AccessManager.class);
            accessMgr.init(ctx);
            if (accessMgr instanceof ItemStateListener) {
                context.getItemStateManager().addListener((ItemStateListener) accessMgr);
            }
            return accessMgr;
        } catch (AccessDeniedException ex) {
            throw ex;
        } catch (Exception ex) {
            String msg = "failed to instantiate AccessManager implementation: "+amConfig.getClassName();
            log.error(msg, ex);
            throw new RepositoryException(msg, ex);
        }
    }

    @Override
    public boolean hasPermission(final String absPath, final String actions) throws RepositoryException {
        try {
            return super.hasPermission(absPath, actions);
        } catch (IllegalArgumentException ignore) {}
        try {
            helper.checkPermission(absPath, actions);
            return true;
        } catch (AccessControlException e) {
            return false;
        }
    }

    @Override
    public void checkPermission(String absPath, String actions) throws AccessControlException, RepositoryException {
        try {
            super.checkPermission(absPath, actions);
        } catch(IllegalArgumentException ignore) {
        }
        helper.checkPermission(absPath, actions);
    }

    @Override
    protected SessionItemStateManager createSessionItemStateManager() {
        SessionItemStateManager mgr = new HippoSessionItemStateManager(context.getRootNodeId(), context.getWorkspace().getItemStateManager());
        context.getWorkspace().getItemStateManager().addListener(mgr);
        return mgr;
    }

    protected ObservationManagerImpl createObservationManager(String wspName)
            throws RepositoryException {
        return SessionImplHelper.createObservationManager(context, this, wspName);
    }

    @Override
    protected org.apache.jackrabbit.core.ItemManager createItemManager() {
        return new HippoItemManager(context);
    }

    @Override
    public String getUserID() {
        return helper.getUserID();
    }

    /**
     * Method to expose the authenticated users' principals
     * @return Set An unmodifiable set containing the principals
     */
    public Set<Principal> getUserPrincipals() {
        return helper.getUserPrincipals();
    }
    
    @Override
    public void logout() {
        helper.logout();
        super.logout();
    }

    //------------------------------------------------< Namespace handling >--
    @Override
    public String getNamespacePrefix(String uri)
            throws NamespaceException, RepositoryException {
        // accessmanager is instantiated before the helper is set
        if (helper == null) {
            return super.getNamespacePrefix(uri);
        }
        return helper.getNamespacePrefix(uri);
    }

    @Override
    public String getNamespaceURI(String prefix)
            throws NamespaceException, RepositoryException {
        // accessmanager is instantiated before the helper is set
        if (helper == null) {
            return super.getNamespaceURI(prefix);
        }
        return helper.getNamespaceURI(prefix);
    }

    @Override
    public String[] getNamespacePrefixes()
            throws RepositoryException {
        return helper.getNamespacePrefixes();
    }

    @Override
    public void setNamespacePrefix(String prefix, String uri)
            throws NamespaceException, RepositoryException {
        helper.setNamespacePrefix(prefix, uri);
        // Clear name and path caches
        namePathResolver = new HippoNamePathResolver(this, true);
    }

    public NodeIterator pendingChanges(Node node, String nodeType, boolean prune) throws NamespaceException, NoSuchNodeTypeException, RepositoryException {
        return helper.pendingChanges(node, nodeType, prune);
    }

    @Override
    public void importEnhancedSystemViewXML(ImportContext importContext)
            throws IOException, RepositoryException {
        ContentHandler handler = helper.getEnhancedSystemViewImportHandler(importContext);
        new DefaultContentHandler(handler).parse(importContext.getInputStream());
    }

    @Override
    public HippoSessionItemStateManager getItemStateManager() {
        return (HippoSessionItemStateManager) context.getItemStateManager();
    }

    @Override
    public Node getCanonicalNode(Node node) throws RepositoryException {
        return helper.getCanonicalNode((NodeImpl)node);
    }

    @Override
    public AuthorizationQuery getAuthorizationQuery() {
        return helper.getAuthorizationQuery();
    }

    @Override
    public Session createDelegatedSession(final InternalHippoSession session, DomainRuleExtension... domainExtensions) throws RepositoryException {
        String workspaceName = repositoryContext.getWorkspaceManager().getDefaultWorkspaceName();

        final Set<Principal> principals = new HashSet<Principal>(subject.getPrincipals());
        principals.add(new AuthorizationFilterPrincipal(helper.getFacetRules(domainExtensions)));
        principals.addAll(session.getSubject().getPrincipals());

        Subject newSubject = new Subject(subject.isReadOnly(), principals, subject.getPublicCredentials(), subject.getPrivateCredentials());
        return repositoryContext.getWorkspaceManager().createSession(newSubject, workspaceName);
    }

    @Override
    public void localRefresh() {
        getItemStateManager().disposeAllTransientItemStates();
    }

    @Override
    public ScheduledExecutorService getExecutor() {
        return context.getRepositoryContext().getExecutor();
    }

    @Override
    public LocalItemStateManager createItemStateManager(RepositoryContext repositoryContext, WorkspaceImpl workspace, SharedItemStateManager sharedStateMgr, EventStateCollectionFactory factory, String attribute, ItemStateCacheFactory cacheFactory) {
        RepositoryImpl repository = (RepositoryImpl) repositoryContext.getRepository();
        LocalItemStateManager mgr = new HippoLocalItemStateManager(sharedStateMgr, workspace, repositoryContext.getItemStateCacheFactory(), attribute, repository.getNodeTypeRegistry(), repository.isStarted(), repositoryContext.getRootNodeId());
        sharedStateMgr.addListener(mgr);
        return mgr;
    }

}
