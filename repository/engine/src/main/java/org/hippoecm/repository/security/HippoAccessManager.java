/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;
import javax.security.auth.Subject;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.commons.iterator.AccessControlPolicyIteratorAdapter;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.id.ItemId;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.id.PropertyId;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.UserPrincipal;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;
import org.apache.jackrabbit.core.state.ChildNodeEntry;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.ItemStateListener;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.ParsingPathResolver;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.dataprovider.HippoNodeId;
import org.hippoecm.repository.jackrabbit.HippoSessionItemStateManager;
import org.hippoecm.repository.security.domain.DomainRule;
import org.hippoecm.repository.security.domain.QFacetRule;
import org.hippoecm.repository.security.principals.FacetAuthPrincipal;
import org.hippoecm.repository.security.principals.GroupPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HippoAccessManager based on facet authorization. A subject (user) has a set of {@link FacetAuthPrincipal}s which hold
 * the domain configuration as defined by a set of {@link DomainRule}s, the roles the subject has for the domain and the
 * JCR permissions the subject has for the domain.
 * <p>
 * For checking if a subject has specific permissions on a item (property), the permissions of the subject on the parent
 * node are checked.
 * <p>
 * The HippoAccessManager also checks if the node is part of a hippo:document in which case the hippo:document is also
 * checked for permissions when the subject does not have the correct permissions on the node itself. If the subject
 * does have the correct permissions on the hippo:document the permissions on the node are granted.
 */
public class HippoAccessManager implements AccessManager, AccessControlManager, ItemStateListener {

    /**
     * Intermediate readAccess state for current thread {@link #canRead(NodeId)} processing
     */
    private final Set<NodeId> inprocessNodeReadAccess = new HashSet<>();

    /**
     * Subject whose access rights this AccessManager should reflect
     */
    private Subject subject;

    /**
     * hierarchy manager used for ACL-based access control model
     */
    private HierarchyManager hierMgr;

    /**
     * Attic aware hierarchy manager used for ACL-based access control model
     */
    private HierarchyManager zombieHierMgr;

    /**
     * The session item state manager used for fetching transient and attic item states
     */
    private HippoSessionItemStateManager itemMgr;

    /**
     * NodeTypeManager for resolving superclass node types
     */
    private NodeTypeManager ntMgr;

    /**
     * NamePathResolver
     */
    private NamePathResolver npRes;

    /**
     * Name of hippo:handle, needed for document model checking
     */
    private Name hippoHandle;

    /**
     * Name of hippo:facetresult, needed for document model checking
     */
    private Name hippoFacetResult;

    /**
     * Name of hippo:facetsearch, needed for document model checking
     */
    private Name hippoFacetSearch;

    /**
     * Name of hippo:facetselect, needed for document model checking
     */
    private Name hippoFacetSelect;

    /**
     * Root NodeId of current session
     */
    private NodeId rootNodeId;

    /**
     * The HippoAccessCache instance
     */
    private HippoAccessCache readAccessCache;

    private Set<NodeId> implicitReads = new ConcurrentHashMap().newKeySet();

    private long reInitCounter = 0;

    /**
     * createdDestroyedNodeIds can be accessed concurrently hence use synchronized(createdDestroyedNodeIds) when using
     * the left and right set. The left set contains the createdNodeIds and the right set contains the destroyedNodeIds
     * which still need to be processed by this HippoAccessManager
     */

    private Pair<Set<NodeId>, Set<NodeId>> createdDestroyedNodeIds = new ImmutablePair<>(new HashSet<>(), new HashSet<>());

    /**
     * In general, if left or right set in createdDestroyedNodeIds is not empty, we process the created and/or destroyed
     * node Ids synchronously during checkPermission / isGranted /canRead methods. *However*, if the
     * createdDestroyedNodeIds exceed the PROCESS_CREATED_NODE_IDS_SYNC_LIMIT, we process it asynchronously. Note that
     * we *never* process createdDestroyedNodeIds from the thread that triggered {@link #stateCreated(ItemState)} or
     * {@link #stateDestroyed(ItemState)} since that thread should return as fast as possible from these methods (which
     * is also the reason why we delay the processingCreatedDestroyedNodeIds of createdDestroyedNodeIds to the thread
     * using the current session or until PROCESS_CREATED_NODE_IDS_SYNC_LIMIT is reached. We use
     * PROCESS_CREATED_NODE_IDS_SYNC_LIMIT since we do not want possibly OOM if there are lots of unused/dorming JCR
     * Sessions which would get too large createdNodeIds sets.
     */
    private final static int PROCESS_CREATED_NODE_IDS_SYNC_LIMIT = 1000;

    private WeakHashMap<HippoNodeId, Boolean> readVirtualAccessCache;

    private static final int DEFAULT_PERM_CACHE_SIZE = 20000;

    /**
     * Cache for determining if a type is a instance of another type
     */
    private final NodeTypeInstanceOfCache ntIOCache = NodeTypeInstanceOfCache.getInstance();

    /**
     * State of the accessManager
     */
    private volatile boolean initialized = false;

    /**
     * Flag whether current user is a regular user
     */
    private boolean isUser = false;

    /**
     * Flag whether the current user is a system user
     */
    private boolean isSystem = false;

    /**
     * The userIds of the logged in user
     */
    private List<String> userIds = new ArrayList<String>();

    private final List<String> groupIds = new ArrayList<String>();
    private final List<String> currentDomainRoleIds = new ArrayList<String>();

    private Map<String, Collection<QFacetRule>> extendedFacetRules;

    /**
     * The logger
     */
    private static final Logger log = LoggerFactory.getLogger(HippoAccessManager.class);

    private static ConcurrentHashMap<String, Privilege> currentPrivileges = new ConcurrentHashMap<String, Privilege>();

    //---------------------------------------- API ---------------------------------------------//
    /**
     * @see AccessManager#init(AMContext, AccessControlProvider, WorkspaceAccessManager)
     */
    public void init(AMContext context, AccessControlProvider acProvider, WorkspaceAccessManager wspAccessMgr)
            throws AccessDeniedException, Exception {
        init(context);
    }

    /**
     * @see AccessManager#init(AMContext)
     */
    public void init(AMContext context) throws AccessDeniedException, Exception {

        if (initialized) {
            throw new IllegalStateException("already initialized");
        }

        subject = context.getSubject();
        npRes = context.getNamePathResolver();

        if (context instanceof HippoAMContext) {
            ntMgr = ((HippoAMContext) context).getNodeTypeManager();
            itemMgr = (HippoSessionItemStateManager) ((HippoAMContext) context).getSessionItemStateManager();
        }

        hierMgr = itemMgr.getHierarchyMgr();
        zombieHierMgr = itemMgr.getAtticAwareHierarchyMgr();

        // Shortcuts for checks
        isSystem = !subject.getPrincipals(SystemPrincipal.class).isEmpty();
        isUser = !subject.getPrincipals(UserPrincipal.class).isEmpty();

        // prefetch userId
        userIds = new ArrayList<>();
        if (isSystem) {
            for (SystemPrincipal principal : subject.getPrincipals(SystemPrincipal.class)) {
                userIds.add(principal.getName());
            }
        } else if (isUser) {
            for (UserPrincipal principal : subject.getPrincipals(UserPrincipal.class)) {
                userIds.add(principal.getName());
            }
        } else if (!subject.getPrincipals(AnonymousPrincipal.class).isEmpty()) {
            userIds.add(subject.getPrincipals(AnonymousPrincipal.class).iterator().next().getName());
        }
        if (userIds.size() == 0) {
            userIds.add("");
        }

        // prefetch groupId's
        for (GroupPrincipal gp : subject.getPrincipals(GroupPrincipal.class)) {
            groupIds.add(gp.getName());
        }

        // cache root NodeId
        rootNodeId = hierMgr.resolveNodePath(PathFactoryImpl.getInstance().getRootPath());

        hippoHandle = npRes.getQName(HippoNodeType.NT_HANDLE);
        hippoFacetResult = npRes.getQName(HippoNodeType.NT_FACETRESULT);
        hippoFacetSearch = npRes.getQName(HippoNodeType.NT_FACETSEARCH);
        hippoFacetSelect = npRes.getQName(HippoNodeType.NT_FACETSELECT);

        // initialize read cache
        int cacheSize = getPermCacheSize();
        if (cacheSize < 0) {
            cacheSize = DEFAULT_PERM_CACHE_SIZE;
        }

        final Set<AuthorizationFilterPrincipal> filterPrincipals = subject.getPrincipals(AuthorizationFilterPrincipal.class);
        if (!filterPrincipals.isEmpty() || userIds.size() != 1) {
            initializeExtendedFacetRules(filterPrincipals);
        }
        readAccessCache = new HippoAccessCache(cacheSize);
        readVirtualAccessCache = new WeakHashMap<>();

        initializeImplicitReadAccess();

        // we're done
        initialized = true;

        log.info("Initialized HippoAccessManager for user {} with cache size {}", getUserIdAsString(), cacheSize);
    }


    private void initializeExtendedFacetRules(final Set<AuthorizationFilterPrincipal> filterPrincipals) throws RepositoryException {
        extendedFacetRules = new HashMap<String, Collection<QFacetRule>>();
        final Set<FacetAuthPrincipal> facetAuthPrincipals = subject.getPrincipals(FacetAuthPrincipal.class);
        for (AuthorizationFilterPrincipal afp : filterPrincipals) {
            final Map<String, Collection<QFacetRule>> facetRules = afp.getExpandedFacetRules(facetAuthPrincipals);
            for (Map.Entry<String, Collection<QFacetRule>> entry : facetRules.entrySet()) {
                final String domainPath = entry.getKey();
                if (!extendedFacetRules.containsKey(domainPath)) {
                    extendedFacetRules.put(domainPath, new ArrayList<QFacetRule>());
                }
                extendedFacetRules.get(domainPath).addAll(entry.getValue());
            }
        }
    }

    /**
     * @see AccessManager#close()
     */
    public synchronized void close() throws Exception {
        checkInitialized();
        initialized = false;

        // clear out all caches
        implicitReads.clear();
        readAccessCache.clear();
        readVirtualAccessCache.clear();
        //requestItemStateCache.clear();
        groupIds.clear();
        currentDomainRoleIds.clear();

        log.info("Closed HippoAccessManager for user " + getUserIdAsString());
    }

    private String getUserIdAsString() {
        return StringUtils.join(userIds, ',');
    }

    /**
     * @see AccessManager#checkPermission(ItemId, int)
     */
    @Deprecated
    public void checkPermission(ItemId id, int permissions) throws AccessDeniedException, ItemNotFoundException,
            RepositoryException {
        log.warn("checkPermission(ItemId, int) is DEPRECATED!", new RepositoryException(
                "Use of deprecated method checkPermission(ItemId, int)"));
        // just use the isGranted method
        if (!isGranted(id, permissions)) {
            throw new AccessDeniedException();
        }
    }

    public void checkPermission(Path path, int permissions) throws AccessDeniedException, ItemNotFoundException,
            RepositoryException {
        // just use the isGranted method
        if (!isGranted(path, permissions)) {
            throw new AccessDeniedException();
        }
    }

    @Override
    public void checkRepositoryPermission(final int permissions) throws AccessDeniedException, RepositoryException {
        // no repository restrictions yet
    }

    /**
     * @see AccessManager#isGranted(ItemId, int)
     * @deprecated
     */
    public boolean isGranted(final ItemId id, final int permissions) throws RepositoryException {
        checkInitialized();

        if (permissions != Permission.READ) {
            log.warn("isGranted(ItemId, int) is DEPRECATED!", new RepositoryException(
                    "Use of deprecated method isGranted(ItemId, int)"));
        }

        if (isSystem) {
            return true;
        }

        // handle properties
        if (!id.denotesNode()) {
            if (permissions == Permission.REMOVE_PROPERTY) {
                // Don't check remove on properties. A write check on the node itself is done.
                return true;
            } else if (permissions == Permission.SET_PROPERTY) {
                // A write check on the parent will be done.
                return true;
            }
            return isGranted(((PropertyId) id).getParentId(), permissions);
        }

        // fast track common read check
        if (permissions == Permission.READ) {
            return canRead((NodeId) id);
        }

        // not a read, remove node from read cache since the action might change the read access for the node
        removeAccessFromCache((NodeId) id);

        return isGranted(hierMgr.getPath(id), permissions);
    }

    /**
     * @see AccessManager#isGranted(Path, int)
     */

    public boolean isGranted(final Path absPath, final int permissions) throws RepositoryException {
        checkInitialized();

        if (!absPath.isAbsolute()) {
            throw new RepositoryException("Absolute path expected");
        }
        if (isSystem) {
            return true;
        }
        if (log.isInfoEnabled()) {
            log.info("Checking [{}] for user {} absPath: {}", permsString(permissions),
                    getUserIdAsString(), npRes.getJCRPath(absPath));
        }

        // fasttrack read permissions check
        if (permissions == Permission.READ) {
            return canRead(absPath);
        }

        // part of combined permissions check
        if ((permissions & Permission.READ) != 0) {
            if (!canRead(absPath)) {
                // all permissions must be matched
                return false;
            }
        }

        // translate permissions to privileges according to 6.11.1.4 of JSR-283
        Set<Privilege> privileges = new HashSet<Privilege>();
        if ((permissions & Permission.ADD_NODE) != 0) {
            privileges.add(privilegeFromName("jcr:addChildNodes"));
        }
        if ((permissions & Permission.REMOVE_NODE) != 0) {
            privileges.add(privilegeFromName("jcr:removeChildNodes"));
        }
        if ((permissions & Permission.SET_PROPERTY) != 0) {
            privileges.add(privilegeFromName("jcr:setProperties"));
        }
        if ((permissions & Permission.REMOVE_PROPERTY) != 0) {
            privileges.add(privilegeFromName("jcr:setProperties"));
        }
        if (privileges.isEmpty()) {
            return true;
        }
        return hasPrivileges(absPath.getAncestor(1), privileges.toArray(new Privilege[privileges.size()]));
    }

    /**
     * Just forwards the call to <code>isGranted(Path,int)</code>
     * @see HippoAccessManager#isGranted(Path, int)
     * @see AccessManager#isGranted(Path, Name, int)
     */

    public boolean isGranted(final Path parentPath, final Name childName, final int permissions) throws RepositoryException {
        Path p = PathFactoryImpl.getInstance().create(parentPath, childName, true);
        return isGranted(p, permissions);
    }

    private boolean canRead(Path absPath) throws RepositoryException {
        checkInitialized();

        // allow everything to the system user
        if (isSystem) {
            return true;
        }

        // find the id
        NodeId id = getNodeId(absPath);
        if (id == null) {
            // this usually happens in clustered environments when the node has been
            // deleted before the cluster addnode event is received.
            log.debug("Unable to find node id, allowing read permissions: {}", absPath);
            return true;
        }

        return canRead(id);
    }

    /**
     * Always allow.
     * @see AccessManager#canAccess(String)
     */

    public boolean canAccess(String workspaceName) throws NoSuchWorkspaceException, RepositoryException {
        processCreatedDestroyedNodeIds();
        // no workspace restrictions yet
        return true;
    }

    public Set<NodeId> getImplicitReads() {
        return implicitReads;
    }

    public long getReInitCounter() {
        return reInitCounter;
    }

    public Set<QFacetRule> getFacetRules(final DomainRule domainRule) {
        if (extendedFacetRules != null) {
            final String domainRulePath = domainRule.getDomainName() + "/" + domainRule.getName();
            final Collection<QFacetRule> extendedRules = extendedFacetRules.get(domainRulePath);
            if (extendedRules != null) {
                final Set<QFacetRule> facetRules = new HashSet<>(domainRule.getFacetRules());
                facetRules.addAll(extendedRules);
                return facetRules;
            }
        }
        return domainRule.getFacetRules();
    }

    //---------------------------------------- Methods ---------------------------------------------//
    /**
     * Check whether a user can read the node with the given id
     *
     * @param id the id of the node to check
     * @return true if the user is allowed to read the node
     * @throws RepositoryException
     */
    private boolean canRead(NodeId id) throws RepositoryException {
        if (isSystem) {
            return true;
        }

        processCreatedDestroyedNodeIds();

        // check cache
        Boolean allowRead = getAccessFromCache(id);
        if (allowRead != null) {
            return allowRead;
        }

        // because the getItemState(id) call below will recursively call us (canRead(id)) again
        // we allow that call to succeed here by caching read access in the inprocessNodeReadAccess instance variable,
        // which will be returned from getAccessFromCache(NodeId) if set instead of looking it up in the
        // backing cache. Note: the HippoAccessManager itself is accessed thread safe.
        // This way we can then use the item state to do the work of determining if the read access is indeed allowed
        // after which we put the real result in the cache before returning.
        // if we wouldn't do this we'd have an infinite loop on our hands
        try {
            inprocessNodeReadAccess.add(id);

            if (log.isDebugEnabled()) {
                log.debug("Checking canRead for node: {}", npRes.getJCRPath(hierMgr.getPath(id)));
            }

            NodeState nodeState;
            try {
                nodeState = (NodeState) getItemState(id);
            } catch (NoSuchItemStateException e) {
                log.info("Node with id '{}' not found, denying access", id);
                log.debug("Trace: ", e);
                removeAccessFromCache(id);
                return false;
            }
            if (nodeState.getStatus() == NodeState.STATUS_NEW && !(nodeState.getId() instanceof HippoNodeId)) {
                // allow read to new nodes in own session
                // the write check is done on save
                addAccessToCache(id, true);
                return true;
            }

            // make sure all parent nodes are readable
            // if node is not readable because of parent, don't cache as we can't invalidate
            if (!rootNodeId.equals(id) && !(id instanceof HippoNodeId)) {
                if (!canRead(nodeState.getParentId())) {
                    removeAccessFromCache(id);
                    return false;
                }
            }

            Set<FacetAuthPrincipal> faps = subject.getPrincipals(FacetAuthPrincipal.class);
            for (FacetAuthPrincipal fap : faps) {
                Set<String> privs = fap.getPrivileges();
                if (privs.contains("jcr:read")) {
                    if (isNodeInDomain(nodeState, fap, true)) {
                        addAccessToCache(id, true);
                        return true;
                    }
                }
            }

            addAccessToCache(id, false);
            if (log.isInfoEnabled()) {
                log.info("DENIED read : {}", npRes.getJCRPath(hierMgr.getPath(id)));
            }
            return false;
        } finally {
            inprocessNodeReadAccess.remove(id);
        }
    }

    private Boolean getAccessFromCache(NodeId id) {
        if (inprocessNodeReadAccess.contains(id)) {
            return Boolean.TRUE;
        }
        if (implicitReads.contains(id)) {
            return Boolean.TRUE;
        }
        if (id instanceof HippoNodeId) {
            return readVirtualAccessCache.get(id);
        } else {
            return readAccessCache.get(id);
        }
    }

    private void addAccessToCache(NodeId id, boolean value) {
        if (id instanceof HippoNodeId) {
            readVirtualAccessCache.put((HippoNodeId) id, value);
        } else {
            readAccessCache.put(id, value);
        }
    }

    private void removeAccessFromCache(NodeId id) {
        if (id instanceof HippoNodeId) {
            readVirtualAccessCache.remove(id);
        } else {
            readAccessCache.remove(id);
        }
    }

    /**
     * Try to read the cache size from the configuration
     * @return the size or -1 when not found
     */
    private int getPermCacheSize() {
        String configPath = "/" + HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.SECURITY_PATH + "/"
                + HippoNodeType.ACCESSMANAGER_PATH + "/" + HippoNodeType.HIPPO_PERMISSIONCACHESIZE;
        try {
            PathResolver resolver = new ParsingPathResolver(PathFactoryImpl.getInstance(), npRes);
            Path path = resolver.getQPath(configPath);
            PropertyId confId = hierMgr.resolvePropertyPath(path);
            if (confId != null) {
                PropertyState state = (PropertyState) getItemState(confId);
                InternalValue[] iVals = state.getValues();
                if (iVals.length > 0 && ((int) iVals[0].getLong()) > 0) {
                    return (int) iVals[0].getLong();
                }
            }
        } catch (NoSuchItemStateException e) {
            // not configured, expected
            log.info("Configuration for permission cache not found at {}", configPath);
        } catch (RepositoryException e) {
            // too bad.. no correct config found
            log.error("Error while retrieving permission cache at " + configPath, e);
        }
        return -1;
    }

    /**
     * Check if a node is in the domain of the facet auth principal by looping of all the
     * domain rules. For each domain all the facet rules are checked.
     *
     * @param nodeState the state of the node to check
     * @param fap       the facet auth principal to check
     * @param checkRead
     * @return true if the node is in the domain of the facet auth
     * @throws RepositoryException
     * @see FacetAuthPrincipal
     */
    private boolean isNodeInDomain(final NodeState nodeState,
                                   final FacetAuthPrincipal fap,
                                   final boolean checkRead) throws RepositoryException {
        log.trace("Checking if node : {} is in domain of {}", nodeState.getId(), fap);
        boolean isInDomain = false;

        currentDomainRoleIds.clear();
        currentDomainRoleIds.addAll(fap.getRoles());

        // check is node matches ONE of the domain rules
        for (DomainRule domainRule : fap.getRules()) {

            boolean allRulesMatched = true;

            // no facet rules means no match
            final Set<QFacetRule> facetRules = getFacetRules(domainRule);
            if (facetRules.isEmpty()) {
                allRulesMatched = false;
                log.debug("No facet rules found for : {} in domain rule: {}", nodeState.getId(), domainRule);
            }
            // check if node matches ALL of the facet rules
            for (QFacetRule facetRule : facetRules) {
                if (!matchFacetRule(nodeState, facetRule)) {
                    allRulesMatched = false;
                    log.trace("Rule doesn't match for : {} facet rule: {}", nodeState.getId(), facetRule);
                    break;
                }
            }
            if (allRulesMatched) {
                // a match is found, don't check other domain rules;
                isInDomain = true;
                log.debug("Node :  {} found in domain {} match {}", nodeState.getId(), fap.getName(), domainRule);
                break;
            } else {
                // check if node is part of a hippo:document
                NodeState docState = null;
                try {
                    docState = getParentDoc(nodeState);
                } catch (NoSuchItemStateException e) {
                    log.error("Unable to retrieve parent state of node with id " + nodeState.getId(), e);
                }
                if (docState != null) {
                    if (checkRead) {
                        Boolean allowRead = getAccessFromCache(docState.getNodeId());
                        if (allowRead != null) {
                            return allowRead;
                        }
                    }
                    return isNodeInDomain(docState, fap, checkRead);
                }
            }
        }
        return isInDomain;
    }

    /**
     * Check if a node matches the current QFacetRule
     * @param nodeState the state of the node to check
     * @param facetRule the facet rule to check
     * @return true if the node matches the facet rule
     * @throws RepositoryException
     * @see org.hippoecm.repository.security.domain.QFacetRule
     */
    private boolean matchFacetRule(NodeState nodeState, QFacetRule facetRule) throws RepositoryException {
        log.trace("Checking node : {} for facet rule: {}", nodeState.getId(), facetRule);

        if (facetRule.isReferenceRule() && !facetRule.referenceExists()) {
            if (facetRule.isEqual()) {
                log.trace("The reference '{}' does not (yet) exist. Since the facet rule indicates that it must be " +
                        "equal, the result is that the nodeState does not match this facet rule");
                return false;
            } else {
                log.trace("The reference '{}' does not (yet) exist. Since the facet rule indicates that it must be " +
                        "not equal, the result is that the nodeState does match this facet rule");
                return true;
            }
        }

        // is this a 'NodeType' facet rule?
        if (facetRule.getFacet().equalsIgnoreCase("nodetype")) {
            boolean match = false;
            log.trace("Checking node : {} for nodeType: {}", nodeState.getId(), facetRule);
            if (isInstanceOfType(nodeState, facetRule.getValue())) {
                match = true;
                log.trace("Found match : {} for nodeType: {}", nodeState.getId(), facetRule.getValue());
            } else if (hasMixinWithValue(nodeState, facetRule)) {
                match = true;
                log.trace("Found match : {} for mixinType: {}", nodeState.getId(), facetRule.getValue());
            }
            if (facetRule.isEqual()) {
                return match;
            } else {
                return !match;
            }
        }

        // is this a 'NodeName' facet rule?
        if (facetRule.getFacet().equalsIgnoreCase("nodename")) {
            boolean match = false;
            if (facetRule.getType() == PropertyType.NAME) {
                log.trace("Checking node : {} for nodename: {}", nodeState.getNodeId(), facetRule);
                Name nodeName = getNodeName(nodeState);
                if (FacetAuthConstants.EXPANDER_USER.equals(facetRule.getValue())) {
                    if (isUser && userIds.contains(npRes.getJCRName(nodeName))) {
                        match = true;
                    }
                } else if (FacetAuthConstants.EXPANDER_GROUP.equals(facetRule.getValue())) {
                    if (isUser && groupIds.contains(npRes.getJCRName(nodeName))) {
                        match = true;
                    }
                } else if (nodeName.equals(facetRule.getValueName())) {
                    match = true;
                }
            }
            if (facetRule.isEqual()) {
                return match;
            } else {
                return !match;
            }
        }

        if (NameConstants.JCR_UUID.equals(facetRule.getFacetName())) {
            boolean uuidMatch = false;
            log.trace("Checking node : {} for matching jcr:uuid with : {}", nodeState.getId(), facetRule);
            if (nodeState.getNodeId().toString().equals(facetRule.getValue())) {
                uuidMatch = true;
            }
            if (facetRule.isEqual()) {
                return uuidMatch;
            } else {
                return !uuidMatch;
            }
        }

        if (NameConstants.JCR_PATH.equals(facetRule.getFacetName())) {
            boolean uuidMatch = false;
            log.trace("Checking node : {} for matching jcr:path with : {}", nodeState.getId(), facetRule);
            try {
                NodeState current = nodeState;
                for (; ; ) {
                    if (current.getNodeId().toString().equals(facetRule.getValue())) {
                        uuidMatch = true;
                        break;
                    }
                    if (current.getParentId() == null) {
                        // no match
                        break;
                    }
                    if (current.getParentId().toString().equals(facetRule.getValue())) {
                        uuidMatch = true;
                        break;
                    }
                    // since we check current nodeId and parent NodeId, we can go two states up in next loop
                    NodeState parent = getParentState(current);
                    if (parent.getParentId() == null) {
                        // no match
                        break;
                    }
                    current = getParentState(parent);
                }

            } catch (NoSuchItemStateException e) {
                // return false, regardless facetRule.isEqual() or not. Namely some repository exception must have happened
                // in getParentState because we check before calling getParentState(parent); whether parent.getParentId() == null
                return false;
            }

            if (facetRule.isEqual()) {
                return uuidMatch;
            } else {
                return !uuidMatch;
            }
        }

        // check if node has the required property value
        if (matchPropertyWithFacetRule(nodeState, facetRule)) {
            log.trace("Found match : {} for facetVal: {}", nodeState.getId(), facetRule);
            return true;
        }
        return false;
    }

    /**
     * Helper function to resolve the name of the node of the nodestate. It tries to lookup the name
     * first in the normal hierarchy manager and second in the zombie hierarchy manager.
     * @param nodeState
     * @return the Name
     * @throws ItemNotFoundException when the name can not be found for the nodestate
     */
    private Name getNodeName(NodeState nodeState) throws ItemNotFoundException {
        Name nodeName = getNodeName(nodeState, hierMgr);
        if (nodeName == null) {
            // try zombieHierMgr if regular hierMgr has failed to retrieve node
            nodeName = getNodeName(nodeState, zombieHierMgr);
        }
        if (nodeName != null) {
            return nodeName;
        }
        String msg = "Node with id " + nodeState.getId() + " with status [" + nodeState.getStatus()
                + "] not found in hierarchy manager or zombie hierarchy manager.";
        // due to concurrency it happen that we cannot find the node name since another jcr session might have removed the
        // Node for the nodeState
        log.info(msg);
        throw new ItemNotFoundException(msg);
    }

    /**
     * Helper function to resolve the name of the node of the nodeState in the specified hierarchy manager.
     * @param nodeState
     * @param hierMgr   hierarchy manager to use for resolving the node name
     * @return the Name or null when the name can not be found.
     */
    private Name getNodeName(NodeState nodeState, HierarchyManager hierMgr) {
        try {
            return hierMgr.getName(nodeState.getId());
        } catch (ItemNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("Node with id " + nodeState.getId() + " with status [" + nodeState.getStatus()
                        + "] not found in hierarchy manager " + hierMgr.getClass().getName());
            }
        } catch (RepositoryException e) {
            // hierMgr throws RepositoryException when it encounters an ItemStateException
            // this can indicate data corruption, but if the error is not caught here it will fail the
            // complete QueryImpl.execute() method instead of skipping just the failed node.

            log.warn(
                    "Error while resolving name for node with id " + nodeState.getId() + " with status ["
                            + nodeState.getStatus() + "] not found in hierarchy manager "
                            + hierMgr.getClass().getName(), e);
        }
        return null;
    }

    /**
     * Helper function to check if a nodeState is of a node type or a
     * instance of the node type (sub class)
     *
     * @param nodeState the node to check
     * @param nodeType  the node type name
     * @return boolean
     * @throws NoSuchNodeTypeException
     */
    private boolean isInstanceOfType(NodeState nodeState, String nodeType) throws NamespaceException,
            NoSuchNodeTypeException, RepositoryException {
        // create NodeType of nodeState's primaryType
        String nodeStateType = npRes.getJCRName(nodeState.getNodeTypeName());

        if (nodeStateType.equals(nodeType)) {
            if (log.isTraceEnabled()) {
                log.trace("MATCH " + nodeState.getId() + " is of type: " + nodeType);
            }
            return true;
        }

        Boolean isInstance = ntIOCache.get(nodeStateType, nodeType);
        if (isInstance != null) {
            return isInstance;
        }

        // get iterator over all types
        NodeTypeIterator allTypes = ntMgr.getAllNodeTypes();
        NodeType nodeStateNodeType = ntMgr.getNodeType(nodeStateType);

        // iterate over All NodeTypes untill...
        while (allTypes.hasNext()) {
            NodeType nt = allTypes.nextNodeType();
            // the correct NodeType is found
            if (nt.equals(nodeStateNodeType)) {
                // get all supertypes of the nodeState's primaryType's NodeType
                NodeType[] superTypes = nt.getSupertypes();
                // check if one of the superTypes matches the nodeType
                for (NodeType type : superTypes) {
                    if (type.getName().equals(nodeType)) {
                        ntIOCache.put(nodeStateType, nodeType, true);
                        return true;
                    }
                }
            }
        }
        ntIOCache.put(nodeStateType, nodeType, false);
        return false;
    }

    /**
     * Check if a node matches the current QFacetRule based on a
     * check on the properties of the node.
     * @param nodeState the state of the node to check
     * @param rule      the facet rule to check
     * @return true if the node matches the facet rule
     * @throws RepositoryException
     * @see org.hippoecm.repository.security.domain.QFacetRule
     */
    private boolean matchPropertyWithFacetRule(NodeState nodeState, QFacetRule rule) throws RepositoryException {

        boolean match = false;

        // jcr:primaryType isn't really a property
        if (rule.getFacetName().equals(NameConstants.JCR_PRIMARYTYPE)) {
            // WILDCARD match, jcr:primaryType == *
            if (FacetAuthConstants.WILDCARD.equals(rule.getValue())) {
                match = true;
            } else if (nodeState.getNodeTypeName().equals(rule.getValueName())) {
                match = true;
            } else {
                match = false;
            }

            // invert match on inequality
            if (rule.isEqual()) {
                return match;
            } else {
                return !match;
            }
        }
        else if (rule.getFacetName().equals(NameConstants.JCR_UUID)) {
            return nodeState.getNodeId().toString().equals(rule.getValue()) ? rule.isEqual() : !rule.isEqual();
        }

        // the hierarchy manager is attic aware. The property can also be in the removed properties
        if (!nodeState.hasPropertyName(rule.getFacetName())
                && !nodeState.getRemovedPropertyNames().contains(rule.getFacetName())) {
            log.trace("Node: {} doesn't have property {}", nodeState.getId(), rule.getFacetName());

            // if this is a filter facet rule the property doesn't have to be set
            if (rule.isFacetOptional()) {
                return true;
            }

            if (FacetAuthConstants.WILDCARD.equals(rule.getValue()) && !rule.isEqual()) {
                return true;
            } else {
                return false;
            }
        }

        // Property is set

        // Check WILDCARD match
        if (FacetAuthConstants.WILDCARD.equals(rule.getValue())) {
            if (rule.isEqual()) {
                return true;
            } else {
                return false;
            }
        }

        // Check property value
        PropertyId propertyId = new PropertyId(nodeState.getNodeId(), rule.getFacetName());
        PropertyState state;
        try {
            state = (PropertyState) getItemState(propertyId);
        } catch (NoSuchItemStateException e) {
            log.error(
                    "Unable to retrieve property state of node " + nodeState.getId() + " for propery "
                            + rule.getFacetName() + " although hasProperty() returned true.", e);
            return false;
        }
        InternalValue[] iVals = state.getValues();

        for (InternalValue iVal : iVals) {
            // types must match
            if (iVal.getType() != rule.getType()) {
                continue;
            }

            if (iVal.getType() == PropertyType.STRING) {
                log.trace("Checking facet rule: {} (string) -> {}", rule, iVal.getString());

                // expander matches
                if (FacetAuthConstants.EXPANDER_USER.equals(rule.getValue())) {
                    if (isUser && userIds.contains(iVal.getString())) {
                        match = true;
                        break;
                    }
                }
                if (FacetAuthConstants.EXPANDER_GROUP.equals(rule.getValue())) {
                    if (isUser && groupIds.contains(iVal.getString())) {
                        match = true;
                        break;
                    }
                }
                if (FacetAuthConstants.EXPANDER_ROLE.equals(rule.getValue())) {
                    if (isUser && currentDomainRoleIds.contains(iVal.getString())) {
                        match = true;
                        break;
                    }
                }

                if (iVal.getString().equals(rule.getValue())) {
                    match = true;
                    break;
                }
            } else if (iVal.getType() == PropertyType.NAME) {
                log.trace("Checking facet rule: {} (name) -> {}", rule, iVal.getName());

                if (iVal.getName().equals(rule.getValueName())) {
                    match = true;
                    break;
                }
            }
        }
        if (rule.isEqual()) {
            return match;
        } else {
            // the property is set but the values don't match
            return !match;
        }
    }

    /**
     * Check if the node has a mixin type with a specific value
     * @param nodeState the node to check
     * @param rule      the mixin type to check for.
     * @return true if the node has the mixin type
     * @throws RepositoryException
     */
    private boolean hasMixinWithValue(NodeState nodeState, QFacetRule rule) throws RepositoryException {
        if (!nodeState.hasPropertyName(NameConstants.JCR_MIXINTYPES)) {
            return false;
        }

        PropertyId propertyId = new PropertyId(nodeState.getNodeId(), NameConstants.JCR_MIXINTYPES);
        PropertyState state;
        try {
            state = (PropertyState) getItemState(propertyId);
        } catch (NoSuchItemStateException e) {
            log.error("Unable to retrieve mixins of node " + nodeState.getId(), e);
            return false;
        }
        InternalValue[] iVals = state.getValues();

        for (InternalValue iVal : iVals) {
            // types must match
            if (iVal.getType() == PropertyType.NAME) {

                // WILDCARD match
                if (rule.getValue().equals(FacetAuthConstants.WILDCARD)) {
                    return true;
                }

                log.trace("Checking facetVal: {} (name) -> {}", rule.getValueName(), iVal.getName());
                if (iVal.getName().equals(rule.getValueName())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the <code>NodeId</code> for the absolute path. If the absolute path points
     * to a property return the NodeId of the parent. This method return null instead of
     * throwing a <code>PathNotFoundException</code> for performance reasons.
     * @param absPath the absolute Path
     * @return the NodeId of the node (holding the property) or null when the node not found
     * @throws RepositoryException
     */
    private NodeId getNodeId(Path absPath) throws RepositoryException {

        if (!absPath.isAbsolute()) {
            throw new RepositoryException("Absolute path expected, got " + absPath);
        }

        if (absPath.denotesRoot()) {
            return rootNodeId;
        }

        try {
            ItemId itemId = hierMgr.resolvePath(absPath);
            if (itemId != null) {
                if (itemId instanceof PropertyId) {
                    return ((PropertyId) itemId).getParentId();
                }
                return (NodeId) itemId;
            }
        } catch (RepositoryException e) {
            // fall through and try zombie hierMgr
            if (log.isDebugEnabled()) {
                log.debug("Error while resolving node id of: " + absPath, e);
            }
        }

        try {
            // try zombie parent, probably a property
            ItemId itemId = zombieHierMgr.resolvePath(absPath);
            if (itemId instanceof PropertyId) {
                return ((PropertyId) itemId).getParentId();
            }
            return (NodeId) itemId;
        } catch (RepositoryException e) {
            // fall thru and throw a path not found exception
            if (log.isDebugEnabled()) {
                log.debug("Error while resolving node id of: " + absPath, e);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Unable to resolve the node id from the path " + absPath);
        }
        return null;
    }

    /**
     * Try to get a state from the item manager by first checking the normal states,
     * then checking the transient states and last checking the attic state.
     * @param id the item id
     * @return the item state
     * @throws NoSuchItemStateException when the state cannot be found
     */
    private ItemState getItemState(ItemId id) throws NoSuchItemStateException {
        if (id == null) {
            throw new IllegalArgumentException("ItemId cannot be null");
        }
        if (itemMgr.hasItemState(id)) {
            try {
                return itemMgr.getItemState(id);
            } catch (ItemStateException e) {
                log.debug("Error while trying to get item state from the normal ism of id: " + id, e);
            }
        } else if (itemMgr.hasTransientItemState(id)) {
            try {
                return itemMgr.getTransientItemState(id);
            } catch (ItemStateException e) {
                log.debug("Error while trying to get item state from the transient ism of id: " + id, e);
            }
        } else if (itemMgr.hasTransientItemStateInAttic(id)) {
            try {
                return itemMgr.getAtticItemState(id);
            } catch (ItemStateException e) {
                log.debug("Error while trying to get item state from the attic ism of id: " + id, e);
            } catch (RepositoryException e) {
                log.debug("Error while trying to get item state from the attic ism of id: " + id, e);
            }
        }

        // nothing found...
        String msg = "Item state not found in normal, transient or attic: " + id;
        NoSuchItemStateException e = new NoSuchItemStateException(msg);
        log.debug(msg, e);
        throw e;
    }

    private NodeState getNodeStateQuietly(ItemId id) {
        try {
            final ItemState itemState = getItemState(id);
            if (!itemState.isNode()) {
                log.warn("ItemId '{}' points to a property which is not allowed", id);
                return null;
            }
            return (NodeState) itemState;
        } catch (NoSuchItemStateException e) {
            log.info("Could not get item state for id '{}', must have been removed already", id, e);
            return null;
        }
    }

    /**
     * Get the NodeState of the parent from the nodeState in a transient and attic
     * safe manner.
     * @param nodeState
     * @return the parent nodestate
     * @throws NoSuchItemStateException when the state cannot be found
     */
    private NodeState getParentState(NodeState nodeState) throws NoSuchItemStateException {
        if (rootNodeId.equals(nodeState.getId())) {
            throw new NoSuchItemStateException("RootNode doesn't have a parent state.");
        }
        try {
            NodeId id = nodeState.getParentId();
            if (id == null) {
                // for nodeState's from the attic the parentId() is null, so use path resolving to find parent
                return (NodeState) getItemState(zombieHierMgr.resolveNodePath(zombieHierMgr.getPath(nodeState.getId())
                        .getAncestor(1)));
            }
            return (NodeState) getItemState(id);
        } catch (RepositoryException e) {
            throw new NoSuchItemStateException("Unable to find parent nodeState of node: " + nodeState.getId(), e);
        }
    }

    /**
     * Helper function to find a hippo:document instance node type. This
     * can be used to check for facet authorization on the root of a
     * document (bonzai tree).
     * @param nodeState the node of which to check the parents
     * @return NodeState the parent node state or null
     * @throws NoSuchItemStateException
     * @throws RepositoryException
     */
    private NodeState getParentDoc(NodeState nodeState) throws NoSuchItemStateException, RepositoryException {

        if (log.isTraceEnabled()) {
            log.trace("Checking " + nodeState.getId() + " ntn: " + nodeState.getNodeTypeName()
                    + " for being part of a document model.");
        }
        // check if this is already the root of a document
        try {
            if (isInstanceOfType(nodeState, HippoNodeType.NT_DOCUMENT)
                    || nodeState.getNodeTypeName().equals(hippoHandle)
                    || nodeState.getNodeTypeName().equals(hippoFacetSearch)
                    || nodeState.getNodeTypeName().equals(hippoFacetSelect)) {
                if (log.isTraceEnabled()) {
                    log.trace("Node is already document root: " + nodeState.getNodeTypeName());
                }
                return null;
            }
        } catch (NamespaceException e) {
            log.warn("NamespaceException while trying to get parent doc", e);
            return null;
        } catch (NoSuchNodeTypeException e) {
            log.warn("NoSuchNodeTypeException while trying to get parent doc", e);
            return null;
        }

        // walk up in the hierarchy
        while (!rootNodeId.equals(nodeState.getId())) {
            // shift one up in hierarchy
            nodeState = getParentState(nodeState);

            if (nodeState.getNodeTypeName().equals(hippoHandle)) {
                if (log.isDebugEnabled()) {
                    log.debug("MATCH hippoHandle: " + nodeState.getNodeTypeName());
                }
                return null;
            }
            if (nodeState.getNodeTypeName().equals(hippoFacetSearch)) {
                if (log.isDebugEnabled()) {
                    log.debug("MATCH hippoFacetSearch: " + nodeState.getNodeTypeName());
                }
                return null;
            }
            if (nodeState.getNodeTypeName().equals(hippoFacetSelect)) {
                if (log.isDebugEnabled()) {
                    log.debug("MATCH hippoFacetSelect: " + nodeState.getNodeTypeName());
                }
                return null;
            }
            try {
                if (isInstanceOfType(nodeState, HippoNodeType.NT_DOCUMENT)) {
                    // if the parent is either a handle or a facetresult we are sure we are dealing with a
                    // real document
                    final NodeState parentState = getParentState(nodeState);
                    final Name parentNodeTypeName = parentState.getNodeTypeName();
                    if (parentNodeTypeName.equals(hippoHandle) || parentNodeTypeName.equals(hippoFacetResult)) {
                        if (log.isDebugEnabled()) {
                            log.debug("MATCH hippoDoc: " + nodeState.getNodeTypeName());
                        }
                        return nodeState;
                    } else {
                        return null;
                    }
                }
            } catch (NamespaceException e) {
                log.warn("NamespaceException while trying to get parent doc", e);
                return null;
            } catch (NoSuchNodeTypeException e) {
                log.warn("NoSuchNodeTypeException while trying to get parent doc", e);
                return null;
            }
        }
        return null;
    }


    public boolean canRead(final Path absPath, final ItemId itemId) throws RepositoryException {
        // try itemId first if both parameters are set as it is faster
        if (itemId != null) {
            if (itemId.denotesNode()) {
                return canRead((NodeId) itemId);
            } else {
                return true;
            }
        } else if (absPath != null) {
            return canRead(absPath);
        } else {
            // itemId and absPath are null
            return false;
        }
    }

    @Override
    public void stateCreated(final ItemState created) {
        if (isSystem) {
            return;
        }
        if (!initialized) {
            log.info("States being created during initialization of this AccessManager");
        }

        // since org.apache.jackrabbit.core.state.SessionItemStateManager.stateCreated() filters out 'node' states
        // we have to listen to the created property states as well and then take the parent
        final NodeId nodeId;
        synchronized (createdDestroyedNodeIds) {
            final Set<NodeId> createdNodeIds = createdDestroyedNodeIds.getLeft();
            if (created.isNode()) {
                nodeId = (NodeId) created.getId();
                createdNodeIds.add(nodeId);

            } else {
                nodeId = created.getParentId();
                if (nodeId == null) {
                    // free floating property? Unlikely but java doc says created.getParentId() can return null
                    return;
                }
                createdNodeIds.add(nodeId);
            }

            if (createdNodeIds.size() + createdDestroyedNodeIds.getRight().size() >= PROCESS_CREATED_NODE_IDS_SYNC_LIMIT) {
                log.debug("createdNodeIds exceeded the limit '{}' implying the created node ids will be processed " +
                        "async instead of in process for the JCR Session to which the HippoAccessManager is tied");
                //exceeding limit, process now async the createdNodeIds already
                new Thread(() -> processCreatedDestroyedNodeIds()).start();
            }
        }

    }

    @Override
    public void stateModified(final ItemState modified) {
        if (isSystem) {
            return;
        }
        if (modified.isNode()) {
            removeAccessFromCache((NodeId) modified.getId());
        } else {
            removeAccessFromCache(modified.getParentId());
        }
    }

    @Override
    public void stateDestroyed(final ItemState destroyed) {
        // opposed to stateCreated, for stateDestroyed the NodeStates are not filtered out
        if (isSystem) {
            return;
        }

        if (!initialized) {
            log.info("States being destroyed during initialization of this AccessManager");
        }

        if (destroyed.isNode()) {
            // first remove from implicitReads since implicitReads protects the item to be removed
            // from the read access cache

            final NodeId id = (NodeId) destroyed.getId();
            synchronized (createdDestroyedNodeIds) {
                final Set<NodeId> createdNodeIds = createdDestroyedNodeIds.getLeft();
                final boolean removed = createdNodeIds.remove(id);
                if (removed) {
                    log.trace("Don't need to process the removed id '{}' since it was created but not yet processed");
                } else {
                    createdDestroyedNodeIds.getRight().add(id);
                }

                if (createdNodeIds.size() + createdDestroyedNodeIds.getRight().size() >= PROCESS_CREATED_NODE_IDS_SYNC_LIMIT) {
                    log.debug("createdNodeIds exceeded the limit '{}' implying the created node ids will be processed " +
                            "async instead of in process for the JCR Session to which the HippoAccessManager is tied");
                    //exceeding limit, process now async the createdNodeIds already
                    new Thread(() -> processCreatedDestroyedNodeIds()).start();
                }
            }

            // remove it from 'implicitReads' such that it also can get removed from the access cache
            implicitReads.remove(id);
            removeAccessFromCache(id);

        }
    }

    @Override
    public void stateDiscarded(final ItemState discarded) {
        if (isSystem) {
            return;
        }
        if (discarded.isNode()) {
            removeAccessFromCache((NodeId) discarded.getId());
        }
    }

    /**
     * Simple Cache for <String, <String,Boolean>> key-value pairs
     */
    private static class NodeTypeInstanceOfCache {

        /**
         * The cache map
         */
        private static Map<String, Map<String, Boolean>> map = new WeakHashMap<String, Map<String, Boolean>>();

        private static NodeTypeInstanceOfCache cache = new NodeTypeInstanceOfCache();

        /**
         * Create a new LRU cache
         */
        private NodeTypeInstanceOfCache() {
        }

        private static NodeTypeInstanceOfCache getInstance() {
            return cache;
        }

        /**
         * Fetch cache value
         * @return cached value or null when not in cache
         */
        synchronized public Boolean get(String type, String instanceOfType) {
            Map<String, Boolean> typeMap = map.get(instanceOfType);
            if (typeMap != null) {
                return typeMap.get(type);
            }
            return null;
        }

        /**
         * Store key-value in cache
         */
        synchronized public void put(String type, String instanceOfType, boolean isInstanceOf) {
            Map<String, Boolean> typeMap = map.get(instanceOfType);
            if (typeMap == null) {
                typeMap = new WeakHashMap<String, Boolean>();
            }
            typeMap.put(type, isInstanceOf);
            map.put(instanceOfType, typeMap);
        }

        /**
         * Remove key-value from cache
         */
        synchronized public void remove(String type, String instanceOfType) {
            Map<String, Boolean> typeMap = map.get(instanceOfType);
            if (typeMap == null) {
                return;
            }

            if (typeMap.containsKey(type)) {
                map.remove(type);
            }
        }

        /**
         * Clear the cache
         */
        synchronized public void clear() {
            map.clear();
        }
    }

    //---------------------------- ACCESS CONTROL MANAGER ---------------------------//

    /**
     * @see AccessControlManager#getSupportedPrivileges(String)
     */
    public Privilege[] getSupportedPrivileges(String absPath) throws PathNotFoundException, RepositoryException {
        checkInitialized();
        checkValidNodePath(absPath);
        // return all known privileges everywhere.
        return currentPrivileges.values().toArray(new Privilege[currentPrivileges.size()]);
    }

    /**
     * @see AccessControlManager#privilegeFromName(String)
     */
    public Privilege privilegeFromName(String privilegeName) throws AccessControlException, RepositoryException {
        checkInitialized();

        if (currentPrivileges.containsKey(privilegeName)) {
            return currentPrivileges.get(privilegeName);
        }

        currentPrivileges.put(privilegeName, new Privilege() {

            private String name;

            public Privilege[] getAggregatePrivileges() {
                return new Privilege[0];
            }

            public Privilege[] getDeclaredAggregatePrivileges() {
                return new Privilege[0];
            }

            public Privilege create(String name) {
                this.name = name;
                return this;
            }

            public String getName() {
                return name;
            }

            public boolean isAbstract() {
                return false;
            }

            public boolean isAggregate() {
                return false;
            }
        }.create(privilegeName));

        return currentPrivileges.get(privilegeName);
    }

    /**
     * @see AccessControlManager#hasPrivileges(String, Privilege[])
     */
    public boolean hasPrivileges(String absPath, Privilege[] privileges) throws PathNotFoundException,
            RepositoryException {
        return hasPrivileges(npRes.getQPath(absPath), privileges);
    }

    /**
     * Check the privileges based on a absolute Path rather than a String representation of a Path
     * @see AccessControlManager#hasPrivileges(String, Privilege[])
     */
    private boolean hasPrivileges(Path absPath, Privilege[] privileges) throws PathNotFoundException,
            RepositoryException {
        checkInitialized();

        // system session can do everything
        if (isSystem) {
            return true;
        }

        // user is always allowed to do nothing
        if (privileges == null || privileges.length == 0) {
            log.debug("No privileges to check for path: {}.", npRes.getJCRPath(absPath));
            return true;
        }

        // get the id of the node or of the parent node if absPath points to a property
        NodeId id = getNodeId(absPath);
        if (id == null) {
            //throw new PathNotFoundException("Node id not found for path: " + absPath);
            return true;
        }

        // fast track read check
        if (privileges.length == 1 && "jcr:read".equals(privileges[0].getName())) {
            return canRead(id);
        }
        // if virtual nodes can be read any operation on them is allowed
        if (id instanceof HippoNodeId) {
            return canRead(id);
        }


        processCreatedDestroyedNodeIds();


        NodeState nodeState;
        try {
            nodeState = (NodeState) getItemState(id);
        } catch (NoSuchItemStateException e) {
            throw new PathNotFoundException("Path not found " + npRes.getJCRPath(absPath), e);
        }

        if (nodeState.getStatus() == NodeState.STATUS_NEW) {
            return true;
        }

        for (Privilege priv : privileges) {
            if (log.isDebugEnabled()) {
                log.debug("Checking [{}] : {}", priv.getName(), npRes.getJCRPath(absPath));
            }
            boolean allowed = false;
            for (FacetAuthPrincipal fap : subject.getPrincipals(FacetAuthPrincipal.class)) {
                if (log.isDebugEnabled()) {
                    log.debug("Checking [" + priv + "] : " + absPath + " against FacetAuthPrincipal: " + fap);
                }

                if (fap.getPrivileges().contains(priv.getName())) {
                    if (isNodeInDomain(nodeState, fap, false)) {
                        allowed = true;
                        if (log.isInfoEnabled()) {
                            log.info("GRANT: " + priv.getName() + " to user " + getUserIdAsString() + " in domain " + fap + " for "
                                    + npRes.getJCRPath(absPath));
                        }
                        if (priv.getName().equals("jcr:setProperties")) {
                            // because the action setProperties is checked, and a property change can result in
                            // read-access being changed, we need to remove the property from the read cache
                            removeAccessFromCache(id);
                        }
                        break;
                    }
                }
            }
            if (!allowed) {
                if (log.isInfoEnabled()) {
                    log.info("DENY: " + priv.getName() + " to user " + getUserIdAsString() + " for " + npRes.getJCRPath(absPath));
                }
                return false;
            }
        }
        return true;
    }

    /**
     * This gives read access to the ancestry of jcr:path hierarchical facet rule constraints if needed. The goal is
     * that if a domain for example is about giving certain privileges to certain users below a certain node, say below
     * /a/b/c, that the users get implicit read access for /a/b.
     */
    private void initializeImplicitReadAccess() throws RepositoryException {
        final Set<FacetAuthPrincipal> faps = subject.getPrincipals(FacetAuthPrincipal.class);
        for (FacetAuthPrincipal fap : faps) {
            final Set<String> privs = fap.getPrivileges();
            if (!privs.contains("jcr:read")) {
                continue;
            }
            for (DomainRule domainRule : fap.getRules()) {
                initializeImplicitReadAccess(domainRule, fap);
            }
        }
    }

    private synchronized void reInitializeImplicitReadAccess() throws RepositoryException {
        implicitReads.clear();
        readAccessCache.clear();
        initializeImplicitReadAccess();
        reInitCounter++;
    }

    private synchronized void initializeImplicitReadAccess(final DomainRule domainRule, final FacetAuthPrincipal fap) {
        // use the possibly extended facet rules!!
        final Set<QFacetRule> facetRules = getFacetRules(domainRule);
        for (QFacetRule qFacetRule : facetRules) {
            if (qFacetRule.isHierarchicalWhiteListRule()) {

                if (qFacetRule.isReferenceRule() && !qFacetRule.referenceExists()) {
                    continue;
                }

                final String path = qFacetRule.getPathReference();
                if (path == null) {
                    continue;
                }

                final NodeId nodeId;

                try {
                    nodeId = getNodeId(npRes.getQPath(path));
                } catch (Exception e) {
                    log.error("Exception while trying to fetch nodeId for '{}'. ", path, e);
                    continue;
                }

                if (nodeId != null) {
                    // apply implicit read access to ancestry of nodeId
                    final NodeState nodeState = getNodeStateQuietly(nodeId);
                    if (nodeState == null) {
                        continue;
                    }

                    try {
                        if (!isNodeInDomain(nodeState, fap, true)) {
                            log.debug("Do not give read access to ancestry since the referenced path is not included " +
                                    "in the domain due some other facet rule, and as a result, the ancestry should " +
                                    "not get implicit read access");
                            continue;
                        }
                    } catch (RepositoryException e) {
                        log.error("Exception while testing itemState for '{}' was in domain.", nodeId, e);
                        continue;
                    }

                    setReadAllowedAncestry(nodeState);
                }
            }
        }
    }

    private void setReadAllowedAncestry(final NodeState currentState) {
        final NodeId parentId = currentState.getParentId();
        if (parentId == null) {
            return;
        }

        if (implicitReads.contains(parentId)) {
            // ancestry is already taken care off by another domain/facet rule
            return;
        }
        implicitReads.add(parentId);
        final NodeState parentState = getNodeStateQuietly(parentId);
        if (parentState == null) {
            return;
        }
        setReadAllowedAncestry(parentState);
    }

    private void processCreatedDestroyedNodeIds() {
        if (isSystem) {
            return;
        }
        synchronized (createdDestroyedNodeIds) {

            final Set<NodeId> createdNodeIds = createdDestroyedNodeIds.getLeft();
            final Set<NodeId> destroyedNodeIds = createdDestroyedNodeIds.getRight();
            if (createdNodeIds.size() == 0 && destroyedNodeIds.size() == 0) {
                return;
            }

            final long start = System.currentTimeMillis();
            // final array to be able to use the reInit var in lambda's
            final boolean[] reInit = {false};

            for (NodeId destroyedNodeId : destroyedNodeIds) {
                final String destroyedUuid = destroyedNodeId.toString();
                // go through all QFacetRule and every QFacetRule that has QFacetRule#isReferenceRule is true and
                // has a value equal to destroyedUuid, should get from its value (or value(s) in future if we support
                // wildcard path references) the destroyed UUID removed
                final Set<FacetAuthPrincipal> faps = subject.getPrincipals(FacetAuthPrincipal.class);

                // for every QFacetRule for the current user that is a 'reference rule' and the reference points to
                // the removed node, remove the uuid from the value
                faps.forEach(fap -> fap.getRules().forEach(domainRule -> {
                    domainRule.getFacetRules().forEach(qFacetRule -> {
                        if (qFacetRule.isReferenceRule() && destroyedUuid.equals(qFacetRule.getValue())) {
                            log.info("QFacetRule '{}' referenced removed node. Remove the value such that it " +
                                    "becomes a 'missing reference QFacetRule'", qFacetRule);
                            qFacetRule.setUUIDReference(null);
                            reInit[0] = true;
                        }
                    });
                }));
            }

            if (!createdNodeIds.isEmpty()) {

                for (NodeId createdNodeId : createdNodeIds) {
                    try {

                        final String path = getPathWithoutAccessTest(createdNodeId);

                        final Set<FacetAuthPrincipal> faps = subject.getPrincipals(FacetAuthPrincipal.class);
                        faps.forEach(fap -> fap.getRules().forEach(domainRule -> {
                            domainRule.getFacetRules().forEach(qFacetRule -> {
                                if (qFacetRule.isReferenceRule() && path.equals(qFacetRule.getPathReference())) {
                                    log.info("QFacetRule '{}' referenced node has been created. Set the value of the " +
                                            "UUID on the QFacetRule", qFacetRule);
                                    qFacetRule.setUUIDReference(UUID.fromString(createdNodeId.toString()));
                                    reInit[0] = true;
                                }
                            });
                        }));

                    } catch (PathNotFoundException | ItemStateException e) {
                        log.debug("Created Node Id already removed again");
                    } catch (RepositoryException e) {
                        log.error("Exception while processingCreatedDestroyedNodeIds createdNodeIds", e);
                    }
                }
            }

            final int numberCreated = createdNodeIds.size();
            final int numberDestroyed = destroyedNodeIds.size();
            createdNodeIds.clear();
            destroyedNodeIds.clear();

            if (reInit[0]) {
                try {
                    reInitializeImplicitReadAccess();
                } catch (RepositoryException e) {
                    log.error("Exception while initializing implicit read access", e);
                }
            }

            log.error("processing {} created nodes and {} destroyed nodes took {} ms to complete{}",
                    numberCreated, numberDestroyed, System.currentTimeMillis() - start,
                    reInit[0] ? ", including a reinitialization of implicit reads" : ".");
        }
    }

    /**
     *    We need to find the path for {@code nodeId}. Unfortunately just using this:
     *    <code>
     *        <pre>
     *              final Path path = hierMgr.getPath(createdNodeId);
     *              final String pathString = npRes.getJCRPath(path);
     *        </pre>
     *    </code>
     *    fails because the 'hierMgr.getPath' results in recursion because it uses
     *    org.hippoecm.repository.jackrabbit.HippoLocalItemStateManager#getCanonicalItemState(ItemId) which in turn
     *    invokes accessManager.isGranted(id, AccessManager.READ) which triggers again processCreatedDestroyedNodeIds.
     *    Just skipping the 'processCreatedDestroyedNodeIds' when recursion is detected is not possible since the
     *    'processCreatedDestroyedNodeIds' can impact the 'read access' for newly created nodes.
     *    Therefor, we have to resolve to the shared item state manager and resolve the path 'manually' instead of via
     *    npRes.getJCRPath(path);
     */
    private String getPathWithoutAccessTest(final NodeId nodeId)
            throws ItemStateException, NamespaceException, RepositoryException {

        final StringBuilder pathBuilder = new StringBuilder();
        final SharedItemStateManager sharedItemStateManager = itemMgr.getSharedItemStateManager();
        final ItemState itemState = sharedItemStateManager.getItemState(nodeId);

        if (itemState.getId().equals(rootNodeId)) {
            return "";
        }
        if (!(itemState instanceof NodeState)) {
            throw new RepositoryException("Expected Node for NodeId");
        }
        populatePath(pathBuilder, (NodeState) itemState, sharedItemStateManager);
        return pathBuilder.toString();

    }

    private void populatePath(final StringBuilder pathBuilder, final NodeState nodeState, final SharedItemStateManager sharedItemStateManager)
            throws ItemStateException, NamespaceException, PathNotFoundException {

        if (nodeState.getId().equals(rootNodeId)) {
            return;
        }
        // item got its parent (concurrently) removed and is 'free floating'
        if (nodeState.getParentId() == null) {
            throw new PathNotFoundException("Cannot construct path since parent is null");
        }
        final NodeState parentState = (NodeState) sharedItemStateManager.getItemState(nodeState.getParentId());

        final ChildNodeEntry childNodeEntry = parentState.getChildNodeEntry(nodeState.getNodeId());
        if (childNodeEntry == null) {
            // child has already been removed again (by other session)
            throw new PathNotFoundException("Cannot construct path since node has been removed already");
        }
        final Name name = childNodeEntry.getName();

        final String jcrName = npRes.getJCRName(name);
        pathBuilder.insert(0, jcrName).insert(0, "/");

        populatePath(pathBuilder, parentState, sharedItemStateManager);
    }

    /**
     * @see AccessControlManager#getPrivileges(String)
     */
    public Privilege[] getPrivileges(String absPath) throws PathNotFoundException, RepositoryException {
        checkInitialized();

        if (!isSystem) {
            processCreatedDestroyedNodeIds();
        }

        NodeId id = getNodeId(npRes.getQPath(absPath));
        if (id == null) {
            throw new PathNotFoundException("Node id not found for path: " + absPath);
        }
        NodeState nodeState;
        try {
            nodeState = (NodeState) getItemState(id);
        } catch (NoSuchItemStateException e) {
            throw new PathNotFoundException("NodeState not found for id " + id + " path: " + absPath);
        }

        Set<Privilege> privileges = new HashSet<Privilege>();
        for (FacetAuthPrincipal fap : subject.getPrincipals(FacetAuthPrincipal.class)) {
            if (isNodeInDomain(nodeState, fap, false)) {
                for (String privilegeName : fap.getPrivileges()) {
                    privileges.add(privilegeFromName(privilegeName));
                }
            }
        }
        return privileges.toArray(new Privilege[privileges.size()]);
    }

    /**
     * Always return empty array of <code>AccessControlPolicy</code>
     *
     * @see AccessControlManager#getPolicies(String)
     */
    public AccessControlPolicy[] getPolicies(String absPath) throws PathNotFoundException, AccessDeniedException,
            RepositoryException {
        checkInitialized();
        //checkPrivileges(absPath, PrivilegeRegistry.READ_AC);

        log.debug("Implementation does not provide applicable policies -> getPolicy() always returns an empty array.");
        return new AccessControlPolicy[0];
    }

    /**
     * Always return empty array of <code>AccessControlPolicy</code>
     *
     * @see AccessControlManager#getEffectivePolicies(String)
     */
    public AccessControlPolicy[] getEffectivePolicies(String absPath) throws PathNotFoundException,
            AccessDeniedException, RepositoryException {
        return new AccessControlPolicy[0];
    }

    /**
     * Always return <code>AccessControlPolicyIteratorAdapter.EMPTY</code>
     *
     * @see AccessControlManager#getApplicablePolicies(String)
     */
    public AccessControlPolicyIterator getApplicablePolicies(String absPath) throws PathNotFoundException,
            AccessDeniedException, RepositoryException {
        checkInitialized();
        //checkPrivileges(absPath, PrivilegeRegistry.READ_AC);
        log.debug("Implementation does not provide applicable policies -> returning empty iterator.");
        return AccessControlPolicyIteratorAdapter.EMPTY;
    }

    /**
     * Always throws <code>AccessControlException</code>
     *
     * @see AccessControlManager#setPolicy(String, AccessControlPolicy)
     */
    public void setPolicy(String absPath, AccessControlPolicy policy) throws PathNotFoundException,
            AccessControlException, AccessDeniedException, RepositoryException {
        checkInitialized();
        //checkPrivileges(absPath, PrivilegeRegistry.MODIFY_AC);
        throw new AccessControlException("AccessControlPolicy " + policy + " cannot be applied.");
    }

    /**
     * Always throws <code>AccessControlException</code>
     *
     * @see AccessControlManager#removePolicy(String, AccessControlPolicy)
     */
    public void removePolicy(String absPath, AccessControlPolicy policy) throws PathNotFoundException,
            AccessControlException, AccessDeniedException, RepositoryException {
        checkInitialized();
        //checkPrivileges(absPath, PrivilegeRegistry.MODIFY_AC);
        throw new AccessControlException("No AccessControlPolicy has been set through this API -> Cannot be removed.");
    }

    //------------------------------ END ACCESS CONTROL MANAGER -------------------------------------------//

    /**
     * Check if this manager has been properly initialized.
     *
     * @throws IllegalStateException If this manager has not been properly initialized.
     */
    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }
    }

    /**
     * Build a qualified path from the specified <code>absPath</code> and test
     * if it is really absolute and points to an existing node.
     *
     * @param absPath
     * @throws PathNotFoundException if no node at <code>absPath</code> exists
     * or the session does not have privilege to retrieve the node.
     * @throws RepositoryException If the given <code>absPath</code> is not
     * absolute or if some other error occurs.
     */
    private void checkValidNodePath(String absPath) throws PathNotFoundException, RepositoryException {
        Path p = npRes.getQPath(absPath);
        if (!p.isAbsolute()) {
            throw new RepositoryException("Absolute path expected " + absPath);
        }
        if (hierMgr.resolveNodePath(p) == null) {
            throw new PathNotFoundException("No such node " + absPath);
        }
    }

    /**
     * Helper method for pretty printing the requested permission
     * @param permissions
     * @return a string representation of the permissions
     */
    private String permsString(int permissions) {
        StringBuilder sb = new StringBuilder();

        // narrow down permissions
        if ((permissions & Permission.READ) != 0) {
            sb.append("re");
        } else {
            sb.append("--");
        }
        if ((permissions & Permission.ADD_NODE) != 0) {
            sb.append("an");
        } else {
            sb.append("--");
        }
        if ((permissions & Permission.REMOVE_NODE) != 0) {
            sb.append("rn");
        } else {
            sb.append("--");
        }
        if ((permissions & Permission.SET_PROPERTY) != 0) {
            sb.append("sp");
        } else {
            sb.append("--");
        }
        if ((permissions & Permission.REMOVE_PROPERTY) != 0) {
            sb.append("rp");
        } else {
            sb.append("--");
        }
        return sb.toString();
    }
}
