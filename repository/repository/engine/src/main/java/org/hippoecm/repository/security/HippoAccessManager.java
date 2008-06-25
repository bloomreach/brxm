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
package org.hippoecm.repository.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.security.auth.Subject;

import org.apache.commons.collections.map.LRUMap;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.UserPrincipal;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.CachingNameResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.conversion.ParsingNameResolver;
import org.apache.jackrabbit.spi.commons.conversion.ParsingPathResolver;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.jackrabbit.HippoHierarchyManager;
import org.hippoecm.repository.jackrabbit.HippoPropertyId;
import org.hippoecm.repository.jackrabbit.HippoSessionItemStateManager;
import org.hippoecm.repository.security.domain.DomainRule;
import org.hippoecm.repository.security.domain.FacetRule;
import org.hippoecm.repository.security.principals.FacetAuthPrincipal;
import org.hippoecm.repository.security.principals.GroupPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * HippoAccessManager based on facet authorization. A subject (user)
 * has a set of {@link FacetAuth}s which hold the domain configuration
 * as defined by a set of {@link DomainRule}s, the roles the subject has
 * for the domain and the JCR permissions the subject has for the domain.
 *
 * For checking if a subject has specific permissions on a item (property), the permissions
 * of the subject on the parent node are checked.
 *
 * The HippoAccessManager also checks if the node is part of a hippo:document in
 * which case the hippo:document is also checked for permissions when the subject
 * does not have the correct permissions on the node itself. If the subject does
 * have the correct permissions on the hippo:document the permissions on the node
 * are granted.
 *
 */
public class HippoAccessManager implements AccessManager {

    /** SVN id placeholder */
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /**
     * Subject whose access rights this AccessManager should reflect
     */
    private Subject subject;

    /**
     * hierarchy manager used for ACL-based access control model
     */
    private HippoHierarchyManager hierMgr;

    /**
     * The session item state manager used for fetching transient and attic item states
     */
    private HippoSessionItemStateManager itemMgr;

    /**
     * NodeTypeManager for resolving superclass node types
     */
    private NodeTypeManager ntMgr;

    /**
     * NamespaceResolver
     */
    private NamespaceResolver nsRes;
    
    /**
     * NameResolver
     */
    private NameResolver nRes;

    /**
     * Hippo Namespace, TODO: move to better place
     */
    public final static String NAMESPACE_URI = "http://www.hippoecm.org/nt/1.0";

    /**
     * Name of hippo:handle, needed for document model checking
     */
    private Name hippoHandle;

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
     * State of the accessManager
     */
    private boolean initialized;

    /**
     * SuperSimpleLRUCache
     * TODO: handle multiple users, multiple session (perhaps move to ISM?)
     */
    private PermissionLRUCache readAccessCache;
    
    
    private static final int DEFAULT_PERM_CACHE_SIZE = 2000;

    /**
     * Flag whether current user is anonymous
     */
    @SuppressWarnings("unused")
    private boolean isAnonymous;

    /**
     * Flag whether current user is a regular user
     */
    private boolean isUser;

    /**
     * Flag whether the current user is a system user
     */
    private boolean isSystem;

    /**
     * The userId of the logged in user
     */
    private String userId;

    private List<String> groupIds = new ArrayList<String>();
    private List<String> currentDomainRoleIds = new ArrayList<String>();
    
    /**
     * A ItemId->ItemState cache that is only valid during a single isGranted.
     * Some property states are resolved many times and this can be expensive 
     * especially for states from the attic.
     */
    private HashMap<ItemId, ItemState> requestItemStateCache = new HashMap<ItemId, ItemState>();

    /**
     * The logger
     */
    private static final Logger log = LoggerFactory.getLogger(HippoAccessManager.class);

    /**
     * Empty constructor
     */
    public HippoAccessManager() {
        initialized = false;
        isAnonymous = false;
        isUser = false;
        isSystem = false;
    }

    /**
     * {@inheritDoc}
     */
    public void init(AMContext context) throws AccessDeniedException, Exception {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }
        subject = context.getSubject();
        nsRes = context.getNamespaceResolver();
        hierMgr = (HippoHierarchyManager) context.getHierarchyManager();
        if (context instanceof HippoAMContext) {
            ntMgr = ((HippoAMContext) context).getNodeTypeManager();
            itemMgr = (HippoSessionItemStateManager) ((HippoAMContext) context).getSessionItemStateManager();
        }
        nRes = new CachingNameResolver(new ParsingNameResolver(NameFactoryImpl.getInstance(), nsRes));

        // Shortcuts for checks
        isSystem = !subject.getPrincipals(SystemPrincipal.class).isEmpty();
        isUser = !subject.getPrincipals(UserPrincipal.class).isEmpty();
        isAnonymous = !subject.getPrincipals(AnonymousPrincipal.class).isEmpty();
        

        // prefetch userId
        if (isSystem) {
            userId = subject.getPrincipals(SystemPrincipal.class).iterator().next().getName();
        } else if (isUser) {
            userId = subject.getPrincipals(UserPrincipal.class).iterator().next().getName();
        } else if (isAnonymous) {
            userId = subject.getPrincipals(AnonymousPrincipal.class).iterator().next().getName();
        } else {
            userId = "";
        }

        // prefetch groupId's
        for (GroupPrincipal gp : subject.getPrincipals(GroupPrincipal.class)) {
            groupIds.add(gp.getName());
        }

        // cache root NodeId
        rootNodeId = (NodeId) hierMgr.resolveNodePath(PathFactoryImpl.getInstance().getRootPath());

        hippoHandle = nRes.getQName(HippoNodeType.NT_HANDLE);
        hippoFacetSearch = nRes.getQName(HippoNodeType.NT_FACETSEARCH);
        hippoFacetSelect = nRes.getQName(HippoNodeType.NT_FACETSELECT);

        // initialize read cache
        int cacheSize = getPermCacheSize();
        if (cacheSize < 0) {
            cacheSize = DEFAULT_PERM_CACHE_SIZE;
        }
        log.debug("Setting cache size: {}", cacheSize);
        readAccessCache = new PermissionLRUCache(cacheSize);
        
        // we're done
        initialized = true;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void close() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }
        readAccessCache.clear();
        initialized = false;
    }

    /**
     * Try to read the cache size from the configuration
     * @return the size or -1 when not found
     */
    private int getPermCacheSize() {
        try {
            PathResolver resolver = new ParsingPathResolver(PathFactoryImpl.getInstance(), nRes);
            Path path = resolver.getQPath("/" + HippoNodeType.CONFIGURATION_PATH + "/"
                    + HippoNodeType.ACCESSMANAGER_PATH + "/" + HippoNodeType.HIPPO_PERMCACHESIZE);
            PropertyId confId = hierMgr.resolvePropertyPath(path);
            if (confId != null) {
                PropertyState state = (PropertyState) getItemState(confId);
                InternalValue[] iVals = state.getValues();
                if (iVals.length > 0 && ((int)iVals[0].getLong()) > 0) {
                    return (int) iVals[0].getLong();
                }
            }
        } catch (NoSuchItemStateException e ) {
            // not configured, expected
        } catch (ItemStateException e ) {
            // too bad.. no correct config found
        } catch (RepositoryException e) {
            // too bad.. no correct config found
        }
        return -1;
    }
    
    /**
     * Check whether a user can access the NodeState with the requested permissions
     * @param nodeState the state of the node to check
     * @param permissions the (bitset) of the permissions
     * @return true if the user is allowed the requested permissions on the node
     * @throws RepositoryException
     * @throws ItemStateException
     * @throws NoSuchItemStateException
     */
    protected boolean canAccessNode(NodeState nodeState, int permissions) throws RepositoryException,
            NoSuchItemStateException, ItemStateException {
        // system and admin have all permissions
        if (isSystem) {
            return true;
        }
        // no facetAuths -> not allowed...
        if (subject.getPrincipals(FacetAuthPrincipal.class).isEmpty()) {
            return false;
        }
        // special hippo node types
//        if (isWorkflowConfig(nodeState)) {
//            nodeState = getWorkflowNode(nodeState);
//            if (nodeState != null) {
//                return checkFacetAuth(nodeState, permissions);
//            } else {
//                return false;
//            }
//        }
        // check for facet authorization
        if (checkFacetAuth(nodeState, permissions)) {
            return true;
        }
        // check if node is part of a document, if so check facet authorization
        nodeState = getParentDoc(nodeState);
        if (nodeState != null) {
            if (checkFacetAuth(nodeState, permissions)) {
                return true;
            }
        }
        return false;
    }

    private boolean isWorkflowConfig(NodeState nodeState) {
        // create NodeType of nodeState's primaryType
        if (log.isTraceEnabled()) {
            log.trace("Checking if node: " + nodeState.getId() + " is part of workflow config");
        }
        String nodeStateType;
        try {
            nodeStateType = nRes.getJCRName(nodeState.getNodeTypeName());
        } catch (NamespaceException e) {
            return false;
        }
        if (nodeStateType.equals(HippoNodeType.NT_TYPE)) {
            return true;
        }
        if (nodeStateType.equals(HippoNodeType.NT_TYPES)) {
            return true;
        }
        if (nodeStateType.equals(HippoNodeType.NT_WORKFLOW)) {
            return true;
        }
        return false;
    }

    private NodeState getWorkflowNode(NodeState nodeState) {
        if (log.isTraceEnabled()) {
            log.trace("Fetching workflow node from node: " + nodeState.getId());
        }
        String nodeStateType;
        try {
            // Workflow subentries are granted by permissions on the parent.
            // Current (hard coded structure:
            // hippo:configuration->hippo:workflows->workflowPlugin->[workflowName]->hippo:types->[workflow]
            // primaryType workflowName -> hippo:workflow
            // primaryType workflow -> hippo:type
            nodeStateType = nRes.getJCRName(nodeState.getNodeTypeName());
            if (nodeStateType.equals(HippoNodeType.NT_WORKFLOW)) {
                return nodeState;
            }
            if (nodeStateType.equals(HippoNodeType.NT_TYPES)) {
                return getParentState(nodeState);
            }
            if (nodeStateType.equals(HippoNodeType.NT_TYPE)) {
                return getParentState(getParentState(nodeState));
            }
        } catch (NoSuchItemStateException e) {
            log.error("NoSuchItemStateException while fetching workflow node from id: " + nodeState.getId());
            log.debug("NoSuchItemStateException: ", e);
            return null;
        } catch (ItemStateException e) {
            log.error("ItemStateException while fetching workflow node from id: " + nodeState.getId());
            log.debug("NoSuchItemStateException: ", e);
            return null;
        } catch (NamespaceException e) {
            log.error("NamespaceException while fetching workflow node from id: " + nodeState.getId());
            log.debug("NoSuchItemStateException: ", e);
            return null;
        }
        return null;
    }


    /**
     * Check whether the node can be accessed with the requested permissions based on the
     * FacetAuths of the subject. The method first checks if the jcr permissions of the
     * FacetAuth match the requested principals. If so, a check is done to check if the
     * Node is in the Domain of the FacetAuth.
     * @param nodeState the state of the node to check
     * @param permissions the (bitset) of the permissions
     * @return true if the user is allowed the requested permissions on the node
     * @throws RepositoryException
     */
    protected boolean checkFacetAuth(NodeState nodeState, int permissions) throws NoSuchItemStateException, ItemStateException, RepositoryException {
        if (log.isTraceEnabled()) {
            log.trace("Checking [" + pString(permissions) + "] for: " + nodeState.getId());
        }

        // loop over facetAuthPrincipals (one per domain)
        // -- first match return true
        // -- else return false

        // For the principal check:
        // - DomainRule_1 OR DomainRule_2 OR ...
        // - For each DomainRule
        // - - FacetRule_1 AND FacetRule_2 AND ...
        //
        //  (facet_x=value_a and facet_y=value_c) OR (facet_x=value_b)

        boolean allowed = false;
        for (FacetAuthPrincipal fap : subject.getPrincipals(FacetAuthPrincipal.class)) {

            if (log.isTraceEnabled()) {
                log.trace("Checking [" + pString(permissions) + "] : " + nodeState.getId()
                        + " against FacetAuthPrincipal: " + fap);
            }

            // Does the user has the correct permissions for the Domain
            if ((permissions & fap.getPermissions()) == permissions) {
                // is the node part of one of the domain rules
                if (isNodeInDomain(nodeState, fap)) {
                    allowed = true;
                    log.info("Permissions [{}] for {} for: {} found for domain {}", new Object[] { pString(permissions),
                            userId, nodeState.getId(), fap });
                    break;
                }
            }
            if (allowed == true) {
                break;
            }
        }
        return allowed;
    }

    /**
     * Check if a node is in the domain of the facet auth principal by looping of all the
     * domain rules. For each domain all the facet rules are checked.
     * @param nodeState the state of the node to check
     * @param fap the facet auth principal to check
     * @return true if the node is in the domain of the facet auth
     * @throws RepositoryException
     * @see FacetAuthPrincipal
     */
    protected boolean isNodeInDomain(NodeState nodeState, FacetAuthPrincipal fap) throws NoSuchItemStateException, ItemStateException, RepositoryException {
        log.trace("Checking if node : {} is in domain of {}", nodeState.getId(), fap);
        boolean isInDomain = false;

        currentDomainRoleIds.clear();
        currentDomainRoleIds.addAll(fap.getRoles());

        // check is node matches ONE of the domain rules
        for (DomainRule domainRule : fap.getRules()) {

            boolean allRulesMatched = true;

            // no facet rules means no match
            if (domainRule.getFacetRules().size() == 0) {
                allRulesMatched = false;
                log.debug("No facet rules found for : {} in domain rule: {}", nodeState.getId(), domainRule);
            }
            // check if node matches ALL of the facet rules
            for (FacetRule facetRule : domainRule.getFacetRules()) {
                if (!matchFacetRule(nodeState, facetRule)) {
                    allRulesMatched = false;
                    log.trace("Rule doesn't match for : {} facet rule: {}", nodeState.getId(), facetRule);
                    break;
                }
            }
            if (allRulesMatched) {
                // a match is found, don't check other domain ruels;
                isInDomain = true;
                log.debug("Node : {} found in {}", nodeState.getId(), domainRule);
                break;
            }
        }
        return isInDomain;
    }

    /**
     * Check if a node matches the current FacetRule
     * @param nodeState the state of the node to check
     * @param facetRule the facet rule to check
     * @return true if the node matches the facet rule
     * @throws RepositoryException 
     * @throws ItemStateException 
     * @throws NoSuchItemStateException 
     * @see FacetRule
     */
    protected boolean matchFacetRule(NodeState nodeState, FacetRule facetRule) throws NoSuchItemStateException, ItemStateException, RepositoryException {
        log.trace("Checking node : {} for facet rule: {}", nodeState.getId(), facetRule);

        // is this a 'NodeType' facet?
        if (facetRule.getFacet().equalsIgnoreCase("nodetype")) {
            boolean match = false;
            log.trace("Checking node : {} for nodeType: {}", nodeState.getId(), facetRule);
            if (isInstanceOfType(nodeState, facetRule.getValue())) {
                match = true;
                log.trace("Found match : {} for nodeType: {}", nodeState.getId(), facetRule.getValue());
            } else if (hasMixinWithValue(nodeState, facetRule.getValue())) {
                match = true;
                log.trace("Found match : {} for mixinType: {}", nodeState.getId(), facetRule.getValue());
            }
            if (facetRule.isEqual()) {
                // return true if the node is of the specified nodetype
                return match;
            } else {
                // return false if the node is of the specified nodetype
                return !match;
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
     * Check if a user has the requested permissions on the item.
     * @param id the item id to check
     * @param permissions the requested permissions
     * @throws AccessDeniedException if the user doesn't have the requested permissions on the item
     */
    public void checkPermission(ItemId id, int permissions) throws AccessDeniedException, ItemNotFoundException,
            RepositoryException {
        // just use the isGranted method
        if (!isGranted(id, permissions)) {
            throw new AccessDeniedException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isGranted(ItemId id, int permissions) throws RepositoryException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }
        if (log.isTraceEnabled()) {
            log.trace("Checking [" + pString(permissions) + "] for: " + id);
        }
        if (isSystem) {
            if (log.isTraceEnabled()) {
                log.trace("Granted [" + pString(permissions) + "] for: " + id + " to system user");
            }
            return true;
        }
        
        // handle properties
        if (!id.denotesNode()) {
            if ((permissions & REMOVE) == REMOVE) {
                // Don't check remove on properties. A write check on the node itself is done.
                return true;
            }
            return isGranted(((PropertyId) id).getParentId(), permissions);
        }

        // Check read access cache
        if ((permissions & READ) == READ) {
            Boolean allowRead = readAccessCache.get(id);
            if (allowRead != null) {
                if (log.isTraceEnabled()) {
                    log.trace("Cached [" + pString(permissions) + "] for: " + id + " granted: " + allowRead);
                }
                return allowRead.booleanValue();
            }
        } else {
            // not a read, remove node from cache
            readAccessCache.remove(id);
        }

        // initialize per request ItemStateCache
        synchronized (requestItemStateCache) {
            requestItemStateCache.clear();
        }
        
        try {
            NodeState nodeState = (NodeState) getItemState(id);
            if (nodeState == null) {
                log.error("NodeState not found for ItemId: " + id);
                throw new RepositoryException("NodeState not found for ItemId : " + id);
            }
            
            // do check
            boolean isGranted = canAccessNode(nodeState, permissions);

            // update read access cache
            if ((permissions & READ) == READ) {
                readAccessCache.put(id, isGranted);
            }

            if (log.isDebugEnabled()) {
                log.debug("Permissions [" + pString(permissions) + "] for: " + id + " -> " + id + " granted: "
                        + isGranted);
            }
            
            if (!isGranted) {
                if (log.isInfoEnabled()) {
                    PathResolver resolver = new ParsingPathResolver(PathFactoryImpl.getInstance(), nRes);
                    log.info("[" + pString(permissions) + "] DENIED for " + userId + " state("+nodeState.getStatus()+") for: " + id + " -> " + resolver.getJCRPath(hierMgr.getPath(id)));
                }
            }
            return isGranted;
        } catch (NoSuchItemStateException e) {
            log.warn("NoSuchItemStateException for: " + id + " : " + e.getMessage());
            throw new ItemNotFoundException("NoSuchItemStateException: Item not found in hierarchy: " + id, e);
        } catch (ItemStateException e) {
            log.error("ItemStateException for id: " + id, e);
            throw new RepositoryException("ItemStateException: " + e.getMessage(), e);
        }
    }

    /**
     * Try to get a state from the item manager by first checking the normal states,
     * then checking the transient states and last checking the attic state.
     * @param id the item id
     * @return the item state
     * @throws NoSuchItemStateException when the state cannot be found
     * @throws ItemStateException when something goes wrong while fetching the state
     */
    public ItemState getItemState(ItemId id) throws NoSuchItemStateException, ItemStateException {
        if (id == null) {
            throw new ItemStateException("ItemId cannot be null");
        }
        synchronized (requestItemStateCache) {
            if (requestItemStateCache.containsKey(id)) {
                return requestItemStateCache.get(id);
            }
            if (itemMgr.hasItemState(id)) {
                ItemState itemState = itemMgr.getItemState(id);
                requestItemStateCache.put(id, itemState);
                return itemState;
            }
            if (itemMgr.hasTransientItemStateInAttic(id)) {
                ItemState itemState = itemMgr.getAtticItemState(id);
                requestItemStateCache.put(id, itemState);
                return itemState;
            }   
        }
        throw new NoSuchItemStateException("Item state not found in normal, transient or attic: " + id);
    }

    /**
     * Get the NodeState of the parent from the nodeState in a transient and attic
     * safe manner.
     * @param nodeState
     * @return the parent nodestate
     * @throws NoSuchItemStateException when the state cannot be found
     * @throws ItemStateException when something goes wrong while fetching the state
     */
    public NodeState getParentState(NodeState nodeState) throws NoSuchItemStateException, ItemStateException  {
        if (rootNodeId.equals(nodeState.getId())) {
            throw new NoSuchItemStateException("RootNode doesn't have a parent state.");
        }
        try {
            // for nodeState's from the attic the parentId() is null, so use path resolving to find parent
            return (NodeState) getItemState(hierMgr.resolveNodePath(hierMgr.getPath(nodeState.getId()).getAncestor(1)));
        } catch (RepositoryException e) {
            throw new NoSuchItemStateException("Unable to find parent nodeState of node: " + nodeState.getId(),e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean canAccess(String workspaceName) throws NoSuchWorkspaceException, RepositoryException {
        // no workspace restrictions yet
        return true;
    }

    /**
     * Helper method for pretty printing the requested permission
     * @param permissions
     * @return the 'unix' style permissions string
     */
    private String pString(int permissions) {
        StringBuffer buf = new StringBuffer();

        // narrow down permissions
        if ((permissions & READ) == READ) {
            buf.append('r');
        } else {
            buf.append('-');
        }

        // narrow down permissions
        if ((permissions & WRITE) == WRITE) {
            buf.append('w');
        } else {
            buf.append('-');
        }

        // narrow down permissions
        if ((permissions & REMOVE) == REMOVE) {
            buf.append('d');
        } else {
            buf.append('-');
        }
        return buf.toString();
    }

    /**
     * Helper function to check if a nodeState is of a node type or a
     * instance of the node type (sub class)
     * TODO: Very expensive function, probably needs some caching
     *
     * @param nodeState the node to check
     * @param nodeTypeName the node type name
     * @return boolean
     * @throws NoSuchNodeTypeException
     */
    private boolean isInstanceOfType(NodeState nodeState, String nodeType) throws NamespaceException,
            NoSuchNodeTypeException, RepositoryException {
        // create NodeType of nodeState's primaryType
        String nodeStateType = nRes.getJCRName(nodeState.getNodeTypeName());

        if (nodeStateType.equals(nodeType)) {
            if (log.isTraceEnabled()) {
                log.trace("MATCH " + nodeState.getId() + " is of type: " + nodeType);
            }
            return true;
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
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check if a node matches the current FacetRule based on a
     * check on the properties of the node.
     * @param nodeState the state of the node to check
     * @param facetRule the facet rule to check
     * @return true if the node matches the facet rule
     * @throws RepositoryException 
     * @see FacetRule
     */
    private boolean matchPropertyWithFacetRule(NodeState nodeState, FacetRule rule) throws NoSuchItemStateException, ItemStateException, RepositoryException {
        
        // jcr:primaryType isn't really a property
        if (rule.getFacetName().equals(NameConstants.JCR_PRIMARYTYPE)) {
            // WILDCARD match, jcr:primaryType == *
            if (FacetAuthConstants.WILDCARD.equals(rule.getValue())) {
                return true;
            }
            if (nodeState.getNodeTypeName().equals(rule.getValueName())) {
                return true;
            }
        }

        // the hierarchy manager is attic aware. The property can also be in the removed properties
        if (!nodeState.hasPropertyName(rule.getFacetName())
                && !nodeState.getRemovedPropertyNames().contains(rule.getFacetName())) {
            log.trace("Node: {} doesn't have property {}", nodeState.getId(), rule.getFacetName());
            return false;
        }
        HippoPropertyId propertyId = new HippoPropertyId(nodeState.getNodeId(), rule.getFacetName());
        PropertyState state = (PropertyState) getItemState(propertyId);
        InternalValue[] iVals = state.getValues();
        boolean match = false;

        for (InternalValue iVal : iVals) {
            // types must match
            if (iVal.getType() != rule.getType()) {
                continue;
            }

            // WILDCARD match
            if (FacetAuthConstants.WILDCARD.equals(rule.getValue())) {
                match = true;
            }

            if (iVal.getType() == PropertyType.STRING) {
                log.trace("Checking facet rule: {} (string) -> {}", rule, iVal.getString());

                // expander matches
                if (FacetAuthConstants.EXPANDER_USER.equals(rule.getValue())) {
                    if (isUser && userId.equals(iVal.getString())) {
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
                log.trace("Checking facet rule: {} (name) -> {}", rule, iVal.getQName());

                if (iVal.getQName().equals(rule.getValueName())) {
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
     * @param value the mixin type to check for. This is the String representation of the Name
     * @return true if the node has the mixin type
     * @throws RepositoryException 
     */
    private boolean hasMixinWithValue(NodeState nodeState, String value) throws NoSuchItemStateException, ItemStateException, RepositoryException {
        if (!nodeState.hasPropertyName(NameConstants.JCR_MIXINTYPES)) {
            return false;
        }

        HippoPropertyId propertyId = new HippoPropertyId(nodeState.getNodeId(), NameConstants.JCR_MIXINTYPES);
        PropertyState state = (PropertyState) getItemState(propertyId);
        InternalValue[] iVals = state.getValues();

        for (InternalValue iVal : iVals) {
            // types must match
            if (iVal.getType() == PropertyType.NAME) {

                // WILDCARD match
                if (value.equals(FacetAuthConstants.WILDCARD)) {
                    return true;
                }

                log.trace("Checking facetVal: {} (name) -> {}", value, iVal.getQName());
                if (iVal.getQName().toString().equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Helper function to find a hippo:document instance node type. This
     * can be used to check for facet authorization on the root of a
     * document (bonzai tree).
     * @param nodeState the node of which to check the parents
     * @return NodeState the parent node state or null
     * @throws NoSuchItemStateException
     * @throws ItemStateException
     * @throws RepositoryException 
     * @throws NamespaceException 
     */
    private NodeState getParentDoc(NodeState nodeState) throws NoSuchItemStateException, ItemStateException,
            NamespaceException, RepositoryException {

        if (log.isTraceEnabled()) {
            log.trace("Checking " + nodeState.getId() + " ntn: " + nodeState.getNodeTypeName()
                    + " for being part of a document model.");
        }
        // check if this is already the root of a document
        if (isInstanceOfType(nodeState, HippoNodeType.NT_DOCUMENT) || nodeState.getNodeTypeName().equals(hippoHandle)
                || nodeState.getNodeTypeName().equals(hippoFacetSearch)
                || nodeState.getNodeTypeName().equals(hippoFacetSelect)) {
            if (log.isDebugEnabled()) {
                log.debug("Node is already document root: " + nodeState.getNodeTypeName());
            }
            return null;
        }
        // walk up in the hierarchy
        while (!rootNodeId.equals((NodeId) nodeState.getId())) {
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
            if (isInstanceOfType(nodeState, HippoNodeType.NT_DOCUMENT)) {
                if (log.isDebugEnabled()) {
                    log.debug("MATCH hippoDoc: " + nodeState.getNodeTypeName());
                }
                return nodeState;
            }
        }
        return null;
    }

    /**
     * Super Simple LRU Cache for <ItemId,Boolean> key-value pairs
     */
    private class PermissionLRUCache {

        /**
         * Set size to zero to disable cache
         */
        private boolean enabled = true;
        
        /**
         * The cache map
         */
        private LRUMap map = null;

        /**
         * Local counter for cache hits
         */
        private long hit;

        /**
         * Local counter for cache misses
         */
        private long miss;

        /**
         * Local counter for total number of cache access
         */
        private long total;

        /**
         * Create a new LRU cache
         * @param size max number of cache objects
         */
        public PermissionLRUCache(int size) {
            if (size < 0) {
                throw new IllegalArgumentException("size < 0");
            }
            hit = 0;
            miss = 0;
            total = 0;
            if (size == 0) { 
                enabled = false;
                return;
            }
            map = new LRUMap(size);
        }

        /**
         * Fetch cache value
         * @param id ItemId
         * @return cached value or null when not in cache
         */
        synchronized public Boolean get(ItemId id) {
            if (!enabled) {
                return null;
            }
            total++;
            Boolean obj = (Boolean) map.get(id.hashCode());
            if (obj == null) {
                miss++;
            } else {
                hit++;
            }
            return obj;
        }

        /**
         * Store key-value in cache
         * @param id ItemId the key
         * @param isGranted the value
         */
        synchronized public void put(ItemId id, boolean isGranted) {
            if (!enabled) {
                return;
            }
            map.put(id.hashCode(), isGranted);
        }

        /**
         * Remove key-value from cache
         * @param id ItemId the key
         */
        synchronized public void remove(ItemId id) {
            if (!enabled) {
                return;
            }
            if (map.containsKey(id.hashCode())) {
                map.remove(id.hashCode());
            }
        }

        /**
         * Clear the cache
         */
        synchronized public void clear() {
            if (!enabled) {
                return;
            }
            map.clear();
        }

        /**
         * Total number of times this cache is accessed
         * @return long
         */
        public long getTotalAccess() {
            return total;
        }

        /**
         * Total number of cache hits
         * @return long
         */
        public long getHits() {
            return hit;
        }

        /**
         * Total number of cache misses
         * @return long
         */
        public long getMisses() {
            return miss;
        }

        /**
         * The current size of the cache
         * @return int
         */
        public int getSize() {
            if (!enabled) {
                return 0;
            }
            return map.size();
        }

        /**
         * The max size of the cache
         * @return int
         */
        public int getMaxSize() {
            if (!enabled) {
                return 0;
            }
            return map.maxSize();
        }
    }
}
