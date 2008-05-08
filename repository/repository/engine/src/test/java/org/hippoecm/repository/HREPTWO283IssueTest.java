/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.TestCase;
import org.junit.*;
import static org.junit.Assert.*;

public class HREPTWO283IssueTest extends TestCase {
    private String[] content1 = {
        "/test",            "nt:unstructured",
        "/test/docs",       "nt:unstructured",
        "jcr:mixinTypes",   "mix:referenceable",
        "/test/docs/funny", "hippo:baddocument",
        "hippo:x",          "test"
    };
    private String[] content2 = {
        "/test/nav",     "hippo:facetselect",
        "hippo:docbase", "/test/docs",
        "hippo:facets",  "lang",
        "hippo:values",  "en",
        "hippo:modes",   "select"
    };

    public void setUp() throws Exception {
        super.setUp();
    }

    @Test public void testIssue() throws RepositoryException {
        Node result;
        build(session, content1);
        build(session, content2);
        session.save();
        session.refresh(false);
        //Utilities.dump(session.getRootNode());

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
}
