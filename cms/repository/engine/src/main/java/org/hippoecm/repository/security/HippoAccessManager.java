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
package org.hippoecm.repository.security;

import javax.security.auth.Subject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.commons.collections.map.LRUMap;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
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
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.jackrabbit.HippoHierarchyManager;
import org.hippoecm.repository.jackrabbit.HippoPropertyId;
import org.hippoecm.repository.security.principals.AdminPrincipal;
import org.hippoecm.repository.security.principals.FacetAuthPrincipal;

/**
 * HippoAccessManager
 */
public class HippoAccessManager implements AccessManager {

    /**
     * Subject whose access rights this AccessManager should reflect
     */
    protected Subject subject;

    /**
     * hierarchy manager used for ACL-based access control model
     */
    protected HippoHierarchyManager hierMgr;

    /**
     * Hippo Namespace, TODO: move to better place
     */
    public final static String NAMESPACE_URI = "http://www.hippoecm.org/nt/1.0";

    /**
     *  Hippo Namespace prefix, TODO: move to better place
     */
    public final static String NAMESPACE_PREFIX = "hippo";

    /**
     * Root NodeId of current session
     */
    protected NodeId rootNodeId;

    /**
     * State of the accessManager
     */
    private boolean initialized;

    /**
     * SuperSimpleLRUCache
     * TODO: handle multiple users, multiple session (perhaps move to ISM?)
     */
    PermissionLRUCache readAccessCache = new PermissionLRUCache(250);

    /**
     * Flag wheter current user is anonymous
     */
    protected boolean isAnonymous;

    /**
     * Flag wheter current user is a regular user
     */
    protected boolean isUser;

    /**
     * Flag wheter the current user is an admni
     */
    protected boolean isAdmin;

    /**
     * Flag wheter the current user is a system user
     */
    protected boolean isSystem;

    /**
     * Empty constructor
     */
    public HippoAccessManager() {
        initialized = false;
        isAnonymous = false;
        isUser = false;
        isAdmin = false;
        isSystem = false;
    }

    private static final Logger log = LoggerFactory.getLogger(HippoAccessManager.class);

    /**
     * {@inheritDoc}
     */
    public void init(org.apache.jackrabbit.core.security.AMContext context) throws AccessDeniedException, Exception {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }
        subject = context.getSubject();
        hierMgr = (HippoHierarchyManager) context.getHierarchyManager();

        // Shortcuts for checks
        isAnonymous = !subject.getPrincipals(AnonymousPrincipal.class).isEmpty();
        isUser = !subject.getPrincipals(UserPrincipal.class).isEmpty();
        isAdmin = !subject.getPrincipals(AdminPrincipal.class).isEmpty();
        isSystem = !subject.getPrincipals(SystemPrincipal.class).isEmpty();

        // cache root NodeId
        rootNodeId = (NodeId) hierMgr.resolvePath(PathFactoryImpl.getInstance().getRootPath());

        // we're done
        initialized = true;
    }

    /**
     * Get the local part of the node type
     * @param ntName
     * @return
     */
    private String getLocalName(String nodeTypeName) {
        int i = nodeTypeName.indexOf(":");
        if (i < 0) {
            return nodeTypeName;
        }
        return nodeTypeName.substring(i+1);
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
     * Check wheter a user can access the NodeState with the requested permissions
     * @param nodeState
     * @param permissions
     * @return
     * @throws RepositoryException
     * @throws ItemStateException
     * @throws NoSuchItemStateException
     */
    protected boolean canAccessItem(NodeState nodeState, int permissions) throws RepositoryException, NoSuchItemStateException, ItemStateException {
        // system and admin have all permissions
        if (isSystem || isAdmin) {
            return true;
        }

        if (canAccessJCRNode(nodeState, permissions)) {
            return true;
        }
        if (canAccessHippoNode(nodeState, permissions)) {
            return true;
        }

        // no facetAuth -> no allowed...
        if (subject.getPrincipals(FacetAuthPrincipal.class).isEmpty()) {
            return false;
        }

        /*
         * 1. AND -> (x=a or x=b) AND (y=c)
         * -- first non match return false;
         * -- else return true
         */
        //boolean allowed = true;
        //for(FacetAuthPrincipal principal : subject.getPrincipals(FacetAuthPrincipal.class)) {
        //    if (!checkFacetAuth(nodeState, principal, permissions)) {
        //        allowed = false;
        //        break;
        //    }
        //}

        /*
         * 2. OR -> (x=a or x=b) OR (y=c)
         * -- first non match return false;
         * -- else return true
         */
        boolean allowed = false;
        for(FacetAuthPrincipal principal : subject.getPrincipals(FacetAuthPrincipal.class)) {
            if (checkFacetAuth(nodeState, principal, permissions)) {
                allowed = true;
                break;
            }
        }

        /*
         * 3. OR -> (x=a and y=c) OR (x=b)
         * -- first match return true
         * -- else return false
         */
        // boolean allowed = false;
        // for(FacetAuthPrincipal principal : subject.getPrincipals(FacetAuthPrincipal.class)) {
        //  if (checkFacetAuth(nodeState, principal, permissions)) {
        //      allowed = true;
        //      break;
        //  }
        // }

        // TODO: node could be part of a bigger Hippo Document (part of the Bonsai tree)
        // in which case a user may also have access to the node.

        return allowed;
    }

    /**
     * Allow access to some JCR nodes based on the node type
     * TODO: checks shouldn't use manual NodeType parsing
     * @param nodeState
     * @param permissions
     * @return
     * @throws RepositoryException
     */
    protected boolean canAccessJCRNode(NodeState nodeState, int permissions) throws RepositoryException {
        // allow reading and modifying of structural nodes
        if (nodeState.getNodeTypeName().equals(NameConstants.NT_UNSTRUCTURED)) {
            if (log.isDebugEnabled()) {
                log.debug("Allow struncture node read access.");
            }
            return true;
        }
        if (nodeState.getNodeTypeName().equals(NameConstants.NT_FOLDER)) {
            if (log.isDebugEnabled()) {
                log.debug("Allow struncture node read access.");
            }
            return true;
        }
        // Allow root read
        if ((permissions & WRITE) != WRITE && (permissions & REMOVE) != REMOVE) {
            if (nodeState.getParentId() == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Allow root node read access.");
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Allow access to some HippoNodes based on the node type
     * TODO: checks shouldn't use manual NodeType parsing
     * @param nodeState
     * @param permissions
     * @return
     * @throws RepositoryException
     * @throws ItemStateException
     * @throws NoSuchItemStateException
     */
    protected boolean canAccessHippoNode(NodeState nodeState, int permissions) throws RepositoryException, NoSuchItemStateException, ItemStateException {
        String namespaceURI = nodeState.getNodeTypeName().getNamespaceURI();
        String localName = nodeState.getNodeTypeName().getLocalName();

        // not a hippo node
        if (!namespaceURI.equals(NAMESPACE_URI)) {
            return false;
        }

        // read & write & remove access
        if (localName.equals(getLocalName(HippoNodeType.NT_FACETSEARCH))) {
            return true;
        }
        if (localName.equals(getLocalName(HippoNodeType.NT_FACETSELECT))) {
            return true;
        }

        // narrow down permissions
        if ((permissions & REMOVE) == REMOVE) {
            return false;
        }

        // only read & write access from here on
        if (localName.equals(getLocalName(HippoNodeType.NT_HANDLE))) {
            return true;
        }

        // narrow down permissions
        if ((permissions & WRITE) == WRITE) {
            return false;
        }

        // only read access from her on
        if (localName.equals(getLocalName(HippoNodeType.NT_FACETRESULT))) {
            return true;
        }
        if (localName.equals(getLocalName(HippoNodeType.NT_FACETSUBSEARCH))) {
            return true;
        }
        if (localName.equals(getLocalName(HippoNodeType.NT_FRONTENDPLUGIN))) {
            return true;
        }
        if (localName.equals(getLocalName(HippoNodeType.NT_PLUGIN))) {
            return true;
        }
        if (localName.equals(getLocalName(HippoNodeType.NT_PLUGINFOLDER))) {
            return true;
        }
        if (localName.equals(getLocalName(HippoNodeType.NT_APPLICATION))) {
            return true;
        }
        if (localName.equals(getLocalName(HippoNodeType.NT_PAGE))) {
            return true;
        }

        // check for parent node
        NodeState parentState = (NodeState) hierMgr.getItemState(nodeState.getParentId());
        namespaceURI = parentState.getNodeTypeName().getNamespaceURI();
        localName = parentState.getNodeTypeName().getLocalName();

        //not a hippo node
//        if (!namespaceURI.equals(NAMESPACE_URI)) {
//            return false;
//        }
//        if (localName.equals(getLocalName(HippoNodeType.NT_FACETRESULT))) {
//            return true;
//        }
//        if (localName.equals(getLocalName(HippoNodeType.NT_FACETSUBSEARCH))) {
//            return true;
//        }
//        if (localName.equals(getLocalName(HippoNodeType.NT_FACETSELECT))) {
//            return true;
//        }
        // else deny..
        return false;
    }

    /**
     * Check permissions for FacetAuth
     * TODO: check for non-String types
     * @throws RepositoryException
     * @throws
     */
    protected boolean checkFacetAuth(NodeState nodeState, FacetAuthPrincipal principal, int permissions) throws RepositoryException {
        // check if a permission is requested that you don't have
        if ((permissions & (int)principal.getPermissions()) != permissions) {
            return false;
        }

        // check if node has the required property
        Name name = NameFactoryImpl.getInstance().create("", principal.getFacet());
        if (nodeState.hasPropertyName(name)) {
            if (log.isDebugEnabled()) {
                log.debug("Found [" + pString(permissions) + "] property: " + name);
            }

            HippoPropertyId propertyId = new HippoPropertyId(nodeState.getNodeId(),name);

            try {
                // check if the property has a required value
                PropertyState state = (PropertyState) hierMgr.getItemState(propertyId);
                InternalValue[] iVals = state.getValues();
                for (InternalValue iVal : iVals) {
                    if (iVal.getType() != PropertyType.STRING) {
                        continue;
                    }
                    for (String val : principal.getValues()) {
                        if (log.isTraceEnabled()) {
                            log.trace("Checking [" + pString(permissions) + "] nodeVal->facetVal: " + iVal.getString() + "->" + val);
                        }
                        if (iVal.getString().equals(val)) {
                            if (log.isDebugEnabled()) {
                                log.debug("Found match [" + pString(permissions) + "] nodeVal->facetVal: " + iVal.getString() + "->" + val);
                            }
                            // found a match!
                            return true;
                        }
                    }
                }
                // no valid value found
                return false;
            } catch (NoSuchItemStateException e) {
                //e.printStackTrace();
                return false;
            } catch (ItemStateException e) {
                //e.printStackTrace();
                return false;
            }
        } else {
            // node doesn't have the required property
            return false;
        }
    }

    /**
     * This method is currently only used by the JCR classloader
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

        // handle property
        if (!id.denotesNode()) {
            try {
                // isProperty -> check permissions on the parent node
                return isGranted(hierMgr.getItemState(id).getParentId(), permissions);
            } catch (NoSuchItemStateException e) {
                // this can be either
                // - a new property (not yet saved)
                // - a deleted property (not yet saved)
                // - a "virtual" property (eg. hippo:count)
                log.debug("Item not found in hierarchy for id: " + id, e);
                return true;
            } catch (ItemStateException e) {
                log.error("ItemSate exception for id: " + id, e);
                return true;
            }
        }

        try {
            ItemState itemState = hierMgr.getItemState(id);
            boolean isGranted = false;

            NodeState nodeState = (NodeState) itemState;

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

            // It is possible to find out what the write tries to do
            // this can facilitate more fine grained permissions
            //if ((permissions & WRITE) == WRITE) {
            // we can figure out what the write is really trying to do...
            //if (nodeState.getAddedChildNodeEntries().size() > 0) {
            //    System.out.println("ADD CHILD NODE! " + hierMgr.getName(id));
            //}
            //if (nodeState.getRemovedChildNodeEntries().size() > 0) {
            //    System.out.println("REMOVE CHILD NODE! " + hierMgr.getName(id));
            //}
            //if (nodeState.getAddedPropertyNames().size() > 0) {
            //    System.out.println("ADD PROPERTY! " + hierMgr.getName(id));
            //}
            //if (nodeState.getRemovedPropertyNames().size() > 0) {
            //    System.out.println("REMOVE PROPERTY! " + hierMgr.getName(id));
            //}
            //}

            // do check
            isGranted = canAccessItem(nodeState, permissions);

            // update read access cache
            if ((permissions & READ) == READ) {
                readAccessCache.put(id, isGranted);
            }

            if (log.isDebugEnabled()) {
                log.debug("Found [" + pString(permissions) + "] for: " + id + " granted: " + isGranted);
            }

            return isGranted;
        } catch (NoSuchItemStateException e) {
            if ((permissions & REMOVE) == REMOVE) {
                // The parent is checked for write access and after
                // write access is granted the node is removed. When
                // trying to check the remove permission on the node
                // itself it doesn't exists anymore in the hierMrg. So
                // just remove it when it's removed...
                readAccessCache.remove(id);
                return true;
            }

            // On read this can be either
            // - a "virtual" node
            // - a "real" error
            if (log.isDebugEnabled()) {
                log.debug("Item not found in hierarchy for id: " + id, e);
            }
            return true;
            //throw new ItemNotFoundException("Item not found in hierarchy: " + id);
        } catch (ItemStateException e) {
            log.error("ItemSate exception for id: " + id, e);
            throw new RepositoryException("ItemStateException: " + e.getMessage());
        }
    }

    /**
     * Helper method for pretty printing the requested permission
     * @param permissions
     * @return
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
        if ((permissions & REMOVE) == REMOVE) {
            buf.append('d');
        } else {
            buf.append('-');
        }

        // narrow down permissions
        if ((permissions & WRITE) == WRITE) {
            buf.append('w');
        } else {
            buf.append('-');
        }
        return buf.toString();
    }

    /**
     * {@inheritDoc}
     */
    public boolean canAccess(String workspaceName) throws NoSuchWorkspaceException, RepositoryException {
        // no workspace restrictions yet
        return true;
    }


    /**
     * Super Simple LRU Cache for <ItemId,Boolean> key-value pairs
     */
    private class PermissionLRUCache {

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
            if (size < 1) {
                throw new IllegalArgumentException("size < 1");
            }
            hit = 0;
            miss = 0;
            total = 0;
            map = new LRUMap(size);
        }

        /**
         * Fetch cache value
         * @param id ItemId
         * @return cached value or null when not in cache
         */
        synchronized public Boolean get(ItemId id) {
            total ++;
            Boolean obj = (Boolean) map.get(id.hashCode());
            if (obj == null) {
                miss ++;
            } else {
                hit ++;
            }
            return obj;
        }

        /**
         * Store key-value in cache
         * @param id ItemId the key
         * @param isGranted the value
         */
        synchronized public void put(ItemId id, boolean isGranted) {
            map.put(id.hashCode(), isGranted);
        }

        /**
         * Remove key-value from cache
         * @param id ItemId the key
         */
        synchronized public void remove(ItemId id) {
            if (map.containsKey(id.hashCode())) {
                map.remove(id.hashCode());
            }
        }

        /**
         * Clear the cache
         */
        synchronized public void clear() {
            map.clear();
        }

        /**
         * Total numer of times this cache is accessed
         * @return long
         */
        public long getTotalAccess() {
            return total;
        }

        /**
         * Total numer of cache hits
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
            return map.size();
        }

        /**
         * The max size of the cache
         * @return int
         */
        public int getMaxSize() {
            return map.maxSize();
        }
    }
}
