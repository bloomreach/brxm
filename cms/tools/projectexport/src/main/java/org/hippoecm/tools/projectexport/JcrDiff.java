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
package org.hippoecm.tools.projectexport;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.repository.api.HippoNode;

class JcrDiff {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public class Pair {
        Node node1;
        Node node2;
        public Pair(Node first, Node second) {
            this.node1 = first;
            this.node2 = second;
        }
    }
    public class ItemVisitor {
        /** visit of a matching pair */
        public void match(Node node1, Node node2) {
        }
        public void entering(Node node1, Node node2) {
        }
        public void leaving(Node node1, Node node2) {
        }
        public void diff(Node node1, Property prop1, Node node2, Property prop2) {
        }
        public void match(Property prop1, Property prop2) {
        }
    }

    ItemVisitor visitor = null;
    Set<String> excludes;
    List<Node> addedNodes = new LinkedList<Node>();
    
    public JcrDiff(Set<Pair> correlate, ItemVisitor visitor) {
        this.visitor = visitor;
    }
    public JcrDiff(Set<String> exclusions) {
        this.excludes = exclusions;
    }
    public boolean diff(Node node1, Node node2) throws RepositoryException {
        addedNodes.clear();
        return compare(node1, node2) == 1.0;
    }

    final double SCORE_NAME = 0.3;
    final double SCORE_PRIMARY = 0.20;
    final double SCORE_MIXINS  = 0.05;
    final double SCORE_ORDERABLE = 0.1;
    final double SCORE_PROPERTIES = 0.2;
    final double SCORE_CHILDREN = 0.15;

    /** a set of paths which contain changes below them, but not above them */
    Set<String> changes;

    private double compare(Node node1, Node node2) throws RepositoryException {
        double score = 1.0;

        // name of nodes comparison
        if(!node1.getName().equals(node2.getName())) {
            score -= SCORE_NAME;
        }

        // primary node type comparison
        if(!node1.getPrimaryNodeType().getName().equals(node2.getPrimaryNodeType().getName())) {
            score -= SCORE_PRIMARY;
        }

        SortedSet<String> nodetypes1 = new TreeSet<String>();
        SortedSet<String> nodetypes2 = new TreeSet<String>();
        for (NodeType nt : node1.getMixinNodeTypes()) {
            nodetypes1.add(nt.getName());
        }
        for (NodeType nt : node2.getMixinNodeTypes()) {
            nodetypes2.add(nt.getName());
        }
        if (!nodetypes1.equals(nodetypes2)) {
            score -= SCORE_MIXINS;
        }

        // property comparison
        ComparePropertySet propertySet1 = new ComparePropertySet(node1);
        ComparePropertySet propertySet2 = new ComparePropertySet(node2);
        Iterator<Property> propertyIter1 = propertySet1.iterator();
        Iterator<Property> propertyIter2 = propertySet2.iterator();
        Property property1 = null;
        Property property2 = null;
        int propertyCount = 0, propertyMismatch = 0;
        while(propertyIter1.hasNext() || propertyIter2.hasNext()) {
            ++propertyCount;
            if(property1 == null && propertyIter1.hasNext()) {
                property1 = propertyIter1.next();
            }
            if(property2 == null && propertyIter2.hasNext()) {
                property2 = propertyIter2.next();
            }
            if(property1 == null || property2 == null) {
                ++propertyMismatch;
                property1 = property2 = null;
                continue;
            }
            int compareResult = property1.getName().compareTo(property2.getName());
            if(compareResult == 0) {
                if(property1.getType() == property2.getType()) {
                    boolean equals = true;
                    switch(property1.getType()) {
                    case PropertyType.BOOLEAN:
                        equals = (property1.getBoolean() != property2.getBoolean());
                        break;
                    case PropertyType.LONG:
                        equals = (property1.getLong() != property2.getLong());
                        break;
                    case PropertyType.DOUBLE:
                        equals = (property1.getDouble() != property2.getDouble());
                        break;
                    case PropertyType.STRING:
                        equals = (property1.getString().equals(property2.getString()));
                        break;
                    default:
                        equals = property1.getString().equals(property2.getString());
                    }
                    if(equals) {
                        if(visitor != null) {
                            visitor.match(property1, property2);
                        }
                    } else {
                        if(visitor != null) {
                            visitor.diff(node1, property1, node2, property2);
                        }
                        ++propertyMismatch;
                    }
                } else {
                    if(visitor != null) {
                        visitor.diff(node1, property1, node2, property2);
                    }
                    ++propertyMismatch;
                }
                property1 = property2 = null;
            } else if(compareResult < 0) {
                if(visitor != null) {
                    visitor.diff(node1, property1, node2, null);
                }
                ++propertyMismatch;
                property1 = null;
            } else { // compareResult > 0
                if(visitor != null) {
                    visitor.diff(node1, null, node2, property2);
                }
                ++propertyMismatch;
                property2 = null;
            }
        }
        if(propertyMismatch > 0) {
            score -= SCORE_PROPERTIES * propertyMismatch / propertyCount;
        }

        boolean childrenModified = false;
        boolean childrenAdded = false;
        int index1 = 0, index2 = 0;
        ArrayList<Node> children1 = new ArrayList<Node>();
        ArrayList<Node> children2 = new ArrayList<Node>();
        List<Node> preliminaryAddedNodes = new LinkedList<Node>();
        for(NodeIterator iter = node1.getNodes(); iter.hasNext(); )
            children1.add(iter.nextNode());
        for(NodeIterator iter = node2.getNodes(); iter.hasNext(); )
            children2.add(iter.nextNode());
        while(index1 < children1.size() || index2 < children2.size()) {
            Node child1 = (index1 < children1.size() ? children1.get(index1) : null);
            Node child2 = (index2 < children2.size() ? children2.get(index2) : null);
            if(child1 == null) {
                childrenAdded = true;
                preliminaryAddedNodes.add(child2);
                if(visitor != null) {
                    visitor.diff(null, null, child2, null);
                }
                ++index2;
                continue;
            }
            if(child2 == null) {
                childrenModified = true;
                if(visitor != null) {
                    visitor.diff(child1, null, null, null);
                }
                ++index1;
                continue;
            }
            if(!child1.isSame(((HippoNode)child1).getCanonicalNode())) {
                ++index1;
                continue;
            }
            if(!child2.isSame(((HippoNode)child2).getCanonicalNode())) {
                ++index2;
                continue;
            }
            int nameCompare = child1.getName().compareTo(child2.getName());
            if(nameCompare < 0) {
                childrenModified = true;
                if(visitor != null) {
                    visitor.diff(child1, null, null, null);
                }
                ++index1;
            } else if(nameCompare > 0) {
                childrenAdded = true;
                if(visitor != null) {
                    visitor.diff(null, null, child2, null);
                }
                ++index2;
            } else {
                if(!excludes.contains(child1.getPath())) {
                    if (compare(child1, child2) != 1.0) {
                        childrenModified = true;
                        if (visitor != null) {
                            visitor.diff(child1, null, child2, null);
                        }
                    } else {
                        if (visitor != null) {
                            visitor.match(child1, child2);
                        }
                    }
                }
                ++index1;
                ++index2;
            }
        }
        if(childrenAdded) {
            addedNodes.addAll(preliminaryAddedNodes);
        }
        if(childrenModified || childrenAdded) {
            score -= SCORE_CHILDREN;
        }

        return score;
    }

    class ComparePropertySet extends TreeSet<Property> {
        public ComparePropertySet() {
            super(new Comparator<Property>() {
                public int compare(Property p1, Property p2) {
                    try {
                        return p1.getName().compareTo(p2.getName());
                    } catch(RepositoryException ex) {
                        return 0;
                    }
                }
                @Override
                public boolean equals(Object obj) {
                    return getClass().equals(obj.getClass());
                }
                @Override
                public int hashCode() {
                    int hash = 5;
                    return hash;
                }
            });
        }

        public ComparePropertySet(Node parent) throws RepositoryException {
            this();
            add(parent);
        }

        public void add(Node parent) throws RepositoryException {
            for (PropertyIterator iter = parent.getProperties(); iter.hasNext();) {
                Property property = iter.nextProperty();
                for (String excludePath : excludes) {
                    property.getPath().matches(excludePath);
                }
            }
        }
    }
}
