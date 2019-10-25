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

import java.io.File;
import java.security.AccessControlException;
import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
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
import org.apache.jackrabbit.core.config.AccessManagerConfig;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.observation.ObservationManagerImpl;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.session.SessionContext;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateListener;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.util.XMLChar;
import org.hippoecm.repository.nodetypes.NodeTypesChangeTracker;
import org.hippoecm.repository.dataprovider.HippoNodeId;
import org.hippoecm.repository.dataprovider.MirrorNodeId;
import org.hippoecm.repository.impl.NodeDecorator;
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
import org.onehippo.repository.xml.EnhancedSystemViewImportHandler;
import org.onehippo.repository.xml.ImportContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;

class SessionImplHelper {

    private static Logger log = LoggerFactory.getLogger(SessionImpl.class);

    private long implicitReadAccessUpdateCounter;

    private final NodeTypeRegistry ntReg;
    private final Subject subject;
    private final InternalHippoSession session;
    private final SessionContext context;
    private final HippoAccessManager ham;

    /**
     * Local namespace mappings. Prefixes as keys and namespace URIs as values.
     * <p>
     * This map is only accessed from synchronized methods (see
     * <a href="https://issues.apache.org/jira/browse/JCR-1793">JCR-1793</a>).
     */
    private final Map<String, String> namespaces = new HashMap<>();
    private AuthorizationQuery authorizationQuery;
    private int nodeTypesChangeCounter = -1;

    /**
     * Clears the local namespace mappings. Subclasses that for example
     * want to participate in a session pools should remember to call
     * <code>super.logout()</code> when overriding this method to avoid
     * namespace mappings to be carried over to a new session.
     */
    public void logout() {
        HippoLocalItemStateManager localISM = (HippoLocalItemStateManager)(context.getWorkspace().getItemStateManager());
        localISM.setEnabled(false);
        localISM.clearChangeLog();
        namespaces.clear();
        authorizationQuery = null;

    }

    //------------------------------------------------< Namespace handling >--

    /**
     * Returns the namespace prefix mapped to the given URI. The mapping is
     * added to the set of session-local namespace mappings unless it already
     * exists there.
     * <p>
     * This behaviour is based on JSR 283 (JCR 2.0), but remains backwards
     * compatible with JCR 1.0.
     *
     * @param uri namespace URI
     * @return namespace prefix
     * @throws NamespaceException if the namespace is not found
     * @throws RepositoryException if a repository error occurs
     */
    String getNamespacePrefix(String uri) throws NamespaceException, RepositoryException {
        for (final Map.Entry<String, String> entry : namespaces.entrySet()) {
            if (entry.getValue().equals(uri)) {
                return entry.getKey();
            }
        }

        // The following throws an exception if the URI is not found, that's OK
        String prefix = session.getWorkspace().getNamespaceRegistry().getPrefix(uri);

        // Generate a new prefix if the global mapping is already taken
        String base = prefix;
        for (int i = 2; namespaces.containsKey(prefix); i++) {
            prefix = base + i;
        }

        namespaces.put(prefix, uri);
        return prefix;
    }

    /**
     * Returns the namespace URI mapped to the given prefix. The mapping is
     * added to the set of session-local namespace mappings unless it already
     * exists there.
     * <p>
     * This behaviour is based on JSR 283 (JCR 2.0), but remains backwards
     * compatible with JCR 1.0.
     *
     * @param prefix namespace prefix
     * @return namespace URI
     * @throws NamespaceException if the namespace is not found
     * @throws RepositoryException if a repository error occurs
     */
    public String getNamespaceURI(String prefix) throws NamespaceException, RepositoryException {
        String uri = namespaces.get(prefix);

        if (uri == null) {
            // Not in local mappings, try the global ones
            uri = session.getWorkspace().getNamespaceRegistry().getURI(prefix);
            if (namespaces.containsValue(uri)) {
                // The global URI is locally mapped to some other prefix,
                // so there are no mappings for this prefix
                throw new NamespaceException("Namespace not found: " + prefix);
            }
            // Add the mapping to the local set, we already know that
            // the prefix is not taken
            namespaces.put(prefix, uri);
        }

        return uri;
    }

    /**
     * Returns the prefixes of all known namespace mappings. All global
     * mappings not already included in the local set of namespace mappings
     * are added there.
     * <p>
     * This behaviour is based on JSR 283 (JCR 2.0), but remains backwards
     * compatible with JCR 1.0.
     *
     * @return namespace prefixes
     * @throws RepositoryException if a repository error occurs
     */
    String[] getNamespacePrefixes()
            throws RepositoryException {
        NamespaceRegistry registry = session.getWorkspace().getNamespaceRegistry();
        String[] uris = registry.getURIs();
        for (final String s : uris) {
            getNamespacePrefix(s);
        }

        return namespaces.keySet().toArray(new String[0]);
    }

    /**
     * Modifies the session local namespace mappings to contain the given
     * prefix to URI mapping.
     * <p>
     * This behaviour is based on JSR 283 (JCR 2.0), but remains backwards
     * compatible with JCR 1.0.
     *
     * @param prefix namespace prefix
     * @param uri namespace URI
     * @throws NamespaceException if the mapping is illegal
     * @throws RepositoryException if a repository error occurs
     */
    void setNamespacePrefix(String prefix, String uri)
            throws NamespaceException, RepositoryException {
        if (prefix == null) {
            throw new IllegalArgumentException("Prefix must not be null");
        } else if (uri == null) {
            throw new IllegalArgumentException("Namespace must not be null");
        } else if (prefix.length() == 0) {
            throw new NamespaceException(
                    "Empty prefix is reserved and can not be remapped");
        } else if (uri.length() == 0) {
            throw new NamespaceException(
                    "Default namespace is reserved and can not be remapped");
        } else if (prefix.toLowerCase().startsWith("xml")) {
            throw new NamespaceException(
                    "XML prefixes are reserved: " + prefix);
        } else if (!XMLChar.isValidNCName(prefix)) {
            throw new NamespaceException(
                    "Prefix is not a valid XML NCName: " + prefix);
        }

        // FIXME Figure out how this should be handled
        // Currently JSR 283 does not specify this exception, but for
        // compatibility with JCR 1.0 TCK it probably should.
        // Note that the solution here also affects the remove() code below
        String previous = namespaces.get(prefix);
        if (previous != null && !previous.equals(uri)) {
            throw new NamespaceException("Namespace already mapped");
        }

        namespaces.remove(prefix);
        namespaces.entrySet().removeIf(entry -> entry.getValue().equals(uri));

        namespaces.put(prefix, uri);
    }

    static AccessManager createAccessManager(SessionContext context, Subject subject) throws RepositoryException {

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

    SessionImplHelper(InternalHippoSession session, RepositoryContext repositoryContext,
                      SessionContext sessionContext, Subject subject) throws RepositoryException {
        this.session = session;
        this.context = sessionContext;
        this.ntReg = repositoryContext.getNodeTypeRegistry();
        this.ham = session.getAccessControlManager();
        this.subject = subject;
    }

    /**
     * Initialize the helper after the session can delegate back.
     *
     * @throws RepositoryException -
     */
    void init() throws RepositoryException {
        HippoLocalItemStateManager localISM = (HippoLocalItemStateManager)(context.getWorkspace().getItemStateManager());
        ((RepositoryImpl)context.getRepository()).initializeLocalItemStateManager(localISM, context.getSessionImpl(), subject);
    }

    protected SessionUser getUser() {
        return ham.isSystemUser() ? null : ham.getUserPrincipal().getUser();
    }

    public boolean isSystemUser() {
        return ham.isSystemUser();
    }

    public void checkPermission(String absPath, String actions) throws AccessControlException, RepositoryException {
        if (!hasPermission(absPath, actions)) {
            throw new AccessControlException("Privileges '" + actions + "' denied for " + absPath);
        }
    }

    public boolean hasPermission(final String absPath, final String actions) throws RepositoryException {
        // check sanity of this session
        context.getSessionState().checkAlive();
        return ham.hasPermission(absPath, actions);
    }

    private SessionItemStateManager getItemStateManager() {
        return context.getItemStateManager();
    }


    static ObservationManagerImpl createObservationManager(SessionContext context, org.apache.jackrabbit.core.SessionImpl session, String wspName)
            throws RepositoryException {
        try {
            final RepositoryImpl repository = (RepositoryImpl) context.getRepository();
            final RepositoryImpl.HippoWorkspaceInfo workspaceInfo = repository.getWorkspaceInfo(
                    wspName);
            return new HippoObservationManager(
                    workspaceInfo.getObservationDispatcher(),
                    session, context.getRepositoryContext().getClusterNode());
        } catch (NoSuchWorkspaceException e) {
            // should never get here
            throw new RepositoryException(
                    "Internal error: failed to create observation manager", e);
        }
    }

    public NodeIterator pendingChanges(Node node, String nodeType, boolean prune) throws RepositoryException {
        Name ntName;
        try {
            ntName = (nodeType!=null ? session.getQName(nodeType) : null);
        } catch (IllegalNameException ex) {
            throw new NoSuchNodeTypeException(nodeType);
        }
        final Set<NodeId> filteredResults = new HashSet<>();
        if (node==null) {
            node = session.getRootNode();
            if (node.isModified()&&(nodeType==null||node.isNodeType(nodeType))) {
                filteredResults.add(((org.apache.jackrabbit.core.NodeImpl)node).getNodeId());
            }
        }
        NodeId nodeId = ((org.apache.jackrabbit.core.NodeImpl)NodeDecorator.unwrap(node)).getNodeId();

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
                HierarchyManager hierMgr = session.getHierarchyManager();
                for (Iterator<NodeId> i = filteredResults.iterator(); i.hasNext();) {
                    if (hierMgr.isAncestor(state.getNodeId(), i.next()))
                        i.remove();
                }
            }

            filteredResults.add(state.getNodeId());
        }

        return new NodeIterator() {
            private final org.apache.jackrabbit.core.ItemManager itemMgr = session.getItemManager();
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

    ContentHandler getEnhancedSystemViewImportHandler(ImportContext importContext) throws RepositoryException {

        // check sanity of this session
        if (!session.isLive()) {
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

        return new EnhancedSystemViewImportHandler(importContext, session);
    }

    Node getCanonicalNode(NodeImpl node) throws RepositoryException {
        NodeId nodeId = node.getNodeId();
        if(nodeId instanceof HippoNodeId) {
            if(nodeId instanceof MirrorNodeId) {
                NodeId upstream = ((MirrorNodeId)nodeId).getCanonicalId();
                try {
                    return session.getNodeById(upstream);
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

    private AuthorizationQuery explicitAuthorizationQuery;

    // used for testing purposes only!!!
    public void setAuthorizationQuery(final AuthorizationQuery authorizationQuery) {
        this.explicitAuthorizationQuery = authorizationQuery;
    }

    AuthorizationQuery getAuthorizationQuery() {

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
                HippoQueryHandler queryHandler = repository.getHippoQueryHandler(session.getWorkspace().getName());
                if (queryHandler != null) {
                    Set<FacetAuthDomain> facetAuthDomains = ham.isSystemUser() ? Collections.emptySet() : ham.getUserPrincipal().getFacetAuthDomains();
                    authorizationQuery = new AuthorizationQuery(facetAuthDomains, queryHandler.getNamespaceMappings(),
                            queryHandler.getIndexingConfig(),  context.getNodeTypeManager(), context.getSessionImpl());
                }
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }

        return authorizationQuery;
    }

    Session createDelegatedSession(final InternalHippoSession session, final DomainRuleExtension... domainExtensions) throws RepositoryException {
        String workspaceName = context.getRepositoryContext().getWorkspaceManager().getDefaultWorkspaceName();

        if (ham.isSystemUser()) {
            throw new IllegalStateException("Cannot create a delegated session for the system user");
        } else if (session.isSystemUser()) {
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
                        .map(r -> new DomainRule(r, facetRules))
                        .collect(Collectors.toSet());
                domainRules.clear();
                domainRules.addAll(extendedDomainRules);
                applied = true;
            }
        }
        return applied;
    }


}
