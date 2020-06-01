/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.decorating;

import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PathsTest extends RepositoryTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testPathProperty() throws Exception {
        build(new String[] {
                "/test", "nt:unstructured",
                "jcr:mixinTypes", "mix:referenceable",
                "/test/sub", "nt:unstructured",
                "jcr:mixinTypes", "mix:referenceable",
                "/test/sub/node", "hippo:testdocument",
                "jcr:mixinTypes", "mix:versionable",

            }, session);
        session.save();
        session.refresh(false);
        Node node = session.getRootNode().getNode("test/sub/node");

        Property prop = node.getProperty(HippoNodeType.HIPPO_PATHS);
        Value[] values = prop.getValues();
        Set valuesSet = new HashSet();
        for (int i = 0; i < values.length; i++) {
            valuesSet.add(values[i].getString());
        }
        assertTrue(values.length == 4);
        assertTrue(valuesSet.contains(session.getRootNode().getIdentifier()));
        assertTrue(valuesSet.contains(session.getRootNode().getNode("test").getIdentifier()));
        assertTrue(valuesSet.contains(session.getRootNode().getNode("test/sub").getIdentifier()));
        assertTrue(valuesSet.contains(session.getRootNode().getNode("test/sub/node").getIdentifier()));
    }

    @Test
    public void testIssue() throws RepositoryException {
        build(new String[] {
                "/test", "nt:unstructured",
                "/test/d", "nt:unstructured",
                "jcr:mixinTypes", "mix:referenceable",
                "/test/f", "nt:unstructured",
                "jcr:mixinTypes", "mix:referenceable",
                "/test/d/x", "hippo:document",
                "jcr:mixinTypes", "mix:versionable"
            }, session);

        Node source = session.getRootNode().getNode("test/d/x");
        Node target = session.getRootNode().getNode("test/f");
        session.save();

        target = target.addNode(source.getName(), source.getPrimaryNodeType().getName());

        NodeType[] mixinNodeTypes = source.getMixinNodeTypes();
        for(int i=0; i<mixinNodeTypes.length; i++) {
            target.addMixin(mixinNodeTypes[i].getName());
        }
        NodeType[] nodeTypes = new NodeType[mixinNodeTypes.length + 1];
        nodeTypes[0] = target.getPrimaryNodeType();
        if(mixinNodeTypes.length > 0) {
            System.arraycopy(mixinNodeTypes, 0, nodeTypes, 1, mixinNodeTypes.length);
        }

        for(PropertyIterator iter = source.getProperties(); iter.hasNext(); ) {
            Property prop = iter.nextProperty();
            if (prop.getDefinition().isMultiple()) {
                boolean isProtected = true;
                for(int i=0; i<nodeTypes.length; i++) {
                    if(nodeTypes[i].canSetProperty(prop.getName(), prop.getValues())) {
                        isProtected = false;
                        break;
                    }
                }
                if (!isProtected) {
                    target.setProperty(prop.getName(), prop.getValues());
                }
            } else {
                boolean isProtected = true;
                for(int i=0; i<nodeTypes.length; i++) {
                    if(nodeTypes[i].canSetProperty(prop.getName(), prop.getValue())) {
                        isProtected = false;
                        break;
                    }
                }
                if (!isProtected) {
                    target.setProperty(prop.getName(), prop.getValue());
                }
            }
        }

        session.save();
        session.refresh(false);
        assertEquals(session.getRootNode().getNode("test/f").getIdentifier(),
                     session.getRootNode().getNode("test/f/x").getProperty("hippo:paths").getValues()[1].getString());
    }
}
