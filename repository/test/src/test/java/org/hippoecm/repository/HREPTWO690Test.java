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

import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HREPTWO690Test extends RepositoryTestCase {

    private String[] content1 = {
        "/test", "nt:unstructured",
        "/test/docs", "nt:unstructured",
        "jcr:mixinTypes", "mix:referenceable",
        "/test/docs/funny", "hippo:document",
        "jcr:mixinTypes", "mix:versionable",
        "jcr:mixinTypes", "hippo:testmixin",
        "hippo:a", "test",
    };
    private String[] content2 = {
        "/test/nav", "hippo:facetselect",
        "hippo:docbase", "/test/docs",
        "hippo:facets", "lang",
        "hippo:values", "en",
        "hippo:modes", "select"
    };

    @Test
    public void testIssue() throws RepositoryException {
        Node result;
        build(content1, session);
        session.save();
        build(content2, session);
        session.save();
        session.refresh(false);
        //Utilities.dump(session.getRootNode());

        result = traverse(session, "/test/docs/funny");
        assertNotNull(result);
        assertTrue(result.isNodeType("hippo:testmixin"));
        assertTrue(result.isNodeType("mix:versionable"));
        assertFalse(result.isNodeType("hipposys:softdocument"));

        result = traverse(session, "/test/nav/funny");
        assertNotNull(result);
        assertTrue(result.isNodeType("hippo:testmixin"));
        assertFalse(result.isNodeType("mix:versionable"));
        assertTrue(result.isNodeType("hipposys:softdocument"));
    }
}
