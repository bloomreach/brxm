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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HREPTWO475Test extends RepositoryTestCase {

    @Test
    public void testIssue() throws RepositoryException {
        session.refresh(false);
        Node root = session.getRootNode().addNode("test");

        Node node = root.addNode("docs","nt:unstructured");
        node.addMixin("mix:referenceable");
        node = node.addNode("doc1",HippoNodeType.NT_HANDLE);
        Node child = node.addNode("doc1","hippo:testdocument");
        child.addMixin("mix:versionable");
        child.setProperty("lang","en");
        child = node.addNode("doc1","hippo:testdocument");
        child.addMixin("mix:versionable");
        child.setProperty("lang","nl");

        node = root.getNode("docs");
        node = node.addNode("doc2",HippoNodeType.NT_HANDLE);
        child = node.addNode("doc2","hippo:testdocument");
        child.addMixin("mix:versionable");
        child.setProperty("lang","nl");
        child = node.addNode("doc2","hippo:testdocument");
        child.addMixin("mix:versionable");
        child.setProperty("lang","en");

        node = root.getNode("docs");
        node = node.addNode("doc3","nt:unstructured");
        child = node.addNode("doc3","hippo:testdocument");
        child.addMixin("mix:versionable");
        child.setProperty("lang","nl");
        child = node.addNode("doc3","hippo:testdocument");
        child.addMixin("mix:versionable");
        child.setProperty("lang","en");

        node = root.getNode("docs").addNode("sub");
        node = node.addNode("doc4",HippoNodeType.NT_HANDLE);
        child = node.addNode("doc4","hippo:testdocument");
        child.addMixin("mix:versionable");
        child.setProperty("lang","en");
        child = node.addNode("doc4","hippo:testdocument");
        child.addMixin("mix:versionable");
        child.setProperty("lang","nl");

        node = root.getNode("docs").getNode("sub");
        node = node.addNode("doc5",HippoNodeType.NT_HANDLE);
        child = node.addNode("doc5","hippo:testdocument");
        child.addMixin("mix:versionable");
        child.setProperty("lang","nl");
        child = node.addNode("doc5","hippo:testdocument");
        child.addMixin("mix:versionable");
        child.setProperty("lang","en");

        node = root.getNode("docs").getNode("sub");
        node = node.addNode("doc6","nt:unstructured");
        child = node.addNode("doc6","hippo:testdocument");
        child.addMixin("mix:versionable");
        child.setProperty("lang","nl");
        child = node.addNode("doc6","hippo:testdocument");
        child.addMixin("mix:versionable");
        child.setProperty("lang","en");

        node = root.addNode("nav","hippo:facetselect");
        node.setProperty("hippo:docbase",session.getRootNode().getNode("test/docs").getIdentifier());
        node.setProperty("hippo:facets",new String[] { "lang" });
        node.setProperty("hippo:values",new String[] { "nl" });
        node.setProperty("hippo:modes",new String[] { "select" });
        session.save();
        session.refresh(false);

        node = root.getNode("nav");

        assertTrue(node.getNode("doc3").hasNode("doc3"));
        assertEquals("nl",node.getNode("doc3").getNode("doc3").getProperty("lang").getString());
        assertTrue(node.getNode("doc3").hasNode("doc3[2]"));
        assertEquals("en",node.getNode("doc3").getNode("doc3[2]").getProperty("lang").getString());

        assertTrue(node.getNode("doc1").hasNode("doc1"));
        assertEquals("nl",node.getNode("doc1").getNode("doc1").getProperty("lang").getString());
        assertFalse(node.getNode("doc1").hasNode("doc1[2]"));
        assertTrue(node.getNode("doc2").hasNode("doc2"));
        assertEquals("nl",node.getNode("doc2").getNode("doc2").getProperty("lang").getString());
        assertFalse(node.getNode("doc2").hasNode("doc2[2]"));

        assertTrue(node.getNode("sub").getNode("doc6").hasNode("doc6"));
        assertEquals("nl",node.getNode("sub").getNode("doc6").getNode("doc6").getProperty("lang").getString());
        assertTrue(node.getNode("sub").getNode("doc6").hasNode("doc6[2]"));
        assertEquals("en",node.getNode("sub").getNode("doc6").getNode("doc6[2]").getProperty("lang").getString());

        assertTrue(node.getNode("sub").getNode("doc4").hasNode("doc4"));
        assertEquals("nl",node.getNode("sub").getNode("doc4").getNode("doc4").getProperty("lang").getString());
        assertFalse(node.getNode("sub").getNode("doc4").hasNode("doc4[2]"));
        assertTrue(node.getNode("sub").getNode("doc5").hasNode("doc5"));
        assertEquals("nl",node.getNode("sub").getNode("doc5").getNode("doc5").getProperty("lang").getString());
        assertFalse(node.getNode("sub").getNode("doc5").hasNode("doc5[2]"));

        session.getRootNode().getNode("test").remove();
        session.save();
    }
}
