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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertTrue;

public class MirrorCombiFacetSelectTest extends RepositoryTestCase {

    private static String[] content1 = new String[] {
        "/test",                                                             "nt:unstructured",
        "/test/documents",                                                   "nt:unstructured",
        "jcr:mixinTypes", "mix:referenceable",
        "/test/documents/articles",                                          "nt:unstructured",
        "/test/documents/articles/foo",                                      "nt:unstructured",
        "jcr:mixinTypes", "mix:referenceable",
        "/test/documents/articles/foo/brave-new-world",                      "hippo:handle",
        "jcr:mixinTypes", "hippo:hardhandle",
        "/test/documents/articles/foo/brave-new-world/brave-new-world",      "hippo:testdocument",
        "language","english",
        "state","published",
        "/test/documents/articles/foo/brave-new-world/brave-new-world",      "hippo:testdocument",
        "language","dutch",
        "state","unpublished",
        "/test/documents/articles/foo/brave-new-world2",                     "hippo:handle",
        "/test/documents/articles/foo/brave-new-world2/brave-new-world2",    "hippo:testdocument",
        "language","english",
        "state","published",
        "/test/documents/articles/foo/brave-new-world2/brave-new-world2",    "hippo:testdocument",
        "language","dutch",
        "/test/documents/articles/the-invisible-man",                        "hippo:handle",
        "/test/documents/articles/the-invisible-man/the-invisible-man",      "hippo:testdocument",
        "language","english",
        "/test/documents/articles/war-of-the-worlds",                        "hippo:handle",
        "jcr:mixinTypes", "mix:referenceable",
        "/test/documents/articles/war-of-the-worlds/war-of-the-worlds",      "hippo:testdocument",
        "language","english",
        "/test/documents/articles/war-of-the-worlds/war-of-the-worlds/handle-below-document", "hippo:handle",
        "/test/documents/articles/war-of-the-worlds/war-of-the-worlds/handle-below-document/handle-below-document", "hippo:testdocument",
        "language","english",
        "/test/documents/articles/war-of-the-worlds/war-of-the-worlds/handle-below-document/handle-below-document", "hippo:testdocument",
        "language","dutch"
    };
    
    private static String[] content2 = new String[] {
        "/test/documents/articles/war-of-the-worlds/war-of-the-worlds/mirror", "hippo:mirror",
        "hippo:docbase", "/test/documents/articles/foo",
        "/test/documents/articles/war-of-the-worlds/war-of-the-worlds/subtypemirror", "hippo:subtypemirror",
        "hippo:docbase", "/test/documents/articles/foo",
        "/test/documents/articles/war-of-the-worlds/war-of-the-worlds/some-image-node", "nt:unstructured",
        "/test/documents/articles/war-of-the-worlds/war-of-the-worlds/some-second-image-node", "nt:unstructured",
        "/test/documents/articles/war-of-the-worlds/war-of-the-worlds",      "hippo:testdocument",
        "language","dutch",
        "/test/documents/articles/war-of-the-worlds/war-of-the-worlds",      "hippo:testdocument",
        "/test/normalselect",                                                "hippo:facetselect",
        "hippo:docbase", "/test/documents/articles/war-of-the-worlds",
        "hippo:facets",  "state",
        "hippo:values",  "published",
        "hippo:modes",   "select",
        "/test/singledview",                                                  "hippo:facetselect",
        "hippo:docbase", "/test/documents/articles/war-of-the-worlds",
        "hippo:facets",  "state",
        "hippo:values",  "published",
        "hippo:modes",   "single",
        "/test/facetselectnofilter",                                          "hippo:facetselect",
        "hippo:docbase", "/test/documents/articles/war-of-the-worlds",
        "hippo:facets",  null,
        "hippo:values",  null,
        "hippo:modes",   null
    };
    

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        build(content1, session);
        session.save();
        build(content2, session);
        session.save();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testMirrorInheritFilterFromFacetSelect() throws Exception {
        session.refresh(false);
 
        // directly to the mirror, then below the 'brave-new-world' handle, we expect 2 document variants
        assertTrue(session.getRootNode().getNode("test/documents/articles/war-of-the-worlds/war-of-the-worlds/mirror").hasNode("brave-new-world"));
        assertTrue(session.getRootNode().getNode("test/documents/articles/war-of-the-worlds/war-of-the-worlds/mirror/brave-new-world").getNodes().getSize() == 2);
      
        // now we go through the facetselect with filter to the mirror: now we expect the mirror to inherit the filter from facetselect, this only 
        // have 1 document variant below brave-new-world:
    
        assertTrue(session.getRootNode().getNode("test/normalselect").hasNode("war-of-the-worlds/mirror"));
        assertTrue(session.getRootNode().getNode("test/normalselect/war-of-the-worlds/mirror").hasNode("brave-new-world"));
    
        // Now the count is 1 for a facetselect with filter and mode 'select' !!!
        assertTrue(session.getRootNode().getNode("test/normalselect/war-of-the-worlds/mirror/brave-new-world").getNodes().getSize() == 1);
    
    
        // now we go through the singledview facetselect with filter to the mirror: now we expect the mirror to inherit the filter from facetselect, this only 
        // have 1 document variant below brave-new-world:
    
        assertTrue(session.getRootNode().getNode("test/singledview").hasNode("war-of-the-worlds/mirror"));
        assertTrue(session.getRootNode().getNode("test/singledview/war-of-the-worlds/mirror").hasNode("brave-new-world"));
    
        // Now the count is 1 for a facetselect with filter and mode 'single' !!!
        assertTrue(session.getRootNode().getNode("test/singledview/war-of-the-worlds/mirror/brave-new-world").getNodes().getSize() == 1);
    
        // now we go through the facetselectnofilter with *no* filter to the mirror: now we expect the mirror to again have 2 document variants
    
        assertTrue(session.getRootNode().getNode("test/facetselectnofilter").hasNode("war-of-the-worlds/mirror"));
        assertTrue(session.getRootNode().getNode("test/facetselectnofilter/war-of-the-worlds/mirror").hasNode("brave-new-world"));
    
        // Now the count is 2 for a facetselect with *no* filter
        assertTrue(session.getRootNode().getNode("test/facetselectnofilter/war-of-the-worlds/mirror/brave-new-world").getNodes().getSize() == 2);
    
    
        // SAME CYCLE FOR SUBTYPE OF MIRROR
    
        assertTrue(session.getRootNode().getNode("test/singledview").hasNode("war-of-the-worlds/subtypemirror"));
        assertTrue(session.getRootNode().getNode("test/singledview/war-of-the-worlds/subtypemirror").hasNode("brave-new-world"));
    
        // Now the count is 1 for a facetselect with filter and mode 'single' !!!
        assertTrue(session.getRootNode().getNode("test/singledview/war-of-the-worlds/subtypemirror/brave-new-world").getNodes().getSize() == 1);
    
        // now we go through the facetselectnofilter with *no* filter to the mirror: now we expect the mirror to again have 2 document variants
    
        assertTrue(session.getRootNode().getNode("test/facetselectnofilter").hasNode("war-of-the-worlds/subtypemirror"));
        assertTrue(session.getRootNode().getNode("test/facetselectnofilter/war-of-the-worlds/subtypemirror").hasNode("brave-new-world"));
    
        // Now the count is 2 for a facetselect with *no* filter
        assertTrue(session.getRootNode().getNode("test/facetselectnofilter/war-of-the-worlds/subtypemirror/brave-new-world").getNodes().getSize() == 2);
    
    }
}
