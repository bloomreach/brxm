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
package org.hippoecm.repository;

import javax.jcr.Node;
import javax.jcr.nodetype.NodeType;
import javax.jcr.RepositoryException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;

import org.hippoecm.repository.TestCase;
import org.junit.*;
import static org.junit.Assert.*;

public class HREPTWO1585Test extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private String[] content = {
        "/test", "nt:unstructured",
        "/test/d", "nt:unstructured",
        "jcr:mixinTypes", "mix:referenceable",
        "/test/f", "nt:unstructured",
        "jcr:mixinTypes", "mix:referenceable",
        "/test/d/x", "hippo:document",
        "jcr:mixinTypes", "hippo:harddocument"
    };

    @Before public void setUp() throws Exception {
        super.setUp();
        build(session, content);
        session.save();
    }

    @After public void tearDown() throws Exception {
        if(session.getRootNode().hasNode("test"))
            session.getRootNode().getNode("test").remove();
        session.save();
        super.tearDown();
    }

    @Test public void testIssue() throws RepositoryException {
        Node source = session.getRootNode().getNode("test/d/x");
        Node target = session.getRootNode().getNode("test/f");

        target = target.addNode(source.getName(), source.getPrimaryNodeType().getName());

        NodeType[] mixinNodeTypes = source.getMixinNodeTypes();
        for(int i=0; i<mixinNodeTypes.length; i++)
            target.addMixin(mixinNodeTypes[i].getName());
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
        assertEquals(session.getRootNode().getNode("test/f").getUUID(),
                     session.getRootNode().getNode("test/f/x").getProperty("hippo:paths").getValues()[0].getString());
    }
}
