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
package org.hippoecm.repository.decorating;

import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;

import org.junit.*;
import static org.junit.Assert.*;

public class FacetedReferenceTest extends org.hippoecm.repository.TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static String[] contents = new String[] {
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
        "/test/documents/articles/brave-new-world/brave-new-world",          "hippo:testdocument",
        "language","english",
        "/test/documents/articles/the-invisible-man",                        "hippo:handle",
        "/test/documents/articles/the-invisible-man/the-invisible-man",      "hippo:testdocument",
        "language","english",
        "/test/documents/articles/war-of-the-worlds",                        "hippo:handle",
        "jcr:mixinTypes", "mix:referenceable",
        "/test/documents/articles/war-of-the-worlds/war-of-the-worlds",      "hippo:testdocument",
        "language","english",
        "/test/documents/articles/war-of-the-worlds/war-of-the-worlds",      "hippo:testdocument",
        "language","dutch",
        "/test/documents/articles/nineteeneightyfour",                       "hippo:handle",
        "/test/documents/articles/nineteeneightyfour/nineteeneightyfour",    "hippo:testdocument",
        "language","dutch",
        "/test/documents/articles/nineteeneightyfour/nineteeneightyfour",    "hippo:testdocument",
        "language","english",
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
        "hippo:modes",   "clear"
    };

    @Before
    public void setUp() throws Exception {
        super.setUp();
        build(session, contents);
        session.save();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test public void testFacetedReference() throws Exception {
        assertNotNull(traverse(session,"/test/documents/articles/war-of-the-worlds/war-of-the-worlds"));
        assertNotNull(traverse(session,"/test/documents/articles/war-of-the-worlds/war-of-the-worlds[language='dutch']"));
        assertNotNull(traverse(session,"/test/documents/articles/war-of-the-worlds/war-of-the-worlds[language='english']"));
        assertNotNull(traverse(session,"/test/english/articles/brave-new-world/brave-new-world"));
        assertNotNull(traverse(session,"/test/english/articles/war-of-the-worlds/war-of-the-worlds[language='english']"));
        assertNull(traverse(session,"/test/english/articles/war-of-the-worlds/war-of-the-worlds[language='dutch']"));
        assertNotNull(traverse(session,"/test/dutch/war-of-the-worlds[language='dutch']"));
        assertNull(traverse(session,"/test/dutch/war-of-the-worlds[language='english']"));
    }
}
