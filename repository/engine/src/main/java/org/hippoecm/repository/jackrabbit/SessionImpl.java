/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
import java.security.AccessControlException;
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
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.observation.EventStateCollectionFactory;
import org.apache.jackrabbit.core.observation.ObservationManagerImpl;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.authentication.AuthContext;
import org.apache.jackrabbit.core.state.ItemStateCacheFactory;
import org.apache.jackrabbit.core.state.LocalItemStateManager;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.hippoecm.repository.query.lucene.AuthorizationQuery;
import org.hippoecm.repository.security.HippoAccessManager;
import org.onehippo.repository.security.SessionUser;
import org.onehippo.repository.security.domain.DomainRuleExtension;
import org.onehippo.repository.xml.DefaultContentHandler;
import org.onehippo.repository.xml.ImportContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;

public class SessionImpl extends org.apache.jackrabbit.core.SessionImpl implements InternalHippoSession {

    private static Logger log = LoggerFactory.getLogger(SessionImpl.class);

    private final SessionImplHelper helper;

    protected SessionImpl(RepositoryContext repositoryContext, AuthContext loginContext, WorkspaceConfig wspConfig)
            throws AccessDeniedException, RepositoryException {
        super(repositoryContext, loginContext, wspConfig);
        namePathResolver = new HippoNamePathResolver(this, true);
        helper = new SessionImplHelper(this, repositoryContext, context, loginContext.getSubject());
        helper.init();
    }

    @Override
    protected AccessManager createAccessManager(Subject subject) throws AccessDeniedException, RepositoryException {
        return SessionImplHelper.createAccessManager(context, subject);
    }

    @Override
    public boolean hasPermission(final String absPath, final String actions) throws RepositoryException {
        return helper.hasPermission(absPath, actions);
    }

    @Override
    public void checkPermission(String absPath, String actions) throws AccessControlException, RepositoryException {
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
    public SessionUser getUser() {
        return helper.getUser();
    }

    @Override
    public boolean isSystemUser() {
        return helper.isSystemUser();
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

    @Override
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
        return helper.createDelegatedSession(session, domainExtensions);
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
        LocalItemStateManager mgr = new HippoLocalItemStateManager(sharedStateMgr, context.getWorkspace(), context.getRepositoryContext().getItemStateCacheFactory(), attribute, ((RepositoryImpl)context.getRepository()).getNodeTypeRegistry(), ((RepositoryImpl)context.getRepository()).isStarted(), ((RepositoryImpl)context.getRepository()).getRootNodeId());
        sharedStateMgr.addListener(mgr);
        return mgr;
    }

    @Override
    public HippoAccessManager getAccessControlManager() throws RepositoryException {
        return (HippoAccessManager)super.getAccessControlManager();
    }
}
