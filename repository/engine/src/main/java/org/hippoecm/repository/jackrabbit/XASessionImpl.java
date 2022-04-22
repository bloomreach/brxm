/*
 *  Copyright 2008-2022 Bloomreach (https://bloomreach.com)
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import javax.security.auth.Subject;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.RepositoryContext;
import org.apache.jackrabbit.core.WorkspaceImpl;
import org.apache.jackrabbit.core.config.AccessManagerConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.observation.EventStateCollectionFactory;
import org.apache.jackrabbit.core.observation.ObservationManagerImpl;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.authentication.AuthContext;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateCacheFactory;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateListener;
import org.apache.jackrabbit.core.state.LocalItemStateManager;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.hippoecm.repository.dataprovider.HippoNodeId;
import org.hippoecm.repository.dataprovider.MirrorNodeId;
import org.hippoecm.repository.impl.NodeDecorator;
import org.hippoecm.repository.nodetypes.NodeTypesChangeTracker;
import org.hippoecm.repository.query.lucene.AuthorizationQuery;
import org.hippoecm.repository.query.lucene.HippoQueryHandler;
import org.hippoecm.repository.security.HippoAMContext;
import org.hippoecm.repository.security.HippoAccessManager;
import org.hippoecm.repository.security.SubjectHelper;
import org.hippoecm.repository.security.domain.DomainRule;
import org.hippoecm.repository.security.domain.FacetAuthDomain;
import org.hippoecm.repository.security.domain.QFacetRule;
import org.hippoecm.repository.security.principals.UserPrincipal;
import org.hippoecm.repository.security.service.SessionDelegateUserImpl;
import org.onehippo.repository.security.SessionDelegateUser;
import org.onehippo.repository.security.SessionUser;
import org.onehippo.repository.security.domain.DomainRuleExtension;
import org.onehippo.repository.security.domain.FacetRule;
import org.onehippo.repository.xml.DefaultContentHandler;
import org.onehippo.repository.xml.EnhancedSystemViewImportHandler;
import org.onehippo.repository.xml.ImportContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;

import static org.onehippo.repository.security.domain.DomainRuleExtension.HIPPO_AVAILABILITY_PREVIEW_RULE;

public class XASessionImpl extends org.apache.jackrabbit.core.XASessionImpl implements InternalHippoSession {

    private static Logger log = LoggerFactory.getLogger(XASessionImpl.class);

    private final HippoAccessManager ham;
    private final NodeTypeRegistry ntReg;

    private AuthorizationQuery authorizationQuery;
    // used for testing purposes only
    private AuthorizationQuery explicitAuthorizationQuery;

    private int nodeTypesChangeCounter = -1;
    private long implicitReadAccessUpdateCounter;

    protected XASessionImpl(RepositoryContext repositoryContext, AuthContext loginContext, WorkspaceConfig wspConfig)
            throws AccessDeniedException, RepositoryException {
        super(repositoryContext, loginContext, wspConfig);
        namePathResolver = new HippoNamePathResolver(this, true);
        final HippoLocalItemStateManager localISM = (HippoLocalItemStateManager)(context.getWorkspace().getItemStateManager());
        ((RepositoryImpl)context.getRepository()).initializeLocalItemStateManager(localISM, context.getSessionImpl(), subject);
        ham = getAccessControlManager();
        ntReg = repositoryContext.getNodeTypeRegistry();
    }

    protected XASessionImpl(RepositoryContext repositoryContext, Subject subject, WorkspaceConfig wspConfig)
            throws AccessDeniedException, RepositoryException {
        super(repositoryContext, subject, wspConfig);
        namePathResolver = new HippoNamePathResolver(this, true);
        final HippoLocalItemStateManager localISM = (HippoLocalItemStateManager)(context.getWorkspace().getItemStateManager());
        ((RepositoryImpl)context.getRepository()).initializeLocalItemStateManager(localISM, context.getSessionImpl(), subject);
        ham = getAccessControlManager();
        ntReg = repositoryContext.getNodeTypeRegistry();
    }

    @Override
    protected AccessManager createAccessManager(Subject subject) throws AccessDeniedException, RepositoryException {
        AccessManagerConfig amConfig = context.getRepository().getConfig().getAccessManagerConfig();
        try {
            HippoAMContext ctx = new HippoAMContext(
                    new File((context.getRepository()).getConfig().getHomeDir()),
                    context.getRepositoryContext().getFileSystem(),
                    context.getSessionImpl(), subject, context.getHierarchyManager(), context.getPrivilegeManager(),
                    context.getSessionImpl(), context.getWorkspace().getName(), context.getNodeTypeManager(), context.getItemStateManager());
            AccessManager accessMgr = amConfig.newInstance(AccessManager.class);
            if (!(accessMgr instanceof HippoAccessManager)) {
                throw new UnsupportedRepositoryOperationException("AccessManager must be instanceof HippoAccessManager. Actual class: "+accessMgr.getClass().getName());
            }
            accessMgr.init(ctx);
            context.getItemStateManager().addListener((ItemStateListener) accessMgr);
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
        // check sanity of this session
        context.getSessionState().checkAlive();
        return ham.hasPermission(absPath, actions);
    }

    @Override
    public void checkPermission(String absPath, String actions) throws AccessControlException, RepositoryException {
        if (!hasPermission(absPath, actions)) {
            throw new AccessControlException("Privileges '" + actions + "' denied for " + absPath);
        }
    }

    @Override
    protected SessionItemStateManager createSessionItemStateManager() {
        SessionItemStateManager mgr = new HippoSessionItemStateManager(context.getRootNodeId(), context.getWorkspace().getItemStateManager());
        context.getWorkspace().getItemStateManager().addListener(mgr);
        return mgr;
    }

    protected ObservationManagerImpl createObservationManager(String wspName)
            throws RepositoryException {
        try {
            final RepositoryImpl repository = (RepositoryImpl) context.getRepository();
            final RepositoryImpl.HippoWorkspaceInfo workspaceInfo = repository.getWorkspaceInfo(wspName);
            return new HippoObservationManager(workspaceInfo.getObservationDispatcher(), this,
                    context.getRepositoryContext().getClusterNode());
        } catch (NoSuchWorkspaceException e) {
            // should never get here
            throw new RepositoryException(
                    "Internal error: failed to create observation manager", e);
        }
    }

    @Override
    protected org.apache.jackrabbit.core.ItemManager createItemManager() {
        return new HippoItemManager(context);
    }

    @Override
    public SessionUser getUser() {
        return ham.isSystemSession() ? null : ham.getUserPrincipal().getUser();
    }

    @Override
    public boolean isSystemSession() {
        return ham.isSystemSession();
    }

    /**
     * Clears the local namespace mappings. Subclasses that for example
     * want to participate in a session pools should remember to call
     * <code>super.logout()</code> when overriding this method to avoid
     * namespace mappings to be carried over to a new session.
     */
    @Override
    public void logout() {
        HippoLocalItemStateManager localISM = (HippoLocalItemStateManager)(context.getWorkspace().getItemStateManager());
        localISM.setEnabled(false);
        localISM.clearChangeLog();
        authorizationQuery = null;
        super.logout();
    }

    @Override
    public void setNamespacePrefix(String prefix, String uri)
            throws NamespaceException, RepositoryException {
        super.setNamespacePrefix(prefix, uri);
        // Clear name and path caches
        namePathResolver = new HippoNamePathResolver(this, true);
    }

    public NodeIterator pendingChanges(Node node, String nodeType, boolean prune) throws NamespaceException, NoSuchNodeTypeException, RepositoryException {
        Name ntName;
        try {
            ntName = (nodeType!=null ? getQName(nodeType) : null);
        } catch (IllegalNameException ex) {
            throw new NoSuchNodeTypeException(nodeType);
        }
        final Set<NodeId> filteredResults = new HashSet<>();
        if (node==null) {
            node = getRootNode();
            if (node.isModified()&&(nodeType==null||node.isNodeType(nodeType))) {
                filteredResults.add(((org.apache.jackrabbit.core.NodeImpl)node).getNodeId());
            }
        }
        NodeId nodeId = ((org.apache.jackrabbit.core.NodeImpl) NodeDecorator.unwrap(node)).getNodeId();

        for(ItemState itemState : getItemStateManager().getDescendantTransientItemStates(nodeId)) {
            NodeState state;
            if (!itemState.isNode()) {
                try {
                    if (filteredResults.contains(itemState.getParentId()))
                        continue;
                    state = (NodeState)getItemStateManager().getItemState(itemState.getParentId());
                } catch (ItemStateException ex) {
                    log.error("Cannot find parent of changed property", ex);
                    continue;
                }
            } else {
                state = (NodeState)itemState;
            }

            /* if the node type of the current node state is not of required
             * type (if set), continue with next.
             */
            if (nodeType!=null) {
                if (!ntName.equals(state.getNodeTypeName())) {
                    Set<Name> mixins = state.getMixinTypeNames();
                    if (!mixins.contains(ntName)) {
                        // build effective node type of mixins & primary type
                        try {
                            if (!ntReg.getEffectiveNodeType(state.getNodeTypeName(),mixins).includesNodeType(ntName)) {
                                continue;
                            }
                        } catch (NodeTypeConflictException ntce) {
                            String msg = "internal error: failed to build effective node type";
                            log.debug(msg);
                            throw new RepositoryException(msg, ntce);
                        }
                    }
                }
            }

            /* if pruning, check that there are already children in the
             * current list.  If so, remove them.
             */
            if (prune) {
                HierarchyManager hierMgr = getHierarchyManager();
                for (Iterator<NodeId> i = filteredResults.iterator(); i.hasNext();) {
                    if (hierMgr.isAncestor(state.getNodeId(), i.next()))
                        i.remove();
                }
            }

            filteredResults.add(state.getNodeId());
        }

        return new NodeIterator() {
            private final org.apache.jackrabbit.core.ItemManager itemMgr = getItemManager();
            private final Iterator<NodeId> iterator = filteredResults.iterator();
            private int pos = 0;
            private Item next;

            public Node nextNode() {
                return (Node)next();
            }

            public long getPosition() {
                return pos;
            }

            public long getSize() {
                return -1;
            }

            public void skip(long skipNum) {
                if (skipNum < 0) {
                    throw new IllegalArgumentException("skipNum must not be negative");
                }
                while (skipNum-- > 0) {
                    iterator.next();
                    ++pos;
                }
            }

            public boolean hasNext() {
                fetchNext();
                return next != null;
            }

            public Object next() {
                fetchNext();
                if (next == null) {
                    throw new NoSuchElementException();
                }
                final Item result = next;
                next = null;
                return result;
            }

            private void fetchNext() {
                while (next == null && iterator.hasNext()) {
                    try {
                        final NodeId id = iterator.next();
                        next = itemMgr.getItem(id);
                        ++pos;
                    } catch (RepositoryException e) {
                        log.error("Failed to fetch next item", e);
                    }
                }
            }

            public void remove() {
                throw new UnsupportedOperationException("remove");
            }
        };
    }

    @Override
    public void importEnhancedSystemViewXML(ImportContext importContext)
            throws IOException, RepositoryException {
        ContentHandler handler = getEnhancedSystemViewImportHandler(importContext);
        new DefaultContentHandler(handler).parse(importContext.getInputStream());
    }

    @Override
    public HippoSessionItemStateManager getItemStateManager() {
        return (HippoSessionItemStateManager) context.getItemStateManager();
    }

    @Override
    public Node getCanonicalNode(Node node) throws RepositoryException {
        NodeId nodeId = ((NodeImpl)node).getNodeId();
        if(nodeId instanceof HippoNodeId) {
            if(nodeId instanceof MirrorNodeId) {
                NodeId upstream = ((MirrorNodeId)nodeId).getCanonicalId();
                try {
                    return getNodeById(upstream);
                } catch(ItemNotFoundException ex) {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return node;
        }
    }

    // testing purposes only!!
    void setAuthorizationQuery(final AuthorizationQuery authorizationQuery) {
        this.explicitAuthorizationQuery = authorizationQuery;
    }

    @Override
    public AuthorizationQuery getAuthorizationQuery() {

        if (explicitAuthorizationQuery != null) {
            log.info("Returning explicitly set authorization query");
            return explicitAuthorizationQuery;
        }

        // potentially trigger the implicit read access cache to update
        ham.updateReferenceFacetRules();

        if (authorizationQuery == null || NodeTypesChangeTracker.getChangesCounter() != nodeTypesChangeCounter
                || ham.getImplicitReadAccessUpdateCounter() != implicitReadAccessUpdateCounter) {

            nodeTypesChangeCounter = NodeTypesChangeTracker.getChangesCounter();
            implicitReadAccessUpdateCounter = ham.getImplicitReadAccessUpdateCounter();
            try {
                final RepositoryImpl repository = (RepositoryImpl)context.getRepository();
                HippoQueryHandler queryHandler = repository.getHippoQueryHandler(getWorkspace().getName());
                if (queryHandler != null) {
                    Set<FacetAuthDomain> facetAuthDomains = ham.isSystemSession() ? Collections.emptySet() : ham.getUserPrincipal().getFacetAuthDomains();
                    authorizationQuery = new AuthorizationQuery(facetAuthDomains, queryHandler.getNamespaceMappings(),
                            queryHandler.getIndexingConfig(),  context.getNodeTypeManager(), this);
                }
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }

        return authorizationQuery;
    }

    @Override
    public Session createDelegatedSession(final InternalHippoSession session, DomainRuleExtension... domainExtensions) throws RepositoryException {
        String workspaceName = context.getRepositoryContext().getWorkspaceManager().getDefaultWorkspaceName();

        if (ham.isSystemSession()) {
            throw new IllegalStateException("Cannot create a delegated session for the system user");
        } else if (session.isSystemSession()) {
            throw new IllegalStateException("Cannot create a delegated session with a system user session");
        }

        UserPrincipal currentUser = ham.getUserPrincipal();
        UserPrincipal sessionUser = SubjectHelper.getFirstPrincipal(session.getSubject(), UserPrincipal.class);

        if (currentUser.getUser() instanceof SessionDelegateUser) {
            throw new IllegalArgumentException("Current session is already delegated");
        }
        if (sessionUser == null) {
            throw new IllegalStateException("Cannot create a delegated session with an anonymous session");
        }

        // create new set of principals, retaining existing ones (if any other,) other than the UserPrincipals
        final Set<Principal> principals = new HashSet<>(subject.getPrincipals());
        principals.addAll(session.getSubject().getPrincipals());

        // create SessionDelegateUser
        SessionDelegateUser sessionDelegateUser = new SessionDelegateUserImpl(currentUser.getUser(), sessionUser.getUser());

        // collect combined set of FacetAuthDomains
        HashSet<FacetAuthDomain> facetAuthDomains = new HashSet<>(currentUser.getFacetAuthDomains());
        facetAuthDomains.addAll(sessionUser.getFacetAuthDomains());
        // and apply possible rule extensions
        applyDomainRuleExtensions(facetAuthDomains, domainExtensions);

        // and replace current UserPrincipal
        principals.removeIf(p -> p instanceof UserPrincipal);
        principals.add(new UserPrincipal(sessionDelegateUser, facetAuthDomains));

        Subject newSubject = new Subject(subject.isReadOnly(), principals, subject.getPublicCredentials(), subject.getPrivateCredentials());
        return context.getRepositoryContext().getWorkspaceManager().createSession(newSubject, workspaceName);
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
    public LocalItemStateManager createItemStateManager(RepositoryContext repositoryContext, WorkspaceImpl workspace,
                                                        SharedItemStateManager sharedStateMgr,
                                                        EventStateCollectionFactory factory, String attribute,
                                                        ItemStateCacheFactory cacheFactory) {
        RepositoryImpl repository = (RepositoryImpl) repositoryContext.getRepository();
        LocalItemStateManager mgr =
                new HippoLocalItemStateManager(sharedStateMgr, workspace, repositoryContext.getItemStateCacheFactory(),
                        attribute, repository.getNodeTypeRegistry(), repository.isStarted(), repositoryContext.getRootNodeId());
        sharedStateMgr.addListener(mgr);
        return mgr;
    }

    @Override
    public HippoAccessManager getAccessControlManager() {
        try {
            return (HippoAccessManager)super.getAccessControlManager();
        } catch (RepositoryException e) {
            // ignore: is never thrown in upstream call
            throw new RuntimeException(e);
        }
    }

    private ContentHandler getEnhancedSystemViewImportHandler(ImportContext importContext) throws RepositoryException {

        // check sanity of this session
        if (!isLive()) {
            throw new RepositoryException("this session has been closed");
        }

        NodeImpl parent = importContext.getImportTargetNode();

        // verify that parent node is checked-out
        if (!parent.isNew()) {
            NodeImpl node = parent;
            while (node.getDepth() != 0) {
                if (node.hasProperty(NameConstants.JCR_ISCHECKEDOUT)) {
                    if (!node.getProperty(NameConstants.JCR_ISCHECKEDOUT).getBoolean()) {
                        String msg = parent.safeGetJCRPath() + ": cannot add a child to a checked-in node";
                        log.debug(msg);
                        throw new VersionException(msg);
                    }
                }
                node = (NodeImpl) node.getParent();
            }
        }


        // check protected flag of parent node
        if (parent.getDefinition().isProtected()) {
            String msg = parent.safeGetJCRPath() + ": cannot add a child to a protected node";
            log.debug(msg);
            throw new ConstraintViolationException(msg);
        }

        // check lock status
        if (!parent.isNew()) {
            context.getWorkspace().getInternalLockManager().checkLock(parent);
        }

        return new EnhancedSystemViewImportHandler(importContext, this);
    }

    private void applyDomainRuleExtensions(final HashSet<FacetAuthDomain> fads,
                                           final DomainRuleExtension[] domainRuleExtensions)
            throws RepositoryException {
        Map<String, Map<String, Set<QFacetRule>>> domainExtensionsMap = new HashMap<>();
        for (DomainRuleExtension domainExtension : domainRuleExtensions) {
            Map<String, Set<QFacetRule>> ruleExtension =
                    domainExtensionsMap.computeIfAbsent(domainExtension.getDomainName(), k -> new HashMap<>());
            Set<QFacetRule> facetRules =
                    ruleExtension.computeIfAbsent(domainExtension.getDomainRuleName(), k -> new HashSet<>());
            for (FacetRule facetRule : domainExtension.getFacetRules()) {
                facetRules.add(new QFacetRule(facetRule, context));
            }
        }

        Set<FacetAuthDomain> updatedFads = new HashSet<>();
        for (FacetAuthDomain fad : fads) {
            HashSet<DomainRule> domainRules = new HashSet<>(fad.getRules());
            boolean updated = applyDomainRuleExtensions(domainRules, domainExtensionsMap.get("*"));
            if (applyDomainRuleExtensions(domainRules, domainExtensionsMap.get(fad.getDomainName()))) {
                updated = true;
            }
            if (updated) {
                FacetAuthDomain updatedFad =
                        new FacetAuthDomain(fad.getDomainName(), fad.getDomainPath(), domainRules, fad.getRoles(),
                                fad.getPrivileges(), fad.getResolvedPrivileges());
                updatedFads.add(updatedFad);
            }
        }
        if (!updatedFads.isEmpty()) {
            fads.removeAll(updatedFads);
            fads.addAll(updatedFads);
        }
    }

    private boolean applyDomainRuleExtensions(final HashSet<DomainRule> domainRules,
                                              final Map<String, Set<QFacetRule>> ruleExtensionsMap) {
        boolean applied = false;
        if (ruleExtensionsMap != null) {
            for (DomainRule domainRule : domainRules) {
                Set<QFacetRule> facetRules = ruleExtensionsMap.get(domainRule.getName());
                if (facetRules != null && !facetRules.isEmpty()) {
                    domainRules.remove(domainRule);
                    domainRules.add(new DomainRule(domainRule, facetRules));
                    applied = true;
                    break;
                }
            }
            final Set<QFacetRule> facetRules = ruleExtensionsMap.get("*");
            if (facetRules != null && !facetRules.isEmpty()) {
                Set<DomainRule> extendedDomainRules = domainRules.stream()
                        // this is an unfortunate temporal workaround: to render the preview in the channel mgr, the user
                        // most be able to
                        .map(r -> {
                            if ("frontend-config".equals(r.getDomainName())){
                                // do not constraint the hippo-frontend read access through a wildcard facetrule for
                                // availability = preview only: this is because the preview jcr session for the preview
                                // website still must be able to access the live prototypes of namespaces. This is
                                // a bit of a workaround. It would be cleaner if we could do this via DomainRuleExtensions
                                // by for example excluding a domain. For now hardcoded here
                                final Set<QFacetRule> filterFacetRules = facetRules.stream()
                                        .filter(rule -> !Objects.equals(rule.getFacetRule(), HIPPO_AVAILABILITY_PREVIEW_RULE))
                                        .collect(Collectors.toSet());
                                if (filterFacetRules.isEmpty()) {
                                    // no extra facet rules to add, just return the original domain rule
                                    return r;
                                }
                                return new DomainRule(r, filterFacetRules);
                            } else {
                                return new DomainRule(r, facetRules);
                            }
                        })
                        .collect(Collectors.toSet());
                domainRules.clear();
                domainRules.addAll(extendedDomainRules);
                applied = true;
            }
        }
        return applied;
    }
}
