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

import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.security.auth.Subject;

import org.apache.commons.collections.map.LRUMap;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.nodetype.NodeTypeDef;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
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
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.jackrabbit.HippoHierarchyManager;
import org.hippoecm.repository.jackrabbit.HippoPropertyId;
import org.hippoecm.repository.security.domain.DomainRule;
import org.hippoecm.repository.security.domain.FacetRule;
import org.hippoecm.repository.security.principals.FacetAuthPrincipal;
import org.hippoecm.repository.security.principals.RolePrincipal;
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
 * TODO: Remove the 'special' checks for the workflow.
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
     * NodeTypeRegistry for resolving superclass node types
     */
    private NodeTypeRegistry ntReg;

    /**
     * NameFactory for create Names
     */
    private static final NameFactory FACTORY = NameFactoryImpl.getInstance();

    /**
     * Hippo Namespace, TODO: move to better place
     */
    public final static String NAMESPACE_URI = "http://www.hippoecm.org/nt/1.0";

    
    /**
     * Name of hippo:docucment, needed for document model checking 
     */
    private final Name hippoDoc;

    /**
     * Name of hippo:handle, needed for document model checking
     */
    private final Name hippoHandle;
    
    /**
     * Name of hippo:facetsearch, needed for document model checking
     */
    private final Name hippoFacetSearch;
    
    /**
     * Name of hippo:facetselect, needed for document model checking
     */
    private final Name hippoFacetSelect;

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
    private PermissionLRUCache readAccessCache = new PermissionLRUCache(250);

    /**
     * Flag wheter current user is anonymous
     */
    @SuppressWarnings("unused")
    private boolean isAnonymous;

    /**
     * Flag wheter current user is a regular user
     */
    @SuppressWarnings("unused")
    private boolean isUser;

    /**
     * Flag wheter the current user is a system user
     */
    private boolean isSystem;

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

        // create names from node types
        // TODO: get the names from somewhere else. The access manager doesn't have 
        // access to the nodetype registry, so the namespace conversion is hard coded.
        hippoDoc = FACTORY.create(NAMESPACE_URI, getLocalName(HippoNodeType.NT_DOCUMENT));
        hippoHandle = FACTORY.create(NAMESPACE_URI, getLocalName(HippoNodeType.NT_HANDLE));
        hippoFacetSearch = FACTORY.create(NAMESPACE_URI, getLocalName(HippoNodeType.NT_FACETSEARCH));
        hippoFacetSelect = FACTORY.create(NAMESPACE_URI, getLocalName(HippoNodeType.NT_FACETSELECT));
    }


    /**
     * {@inheritDoc}
     */
    public void init(AMContext context) throws AccessDeniedException, Exception {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }
        subject = context.getSubject();
        hierMgr = (HippoHierarchyManager) context.getHierarchyManager();
        //nsResolver = context.getNamespaceResolver();
        if (context instanceof HippoAMContext) {
            ntReg = ((HippoAMContext) context).getNodeTypeRegistry();
        }

        // Shortcuts for checks
        isAnonymous = !subject.getPrincipals(AnonymousPrincipal.class).isEmpty();
        isUser = !subject.getPrincipals(UserPrincipal.class).isEmpty();
        isSystem = !subject.getPrincipals(SystemPrincipal.class).isEmpty();

        // cache root NodeId
        rootNodeId = (NodeId) hierMgr.resolvePath(PathFactoryImpl.getInstance().getRootPath());

        
        // we're done
        initialized = true;
    }

    /**
     * Get the local part of the node type name string.
     * TODO: Remove this method when nodetype names are available
     * @param nodeTypeName string with prefix, eg 'hippo:document'
     * @return the local part of the nodetype name
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
     * @param nodeState the state of the node to check
     * @param permissions the (bitset) of the permissions
     * @return true if the user is allowed the requested permissions on the node
     * @throws RepositoryException
     * @throws ItemStateException
     * @throws NoSuchItemStateException
     */
    protected boolean canAccessNode(NodeState nodeState, int permissions) throws RepositoryException, NoSuchItemStateException, ItemStateException {
        // system and admin have all permissions
        if (isSystem) {
            return true;
        }
        // special hippo node types
        if (canAccessHippoNode(nodeState, permissions)) {
            return true;
        }
        // no facetAuths -> not allowed...
        if (subject.getPrincipals(FacetAuthPrincipal.class).isEmpty()) {
            return false;
        }
        // check for facet auuthorization
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


    /**
     * Allow access to some special HippoNodes based on the node type, 
     * TODO: checks shouldn't use manual NodeType parsing
     * TODO: this function is only used for the workflow checks and should be remove
     *       when the workflow is handled better.
     * @param nodeState the state of the node to check
     * @param permissions the (bitset) of the permissions
     * @return true if the user is allowed the requested permissions on the node
     * @throws RepositoryException
     * @throws ItemStateException
     * @throws NoSuchItemStateException
     */
    protected boolean canAccessHippoNode(NodeState nodeState, int permissions) throws RepositoryException, NoSuchItemStateException, ItemStateException {
        if (log.isTraceEnabled()) {
            log.trace("Checking [" + pString(permissions) + "] for: " + nodeState.getId());
        }

        //-----------------------  read access -------------------//
        if (permissions != READ) {
            return false;
        }

        // check namespace
        String namespaceURI = nodeState.getNodeTypeName().getNamespaceURI();
        String localName = nodeState.getNodeTypeName().getLocalName();
        if (!namespaceURI.equals(NAMESPACE_URI)) {
            return false;
        }

        // FIXME: make more generic
        // Special cases for worlkflow configuration based on roles.
        // Workflow subentries are granted by permissions on the parent.
        // Current (hard coded structure:
        // hippo:configuration->hippo:workflows->workflowPlugin->[workflowName]->hippo:types->[workflow]
        // primaryType workflowName -> hippo:workflow
        // primaryType workflow -> hippo:type
        if (localName.equals(getLocalName(HippoNodeType.NT_TYPE))) {
            // shift one up in hierarchy
            nodeState = (NodeState) hierMgr.getItemState(nodeState.getParentId());
            localName = nodeState.getNodeTypeName().getLocalName();
        }
        if (localName.equals(getLocalName(HippoNodeType.NT_TYPES))) {
            // shift one up in hierarchy
            nodeState = (NodeState) hierMgr.getItemState(nodeState.getParentId());
            localName = nodeState.getNodeTypeName().getLocalName();
        }
        if (localName.equals(getLocalName(HippoNodeType.NT_WORKFLOW))) {
            return checkWorkflow(nodeState, permissions);
        }

        // else deny..
        return false;
    }

    /**
     * Check wheter the node can be accessed with the requested permissins based on the 
     * FacetAuths of the subject. The method first checks if the jcr permissions of the 
     * FacetAuth match the requested principals. If so, a check is done to check if the 
     * Node is in de Domain of the FacetAuth.
     * @param nodeState the state of the node to check
     * @param permissions the (bitset) of the permissions
     * @return true if the user is allowed the requested permissions on the node
     * @throws RepositoryException
     */
    protected boolean checkFacetAuth(NodeState nodeState, int permissions) throws RepositoryException {
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
                log.trace("Checking [" + pString(permissions) + "] : " + nodeState.getId() + " against FacetAuthPrincipal: " + fap);
            }
            
            // Does the user has the correct permissions for the Domain
            if ((permissions & fap.getPermissions()) == permissions) {
                // is the node part of one of the domain rules
                if (isNodeInDomain(nodeState, fap)) {
                    allowed = true;
                    log.debug("Permissions [{}] for: {} found for domain {}", new Object[]{pString(permissions), nodeState.getId(), fap});
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
     * domain rules. Foreach domain all the facet rules are checked.
     * @param nodeState the state of the node to check
     * @param fap the facet auth principal to check
     * @return true if the node is in the domain of the facet auth
     * @throws RepositoryException
     * @see FacetAuthPrincipal
     */
    protected boolean isNodeInDomain(NodeState nodeState, FacetAuthPrincipal fap) throws RepositoryException {
        log.trace("Checking if node : {} is in domain of {}", nodeState.getId(), fap);
        boolean isInDomain = false;

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
                log.trace("Node : {} found in domain rule of {}", nodeState.getId(), domainRule);
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
     * @see FacetRule
     */
    protected boolean matchFacetRule(NodeState nodeState, FacetRule facetRule) {
        log.trace("Checking node : {} for facet rule: {}", nodeState.getId(), facetRule);
                
        // is this a 'NodeType' facet?
        if (facetRule.getFacet().equalsIgnoreCase("nodetype")) {
            boolean match = false;
            log.trace("Checking node : {} for nodeType: {}", nodeState.getId(), facetRule);
            Name valName = FACTORY.create(facetRule.getValue());
            if (isInstanceOfType(nodeState, valName)) {
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
     * Check for (read) permissions on workflow configuration based on roles
     * @throws RepositoryException
     */
    protected boolean checkWorkflow(NodeState nodeState, int permissions) throws RepositoryException {
        // only allow read access to the workflow configuration
        if (permissions != READ) {
            return false;
        }

        // check if node has the required property
        Name name = NameFactoryImpl.getInstance().create(NAMESPACE_URI ,getLocalName(HippoNodeType.HIPPO_ROLES));

        if (log.isTraceEnabled()) {
            log.trace("Checking [" + pString(permissions) + "] for hippo role: " + nodeState.getNodeId() + " in prop " + name);
        }
        if (nodeState.hasPropertyName(name)) {
            if (log.isDebugEnabled()) {
                log.debug("Found [" + pString(permissions) + "] property hippo role: " + name);
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
                    for(RolePrincipal principal : subject.getPrincipals(RolePrincipal.class)) {
                        String val = principal.getName();
                        if (log.isTraceEnabled()) {
                            log.trace("Checking [" + pString(permissions) + "] nodeVal->roleVal: " + iVal.getString() + "->" + val);
                        }
                        if (iVal.getString().equals(val)) {
                            if (log.isDebugEnabled()) {
                                log.debug("Found match [" + pString(permissions) + "] nodeVal->roleVal: " + iVal.getString() + "->" + val);
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
            // node doesn't have the hippo roles property
            return false;
        }
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

        // handle property
        if (!id.denotesNode()) {
            try {
                // isProperty -> check permissions on the parent node
                return isGranted(hierMgr.getItemState(id).getParentId(), permissions);
            } catch (NoSuchItemStateException e) {
                // this can be either
                // - a new property (not yet saved)
                // - a deleted property (not yet saved)
                // - a "virtual" property (eg. hippo:count)?
                if ((permissions & REMOVE) == REMOVE) {
                    if (log.isTraceEnabled()) {
                        log.trace("Checking [" + pString(permissions) + "] for: " + id);
                        log.trace("Property item not found (probably parent node removed) in hierarchy for id: " + id);
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Checking [" + pString(permissions) + "] for: " + id);
                        log.debug("Property item not found (new or virtual?) in hierarchy for id: " + id);
                    }
                }
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
            isGranted = canAccessNode(nodeState, permissions);

            // update read access cache
            if ((permissions & READ) == READ) {
                readAccessCache.put(id, isGranted);
            }

            if (log.isDebugEnabled()) {
                log.debug("Permissions [" + pString(permissions) + "] for: " + id + " -> " + nodeState.getNodeTypeName() + " granted: " + isGranted);
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
     * 
     * @param nodeState the node to check
     * @param nodeTypeName the node type name
     * @return boolean
     * @throws NoSuchNodeTypeException 
     */
    private boolean isInstanceOfType(NodeState nodeState, Name nodeTypeName) {
        if (nodeState.getNodeTypeName().equals(nodeTypeName)) {
            if (log.isTraceEnabled()) {
                log.trace("MATCH " + nodeState.getId() + " is of type: " + nodeTypeName);
            }
            return true;
        }
        NodeTypeDef ntd;
        try {
            ntd = ntReg.getNodeTypeDef(nodeState.getNodeTypeName());

            Name[] names = ntd.getSupertypes();
            for (Name n : names) {
                if (log.isTraceEnabled()) {
                    log.trace("CHECK " + nodeState.getId() + " " + n + " -> " + nodeTypeName);
                }
                if (n.equals(nodeTypeName)) {
                    if (log.isTraceEnabled()) {
                        log.trace("MATCH " + nodeState.getId() + " is a instance of type: " + nodeTypeName);
                    }
                    return true;
                }
            }
        } catch (NoSuchNodeTypeException e) {
            log.warn("NodeTypeDef not found for node: {}, type: ", nodeState.getId(), nodeState.getNodeTypeName());
            log.debug("NodeTypeDef not found", e);
        }
        return false;
    }
    
    /**
     * Check if a node matches the current FacetRule based on a 
     * check on the properties of the node.
     * @param nodeState the state of the node to check
     * @param facetRule the facet rule to check
     * @return true if the node matches the facet rule
     * @see FacetRule
     */
    private boolean matchPropertyWithFacetRule(NodeState nodeState, FacetRule rule) {
        // the hierchy manager is attic aware. The property can also be in the removed properties
        if (!nodeState.hasPropertyName(rule.getFacetName()) && !nodeState.getRemovedPropertyNames().contains(rule.getFacetName())) {
            log.trace("Node: {} doesn't have property {}", nodeState.getId(), rule.getFacetName());
            return false;
        }
        
        try {
            HippoPropertyId propertyId = new HippoPropertyId(nodeState.getNodeId(), rule.getFacetName());
            PropertyState state = (PropertyState) hierMgr.getItemState(propertyId);
            InternalValue[] iVals = state.getValues();
            boolean match = false;
            
            for (InternalValue iVal : iVals) {
                // types must match
                if (iVal.getType() == rule.getType()) {

                    // WILDCARD match
                    if (FacetAuthHelper.WILDCARD.equals(rule.getValue())) {
                        match = true;
                    }
                    
                    if (iVal.getType() == PropertyType.STRING) {
                        log.trace("Checking facet rule: {} (string) -> {}", rule, iVal.getString());
                        if (iVal.getString().equals(rule.getValue())) {
                            match = true;
                            break;
                        }
                    } else if (iVal.getType() == PropertyType.NAME) {
                        log.trace("Checking facet rule: {} (name) -> {}", rule, iVal.getQName());
                        if (iVal.getQName().toString().equals(rule.getValue())) {
                            match = true;
                            break;
                        }
                    }
                }
            }
            if (rule.isEqual()) {
                return match;
            } else {
                // the property is set but the values don't match
                return !match;
            }
        } catch (NoSuchItemStateException e) {
            log.debug("ItemState not found for node state: {} : error: {}", nodeState,e.getMessage());
            return false;
        } catch (ItemStateException e) {
            log.debug("ItemState exception while checking node state: {} error: {}", nodeState,e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if the node has a mixin type with a specific value
     * @param nodeState the node to check
     * @param value the mixin type to check for. This is the String representation of the Name
     * @return true if the node has the mixin type
     */
    private boolean hasMixinWithValue(NodeState nodeState, String value) {
        if (!nodeState.hasPropertyName(NameConstants.JCR_MIXINTYPES)) {
            return false;
        }
        
        HippoPropertyId propertyId = new HippoPropertyId(nodeState.getNodeId(), NameConstants.JCR_MIXINTYPES);
        try {
            PropertyState state = (PropertyState) hierMgr.getItemState(propertyId);
            InternalValue[] iVals = state.getValues();
            
            for (InternalValue iVal : iVals) {
                // types must match
                if (iVal.getType() == PropertyType.NAME) {

                    // WILDCARD match
                    if (value.equals(FacetAuthHelper.WILDCARD)) {
                        return true;
                    }
                    
                    log.trace("Checking facetVal: {} (name) -> {}", value, iVal.getQName());
                    if (iVal.getQName().toString().equals(value)) {
                        return true;
                    }
                }
            }
        } catch (NoSuchItemStateException e) {
            //e.printStackTrace();
            return false;
        } catch (ItemStateException e) {
            //e.printStackTrace();
            return false;
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
     * @throws NoSuchNodeTypeException
     */
    private NodeState getParentDoc(NodeState nodeState) throws NoSuchItemStateException, ItemStateException, NoSuchNodeTypeException {
        if (ntReg == null) {
            return null;
        }
        if (log.isTraceEnabled()) {
            log.trace("Checking " + nodeState.getId() + " ntn: " + nodeState.getNodeTypeName() + " for being part of a document model.");
        }
        // check if this is already the root of a document
        if (isInstanceOfType(nodeState, hippoDoc) || 
                nodeState.getNodeTypeName().equals(hippoHandle) || 
                nodeState.getNodeTypeName().equals(hippoFacetSearch) ||
                nodeState.getNodeTypeName().equals(hippoFacetSelect)) {
            if (log.isDebugEnabled()) {
                log.debug("Node is already document root: " + nodeState.getNodeTypeName());
            }
            return null;
        }
        // walk up in the hierarchy
        while (!rootNodeId.equals((NodeId)nodeState.getId())) {
            // shift one up in hierarchy
            nodeState = (NodeState) hierMgr.getItemState(nodeState.getParentId());
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
            if (isInstanceOfType(nodeState, hippoDoc)) {
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
