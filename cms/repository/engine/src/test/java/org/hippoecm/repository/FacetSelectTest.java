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
import javax.jcr.NodeIterator;

import org.junit.After;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.hippoecm.repository.util.Utilities;

public class FacetSelectTest extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    String[] content = new String[] {
        "/test",              "nt:unstructured",
        "/test/docs",         "hippo:testdocument",
        "jcr:mixinTypes",     "hippo:harddocument",
        "/test/docs/doc",     "hippo:handle",
        "jcr:mixinTypes",     "hippo:hardhandle",
        "/test/docs/doc/doc", "hippo:document",
        "jcr:mixinTypes",     "hippo:harddocument",
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
        "jcr:mixinTypes",     "hippo:harddocument",
        "/test/docs/one",     "hippo:handle",
        "jcr:mixinTypes",     "hippo:hardhandle",
        "/test/docs/one/one", "hippo:testdocument",
        "jcr:mixinTypes",     "hippo:harddocument",
        "vehicle", "car",
        "color", "red",
        "/test/docs/two",     "hippo:handle",
        "jcr:mixinTypes",     "hippo:hardhandle",
        "/test/docs/two/two", "hippo:testdocument",
        "jcr:mixinTypes",     "hippo:harddocument",
        "vehicle", "car",
        "color", "blue",
        "/test/docs/three",     "hippo:handle",
        "jcr:mixinTypes",     "hippo:hardhandle",
        "/test/docs/three/three", "hippo:testdocument",
        "jcr:mixinTypes",     "hippo:harddocument",
        "vehicle", "bike",
        "color", "red",
        "/test/docs/four",     "hippo:handle",
        "jcr:mixinTypes",     "hippo:hardhandle",
        "/test/docs/four/four", "hippo:testdocument",
        "jcr:mixinTypes",     "hippo:harddocument",
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
    String[] combineContent3a = new String[] {
        "/test/filter2",      "hippo:facetselect",
        "hippo:docbase",      "/test/filter1",
        "hippo:facets",       "color",
        "hippo:values",       "red",
        "hippo:modes",        "select"
    };
    String[] combineContent3b = new String[] {
        "/test/filter2",      "hippo:facetselect",
        "hippo:docbase",      "/test",
        "hippo:facets",       "color",
        "hippo:values",       "red",
        "hippo:modes",        "select"
    };

    @After
    public void tearDown() throws Exception {
        if(session.getRootNode().hasNode("test")) {
	    session.getRootNode().getNode("test").remove();
	}
	session.save();
	super.tearDown();
    }

    @Test
    public void testBasics() throws Exception {
        build(session, content);
        session.save();
        session.refresh(false);
        assertNotNull(traverse(session, "/test/mirror/doc/doc"));
    }

    @Test
    public void testSubtyped() throws Exception {
        build(session, content);
        session.save();
        session.refresh(false);
        assertNotNull(traverse(session, "/test/spiegel/doc/doc"));
    }

    @Test
    public void testNoRoot() throws Exception {
        build(session, content);
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

    @Ignore
    public void testCombineDirect() throws Exception {
        build(session, combineContent1);
        session.save();
        build(session, combineContent2);
        session.save();
        build(session, combineContent3a);
        session.save();
        session.refresh(false);
	/* The following iteration loop, will make the test functional, but it shouldn't be required */
	for(NodeIterator iter = traverse(session,"/test/filter1").getNodes(); iter.hasNext(); ) {
	    Node child = iter.nextNode();
	    child.getNodes();
	}
        assertNotNull(traverse(session, "/test/filter2/one/one"));
        assertNull(traverse(session, "/test/filter2/two/two"));
        assertFalse(session.getRootNode().getNode("test/filter2/two").getNodes().hasNext());
        assertFalse(session.getRootNode().getNode("test/filter2/three").getNodes().hasNext());
        assertFalse(session.getRootNode().getNode("test/filter2/four").getNodes().hasNext());
    }

    @Ignore
    public void testCombineIndirect() throws Exception {
        build(session, combineContent1);
        session.save();
        build(session, combineContent2);
        session.save();
        build(session, combineContent3b);
        session.save();
        session.refresh(false);
        assertNotNull(traverse(session, "/test/filter2/filter1/one/one"));
        assertNull(traverse(session, "/test/filter2/filter1/two/two"));
        assertFalse(session.getRootNode().getNode("test/filter2/filter1/two").getNodes().hasNext());
        assertFalse(session.getRootNode().getNode("test/filter2/filter1/three").getNodes().hasNext());
        assertFalse(session.getRootNode().getNode("test/filter2/filter1/four").getNodes().hasNext());
    }

}
