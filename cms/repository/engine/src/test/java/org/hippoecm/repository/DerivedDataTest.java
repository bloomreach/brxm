/*
 * Copyright 2007 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.ConstraintViolationException;

import org.hippoecm.repository.ext.DerivedDataFunction;

import junit.framework.TestCase;

public class DerivedDataTest extends TestCase {

    private static final String SYSTEMUSER_ID = "admin";
    private static final char[] SYSTEMUSER_PASSWORD = "admin".toCharArray();

    protected HippoRepository server;
    protected Session session;
    protected Node root;

    public void setUp() throws Exception {
        server = HippoRepositoryFactory.getHippoRepository();
        session = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);

        if(session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        session.save();

        Node configuration = session.getRootNode().getNode("hippo:configuration/hippo:derivatives");
        configuration = configuration.addNode("org.hippoecm.repository.DerivedDataTest");
        configuration.setProperty("hippo:nodetype", "hippo:testderived");
        configuration.setProperty("hippo:classname", "org.hippoecm.repository.DerivedDataTest$Function");
        configuration.getNode("hippo:accessed").addNode("aa","hippo:relativepropertyreference").
            setProperty("hippo:relPath","hippo:a");
        configuration.getNode("hippo:accessed").addNode("bb","hippo:relativepropertyreference").
            setProperty("hippo:relPath","hippo:b");
        configuration.getNode("hippo:derived").addNode("cc","hippo:relativepropertyreference").
            setProperty("hippo:relPath","hippo:c");
        session.save();
        root = session.getRootNode().addNode("test","nt:unstructured");
    }

    public void tearDown() throws Exception {
        session.refresh(false);
        if(session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        if(session.getRootNode().hasNode("hippo:configuration/hippo:derivatives/org.hippoecm.repository.DerivedDataTest")) {
            session.getRootNode().getNode("hippo:configuration/hippo:derivatives/org.hippoecm.repository.DerivedDataTest") .
                remove();
        }
        if(session != null) {
            session.logout();
        }
        if (server != null) {
            server.close();
        }
    }

    public void testSimple() throws Exception {
        Node folder = root.addNode("folder","nt:unstructured");
        folder.addMixin("mix:referenceable");
        Node document = folder.addNode("document", "hippo:testderiveddocument");
        document.addMixin("hippo:testderived");
        document.addMixin("hippo:harddocument");
        document.setProperty("hippo:a", 3);
        document.setProperty("hippo:b", 4);
        document.setProperty("hippo:c", 6);
        session.save();
        assertEquals(5, session.getRootNode().getNode("test/folder/document").getProperty("hippo:c").getLong());
    }

    public void testAncestors() throws Exception {
        Node folder1 = root.addNode("folder1","nt:unstructured");
        folder1.addMixin("mix:referenceable");
        Node folder2= root.addNode("folder2","nt:unstructured");
        folder2.addMixin("mix:referenceable");
        Node document = folder2.addNode("document", "hippo:testderiveddocument");
        document.addMixin("hippo:testderived");
        document.addMixin("hippo:harddocument");
        document.setProperty("hippo:a", 3);
        document.setProperty("hippo:b", 4);
        document.setProperty("hippo:c", 6);
        session.save();

        Property p = session.getRootNode().getNode("test/folder2/document").getProperty("hippo:paths");
        assertTrue(p.getDefinition().isMultiple());
        Value[] values = p.getValues();
        assertEquals(1, values.length);
        values[0].getString().equals(folder2.getUUID());

        session.move(document.getPath(), folder1.getPath()+"/"+document.getName());
        session.save();
        p = session.getRootNode().getNode("test/folder1/document").getProperty("hippo:paths");
        assertTrue(p.getDefinition().isMultiple());
        values = p.getValues();
        assertEquals(1, values.length);
        values[0].getString().equals(folder1.getUUID());
    }

    private void disabledTest() throws RepositoryException {
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
                System.err.println("property \""+p.getName()+"\"");
            }
        } catch(ConstraintViolationException ex) {
            System.err.println(ex.getMessage());
            ex.printStackTrace(System.err);
        }
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
