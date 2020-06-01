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
package org.hippoecm.repository;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;

import org.hippoecm.repository.ext.DerivedDataFunction;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DerivedDataTest extends RepositoryTestCase {

    private static final Logger log = LoggerFactory.getLogger(DerivedDataTest.class);

    protected Node root;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Node configuration = session.getRootNode().getNode("hippo:configuration/hippo:derivatives");
        configuration = configuration.addNode("org.hippoecm.repository.DerivedDataTest");
        configuration.setProperty("hipposys:nodetype", "hippo:testderived");
        configuration.setProperty("hipposys:classname", "org.hippoecm.repository.DerivedDataTest$Function");
        configuration.getNode("hipposys:accessed").addNode("aa","hipposys:relativepropertyreference").
            setProperty("hipposys:relPath","hippo:a");
        configuration.getNode("hipposys:accessed").addNode("bb","hipposys:relativepropertyreference").
            setProperty("hipposys:relPath","hippo:b");
        configuration.getNode("hipposys:derived").addNode("cc","hipposys:relativepropertyreference").
            setProperty("hipposys:relPath","hippo:c");
        session.save();
        root = session.getRootNode().addNode("test","nt:unstructured");
    }

    @After
    public void tearDown() throws Exception {
        session.refresh(false);
        if(session.getRootNode().hasNode("hippo:configuration/hippo:derivatives/org.hippoecm.repository.DerivedDataTest")) {
            session.getRootNode().getNode("hippo:configuration/hippo:derivatives/org.hippoecm.repository.DerivedDataTest") .
                remove();
            session.save();
        }
        super.tearDown();
    }

    @Test
    public void testSimple() throws Exception {
        Node folder = root.addNode("folder","nt:unstructured");
        folder.addMixin("mix:referenceable");
        Node document = folder.addNode("document", "hippo:testderiveddocument");
        document.addMixin("hippo:testderived");
        document.addMixin("mix:versionable");
        document.setProperty("hippo:a", 3);
        document.setProperty("hippo:b", 4);
        document.setProperty("hippo:c", 6);
        session.save();
        session.refresh(false);
        assertEquals(5, session.getRootNode().getNode("test/folder/document").getProperty("hippo:c").getLong());
    }

    @Test
    public void testAncestors() throws Exception {
        Node folder1 = root.addNode("folder1","nt:unstructured");
        folder1.addMixin("mix:referenceable");
        Node folder2= root.addNode("folder2","nt:unstructured");
        folder2.addMixin("mix:referenceable");
        Node document = folder2.addNode("document", "hippo:testderiveddocument");
        document.addMixin("hippo:testderived");
        document.addMixin("mix:versionable");
        document.setProperty("hippo:a", 3);
        document.setProperty("hippo:b", 4);
        document.setProperty("hippo:c", 6);
        session.save();

        Property p = session.getRootNode().getNode("test/folder2/document").getProperty("hippo:paths");
        assertTrue(p.getDefinition().isMultiple());
        Set<String> values = new HashSet<String>();
        for (Value value : p.getValues()) {
            values.add(value.getString());
        }
        assertEquals(4, values.size());
        assertTrue(values.contains(session.getRootNode().getIdentifier()));
        assertTrue(values.contains(folder2.getIdentifier()));
        assertTrue(values.contains(folder2.getNode("document").getIdentifier()));

        session.move(document.getPath(), folder1.getPath()+"/"+document.getName());
        session.save();
        p = session.getRootNode().getNode("test/folder1/document").getProperty("hippo:paths");
        assertTrue(p.getDefinition().isMultiple());
        values.clear();
        for (Value value : p.getValues()) {
            values.add(value.getString());
        }
        assertEquals(4, values.size());
        assertTrue(values.contains(session.getRootNode().getIdentifier()));
        assertTrue(values.contains(folder1.getIdentifier()));
        assertTrue(values.contains(folder1.getNode("document").getIdentifier()));
    }

    @Ignore
    public void disabledTest() throws RepositoryException {
        try {
            Node node, other;
            node = session.getRootNode().addNode("test");
            node.addMixin("mix:referenceable");
            other = node.addNode("other");
            other.addMixin("mix:referenceable");
            session.save();
            node = session.getRootNode().getNode("test").addNode("doc", "hippo:test");
            node.setProperty("hippo:compute", other);
            node.setProperty("hippo:related", new Value[] { session.getValueFactory().createValue(other) });
            session.save();
            node = session.getRootNode().getNode("test").getNode("doc");
            for(PropertyIterator iter = node.getProperties(); iter.hasNext(); ) {
                Property p = iter.nextProperty();
                log.info("property \""+p.getName()+"\"");
            }
        } catch(ConstraintViolationException ex) {
            fail("ConstraintViolationException: " + ex.getMessage());
            log.warn("ConstraintViolationException: " + ex.getMessage(), ex);
        }
    }

    @Test
    public void testDerivedNotReferenceable() throws Exception {
        Node folder = root.addNode("folder","nt:unstructured");
        folder.addMixin("mix:referenceable");

        Node document = folder.addNode("document", "hippo:testderiveddocument");
        document.addMixin("hippo:testderived");
        document.setProperty("hippo:a", 3);
        document.setProperty("hippo:b", 4);
        document.setProperty("hippo:c", 6);
        session.save();
        session.refresh(false);
        assertEquals(5, session.getRootNode().getNode("test/folder/document").getProperty("hippo:c").getLong());
    }

    public static class Function extends DerivedDataFunction {
        static final long serialVersionUID = 1;
        public Map<String,Value[]> compute(Map<String,Value[]> parameters) {
            try {
                double a = parameters.get("aa")[0].getLong();
                double b = parameters.get("bb")[0].getLong();
                double c = Math.sqrt(a * a + b * b);
                parameters.put("cc", new Value[] { getValueFactory().createValue(Math.round(c)) });
                return parameters;
            } catch(ValueFormatException ex) {
                return null;
            } catch(RepositoryException ex) {
                return null;
            }
        }
    }
}
