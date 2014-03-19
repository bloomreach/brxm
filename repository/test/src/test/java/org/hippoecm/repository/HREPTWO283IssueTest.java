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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HREPTWO283IssueTest extends RepositoryTestCase {

    private String[] content1 = {
        "/test",             "nt:unstructured",
        "/test/docs",        "nt:unstructured",
        "jcr:mixinTypes",    "mix:referenceable",
        "/test/docs/funny",  "hippo:baddocument",
        "hippo:x",           "test",
        "/test/docs/proper", "hippo:document",
        "jcr:mixinTypes",    "mix:versionable"
    };
    private String[] content2 = {
        "/test/nav",     "hippo:facetselect",
        "hippo:docbase", "/test/docs",
        "hippo:facets",  "lang",
        "hippo:values",  "en",
        "hippo:modes",   "select"
    };

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testIssue() throws RepositoryException {
        Node result;
        build(content1, session);
        session.save();
        session.refresh(false);
        build(content2, session);
        session.save();
        session.refresh(false);

        result = traverse(session, "/test/docs/funny");
        assertNotNull(result);
        assertTrue(result.isNodeType("hippo:baddocument"));
        assertTrue(result.isNodeType("mix:referenceable"));

        result = traverse(session, "/test/nav/funny");
        assertNotNull(result);
        assertTrue(result.isNodeType("hippo:baddocument"));

        // Actual test for issue follows (assert should be assertFalse)
        assertTrue(result.isNodeType("mix:referenceable"));
    }

    @Test
    public void testNoBadRemove() throws RepositoryException {
        build(content1, session);
        session.save();
        session.refresh(false);
        build(content2, session);
        session.save();
        session.refresh(false);

        assertNotNull(traverse(session, "/test/docs/funny"));
        assertNotNull(traverse(session, "/test/docs/proper"));
        assertNotNull(traverse(session, "/test/nav/funny"));
        assertNotNull(traverse(session, "/test/nav/proper"));
        traverse(session, "/test/nav").remove();
        session.save();
        session.refresh(false);
        assertNotNull(traverse(session, "/test/docs/funny"));
        assertNotNull(traverse(session, "/test/docs/proper"));
    }
}
