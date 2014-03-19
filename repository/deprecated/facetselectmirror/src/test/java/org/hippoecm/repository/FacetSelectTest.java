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
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FacetSelectTest extends RepositoryTestCase {

    String[] content = new String[] {
        "/test",              "nt:unstructured",
        "/test/docs",         "hippo:testdocument",
        "jcr:mixinTypes",     "mix:versionable",
        "/test/docs/doc",     "hippo:handle",
        "jcr:mixinTypes",     "hippo:hardhandle",
        "/test/docs/doc/doc", "hippo:document",
        "jcr:mixinTypes",     "mix:versionable",
        "/test/mirror",       "hippo:facetselect",
        "hippo:docbase",      "/test/docs",
        "hippo:values",       null,
        "hippo:facets",       null,
        "hippo:modes",        null,
        "/test/spiegel",      "hippo:testmirror",
        "hippo:docbase",      "/test/docs",
        "hippo:values",       null,
        "hippo:facets",       null,
        "hippo:modes",        null,
        "/test/notallowed",   "hippo:facetselect",
        "hippo:docbase",      "/",
        "hippo:values",       null,
        "hippo:facets",       null,
        "hippo:modes",        null
    };

    String[] combineContent1 = new String[] {
        "/test",              "nt:unstructured",
        "jcr:mixinTypes",     "mix:referenceable",
        "/test/docs",         "hippo:testdocument",
        "jcr:mixinTypes",     "mix:versionable",
        "/test/docs/one",     "hippo:handle",
        "jcr:mixinTypes",     "hippo:hardhandle",
        "/test/docs/one/one", "hippo:testdocument",
        "jcr:mixinTypes",     "mix:versionable",
        "vehicle", "car",
        "color", "red",
        "/test/docs/two",     "hippo:handle",
        "jcr:mixinTypes",     "hippo:hardhandle",
        "/test/docs/two/two", "hippo:testdocument",
        "jcr:mixinTypes",     "mix:versionable",
        "vehicle", "car",
        "color", "blue",
        "/test/docs/three",     "hippo:handle",
        "jcr:mixinTypes",     "hippo:hardhandle",
        "/test/docs/three/three", "hippo:testdocument",
        "jcr:mixinTypes",     "mix:versionable",
        "vehicle", "bike",
        "color", "red",
        "/test/docs/four",     "hippo:handle",
        "jcr:mixinTypes",     "hippo:hardhandle",
        "/test/docs/four/four", "hippo:testdocument",
        "jcr:mixinTypes",     "mix:versionable",
        "vehicle", "bike",
        "color", "blue"
    };
    String[] combineContent2 = new String[] {
        "/test/filter1",      "hippo:facetselect",
        "jcr:mixinTypes",     "mix:referenceable",
        "hippo:docbase",      "/test/docs",
        "hippo:facets",       "vehicle",
        "hippo:values",       "car",
        "hippo:modes",        "select"
    };
    String[] combineContentFilterPointsToFilter = new String[] {
        "/test/filterTofilter",      "hippo:facetselect",
        "hippo:docbase",      "/test/filter1",
        "hippo:facets",       "color",
        "hippo:values",       "red",
        "hippo:modes",        "select"
    };
    String[] combineContentFilterPointsToParentOfFilter = new String[] {
        "/test/filterToParentOfFilter",      "hippo:facetselect",
        "hippo:docbase",      "/test",
        "hippo:facets",       "color",
        "hippo:values",       "red",
        "hippo:modes",        "select"
    };

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        if(session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        session.save();
        super.tearDown();
    }

    @Test
    public void testRequestsNotFirst() throws Exception {
        final String[] data = new String[] {
            "/test", "nt:unstructured",
            "/test/docs", "hippo:testdocument",
            "jcr:mixinTypes", "mix:versionable",
            "/test/docs/doc", "hippo:handle",
            "jcr:mixinTypes", "hippo:hardhandle",
            "jcr:mixinTypes", "hippo:translated",
            "/test/docs/doc/hippo:translation", "hippo:translation",
            "hippo:language", "",
            "hippo:message", "",
            "/test/docs/doc/hippo:request", "hipposys:request",
            "/test/docs/doc/doc", "hippo:testdocument",
            "jcr:mixinTypes", "mix:versionable",
            "state", "published",
            "/test/test1", "hippo:facetselect",
            "hippo:docbase", "/test/docs",
            "hippo:facets", "state",
            "hippo:values", "published",
            "hippo:modes", "single",
            "/test/testa", "hippo:facetselect",
            "hippo:docbase", "/test/docs/doc",
            "hippo:facets", "state",
            "hippo:values", "published",
            "hippo:modes", "single",
            "/test/test2", "hippo:facetselect",
            "hippo:docbase", "/test/docs",
            "hippo:facets", "state",
            "hippo:values", "published",
            "hippo:modes", "prefer-single",
            "/test/testb", "hippo:facetselect",
            "hippo:docbase", "/test/docs/doc",
            "hippo:facets", "state",
            "hippo:values", "published",
            "hippo:modes", "prefer-single",
            "/test/test3", "hippo:facetselect",
            "hippo:docbase", "/test/docs",
            "hippo:facets", "state",
            "hippo:values", "published",
            "hippo:modes", "prefer",
            "/test/testc", "hippo:facetselect",
            "hippo:docbase", "/test/docs/doc",
            "hippo:facets", "state",
            "hippo:values", "published",
            "hippo:modes", "prefer",
            "/test/test4", "hippo:facetselect",
            "hippo:docbase", "/test/docs",
            "hippo:facets", "state",
            "hippo:values", "published",
            "hippo:modes", "select",
            "/test/testd", "hippo:facetselect",
            "hippo:docbase", "/test/docs/doc",
            "hippo:facets", "state",
            "hippo:values", "published",
            "hippo:modes", "select",
            "/test/test5", "hippo:facetselect",
            "hippo:docbase", "/test/docs",
            "hippo:facets", null,
            "hippo:values", null,
            "hippo:modes", null,
            "/test/teste", "hippo:facetselect",
            "hippo:docbase", "/test/docs/doc",
            "hippo:facets", null,
            "hippo:values", null,
            "hippo:modes", null,
            "/test/test6", "hippo:mirror",
            "hippo:docbase", "/test/docs",
            "/test/testf", "hippo:mirror",
            "hippo:docbase", "/test/docs/doc"
        };
        build(data, session);
        session.save();
        session.refresh(false);
        assertEquals("doc",traverse(session, "/test/test1/doc").getNodes().nextNode().getName());
        assertEquals("doc",traverse(session, "/test/testa").getNodes().nextNode().getName());
        assertEquals("doc",traverse(session, "/test/test2/doc").getNodes().nextNode().getName());
        assertEquals("doc",traverse(session, "/test/testb").getNodes().nextNode().getName());
        assertEquals("doc",traverse(session, "/test/test3/doc").getNodes().nextNode().getName());
        assertEquals("doc",traverse(session, "/test/testc").getNodes().nextNode().getName());
        assertEquals("doc",traverse(session, "/test/test4/doc").getNodes().nextNode().getName());
        assertEquals("doc",traverse(session, "/test/testd").getNodes().nextNode().getName());
        assertEquals("doc",traverse(session, "/test/test5/doc").getNodes().nextNode().getName());
        assertEquals("doc",traverse(session, "/test/teste").getNodes().nextNode().getName());
        assertEquals("doc",traverse(session, "/test/test6/doc").getNodes().nextNode().getName());
        assertEquals("doc",traverse(session, "/test/testf").getNodes().nextNode().getName());
    }
    

    @Test
    public void testTranslationBelowHandlePresentAndNotFirst() throws Exception {
        final String[] data = new String[] {
            "/test", "nt:unstructured",
            "/test/docs", "hippo:testdocument",
            "jcr:mixinTypes", "mix:versionable",
            "/test/docs/doc", "hippo:handle",
            "jcr:mixinTypes", "hippo:hardhandle",
            "jcr:mixinTypes", "hippo:translated",
            "/test/docs/doc/hippo:translation", "hippo:translation",
            "hippo:language", "",
            "hippo:message", "Document",
            "/test/docs/doc/hippo:request", "hipposys:request",
            "/test/docs/doc/doc", "hippo:testdocument",
            "jcr:mixinTypes", "mix:versionable",
            "state", "published",
            "/test/test1", "hippo:facetselect",
            "hippo:docbase", "/test/docs",
            "hippo:facets", "state",
            "hippo:values", "published",
            "hippo:modes", "single",
            "/test/test2", "hippo:facetselect",
            "hippo:docbase", "/test/docs",
            "hippo:facets", "state",
            "hippo:values", "unpublished",
            "hippo:modes", "prefer-single",
            "/test/test3", "hippo:facetselect",
            "hippo:docbase", "/test/docs",
            "hippo:facets", "state",
            "hippo:values", "published",
            "hippo:modes", "select",
            "/test/test4", "hippo:facetselect",
            "hippo:docbase", "/test/docs",
            "hippo:facets", "state",
            "hippo:values", "xxx",
            "hippo:modes", "single",
        };
        build(data, session);
        session.save();
        session.refresh(false);
        
        // facetselect with mode = single. We expect one hippo:document below the handle, *and* the hippo:translation node
        {
            Node doc = traverse(session, "/test/test1/doc");
            assertEquals("Document", ((HippoNode)doc).getLocalizedName());
            NodeIterator it = doc.getNodes();
            Node firstChild = it.nextNode();
            assertEquals("doc",firstChild.getName());
            assertEquals("Document", ((HippoNode)firstChild).getLocalizedName());
            assertEquals("hippo:translation",it.nextNode().getName());
        }
        
        // facetselect with mode = prefer-single. We expect one hippo:document below the handle, *and* the hippo:translation node
        {
            Node doc = traverse(session, "/test/test2/doc");
            assertEquals("Document", ((HippoNode)doc).getLocalizedName());
            NodeIterator it = doc.getNodes();
            Node firstChild = it.nextNode();
            assertEquals("doc",firstChild.getName());
            assertEquals("Document", ((HippoNode)firstChild).getLocalizedName());
            assertEquals("hippo:translation",it.nextNode().getName());
        }
        
        // facetselect with mode = select. We expect one hippo:document below the handle, the hippo:request *and* the hippo:translation node
        {
            Node doc = traverse(session, "/test/test3/doc");
            assertEquals("Document", ((HippoNode)doc).getLocalizedName());
            NodeIterator it = doc.getNodes();
            Node firstChild = it.nextNode();
            assertEquals("doc",firstChild.getName());
            assertEquals("Document", ((HippoNode)firstChild).getLocalizedName());
            
            // we do not know whether first the request comes and then the translation, or other way around:
            assertTrue(doc.hasNode("hippo:request"));
            assertTrue(doc.hasNode("hippo:translation"));
        }
        
        // facetselect with mode = single. We expect no hippo:document (because facetselect state = xxx which no doc has) below the handle, hence, also, no hippo:translation node should be there
        {
            Node doc = traverse(session, "/test/test4/doc");
            // localized name returns 'doc' and not Document because translation will be only there when at least one hippo:document below the handle
            assertEquals("doc", ((HippoNode)doc).getLocalizedName());
            assertTrue(doc.getNodes().getSize() == 0);
        }
    }

    @Test
    public void testBasics() throws Exception {
        build(content, session);
        session.save();
        session.refresh(false);
        assertNotNull(traverse(session, "/test/mirror/doc/doc"));
    }

    @Test
    public void testSubtyped() throws Exception {
        build(content, session);
        session.save();
        session.refresh(false);
        assertNotNull(traverse(session, "/test/spiegel/doc/doc"));
    }

    @Test
    public void testNoRoot() throws Exception {
        build(content, session);
        session.save();
        session.refresh(false);
        Node mirror = traverse(session, "/test/notallowed");
        assertNotNull(mirror);
        assertFalse(mirror.hasNodes());
        NodeIterator iter = mirror.getNodes();
        while(iter.hasNext()) {
            fail("no child nodes allowed");
        }
    }

    /*
     * This test is to make sure, that a facetselect/mirror directly pointing to another mirror/facetselect is not allowed
     * and will return an unpopulated facetselect/mirror with no children
     * @throws Exception
     */
    @Test
    public void testNotAllowedCombineDirect() throws Exception {
        build(combineContent1, session);
        session.save();
        build(combineContent2, session);
        session.save();
        build(combineContentFilterPointsToFilter, session);
        session.save();
        session.refresh(true);

        assertTrue(session.getRootNode().getNode("test").hasNode("filterTofilter"));
        assertFalse(session.getRootNode().getNode("test").getNode("filterTofilter").hasNodes());
       
    }

    @Test
    public void testCombineIndirect() throws Exception {
        build(combineContent1, session);
        session.save();
        build(combineContent2, session);
        session.save();
        build(combineContentFilterPointsToParentOfFilter, session);
        session.save();
        session.refresh(false);
        
        assertTrue(session.getRootNode().hasNode("test/filterToParentOfFilter/filter1/one/one"));
        assertFalse(session.getRootNode().hasNode("test/filterToParentOfFilter/filter1/three/three"));
        assertNotNull(traverse(session, "/test/filter1/two"));
        assertNotNull(traverse(session, "/test/filter1/two/two"));
        assertNotNull(traverse(session, "/test/filter1/three"));
        assertNull(traverse(session, "/test/filter1/three/three"));
        
        assertNotNull(traverse(session, "/test/filterToParentOfFilter/filter1/one/one"));
        assertNull(traverse(session, "/test/filterToParentOfFilter/filter1/two/two"));
        assertFalse(session.getRootNode().getNode("test/filterToParentOfFilter/filter1/two").getNodes().hasNext());
        assertFalse(session.getRootNode().getNode("test/filterToParentOfFilter/filter1/three").getNodes().hasNext());
        assertFalse(session.getRootNode().getNode("test/filterToParentOfFilter/filter1/four").getNodes().hasNext());
    }

    void recurse(Node node) throws RepositoryException {
        if (node instanceof HippoNode) {
            HippoNode hn = (HippoNode) node;
            Node canonicalNode = hn.getCanonicalNode();
            if (canonicalNode == null) {
                return;
            }
            if (!canonicalNode.isSame(node)) {
                return;
            }
        }
        for (NodeIterator nodes = node.getNodes(); nodes.hasNext(); ) {
            recurse(nodes.nextNode());
        }
    }

    @Test
    public void testMultiSessionObservation() throws RepositoryException {
        String[] content = {
            "/test", "nt:unstructured",
                "jcr:mixinTypes", "mix:referenceable",
            "/test/target", "nt:unstructured",
                "jcr:mixinTypes", "mix:referenceable",
            "/test/nav", "hippo:facetselect",
                "hippo:docbase", "/test/target",
                "hippo:facets", null,
                "hippo:modes", null,
                "hippo:values", null
        };
        build(content, session);
        session.save();
        session.refresh(false);

        final Session session2 = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
        class SessionEventListener implements javax.jcr.observation.EventListener {

            private Session session;

            SessionEventListener(Session session) throws RepositoryException {
                this.session = session;
                session.getWorkspace().getObservationManager().addEventListener(this,
                        Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_ADDED | Event.PROPERTY_REMOVED, "/",
                        true, null, null, true);
            }

            void destroy() throws RepositoryException {
                session.getWorkspace().getObservationManager().removeEventListener(this);
            }

            public void onEvent(EventIterator events) {
            }
        };
        new SessionEventListener(session2);

        SessionEventListener listener = new SessionEventListener(session);

        recurse(session.getNode("/test"));
        recurse(session2.getNode("/test"));

        traverse(session, "/test/nav").remove();
        traverse(session2, "/test").setProperty("x", "x");
        session2.save();
        session.save();
        session2.logout();

        listener.destroy();
    }

    @Test
    public void testRenamedFacetSelectIsIndexed() throws RepositoryException {
        String[] content = {
            "/test", "nt:unstructured",
                "jcr:mixinTypes", "mix:referenceable",
            "/test/target", "nt:unstructured",
                "jcr:mixinTypes", "mix:referenceable",
            "/test/nav", "hippo:facetselect",
                "hippo:docbase", "/test/target",
                "hippo:facets", null,
                "hippo:modes", null,
                "hippo:values", null
        };
        build(content, session);
        session.save();
        session.refresh(false);

        recurse(session.getNode("/test"));

        session.move("/test/nav", "/test/mirror");
        session.save();
        session.refresh(false);

        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery("//element(mirror,hippo:facetselect)", Query.XPATH);
        QueryResult result = query.execute();
        assertTrue(result.getNodes().hasNext());
    }

    @Test
    public void testCopiedFacetSelectIsIndexed() throws RepositoryException {
        String[] content = {
            "/test", "nt:unstructured",
            "jcr:mixinTypes", "mix:referenceable",
            "/test/target", "nt:unstructured",
            "jcr:mixinTypes", "mix:referenceable",
            "/test/target/dummy", "nt:unstructured",
            "/test/nav", "hippo:facetselect",
            "hippo:docbase", "/test/target",
            "hippo:facets", null,
            "hippo:modes", null,
            "hippo:values", null,
            "/test/nav2", "hippo:facetselect",
            "hippo:docbase", "/test/target",
            "hippo:facets", null,
            "hippo:modes", null,
            "hippo:values", null
        };
        build(content, session);
        session.save();
        session.refresh(false);

        recurse(session.getNode("/test"));
        JcrUtils.copy(session.getNode("/test/nav"), "mirror", session.getNode("/test"));
        session.save();
        session.refresh(false);

        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery("//element(mirror,hippo:facetselect)", Query.XPATH);
        QueryResult result = query.execute();
        assertTrue(result.getNodes().hasNext());
        assertTrue(result.getNodes().hasNext());
        assertTrue(result.getNodes().hasNext());
    }

    @Test
    public void testMovedFacetSelectIsIndexed() throws RepositoryException {
        String[] content = {
            "/test", "nt:unstructured",
                "jcr:mixinTypes", "mix:referenceable",
            "/test/target", "nt:unstructured",
                "jcr:mixinTypes", "mix:referenceable",
            "/test/nav", "hippo:facetselect",
                "hippo:docbase", "/test/target",
                "hippo:facets", null,
                "hippo:modes", null,
                "hippo:values", null,
            "/test/folder", "nt:unstructured"
        };
        build(content, session);
        session.save();
        session.refresh(false);

        recurse(session.getNode("/test"));

        session.move("/test/nav", "/test/folder/mirror");
        session.save();
        session.refresh(false);

        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery("//element(mirror,hippo:facetselect)", Query.XPATH);
        QueryResult result = query.execute();
        assertTrue(result.getNodes().hasNext());
    }

    @Test
    public void testSingleFilterBlocksEmptyMultiValue() throws Exception {
        final String[] data = new String[] {
            "/test", "nt:unstructured",
            "/test/docs", "hippo:testdocument",
                "jcr:mixinTypes", "mix:versionable",
            "/test/docs/doc", "hippo:handle",
                "jcr:mixinTypes", "hippo:hardhandle",
            "/test/docs/doc/doc", "hippo:testdocument",
                "jcr:mixinTypes", "mix:versionable",
            "/test/test4", "hippo:facetselect",
                "hippo:docbase", "/test/docs",
                "hippo:facets", "state",
                "hippo:values", "xxx",
                "hippo:modes", "single",
        };
        build(data, session);
        Node document = session.getNode("/test/docs/doc/doc");
        document.setProperty("state", new String[] {});
        session.save();
        session.refresh(false);

        // facetselect with mode = single. We expect no hippo:document (because facetselect state = xxx which no doc has) below the handle
        {
            Node doc = traverse(session, "/test/test4/doc");
            assertTrue(doc.getNodes().getSize() == 0);
        }
    }

}
