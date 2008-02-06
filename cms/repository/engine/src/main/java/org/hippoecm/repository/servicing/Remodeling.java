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
package org.hippoecm.repository.servicing;

import java.io.InputStream;
import java.io.Serializable;

import java.util.Comparator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Iterator;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query ;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jackrabbit.core.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.spi.Name;

import org.hippoecm.repository.api.HippoNodeType;

public class Remodeling // implements Serializable
{
    protected final static Logger log = LoggerFactory.getLogger(Remodeling.class);

    /** The prefix of the namespace which has been changed
     */
    private String prefix;

    /** Paths to the changed nodes.
     */
    private Set<String> changes;

    /** Reference to the session in which the changes are prepared
     */
    transient Session session;

    Remodeling(Session session, String prefix) throws RepositoryException {
        this.session = session;
        this.prefix = prefix;
        changes = new TreeSet<String>();
    }

    public NodeIterator getNodes() {
        return new ChangedNodesIterator();
    }

    private class ChangedNodesIterator implements NodeIterator {
        Iterator<String> iter;
        int index;
        ChangedNodesIterator() {
            iter = changes.iterator();
            index = 0;
        }
        public Node nextNode() {
            try {
                Node node = (Node) session.getItem(iter.next());
                ++index;
                return node;
            } catch(PathNotFoundException ex) {
                return null;
            } catch(RepositoryException ex) {
                return null;
            }
        }
        public boolean hasNext() {
            return iter.hasNext();
        }
        public Object next() throws NoSuchElementException {
            try {
                Object object = session.getItem(iter.next());
                ++index;
                return object;
            } catch(RepositoryException ex) {
                return null;
            }
        }
        public void remove() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }
        public void skip(long skipNum) {
            while(skipNum-- > 0) {
                iter.next();
                ++index;
            }
        }
        public long getSize() {
            return changes.size();
        }
        public long getPosition() {
            return index;
        }
    }

    protected void traverse(Set<String> types, Node node, boolean copy, Node target) throws RepositoryException {
        if(copy) {
            for(PropertyIterator iter = node.getProperties(); iter.hasNext(); ) {
                Property prop = iter.nextProperty();
                if(!prop.equals("jcr:primaryType"))
                    target.setProperty(prop.getName(), prop.getValue());
            }
        }
        for(NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
            Node child = iter.nextNode();
            NodeType nodeType = child.getPrimaryNodeType();
            boolean found = false;
            for(Iterator<String> find = types.iterator(); find.hasNext(); )
                if(nodeType.isNodeType((find.next()))) {
                    found = true;
                    break;
                }
            if(found) {
                if(!copy)
                    iter.remove();
                Node newChild = target.addNode(child.getName(),
                                               prefix + nodeType.getName().substring(nodeType.getName().indexOf(":")));
                changes.add(newChild.getPath());
                traverse(types, child, true, newChild);
            } else if(copy) {
                Node newChild = target.addNode(child.getName(), nodeType.getName());
                traverse(types, child, true, newChild);
            } else {
                traverse(types, child, false, child);
            }
        }
    }

    public static Remodeling remodel(Session session, String prefix, InputStream cnd) throws NamespaceException, RepositoryException {
        Workspace workspace = session.getWorkspace();
        NamespaceRegistry nsreg = workspace.getNamespaceRegistry();
        NodeTypeManagerImpl ntmgr = (NodeTypeManagerImpl) workspace.getNodeTypeManager();
        NodeTypeRegistry ntreg = ntmgr.getNodeTypeRegistry();
        QueryManager qmgr = workspace.getQueryManager();

        // obtain namespace URI for prefix as in use
        String oldNamespaceURI = nsreg.getURI(prefix);

        // compute namespace URI for new model to be used
        int pos = oldNamespaceURI.lastIndexOf("/");
        if(pos < 0)
            throw new RepositoryException("Internal error; invalid namespace URI found in repository itself");
        if(oldNamespaceURI.lastIndexOf(".") > pos)
            pos = oldNamespaceURI.lastIndexOf(".");
        int newNamespaceVersion = Integer.parseInt(oldNamespaceURI.substring(pos+1));
        ++newNamespaceVersion;
        String newNamespaceURI = oldNamespaceURI.substring(0,pos+1) + newNamespaceVersion;

        // push new node type definition such that it will be loaded
        try {
            Node base = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH).getNode(HippoNodeType.INITIALIZE_PATH);
            Node node = base.getNode(prefix);
            node.setProperty(HippoNodeType.HIPPO_NAMESPACE, newNamespaceURI);
            node.setProperty(HippoNodeType.HIPPO_NODETYPES, cnd);
            session.save();

            // wait for node types to be reloaded
            session.refresh(true);
            while(base.getNode(prefix).hasProperty(HippoNodeType.HIPPO_NODETYPES) ||
                  base.getNode(prefix).hasProperty(HippoNodeType.HIPPO_NODETYPESRESOURCE)) {
                try {
                    Thread.sleep(500);
                    org.hippoecm.repository.Utilities.dump(base);
                } catch(InterruptedException ex) {
                }
                session.refresh(true);
            }
        } catch(ConstraintViolationException ex) {
            throw new RepositoryException("Hippo repository configuration not in order");
        } catch(LockException ex) {
            throw new RepositoryException("Hippo repository configuration not in order");
        } catch(ValueFormatException ex) {
            throw new RepositoryException("Hippo repository configuration not in order");
        } catch(VersionException ex) {
            throw new RepositoryException("Hippo repository configuration not in order");
        } catch(PathNotFoundException ex) {
            throw new RepositoryException("Hippo repository configuration not in order");
        }

        // compute old prefix, similar as in LocalHippoResository.initializeNamespace(NamespaceRegistry,String,uri)
        String oldPrefix = prefix + "_" + oldNamespaceURI.lastIndexOf("/");

        Set<Node> newNodes = new TreeSet<Node>();
        Set<String> changedNodeTypes = new TreeSet<String>();
        Name[] allNodeTypes = ntreg.getRegisteredNodeTypes();
        for(int i=0; i<allNodeTypes.length; i++) {
            if(allNodeTypes[i].getNamespaceURI().equals(oldNamespaceURI)) {
                changedNodeTypes.add(allNodeTypes[i].toString());
            }
        }
        Remodeling remodel = new Remodeling(session, prefix);
        remodel.traverse(changedNodeTypes, session.getRootNode(), false, session.getRootNode());
        return remodel;

        /*
        // find all nodes in namespace that was changed, and put into ordered set for depth-first traversal
        // for all these old-namespaced node types find the nodes of these types
        Name[] nodetypes = ntreg.getRegisteredNodeTypes();
        Set<Node> newNodes = new TreeSet<Node>();
        SortedSet<Node> oldNodes = new TreeSet<Node>(new Comparator<Node>() {
            boolean errorFlagged = false;
            public int compare(Node o1, Node o2) {
                try {
                    int order = o2.getDepth() - o1.getDepth();
                    if(order == 0)
                    order = o2.getIndex() - o1.getIndex();
                    return order;
                } catch(RepositoryException ex) {
                    if(!errorFlagged) {
                        log.error("Cannot determin depth of nodes to remap");
                        errorFlagged = true;
                    }
                    return 0;
                }
            }});
        try {
            for(int i=0; i<nodetypes.length; i++) {
                if(nodetypes[i].getNamespaceURI().equals(oldNamespaceURI)) {
                    Query query = qmgr.createQuery("select * from "+oldPrefix, Query.SQL);
                    QueryResult result = query.execute();
                    for(NodeIterator iter = result.getNodes(); iter.hasNext(); ) {
                        Node node = iter.nextNode();
                        if(node != null)
                            oldNodes.add(node);
                    }
                }
            }
        } catch(InvalidQueryException ex) {
            throw new RepositoryException("Internal error finding old namespaced nodes", ex);
        }

        // copy the content of these nodes to an more-or-less identical copy
        Map<String,Node> uuids = new TreeMap<String,Node>();
        try {
            for(Node node : oldNodes) {
                String nodeType = node.getPrimaryNodeType().getName();
                nodeType = prefix + nodeType.substring(nodeType.indexOf(":"));
                * We may need to check if the parent is also in the set, and if
                 * so, use the node instance from the set.  In order to implement
                 * this, it may be necessary to turn the set into an ordered map
                 *
                Node newNode = node.getParent().addNode(node.getName(), nodeType);
                SessionDecorator.copy(node, newNode);
                if(node.isNodeType("mix:referenceable")) {
                    uuids.put(node.getUUID(), newNode);
                }
                newNodes.add(newNode);
            }
        } catch(UnsupportedRepositoryOperationException ex) {
            throw new RepositoryException("cannot copy old nodes", ex);
        } catch(ConstraintViolationException ex) {
            throw new RepositoryException("cannot copy old nodes", ex);
        } catch(VersionException ex) {
            throw new RepositoryException("cannot copy old nodes", ex);
        } catch(NoSuchNodeTypeException ex) {
            throw new RepositoryException("cannot copy old nodes", ex);
        } catch(AccessDeniedException ex) {
            throw new RepositoryException("cannot copy old nodes", ex);
        } catch(PathNotFoundException ex) {
            throw new RepositoryException("cannot copy old nodes", ex);
        } catch(LockException ex) {
            throw new RepositoryException("cannot copy old nodes", ex);
        } catch(ItemNotFoundException ex) {
            throw new RepositoryException("cannot copy old nodes, conflict in copying", ex);
        } catch(ItemExistsException ex) {
            throw new RepositoryException("cannot copy old nodes because of same-name siblings", ex);
        }

        // remove the old nodes, has to be done after recreating to preserve ordering
        try {
            for(Node node : oldNodes) {
                node.remove();
            }
        } catch(ConstraintViolationException ex) {
            throw new RepositoryException("cannot remove old nodes", ex);
        } catch(LockException ex) {
            throw new RepositoryException("cannot remove old nodes", ex);
        } catch(VersionException ex) {
            throw new RepositoryException("cannot remove old nodes", ex);
        }

        // hunt all properties containing uuids to the old nodes and remap them
        try {
            Query query = qmgr.createQuery("//*[?", Query.XPATH);
            QueryResult result = query.execute();
            for(NodeIterator iter = result.getNodes(); iter.hasNext(); ) {
                Node n = iter.nextNode();
                for(PropertyIterator piter = n.getProperties(); piter.hasNext(); ) {
                    Property p = piter.nextProperty();
                    if(!p.getName().equals("jcr:uuid") && p.getType() == PropertyType.REFERENCE) {
                        if(uuids.containsKey(p.getString()))
                            p.setValue(uuids.get(p.getString()));
                    }
                }
            }
        } catch(ConstraintViolationException ex) {
            throw new RepositoryException("Internal error finding old uuid based nodes", ex);
        } catch(LockException ex) {
            throw new RepositoryException("Internal error finding old uuid based nodes", ex);
        } catch(VersionException ex) {
            throw new RepositoryException("Internal error finding old uuid based nodes", ex);
        } catch(ValueFormatException ex) {
            throw new RepositoryException("Internal error finding old uuid based nodes", ex);
        } catch(InvalidQueryException ex) {
            throw new RepositoryException("Internal error finding old uuid based nodes", ex);
        }

        return new Remodeling(session, newNodes);
*/
    }
}
