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

import java.io.File;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import javax.security.auth.Subject;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.config.AccessManagerConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.nodetype.NodeTypeConflictException;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.AuthContext;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.UserPrincipal;
import org.apache.jackrabbit.core.state.LocalItemStateManager;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;

import org.hippoecm.repository.security.HippoAMContext;
import org.hippoecm.repository.security.principals.AdminPrincipal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionImpl extends org.apache.jackrabbit.core.SessionImpl {
    private static Logger log = LoggerFactory.getLogger(SessionImpl.class);

    /**
     * the user ID that was used to acquire this session
     */
    private String userId;

    protected SessionImpl(RepositoryImpl rep, AuthContext loginContext, WorkspaceConfig wspConfig)
            throws AccessDeniedException, RepositoryException {
        super(rep, loginContext, wspConfig);
        SetUserId();
        HippoLocalItemStateManager localISM = (HippoLocalItemStateManager) ((WorkspaceImpl)wsp).getItemStateManager();
        ((RepositoryImpl)rep).initializeLocalItemStateManager(localISM, this, loginContext.getSubject());
    }

    protected SessionImpl(RepositoryImpl rep, Subject subject, WorkspaceConfig wspConfig) throws AccessDeniedException,
            RepositoryException {
        super(rep, subject, wspConfig);
        SetUserId();
        HippoLocalItemStateManager localISM = (HippoLocalItemStateManager) ((WorkspaceImpl)wsp).getItemStateManager();
        ((RepositoryImpl)rep).initializeLocalItemStateManager(localISM, this, subject);
    }

    @Override
    protected SessionItemStateManager createSessionItemStateManager(LocalItemStateManager manager) {
        return new HippoSessionItemStateManager(((RepositoryImpl) rep).getRootNodeId(), manager, this);
    }

    @Override
    protected org.apache.jackrabbit.core.WorkspaceImpl createWorkspaceInstance(WorkspaceConfig wspConfig,
          SharedItemStateManager stateMgr, org.apache.jackrabbit.core.RepositoryImpl rep,
          org.apache.jackrabbit.core.SessionImpl session) {
        return new WorkspaceImpl(wspConfig, stateMgr, rep, session);
    }

    @Override
    protected org.apache.jackrabbit.core.ItemManager createItemManager(SessionItemStateManager itemStateMgr, HierarchyManager hierMgr) {
        return new ItemManager(itemStateMgr, hierMgr, this, ntMgr.getRootNodeDefinition(), ((RepositoryImpl)rep).getRootNodeId());
    }

    @Override
    protected AccessManager createAccessManager(Subject subject, HierarchyManager hierMgr) throws AccessDeniedException, RepositoryException {
        AccessManagerConfig amConfig = rep.getConfig().getAccessManagerConfig();
        try {
            HippoAMContext ctx = new HippoAMContext( new File(((RepositoryImpl)rep).getConfig().getHomeDir()),
                                                    ((RepositoryImpl)rep).getFileSystem(),
                                                    subject,
                                                    getItemStateManager().getAtticAwareHierarchyMgr(),
                                                    ((RepositoryImpl)rep).getNamespaceRegistry(),
                                                    wsp.getName(),
                                                    ((RepositoryImpl)rep).getNodeTypeRegistry());
            AccessManager accessMgr = (AccessManager) amConfig.newInstance();
            accessMgr.init(ctx);
            return accessMgr;
        } catch (AccessDeniedException ex) {
            throw ex;
        } catch (Exception ex) {
            String msg = "failed to instantiate AccessManager implementation: " + amConfig.getClassName();
            log.error(msg, ex);
            throw new RepositoryException(msg, ex);
        }
    }

    /**
     * Override jackrabbits default userid, because it just uses
     * the first principal it can find, which can lead to strange "usernames"
     */
    protected void SetUserId() {
        if (!subject.getPrincipals(SystemPrincipal.class).isEmpty()) {
            Principal principal = (Principal)  subject.getPrincipals(SystemPrincipal.class).iterator().next();
            userId = principal.getName();
        } else if (!subject.getPrincipals(AdminPrincipal.class).isEmpty()) {
            Principal principal = (Principal) subject.getPrincipals(AdminPrincipal.class).iterator().next();
            userId = principal.getName();
        } else if (!subject.getPrincipals(UserPrincipal.class).isEmpty()) {
            Principal principal = (Principal) subject.getPrincipals(UserPrincipal.class).iterator().next();
            userId = principal.getName();
        } else if (!subject.getPrincipals(AnonymousPrincipal.class).isEmpty()) {
            Principal principal = (Principal) subject.getPrincipals(AnonymousPrincipal.class).iterator().next();
            userId = principal.getName();
        } else {
            userId = "Unknown";
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getUserID() {
        return userId;
    }

    /**
     * Method to expose the authenticated users' principals
     * @return Set An unmodifialble set containing the principals
     */
    public Set<Principal> getUserPrincipals() {
        return Collections.unmodifiableSet(subject.getPrincipals());
    }

    public NodeIterator pendingChanges(Node node, String nodeType, boolean prune) throws NamespaceException,
                                                                              NoSuchNodeTypeException, RepositoryException {
        Name ntName;
        try {
            ntName = getQName(nodeType);
        } catch(IllegalNameException ex) {
            throw new NoSuchNodeTypeException(nodeType);
        }
        final Set<NodeId> filteredResults = new HashSet<NodeId>();
        if(node == null) {
            node = getRootNode();
            if(node.isModified() && (nodeType == null || node.isNodeType(nodeType))) {
                filteredResults.add(((NodeImpl)node).getNodeId());
            }
        }
        NodeId nodeId = ((NodeImpl)node).getNodeId();

        Iterator iter = itemStateMgr.getDescendantTransientItemStates(nodeId);
        while(iter.hasNext()) {
            NodeState state = (NodeState) iter.next();

            /* if the node type of the current node state is not of required
             * type (if set), continue with next.
             */
            if(nodeType != null) {
                if(!ntName.equals(state.getNodeTypeName())) {
                    Set mixins = state.getMixinTypeNames();
                    if(!mixins.contains(ntName)) {
                        // build effective node type of mixins & primary type
                        NodeTypeRegistry ntReg = getNodeTypeManager().getNodeTypeRegistry();
                        Name[] types = new Name[mixins.size() + 1];
                        mixins.toArray(types);
                        types[types.length - 1] = state.getNodeTypeName();
                        try {
                            if(!ntReg.getEffectiveNodeType(types).includesNodeType(ntName))
                                continue;
                        } catch(NodeTypeConflictException ntce) {
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
            if(prune) {
                HierarchyManager hierMgr = getHierarchyManager();
                for(Iterator<NodeId> i = filteredResults.iterator(); i.hasNext(); ) {
                    if(hierMgr.isAncestor(state.getNodeId(), i.next()))
                        i.remove();
                }
            }

            filteredResults.add(state.getNodeId());
        }

        return new NodeIterator() {
                private final org.apache.jackrabbit.core.ItemManager itemMgr = getItemManager();
                private Iterator<NodeId> iterator = filteredResults.iterator();
                private int pos = 0;
                public Node nextNode() {
                    return (Node) next();
                }
                public long getPosition() {
                    return pos;
                }
                public long getSize() {
                    return -1;
                }
                public void skip(long skipNum) {
                    if(skipNum < 0) {
                        throw new IllegalArgumentException("skipNum must not be negative");
                    } else if(skipNum == 0) {
                        return;
                    } else {
                        do {
                            NodeId id = iterator.next();
                            ++pos;
                        } while(--skipNum > 0);
                    }
                }
                public boolean hasNext() {
                    return iterator.hasNext();
                }
                public Object next() {
                    try {
                        NodeId id = iterator.next();
                        ++pos;
                        return itemMgr.getItem(id);
                    } catch(AccessDeniedException ex) {
                        return null;
                    } catch(ItemNotFoundException ex) {
                        return null;
                    } catch(RepositoryException ex) {
                        return null;
                    }
                }
                public void remove() {
                    throw new UnsupportedOperationException("remove");
                }
            };
    }
}
