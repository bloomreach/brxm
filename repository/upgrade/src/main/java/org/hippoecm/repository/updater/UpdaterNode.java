/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.repository.updater;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;

import javax.jcr.AccessDeniedException;
import javax.jcr.Binary;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidLifecycleTransitionException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.ItemDefinition;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;

import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoWorkspace;

final public class UpdaterNode extends UpdaterItem implements Node {

    boolean hollow;
    Map<String, List<UpdaterItem>> children;// = new LinkedHashMap<String, List<UpdaterItem>>();
    Map<UpdaterItem, String> reverse;// = new HashMap<UpdaterItem, String>();
    Set<UpdaterItem> removed;// = new HashSet<UpdaterItem>();

    // ordering with respect to siblings
    UpdaterNode predecessor;
    UpdaterNode successor;

    // child node ordering
    UpdaterNode head;
    UpdaterNode tail;

    UpdaterNode(UpdaterSession session, UpdaterNode target) throws RepositoryException {
        super(session, target);
        hollow = false;
        children = new LinkedHashMap<String, List<UpdaterItem>>();
        reverse = new HashMap<UpdaterItem, String>();
        removed = new HashSet<UpdaterItem>();
    }

    UpdaterNode(UpdaterSession session, Node origin, UpdaterNode target) {
        super(session, origin, target);
        hollow = true;
    }

    private final void substantiate() throws RepositoryException {
        if (!hollow)
            return;
        /* This is a precaution measure, checkout any node (or parent) that is being visited
         * regardless if it is necessary (the commit phase has a more fine grained checkout)
         * FIXME: when having a good testset, it should be measured whether these two checkouts
         * are truely neccesary
         */
        if(((Node)origin).isNodeType("mix:versionable")) {
            if (!((Node)origin).isCheckedOut()) {
                ((Node)origin).checkout();
            }
        }
        if(origin.getDepth() > 0 && origin.getParent().isNodeType("mix:versionable")) {
            if (!origin.getParent().isCheckedOut()) {
                origin.getParent().checkout();
            }
        }
        children = new LinkedHashMap<String, List<UpdaterItem>>();
        reverse = new HashMap<UpdaterItem, String>();
        removed = new HashSet<UpdaterItem>();
        UpdaterNode predecessor = null;
        for (NodeIterator iter = ((Node) origin).getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            String name = child.getName();
            if (!children.containsKey(name))
                children.put(name, new LinkedList<UpdaterItem>());
            List<UpdaterItem> siblings = children.get(name);
            UpdaterNode updaterNode = new UpdaterNode(session, child, this);
            siblings.add(updaterNode);
            if (predecessor != null) {
                updaterNode.predecessor = predecessor;
                predecessor.successor = updaterNode;
            }
            predecessor = updaterNode;
            if (head == null) {
                head = updaterNode;
            }
            tail = updaterNode;
        }
        for (PropertyIterator iter = ((Node) origin).getProperties(); iter.hasNext();) {
            Property child = iter.nextProperty();
            String name = ":" + child.getName();
            if (!children.containsKey(name))
                children.put(name, new LinkedList<UpdaterItem>());
            List<UpdaterItem> siblings = children.get(name);
            siblings.add(new UpdaterProperty(session, child, this));
        }
        for (Map.Entry<String, List<UpdaterItem>> items : children.entrySet()) {
            String name = items.getKey();
            for (UpdaterItem item : items.getValue()) {
                reverse.put(item, name);
            }
        }
        hollow = false;
    }

    private UpdaterProperty setProperty(String name, UpdaterProperty property) throws RepositoryException {
        name = ":" + name;
        List<UpdaterItem> siblings = new LinkedList<UpdaterItem>();
        siblings.add(property);
        children.put(name, siblings);
        reverse.put(property, name);
        return property;
    }

    public void setPrimaryNodeType(String name) throws RepositoryException {
        setProperty("jcr:primaryType", name);
    }

    private String[] getInternalProperty(String name) throws ValueFormatException, RepositoryException {
        if (hollow) {
            if (origin != null && ((Node) origin).hasProperty(name)) {
                String[] strings;
                Property property = ((Node) origin).getProperty(name);
                if (property.getDefinition().isMultiple()) {
                    Value[] values = property.getValues();
                    strings = new String[values.length];
                    for (int i = 0; i < strings.length; i++)
                        strings[i] = values[i].getString();
                } else
                    strings = new String[]{property.getString()};
                return strings;
            } else
                return new String[0];
        } else {
            List<UpdaterItem> items = children.get(":" + name);
            if (items != null && items.size() > 0) {
                String[] strings;
                UpdaterProperty property = (UpdaterProperty) items.get(0);
                if (property.isMultiple()) {
                    Value[] values = property.getValues();
                    strings = new String[values.length];
                    for (int i = 0; i < strings.length; i++)
                        strings[i] = values[i].getString();
                } else
                    strings = new String[]{property.getString()};
                return strings;
            } else
                return new String[0];
        }
    }

    void commit() throws RepositoryException {
        Item oldOrigin = null;
        boolean nodeTypesChanged;
        boolean nodeLocationChanged;
        boolean nodeRelinked = false;
        String nodeName = getName();
        String noSameNameSiblingWorkaround = "hipposys:unstructured";

        if (origin != null) {
            if (origin instanceof HippoNode) {
                try {
                    if (!origin.isSame(((HippoNode)origin).getCanonicalNode())) {
                        return;
                    }
                } catch (RepositoryException ex) {
                    return;
                }
            }
        }

        if (origin != null && getInternalProperty("jcr:primaryType").length > 0) {
            nodeTypesChanged = !((Node) origin).getPrimaryNodeType().getName().equals(getInternalProperty("jcr:primaryType")[0]);
        } else {
            nodeTypesChanged = true;
        }

        if (origin == null || parent == null) {
            nodeLocationChanged = false;
        } else {
            nodeLocationChanged = (!parent.origin.isSame(origin.getParent()) || !origin.getName().equals(nodeName));
        }

        if(UpdaterEngine.log.isDebugEnabled()) {
            UpdaterEngine.log.debug("commit node "+getPath()+" origin "+(origin!=null?origin.getPath():"null")+(origin != null&&((Node)origin).isNodeType("mix:referenceable") ? ",uuid="+((Node)origin).getUUID() : "")+(nodeTypesChanged?" type changed":"")+(nodeLocationChanged?" location changed":""));
        }

        if (!hollow && origin != null && origin.isNode()) {
            if(((Node)origin).isNodeType("mix:versionable") && !((Node)origin).isCheckedOut()) {
                if(UpdaterEngine.log.isDebugEnabled()) {
                    UpdaterEngine.log.debug("commit checkout "+origin.getPath());
                }
                ((Node)origin).checkout();
            }
        }
        if (nodeTypesChanged) {
            if (!hollow) {
                oldOrigin = origin;
                {
                    if(UpdaterEngine.log.isDebugEnabled()) {
                        UpdaterEngine.log.debug("commit create "+getPath()+" in "+((Node)parent.origin).getPath()+" (primary type "+((Node)parent.origin).getProperty("jcr:primaryType").getString()+") type "+getInternalProperty("jcr:primaryType")[0]);
                    }
                    if(!((Node)parent.origin).isCheckedOut()) {
                        if(UpdaterEngine.log.isDebugEnabled()) {
                            UpdaterEngine.log.debug("commit checkout parent "+((Node)parent.origin).getPath());
                        }
                        ((Node)parent.origin).checkout();
                    }
                    String[] primaryType = getInternalProperty("jcr:primaryType");
                    if (primaryType != null && primaryType.length > 0) {
                        try {
                            try {
                                origin = ((Node)parent.origin).addNode(nodeName, primaryType[0]);
                            } catch (ConstraintViolationException ex) {
                                UpdaterEngine.log.error("failed to create " + getPath() + " in " + ((Node)parent.origin).getPath() + " (primary type " + ((Node)parent.origin).getProperty("jcr:primaryType").getString() + ") type " + getInternalProperty("jcr:primaryType")[0], ex);
                                throw ex;
                            }
                            noSameNameSiblingWorkaround = null;
                            // remove any autocreated non-protected nodes and properties
                            for (NodeIterator autoCreatedIter = ((Node)origin).getNodes(); autoCreatedIter.hasNext();) {
                                Node autoCreated = autoCreatedIter.nextNode();
                                ItemDefinition definition = autoCreated.getDefinition();
                                if (definition.isAutoCreated() && !definition.isProtected()) {
                                    autoCreated.remove();
                                }
                            }
                            for (PropertyIterator autoCreatedIter = ((Node)origin).getProperties(); autoCreatedIter.hasNext();) {
                                Property autoCreated = autoCreatedIter.nextProperty();
                                ItemDefinition definition = autoCreated.getDefinition();
                                if (definition.isAutoCreated() && !definition.isProtected()) {
                                    autoCreated.remove();
                                }
                            }
                        } catch(ItemExistsException ex) {
                            if(UpdaterEngine.log.isDebugEnabled()) {
                                UpdaterEngine.log.debug("commit work around no-same-name-sibling "+parent.origin.getPath()+"/"+noSameNameSiblingWorkaround);
                            }
                            origin = ((Node)parent.origin).addNode(noSameNameSiblingWorkaround, primaryType[0]);
                        }
                    } else {
                        origin = ((Node)parent.origin).addNode(nodeName);
                        noSameNameSiblingWorkaround = null;
                    }
                    if (oldOrigin != null) {
                        nodeRelinked = true;
                    }
                }
            } else {
                noSameNameSiblingWorkaround = null;
            }
        } else if(nodeLocationChanged) {
            String name = nodeName;
            UpdaterEngine.log.debug("move unchanged node "+origin.getPath()+" to "+parent.origin.getPath()+"/"+name+" ("+((Node)parent.origin).getProperty("jcr:primaryType").getString()+")");
            if(!origin.getParent().isCheckedOut()) {
                if(UpdaterEngine.log.isDebugEnabled()) {
                    UpdaterEngine.log.debug("commit checkout parent "+((Node)parent.origin).getPath());
                }
                origin.getParent().checkout();
            }
            try {
                origin.getSession().move(origin.getPath(), (parent.origin.getPath().equals("/") ? "/"+name : parent.origin.getPath()+"/"+name));
                origin = null;
                for(NodeIterator findMoved = ((Node)parent.origin).getNodes(name); findMoved.hasNext(); ) {
                    origin = findMoved.nextNode();
                }
                if(origin == null) {
                    UpdaterEngine.log.error("could not find moved node in "+parent.origin.getPath()+" named "+name);
                }
            } catch (ConstraintViolationException ex) {
                // happens in case of mandatory node that was kept left behind.
                UpdaterEngine.log.warn("dropping old mandatory node"+((Node)parent.origin).getPath());
            } catch(ItemExistsException ex) {
                origin = ((Node)parent.origin).getNode(name);
            }
            noSameNameSiblingWorkaround = null;
        } else {
            noSameNameSiblingWorkaround = null;
        }

        if (!hollow) {
            Set<String> curMixins = new TreeSet<String>();
            Set<String> newMixins = new TreeSet<String>();
            String[] mixins = getInternalProperty("jcr:mixinTypes");
            if (mixins != null) {
                for(String mixin : mixins) {
                    newMixins.add(mixin);
                }
            }
            if (!((Node)origin).isCheckedOut()) {
                if(UpdaterEngine.log.isDebugEnabled()) {
                    UpdaterEngine.log.debug("commit checkout "+((Node)origin).getPath());
                }
                ((Node)origin).checkout();
            }
            if(((Node)origin).hasProperty("jcr:mixinTypes")) {
                for(Value mixin : ((Node)origin).getProperty("jcr:mixinTypes").getValues()) {
                    curMixins.add(mixin.getString());
                }
            }
            for(String mixin : newMixins) {
                if(!curMixins.contains(mixin)) {
                    if(UpdaterEngine.log.isDebugEnabled()) {
                        UpdaterEngine.log.debug("commit addMixin "+origin.getPath()+" mixin "+mixin);
                    }
                    try {
                        ((Node)origin).addMixin(mixin);
                    } catch(ConstraintViolationException ex) {
                        // deliberate ignore, node type already accepts any child node type
                    }
                }
            }
            if (nodeRelinked) {
                session.relink((Node)oldOrigin, (Node)origin);
            }

            List<NodeType> nodetypes = new LinkedList<NodeType>();
            NodeTypeManager ntMgr = origin.getSession().getWorkspace().getNodeTypeManager();
            nodetypes.add(ntMgr.getNodeType(getInternalProperty("jcr:primaryType")[0]));
            for(String mixin : newMixins) {
                nodetypes.add(ntMgr.getNodeType(mixin));
            }
            for (Map.Entry<String, List<UpdaterItem>> items : children.entrySet()) {
                String name = items.getKey();
                if (name.startsWith(":")) {
                    name = name.substring(1);
                    boolean isValid = false;
                    for(NodeType nodeType : nodetypes) {
                        PropertyDefinition[] defs = nodeType.getPropertyDefinitions();
                        boolean breakLoop = false;
                        for (int j = 0; j < defs.length; j++) {
                            if (defs[j].getName().equals("*")) {
                                isValid = true;
                            } else if (defs[j].getName().equals(name)) {
                                if (defs[j].isProtected()) {
                                    isValid = false;
                                } else {
                                    isValid = true;
                                }
                                // break out of the outermost loop
                                breakLoop = true;
                                break;
                            }
                        }
                        if(breakLoop)
                            break;
                    }
                    if (!isValid) {
                        continue;
                    }

                    if (items.getValue().size() > 0) {
                        UpdaterProperty property = (UpdaterProperty) items.getValue().get(0);
                        property.commit();
                    }
                } else {
                    for (UpdaterItem item : items.getValue()) {
                        ((UpdaterNode) item).commit();
                    }
                }
            }
            
            Node originNode = (Node) origin;
            if (originNode.getPrimaryNodeType().hasOrderableChildNodes() && tail != null) {
                UpdaterNode cursor = tail.predecessor;
                while (cursor != null) {
                    Node sucessorNode = (Node) cursor.successor.origin;
                    Node cursorNode = (Node) cursor.origin;
                    originNode.orderBefore(cursorNode.getName() + "[" + cursorNode.getIndex() + "]",
                            sucessorNode.getName() + "[" + sucessorNode.getIndex() + "]");
                    cursor = cursor.predecessor;
                }
            }
            for (UpdaterItem item : removed) {
                if (item.origin != null) {
                    ItemDefinition definition = ( item.origin.isNode() ? ((Node)item.origin).getDefinition() : ((Property)item.origin).getDefinition() );
                    if(UpdaterEngine.log.isDebugEnabled()) {
                        UpdaterEngine.log.debug("commit remove old child "+item.origin.getPath());
                    }
                    if(!definition.isProtected()) {
                        item.origin.remove();
                    }
                }
            }
            for(String mixin : curMixins) {
                if(!newMixins.contains(mixin)) {
                    if(UpdaterEngine.log.isDebugEnabled()) {
                        UpdaterEngine.log.debug("commit removeMixin "+origin.getPath()+" mixin "+mixin);
                    }
                    ((Node)origin).removeMixin(mixin);
                }
            }
            if (oldOrigin != null) {
                if(UpdaterEngine.log.isDebugEnabled()) {
                    UpdaterEngine.log.debug("commit remove old origin "+oldOrigin.getPath());
                }
                try {
                    /* let the node exists (will probably be removed later, in case of a mandatory node.1 */
                    if(!((Node)oldOrigin).getDefinition().isMandatory() || ((Node)oldOrigin).getDefinition().allowsSameNameSiblings()) {
                        oldOrigin.remove();
                    } else {
                        UpdaterEngine.log.warn("not removing mandatory node "+((Node)oldOrigin).getPath());
                    }
                } catch (RepositoryException ex) {
                    try {
                        UpdaterEngine.log.warn("cannot remove no longer available old origin node "+origin.getPath());
                    } catch(RepositoryException e) {
                        UpdaterEngine.log.warn("cannot remove no longer available old origin node");
                    }
                }
            }
            if (noSameNameSiblingWorkaround != null) {
                if (UpdaterEngine.log.isDebugEnabled()) {
                    UpdaterEngine.log.debug("commit restore from no-same-name-sibling "+parent.origin.getPath()+"/"+nodeName);
                }

                if (((Node)parent.origin).hasNode(nodeName)) {
                    Node existingNode = ((Node)parent.origin).getNode(nodeName);
                    NodeDefinition existingNodeDefinition = existingNode.getDefinition();
                    if (!existingNodeDefinition.allowsSameNameSiblings() && existingNodeDefinition.isMandatory()) {
                        if (UpdaterEngine.log.isDebugEnabled()) {
                            UpdaterEngine.log.debug("commit restore from no-same-name-sibling remove old mandatory child");
                        }
                        existingNode.remove();
                    }
                }
                parent.origin.getSession().move(parent.origin.getPath() + "/" + noSameNameSiblingWorkaround, parent.origin.getPath() + "/" + nodeName);
            }
        }
    }

    private UpdaterNode resolveNode(String relPath) throws PathNotFoundException, RepositoryException {
        if (!relPath.contains("/"))
            return this;
        Node node = this, last = this;
        for (StringTokenizer iter = new StringTokenizer(relPath, "/"); iter.hasMoreTokens();) {
            last = node;
            node = node.getNode(iter.nextToken());
        }
        ((UpdaterNode)last).substantiate();
        return (UpdaterNode) last;
    }

    private String resolveName(String relPath) {
        if (relPath.contains("/"))
            relPath = relPath.substring(relPath.lastIndexOf("/") + 1);
        if (relPath.contains("[") && relPath.endsWith("]"))
            relPath = relPath.substring(0, relPath.indexOf("["));
        return relPath;
    }

    private int resolveIndex(String relPath) {
        if (relPath.contains("/"))
            relPath = relPath.substring(relPath.lastIndexOf("/") + 1);
        if (relPath.contains("["))
            return Integer.parseInt(relPath.substring(relPath.indexOf("[") + 1, relPath.lastIndexOf("]")));
        else
            return 0;
    }

    private UpdaterItem getItem(String relPath, boolean isProperty) throws PathNotFoundException, RepositoryException {
        substantiate();
        String name = relPath;
        String sequel = null;
        int index = 0;
        if (name.contains("/")) {
            sequel = name.substring(name.indexOf("/") + 1);
            name = name.substring(0, name.indexOf("/"));
        }
        if (name.contains("[") && name.endsWith("]")) {
            index = Integer.parseInt(name.substring(name.indexOf("[") + 1, name.length() - 1)) - 1;
            name = name.substring(0, name.indexOf("["));
        }
        if (sequel != null) {
            List<UpdaterItem> items = children.get(name);
            if (items == null)
                throw new PathNotFoundException(name);
            if (index >= items.size())
                throw new PathNotFoundException(name + "[" + index + "]");
            UpdaterNode item = (UpdaterNode) items.get(index);
            return item.getItem(sequel, isProperty);
        } else {
            if (isProperty) {
                List<UpdaterItem> items = children.get(":" + name);
                if (items == null)
                    throw new PathNotFoundException(name);
                if (items.isEmpty())
                    throw new PathNotFoundException(name);
                return items.get(0);
            } else {
                List<UpdaterItem> items = children.get(name);
                if (items == null)
                    throw new PathNotFoundException(name);
                if (index >= items.size())
                    throw new PathNotFoundException(name + "[" + index + "]");
                return items.get(index);
            }
        }
    }

    public NodeType[] getNodeTypes() throws RepositoryException {
        Vector<NodeType> nodeTypes = new Vector<NodeType>();
        try {
            NodeType nodeType = session.getNewType(getProperty("jcr:primaryType").getString());
            nodeTypes.add(nodeType);
            if (hasProperty("jcr:mixinTypes")) {
                Value[] mixins = getProperty("jcr:mixinTypes").getValues();
                for (int i = 0; i < mixins.length; i++) {
                    try {
                        nodeType = session.getNewType(mixins[i].getString());
                        nodeTypes.add(nodeType);
                    } catch(NoSuchNodeTypeException ex) {
                        // ignore
                    }
                }
            }
        } catch (PathNotFoundException ex) {
        }
        return nodeTypes.toArray(new NodeType[nodeTypes.size()]);
    }

    // javax.jcr.Item interface

    public boolean isNode() {
        return true;
    }

    @Deprecated
    public void accept(ItemVisitor visitor) throws RepositoryException {
        visitor.visit(this);
    }

    @Override
    public void remove() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (predecessor != null) {
            predecessor.successor = successor;
        }
        if (successor != null) {
            successor.predecessor = predecessor;
        }
        if (parent.tail == this) {
            parent.tail = predecessor;
        }
        if (parent.head == this) {
            parent.head = successor;
        }
        super.remove();
    }

    // javax.jcr.Node interface

    public Node addNode(String relPath) throws RepositoryException {
        substantiate();
        return this.addNode(relPath, null);
    }

    public Node addNode(String relPath, String primaryNodeTypeName) throws RepositoryException {
        substantiate();
        String name = relPath;
        if (relPath.contains("/")) {
            HierarchyResolver manager = ((UpdaterWorkspace) (session.getWorkspace())).getHierarchyResolver();
            HierarchyResolver.Entry last = new HierarchyResolver.Entry();
            manager.getItem(this, relPath, false, last);
            name = last.relPath;
            if (name.contains("/")) {
                throw new PathNotFoundException(name.substring(0, name.lastIndexOf("/")));
            }
        }
        if(name.contains("[")) {
            name = name.substring(0, name.indexOf("["));
        }
        UpdaterNode child = new UpdaterNode(session, this);
        if (tail != null) {
            child.predecessor = tail;
            tail.successor = child;
        }
        tail = child;
        if (head == null) {
            head = child;
        }
        List<UpdaterItem> siblings;
        if (children.containsKey(name)) {
            siblings = children.get(name);
        } else {
            siblings = new LinkedList<UpdaterItem>();
            children.put(name, siblings);
        }
        siblings.add(child);
        reverse.put(child, name);
        child.setPrimaryNodeType(primaryNodeTypeName);
        return child;
    }

    // FIXME: test whether this actually works
    public void orderBefore(String srcChildRelPath, String destChildRelPath) throws UnsupportedRepositoryOperationException, VersionException, ConstraintViolationException, ItemNotFoundException, LockException, RepositoryException {
        substantiate();
        if (srcChildRelPath.contains("/") || (destChildRelPath != null && destChildRelPath.contains("/")))
            throw new ConstraintViolationException();
        if (!getPrimaryNodeType().hasOrderableChildNodes())
            throw new UnsupportedRepositoryOperationException();
        UpdaterNode srcNode = (UpdaterNode) getItem(srcChildRelPath, false);
        if (destChildRelPath == null) {
            // order last
            if (tail == srcNode) {
                return;
            }

            UpdaterNode predecessor = srcNode.predecessor;
            UpdaterNode successor = srcNode.successor;
            if (predecessor != null) {
                predecessor.successor = successor;
            }
            if (successor != null) {
                successor.predecessor = predecessor;
            }
            if (head == srcNode && tail != srcNode) {
                head = srcNode.successor;
            }
            tail.successor = srcNode;
            srcNode.predecessor = tail;
            srcNode.successor = null;
            tail = srcNode;

            LinkedList<UpdaterItem> siblings = (LinkedList<UpdaterItem>) children.get(srcNode.getName());
            siblings.remove(srcNode);
            siblings.addLast(srcNode);
        } else {
            UpdaterNode destNode = (UpdaterNode) getItem(destChildRelPath, false);
            if (srcNode == destNode || srcNode.predecessor == null || destNode.successor == null
                    || srcNode.successor == destNode || tail == destNode || head == srcNode) {
                return;
            }

            if (head == destNode) {
                head = srcNode;
            }
            if (tail == srcNode) {
                tail = srcNode.predecessor;
            } else {
                srcNode.successor.predecessor = srcNode.predecessor;
            }
            srcNode.predecessor.successor = srcNode.successor;

            srcNode.predecessor = destNode.predecessor;
            if (destNode.predecessor != null) {
                destNode.predecessor.successor = srcNode;
            }
            srcNode.successor = destNode;
            destNode.predecessor = srcNode;

            int index = 0;
            String name = srcNode.getName();
            UpdaterNode cursor = head;
            while (cursor != null) {
                if (cursor == srcNode) {
                    break;
                }
                if (name.equals(cursor.getName())) {
                    index++;
                }
                cursor = cursor.successor;
            }
            LinkedList<UpdaterItem> siblings = (LinkedList<UpdaterItem>) children.get(srcNode.getName());
            siblings.remove(srcNode);
            siblings.add(index, srcNode);
        }
    }

    public Property setProperty(String name, Value value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return setProperty(name, value, value.getType());
    }

    public Property setProperty(String name, Value value, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        substantiate();
        if (!hasProperty(name)) {
            UpdaterNode propertyParent = resolveNode(name);
            UpdaterProperty child = new UpdaterProperty(session, propertyParent);
            propertyParent.setProperty(resolveName(name), child);
            child.setValue(value);
            return child;
        } else {
            Property child = getProperty(name);
            child.setValue(value);
            return child;
        }
    }

    public Property setProperty(String name, Value[] values) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        substantiate();
        if (values == null) {
            Property property = getProperty(name);
            property.remove();
            return property;
        }
        if (!hasProperty(name)) {
            UpdaterProperty child = new UpdaterProperty(session, this);
            resolveNode(name).setProperty(resolveName(name), child);
            child.setValue(values);
            return child;
        } else {
            Property child = getProperty(name);
            child.setValue(values);
            return child;
        }
    }

    public Property setProperty(String name, Value[] values, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return setProperty(name, values);
    }

    public Property setProperty(String name, String[] strings) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (strings == null) {
            Property property = getProperty(name);
            property.remove();
            return property;
        }
        Value[] values = new Value[strings.length];
        for (int i = 0; i < values.length; i++) {
            values[i] = session.valueFactory.createValue(strings[i]);
        }
        return setProperty(name, values);
    }

    public Property setProperty(String name, String[] values, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return setProperty(name, values);
    }

    public Property setProperty(String name, String value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return setProperty(name, session.valueFactory.createValue(value));
    }

    public Property setProperty(String name, String value, int type) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return setProperty(name, session.valueFactory.createValue(value));
    }

    public Property setProperty(String name, InputStream value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return setProperty(name, session.valueFactory.createValue(value));
    }

    public Property setProperty(String name, boolean value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return setProperty(name, session.valueFactory.createValue(value));
    }

    public Property setProperty(String name, double value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return setProperty(name, session.valueFactory.createValue(value));
    }

    public Property setProperty(String name, long value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return setProperty(name, session.valueFactory.createValue(value));
    }

    public Property setProperty(String name, Calendar value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return setProperty(name, session.valueFactory.createValue(value));
    }

    public Property setProperty(String name, Node value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return setProperty(name, session.valueFactory.createValue(value));
    }

    public Node getNode(String relPath) throws PathNotFoundException, RepositoryException {
        substantiate();
        return (UpdaterNode) getItem(relPath, false);
    }

    public NodeIterator getNodes() throws RepositoryException {
        substantiate();
        Set<UpdaterNode> set = new LinkedHashSet<UpdaterNode>();
        for (Map.Entry<String, List<UpdaterItem>> items : children.entrySet()) {
            if (!items.getKey().startsWith(":")) {
                for (UpdaterItem item : items.getValue()) {
                    set.add((UpdaterNode) item);
                }
            }
        }
        return new SetNodeIterator(set);
    }

    public NodeIterator getNodes(String namePattern) throws RepositoryException {
        substantiate();
        Set<UpdaterNode> set = new LinkedHashSet<UpdaterNode>();
        if (children.containsKey(namePattern)) {
            for (UpdaterItem item : children.get(namePattern)) {
                set.add((UpdaterNode) item);
            }
        }
        return new SetNodeIterator(set);
    }

    public Property getProperty(String relPath) throws PathNotFoundException, RepositoryException {
        substantiate();
        return (UpdaterProperty) getItem(relPath, true);
    }

    public PropertyIterator getProperties() throws RepositoryException {
        substantiate();
        Set<UpdaterProperty> set = new LinkedHashSet<UpdaterProperty>();
        for (Map.Entry<String, List<UpdaterItem>> items : children.entrySet()) {
            if (items.getKey().startsWith(":")) {
                for (UpdaterItem item : items.getValue()) {
                    set.add((UpdaterProperty) item);
                }
            }
        }
        return new SetPropertyIterator(set);
    }

    public PropertyIterator getProperties(String namePattern) throws RepositoryException {
        substantiate();
        Set<UpdaterProperty> set = new LinkedHashSet<UpdaterProperty>();
        if (children.containsKey(":" + namePattern)) {
            for (UpdaterItem item : children.get(":" + namePattern)) {
                set.add((UpdaterProperty) item);
            }
        }
        return new SetPropertyIterator(set);
    }

    @Deprecated
    public Item getPrimaryItem() throws ItemNotFoundException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public int getIndex() throws RepositoryException {
        if (parent == null)
            return 1;
        String name = parent.reverse.get(this);
        if (!parent.children.containsKey(name)) {
            throw new ItemNotFoundException();
        }
        Iterator<UpdaterItem> iter = parent.children.get(name).iterator();
        for (int index = 0; iter.hasNext(); index++) {
            if (iter.next() == this) {
                return index + 1;
            }
        }
        throw new UpdaterException("internal error");
    }

    public PropertyIterator getReferences() throws RepositoryException {
        substantiate();
        // FIXME: not supported at this time
        return new SetPropertyIterator(new LinkedHashSet<UpdaterProperty>());
    }

    public boolean hasNode(String relPath) throws RepositoryException {
        substantiate();
        try {
            if (resolveNode(relPath).children.containsKey(resolveName(relPath))) {
                if (resolveIndex(relPath) < resolveNode(relPath).children.get(resolveName(relPath)).size())
                    return true;
            }
        } catch(PathNotFoundException ex) {
            // deliberate ignore
        }
        return false;
    }

    public boolean hasProperty(String relPath) throws RepositoryException {
        substantiate();
        try {
            if (resolveNode(relPath).children.containsKey(":" + resolveName(relPath))) {
                if (resolveIndex(relPath) < resolveNode(relPath).children.get(":" + resolveName(relPath)).size())
                    return true;
            }
        } catch(PathNotFoundException ex) {
            // deliberate ignore
        }
        return false;
    }

    public boolean hasNodes() throws RepositoryException {
        substantiate();
        for (Map.Entry<String, List<UpdaterItem>> items : children.entrySet()) {
            if (!items.getKey().startsWith(":") && !items.getValue().isEmpty())
                return true;
        }
        return false;
    }

    public boolean hasProperties() throws RepositoryException {
        substantiate();
        for (Map.Entry<String, List<UpdaterItem>> items : children.entrySet()) {
            if (items.getKey().startsWith(":") && !items.getValue().isEmpty())
                return true;
        }
        return false;
    }

    public NodeType getPrimaryNodeType() throws RepositoryException {
        return getNodeTypes()[0];
    }

    public NodeType[] getMixinNodeTypes() throws RepositoryException {
        NodeType[] types = getNodeTypes();
        NodeType[] mixins = new NodeType[types.length-1];
        System.arraycopy(types, 1, mixins, 0, mixins.length);
        return mixins;
    }

    public boolean isNodeType(String nodeTypeName) throws RepositoryException {
        substantiate();
        try {
            for (NodeType type : getNodeTypes()) {
                if (type.isNodeType(nodeTypeName))
                    return true;
            }
        } catch (PathNotFoundException ex) {
            // deliberate ignore, in case of virtual nodes without primary type (bad idea, but be leniant at this time
        }
        return false;
    }

    public void addMixin(String mixinName) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        if (hasProperty("jcr:mixinTypes")) {
            Property mixinsProperty = getProperty("jcr:mixinTypes");
            Value[] mixins = mixinsProperty.getValues();
            for (int i = 0; i < mixins.length; i++) {
                if (mixins[i].getString().equals(mixinName))
                    return; // mixin already set
            }
            Value[] newMixins = new Value[mixins.length + 1];
            System.arraycopy(mixins, 0, newMixins, 0, mixins.length);
            newMixins[mixins.length] = session.valueFactory.createValue(mixinName);
            mixinsProperty.setValue(newMixins);
        } else {
            setProperty("jcr:mixinTypes", new String[] { mixinName });
        }
    }

    public void removeMixin(String mixinName) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        if (hasProperty("jcr:mixinTypes")) {
            Property mixinsProperty = getProperty("jcr:mixinTypes");
            Value[] mixins = mixinsProperty.getValues();
            for (int i = 0; i < mixins.length; i++) {
                if (mixins[i].getString().equals(mixinName)) {
                    Value[] newMixins = new Value[mixins.length - 1];
                    System.arraycopy(mixins, 0, newMixins, 0, i);
                    System.arraycopy(mixins, i + 1, newMixins, i, newMixins.length - i);
                    mixinsProperty.setValue(newMixins);
                    return;
                }
            }
        }
    }

    @Deprecated
    public boolean canAddMixin(String mixinName) throws NoSuchNodeTypeException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public NodeDefinition getDefinition() throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public Version checkin() throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void checkout() throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void doneMerge(Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void cancelMerge(Version version) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void update(String srcWorkspaceName) throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public NodeIterator merge(String srcWorkspace, boolean bestEffort) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public String getCorrespondingNodePath(String workspaceName) throws ItemNotFoundException, NoSuchWorkspaceException, AccessDeniedException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public boolean isCheckedOut() throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void restore(String versionName, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void restore(Version version, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void restore(Version version, String relPath, boolean removeExisting) throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void restoreByLabel(String versionLabel, boolean removeExisting) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public VersionHistory getVersionHistory() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public Version getBaseVersion() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public Lock lock(boolean isDeep, boolean isSessionScoped) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public Lock getLock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public void unlock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public boolean holdsLock() throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    @Deprecated
    public boolean isLocked() throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public Property setProperty(String name, Binary value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public Property setProperty(String name, BigDecimal value) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public NodeIterator getNodes(String[] nameGlobs) throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public PropertyIterator getProperties(String[] nameGlobs) throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public String getIdentifier() throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public PropertyIterator getReferences(String name) throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public PropertyIterator getWeakReferences() throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public PropertyIterator getWeakReferences(String name) throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public void setPrimaryType(String nodeTypeName) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public NodeIterator getSharedSet() throws RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public void removeSharedSet() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public void removeShare() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public void followLifecycleTransition(String transition) throws UnsupportedRepositoryOperationException, InvalidLifecycleTransitionException, RepositoryException {
        throw new UpdaterException("illegal method");
    }

    public String[] getAllowedLifecycleTransistions() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UpdaterException("illegal method");
    }
}
