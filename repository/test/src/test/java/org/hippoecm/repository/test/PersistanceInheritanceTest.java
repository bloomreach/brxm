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
package org.hippoecm.repository.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.hippoecm.repository.TestCase;
import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.HippoWorkspace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PersistanceInheritanceTest extends TestCase
{

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Node node, root = session.getRootNode();

        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery("test/$name", Query.XPATH);
        node = query.storeAsNode("/hippo:configuration/hippo:documents/test");
        String statement = node.getProperty("jcr:statement").getString();
        String language = node.getProperty("jcr:language").getString();
        node.remove();
        node = root.getNode("hippo:configuration/hippo:documents");
        node = node.addNode("test", "hipposys:ocmquery");
        node.setProperty("jcr:statement",statement);
        node.setProperty("jcr:language",language);
        node.setProperty("hipposys:classname","org.hippoecm.repository.test.SubClass");
        Node types = node.getNode("hipposys:types");
        node = types.addNode("org.hippoecm.repository.test.SubClass","hipposys:type");
        node.setProperty("hipposys:nodetype","nt:unstructured");
        node.setProperty("hipposys:display","Sub");
        node.setProperty("hipposys:classname","org.hippoecm.repository.test.SubClass");
        node = types.addNode("org.hippoecm.repository.test.SuperClass","hipposys:type");
        node.setProperty("hipposys:nodetype","nt:unstructured");
        node.setProperty("hipposys:display","Super");
        node.setProperty("hipposys:classname","org.hippoecm.repository.test.SuperClass");

        session.save();

        if(root.hasNode("test"))
            root.getNode("test").remove();
        node = root.addNode("test");
        node = node.addNode("mydocument", "hippo:testdocument");
        node.addMixin("hippo:harddocument");
        node.setProperty("a", "1");
        node.setProperty("b", "2");

        session.save();
    }

    @After
    public void tearDown() throws Exception {
        if(session.getRootNode().hasNode("hippo:configuration/hippo:documents") &&
           session.getRootNode().hasNode("hippo:configuration/hippo:documents/test")) {
             session.getRootNode().getNode("hippo:configuration/hippo:documents/test").remove();
        }
        session.getRootNode().getNode("test").remove();
        super.tearDown();
    }

    @Test
    public void testInheritance() throws RepositoryException {
        HippoWorkspace wsp = (HippoWorkspace)(session.getWorkspace());
        DocumentManager dmngr = wsp.getDocumentManager();
        SubClass document = (SubClass) dmngr.getDocument("test", "mydocument");
        assertNotNull(document);
        assertEquals("1", document.superfield);
        assertEquals("2", document.subfield);
    }
}
