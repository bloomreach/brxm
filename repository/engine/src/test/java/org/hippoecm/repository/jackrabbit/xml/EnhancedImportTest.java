/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.jackrabbit.xml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.ImportMergeBehavior;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertTrue;

public class EnhancedImportTest extends RepositoryTestCase {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        while (session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        while (session.getRootNode().hasNode("compare")) {
            session.getRootNode().getNode("compare").remove();
        }
        session.getRootNode().addNode("test", "nt:unstructured");
        session.save();
        excludes = new TreeSet<String>();
        excludes.add(".*/jcr:uuid");
    }

    @After
    @Override
    public void tearDown() throws Exception {
        while (session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        while (session.getRootNode().hasNode("compare")) {
            session.getRootNode().getNode("compare").remove();
        }
        session.save();
        super.tearDown();
    }

    private void test() throws IOException, RepositoryException {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        String name = stack[2].getMethodName().substring(4).toLowerCase();
        session.importXML("/test", getClass().getResourceAsStream(name + "-fixture.xml"), ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        session.save();
        ((HippoSession) session).importDereferencedXML("/test", getClass().getResourceAsStream(name + "-merge.xml"), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW, ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_REMOVE, ImportMergeBehavior.IMPORT_MERGE_ADD_OR_SKIP);
        session.save();
        if (session.getRootNode().hasNode("compare")) {
            do {
                session.getRootNode().getNode("compare").remove();
            } while (session.getRootNode().hasNode("compare"));
        }
        session.getRootNode().addNode("compare", "nt:unstructured");
        session.save();
        session.importXML("/compare", getClass().getResourceAsStream(name + "-result.xml"), ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        session.save();
        assertTrue(compare(session.getRootNode().getNode("test"), session.getRootNode().getNode("compare/test")));
    }

    @Test
    public void testSanity() throws Exception {
        test();
    }

    @Test
    public void testSkip() throws Exception {
        test();
    }

    @Test
    public void testCombine() throws Exception {
        test();
    }

    @Test
    public void testOverlay() throws Exception {
        test();
    }

    @Test
    public void testOverride() throws Exception {
        test();
    }

    @Test
    public void testAppend() throws Exception {
        test();
    }

    @Test
    public void testInsert() throws Exception {
        test();
    }

    @Test
    public void testProperty() throws Exception {
        test();
    }
    
    @Test
    public void testCombineTopProperty() throws Exception {
        test();
    }

    // FIXME: below is a stripped down version of the compare method implemented in the class JcrDiff in
    // the project export project.  THat method is more generic, but we cannot use that dependency here.

    Set<String> excludes;

    private boolean compare(Node node1, Node node2) throws RepositoryException {
        if(!node1.getName().equals(node2.getName())) {
            return false;
        }
        if(!node1.getPrimaryNodeType().getName().equals(node2.getPrimaryNodeType().getName())) {
            return false;
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
            return false;
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
                    if (property1.isMultiple()) {
                        Value[] values1 = property1.getValues();
                        Value[] values2 = property2.getValues();
                        if (values1.length == values2.length) {
                            for (int i = 0; i < values1.length; i++) {
                                switch (property1.getType()) {
                                    case PropertyType.BOOLEAN:
                                        equals = (values1[i].getBoolean() != values2[i].getBoolean());
                                        break;
                                    case PropertyType.LONG:
                                        equals = (values1[i].getLong() != values2[i].getLong());
                                        break;
                                    case PropertyType.DOUBLE:
                                        equals = (values1[i].getDouble() != values2[i].getDouble());
                                        break;
                                    case PropertyType.STRING:
                                        equals = (values1[i].getString().equals(values2[i].getString()));
                                        break;
                                    default:
                                        equals = values1[i].getString().equals(values2[i].getString());
                                }
                            }
                        } else {
                            equals = false;
                        }
                    } else {
                        switch (property1.getType()) {
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
                    }
                    if(!equals) {
                        ++propertyMismatch;
                    }
                } else {
                    ++propertyMismatch;
                }
                property1 = property2 = null;
            } else if(compareResult < 0) {
                ++propertyMismatch;
                property1 = null;
            } else { // compareResult > 0
                ++propertyMismatch;
                property2 = null;
            }
        }
        if(propertyMismatch > 0) {
            return false;
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
                ++index2;
                continue;
            }
            if(child2 == null) {
                childrenModified = true;
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
                ++index1;
            } else if(nameCompare > 0) {
                childrenAdded = true;
                ++index2;
            } else {
                if(!excludes.contains(child1.getPath())) {
                    if (!compare(child1, child2)) {
                        childrenModified = true;
                    }
                }
                ++index1;
                ++index2;
            }
        }
        if(childrenModified || childrenAdded) {
            return false;
        }

        return true;
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
                    if (!property.getPath().matches(excludePath)) {
                        super.add(property);
                    }
                }
            }
        }
    }
}
