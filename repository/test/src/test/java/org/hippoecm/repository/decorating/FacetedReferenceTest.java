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

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FacetedReferenceTest extends RepositoryTestCase {

    private static String[] content1 = new String[] {
        "/test",                                                             "nt:unstructured",
        "/test/documents",                                                   "nt:unstructured",
        "jcr:mixinTypes", "mix:referenceable",
        "/test/documents/pages",                                             "nt:unstructured",
        "/test/documents/pages/index",                                       "hippo:handle",
        "/test/documents/pages/index/index",                                 "hippo:testdocument",
        "/test/documents/pages/index/index/links",                           "nt:unstructured",
        "/test/documents/pages/index/index/thema",                           "nt:unstructured",
        "/test/documents/articles",                                          "nt:unstructured",
        "/test/documents/articles/brave-new-world",                          "hippo:handle",
        "jcr:mixinTypes", "mix:referenceable",
        "/test/documents/articles/brave-new-world/brave-new-world",          "hippo:testdocument",
        "jcr:mixinTypes", "mix:referenceable",
        "language","english",
        "/test/documents/articles/the-invisible-man",                        "hippo:handle",
        "jcr:mixinTypes", "mix:referenceable",
        "/test/documents/articles/the-invisible-man/the-invisible-man",      "hippo:testdocument",
        "jcr:mixinTypes", "mix:referenceable",
        "language","english",
        "/test/documents/articles/war-of-the-worlds",                        "hippo:handle",
        "jcr:mixinTypes", "mix:referenceable",
        "/test/documents/articles/war-of-the-worlds/war-of-the-worlds",      "hippo:testdocument",
        "jcr:mixinTypes", "mix:referenceable",
        "language","english",
        "/test/documents/articles/war-of-the-worlds/war-of-the-worlds",      "hippo:testdocument",
        "jcr:mixinTypes", "mix:referenceable",
        "language","dutch",
        "/test/documents/articles/nineteeneightyfour",                       "hippo:handle",
        "jcr:mixinTypes", "mix:referenceable",
        "/test/documents/articles/nineteeneightyfour/nineteeneightyfour",    "hippo:testdocument",
        "jcr:mixinTypes", "mix:referenceable",
        "language","english",
        "/test/documents/articles/nineteeneightyfour/nineteeneightyfour",    "hippo:testdocument",
        "jcr:mixinTypes", "mix:referenceable",
        "language","dutch"
    };
    private static String[] content2 = new String[] {
        "/test/english",                                                     "hippo:facetselect",
        "hippo:docbase", "/test/documents",
        "hippo:facets",  "language",
        "hippo:values",  "english",
        "hippo:modes",   "stick",
        "/test/dutch",                                                       "hippo:facetselect",
        "hippo:docbase", "/test/documents/articles/war-of-the-worlds",
        "hippo:facets",  "language",
        "hippo:facets",  "state",
        "hippo:values",  "dutch",
        "hippo:values",  "published",
        "hippo:modes",   "stick",
        "hippo:modes",   "clear",
        "/test/prefer",                                                       "hippo:facetselect",
        "hippo:docbase", "/test/documents",
        "hippo:facets",  "language",
        "hippo:values",  "dutch",
        "hippo:modes",   "prefer",
        "/test/preferonce",                                                   "hippo:facetselect",
        "hippo:docbase", "/test/documents",
        "hippo:facets",  "language",
        "hippo:values",  "dutch",
        "hippo:modes",   "prefer-single",
        "/test/preferonce_onhandle",                                                   "hippo:facetselect",
        "hippo:docbase", "/test/documents/articles/nineteeneightyfour",
        "hippo:facets",  "language",
        "hippo:values",  "dutch",
        "hippo:modes",   "prefer-single"
    };

    @Before
    public void setUp() throws Exception {
        super.setUp();
        while (session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
            session.save();
        }
        build(content1, session);
        session.save();
        build(content2, session);
        session.save();
    }

    @After
    public void tearDown() throws Exception {
        session.refresh(false);
        if (session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
            session.save();
        }
        super.tearDown();
    }
    
    @Test
    public void testFacetedReference() throws Exception {
        assertNotNull(traverse(session,"/test/documents/articles/war-of-the-worlds/war-of-the-worlds"));
        assertNotNull(traverse(session,"/test/documents/articles/war-of-the-worlds/war-of-the-worlds[language='dutch']"));
        assertNotNull(traverse(session,"/test/documents/articles/war-of-the-worlds/war-of-the-worlds[language='english']"));
        assertNotNull(traverse(session,"/test/english/articles/brave-new-world/brave-new-world"));
        assertNotNull(traverse(session,"/test/english/articles/war-of-the-worlds/war-of-the-worlds[language='english']"));
        assertNull(traverse(session,"/test/english/articles/war-of-the-worlds/war-of-the-worlds[language='dutch']"));
        assertNotNull(traverse(session,"/test/dutch/war-of-the-worlds[@language='dutch']"));
        assertNull(traverse(session,"/test/dutch/war-of-the-worlds[@language='english']"));
    }

    @Test
    public void testPreferenceOrder() throws Exception {
        Node handle = traverse(session, "/test/prefer/articles/war-of-the-worlds");
        assertNotNull(handle);
        NodeIterator iter = handle.getNodes(handle.getName());
        assertTrue(iter.hasNext());
        Node document = iter.nextNode();
        assertNotNull(document);
        assertEquals("dutch", document.getProperty("language").getString());
        assertTrue(iter.hasNext());
        document = iter.nextNode();
        assertNotNull(document);
        assertEquals("english", document.getProperty("language").getString());
        assertFalse(iter.hasNext());

        handle = traverse(session, "/test/prefer/articles/the-invisible-man");
        iter = handle.getNodes(handle.getName());
        assertTrue(iter.hasNext());
        document = iter.nextNode();
        assertNotNull(document);
        assertEquals("english", document.getProperty("language").getString());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testPreferenceOnceOrder() throws Exception {
        Node node = traverse(session, "/test/preferonce/articles/war-of-the-worlds");
        NodeIterator iter = node.getNodes(node.getName());
        assertTrue(iter.hasNext());
        node = iter.nextNode();
        assertNotNull(node);
        assertEquals("dutch", node.getProperty("language").getString());
        assertFalse(iter.hasNext());

        node = traverse(session, "/test/preferonce/articles/the-invisible-man");
        iter = node.getNodes(node.getName());
        assertTrue(iter.hasNext());
        node = iter.nextNode();
        assertNotNull(node);
        assertEquals("english", node.getProperty("language").getString());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testPreferSingleDirectOnHandle() throws Exception {
        HippoNode n = (HippoNode)session.getItem("/test/preferonce_onhandle");
        assertEquals( n.getNode("nineteeneightyfour").getProperty("language").getString(), "dutch" );
    }
    
    @Test
    public void testSimpleMirrorAfterPreferSingle() throws Exception {
        /*
         * After a prefer-single mode, a facetselects below this prefer single
         * should inherit the prefer-single (thus correct ordening).
         * This is a test for this scenario
         */
        HippoNode n = (HippoNode)session.getItem("/test/preferonce/articles/war-of-the-worlds/war-of-the-worlds");
        assertEquals("dutch", n.getProperty("language").getString());
        assertNotNull(n.getCanonicalNode());
        Node dutchNodeCanonical = n.getCanonicalNode();
        Node mirror = dutchNodeCanonical.addNode("mirror","hippo:facetselect");
        Node docBaseNode = (Node)session.getItem("/test/documents/articles/nineteeneightyfour");
        mirror.setProperty(HippoNodeType.HIPPO_DOCBASE, docBaseNode.getIdentifier());
        mirror.setProperty(HippoNodeType.HIPPO_FACETS, new String[]{});
        mirror.setProperty(HippoNodeType.HIPPO_VALUES, new String[]{});
        mirror.setProperty(HippoNodeType.HIPPO_MODES, new String[]{});
        session.save();
        session.refresh(false);
        HippoNode n2 = (HippoNode)session.getItem("/test/preferonce/articles/war-of-the-worlds/war-of-the-worlds");
        n2.getNode("mirror/nineteeneightyfour").getProperty("language").getString();
        assertEquals("dutch", n2.getNode("mirror/nineteeneightyfour").getProperty("language").getString());
    }

}
