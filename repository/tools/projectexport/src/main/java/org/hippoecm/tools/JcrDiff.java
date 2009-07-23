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
package org.hippoecm.tools;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

public class JcrDiff {
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

    ItemVisitor visitor;
    Set<String> excludes;
    
    public JcrDiff(Set<Pair> correlate, ItemVisitor visitor) {
        this.visitor = visitor;
    }
    public JcrDiff(Set<String> exclusions) {
    }
    public boolean diff(Node node1, Node node2) {
        return false;
    }

    final double SCORE_NAME = 0.3;
    final double SCORE_PRIMARY = 0.25;
    final double SCORE_ORDERABLE = 0.1;
    final double SCORE_PROPERTIES = 0.2;
    final double SCORE_CHILDREN = 0.15;

    /** a set of paths which contain changes below them, but not above them */
    Set<String> changes;

    private double compare(Node node1, Node node2) throws RepositoryException {
        double score = 1.0;

        Comparator propertyComparator = new Comparator<Property>() {
            public int compare(Property p1, Property p2) {
                try {
                return p1.getName().compareTo(p2.getName());
                } catch(RepositoryException ex) {
                    return 0;
                }
            }
        };
        SortedSet<Property> props1 = new TreeSet<Property>(propertyComparator);
        SortedSet<Property> props2 = new TreeSet<Property>(propertyComparator);
        //addCompareProperties(props1, node1);
        //addCompareProperties(props2, node2);

        // name of nodes comparison
        if(!node1.getName().equals(node2.getName())) {
            score -= SCORE_NAME;
        }

        // primary node type comparison
        if(node1.getPrimaryNodeType().getName().equals(node2.getPrimaryNodeType().getName())) {
            score -= SCORE_PRIMARY;
        }

        // mixin node types comparison
        
        // orderability of child nodes comparison

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
                        equals = (property1.getString() != property2.getString());
                        break;
                    default:
                        equals = property1.getString().equals(property2.getString());
                    }
                    if(equals) {
                        visitor.match(property1, property2);
                    } else {
                        visitor.diff(node1, property1, node2, property2);
                        ++propertyMismatch;
                    }
                } else {
                    visitor.diff(node1, property1, node2, property2);
                    ++propertyMismatch;
                }
                property1 = property2 = null;
            } else if(compareResult < 0) {
                visitor.diff(node1, property1, node2, null);
                ++propertyMismatch;
                property1 = null;
            } else { // compareResult > 0
                visitor.diff(node1, null, node2, property2);
                ++propertyMismatch;
                property2 = null;
            }
        }
        if(propertyMismatch > 0) {
            score -= SCORE_PROPERTIES * propertyMismatch / propertyCount;
        }

        // compare child nodes
        CompareChildNodeSet childrenSet1 = new CompareChildNodeSet(node1);
        CompareChildNodeSet childrenSet2 = new CompareChildNodeSet(node2);
        Iterator<Node> childIter1 = childrenSet1.iterator();
        Iterator<Node> childIter2 = childrenSet2.iterator();
        Node child1 = null;
        Node child2 = null;
        int childCount = 0, childMismatch = 0;
        while(childIter1.hasNext() || childIter2.hasNext()) {
            //if(child1 == null && childIter1.hasNext()
        }

        return score;
    }

    class ComparePropertySet extends TreeSet<Property> {
        public ComparePropertySet() {
            super(new Comparator<Property>() {
                public boolean equals(Object obj) {
                    return getClass().equals(obj.getClass());
                }

                public int compare(Property p1, Property p2) {
                    try {
                    return p1.getName().compareTo(p2.getName());
                    } catch(RepositoryException ex) {
                        return 0;
                    }
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

    class CompareChildNodeSet extends TreeSet<Node> {
        public CompareChildNodeSet(Node parent) throws RepositoryException {
            super(parent.getDefinition().getDeclaringNodeType().hasOrderableChildNodes() ? new Comparator<Node>() {
                public boolean equals(Object obj) {
                    return getClass().equals(obj.getClass());
                }

                public int compare(Node n1, Node n2) {
                    try {
                    return n1.getName().compareTo(n2.getName());
                    }  catch(RepositoryException ex) {
                        return 0;
                    }
                }
            } : new Comparator<Node>() {
                        public boolean equals(Object obj) {
                            return getClass().equals(obj.getClass());
                        }

                        public int compare(Node n1, Node n2) {
try {
    return n1.getName().compareTo(n2.getName());
                    }  catch(RepositoryException ex) {
                        return 0;
                    }
                        }
                    });
        }
    }
}

