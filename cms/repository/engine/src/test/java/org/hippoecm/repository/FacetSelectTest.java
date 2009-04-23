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

import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.hippoecm.repository.api.HippoNodeType;

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
}
