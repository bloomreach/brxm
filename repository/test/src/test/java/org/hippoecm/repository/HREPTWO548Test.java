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

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HREPTWO548Test extends RepositoryTestCase {

    private String[] content1 = {
        "/test", "nt:unstructured",
        "/test/docs", "nt:unstructured",
        "jcr:mixinTypes", "mix:referenceable",
        "/test/docs/red", "hippo:document",
        "jcr:mixinTypes", "mix:versionable"
    };
    private String[] content2 = {
        "/test/nav", "hippo:facetselect",
        "hippo:docbase", "/test/docs",
        "hippo:facets", "lang",
        "hippo:values", "en",
        "hippo:modes", "select"
    };
    private String[] content3 = {
        "/test/docs/blue", "hippo:document",
        "jcr:mixinTypes", "mix:versionable"
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

        result = traverse(session, "/test/docs/red");
        assertNotNull(result);

        Node browse = traverse(session, "/test/nav");
        assertNotNull(result);
        assertTrue(browse.hasNode("red"));
        assertFalse(browse.hasNode("blue"));

        session.refresh(false);
        try {
            assertFalse(browse.hasNode("yellow"));
        } catch(InvalidItemStateException ex) {
            // allowed result
        }

        browse = traverse(session, "/test/nav");

        { // intermezzo: other session adds node
            Session session2 = server.login(SYSTEMUSER_ID, SYSTEMUSER_PASSWORD);
            session2.getRootNode().getNode("test/docs");
            build(content3, session2);
            session2.save();
            session2.logout();
        }

        assertTrue(browse.hasNode("red"));
        assertFalse(browse.hasNode("blue"));

        // with refresh(true) you DON'T get your virtual tree updated
        session.refresh(true);

        browse = traverse(session, "/test/nav");
        assertNotNull(result);
        assertTrue(browse.hasNode("red"));
        assertFalse(browse.hasNode("blue"));

        // with refresh(false) you DO get your virtual tree updated
        session.refresh(false);

        browse = traverse(session, "/test/nav");
        assertNotNull(result);
        assertTrue(browse.hasNode("red"));
        assertTrue(browse.hasNode("blue"));
    }
}
