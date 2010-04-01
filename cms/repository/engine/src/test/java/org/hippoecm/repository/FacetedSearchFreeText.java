/*
 *  Copyright 2010 Hippo.
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
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.api.HippoNodeType;

import org.hippoecm.repository.util.Utilities;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class FacetedSearchFreeText extends TestCase {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    public static String[] content = new String[] {
        "/test",           "nt:unstructured",
        "jcr:mixinTypes",  "mix:referenceable",
        "/test/docs",      "nt:unstructured",
        "jcr:mixinTypes",  "mix:referenceable",
        "/test/docs/a",    "hippo:handle",
        "jcr:mixinTypes",  "hippo:hardhandle",
        "/test/docs/a/a",  "hippo:testdocument",
        "jcr:mixinTypes",  "hippo:harddocument",
        "x",               "a",
        "y",               "z",
        "text",            "aap",
        "/test/docs/b",    "hippo:handle",
        "jcr:mixinTypes",  "hippo:hardhandle",
        "/test/docs/b/b",  "hippo:testdocument",
        "jcr:mixinTypes",  "hippo:harddocument",
        "x",               "b",
        "y",               "z",
        "text",            "noot",
        "/test/nav",       "hippo:facetsearch",
        "hippo:facets",    "y",
        "hippo:facets",    "x",
        "hippo:docbase",   "/test/docs",
        "hippo:queryname", "test"
    };

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        if (session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        build(session, content);
        session.save();
    }

    @Override
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() throws RepositoryException {
        try {
            //Node node = session.getRootNode().getNode("test");
            //node = node.getNodes("nav['x']").nextNode();
            Node node = session.getRootNode();

            //node = node.getNode("test/nav");
            node = node.getNode("test/nav[[aap]]");
            Utilities.dump(System.err, node);

            //NodeIterator iter = node.getNode("hippo:resultset").getNodes();
            //assertTrue(iter.hasNext());
            //assertEquals("doc", iter.nextNode().getName());
        } catch (PathNotFoundException ex) {
            ex.printStackTrace(System.err);
        }
    }
}
