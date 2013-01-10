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
package org.hippoecm.repository.sample;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.HippoWorkspace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SamplePersistencyTest extends RepositoryTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        SampleWorkflowSetup.commonStart(server);
    }

    @After
    public void tearDown() throws Exception {
        SampleWorkflowSetup.commonEnd(server);
        super.tearDown();
    }

    @Test
    public void testAuthorDocument() throws RepositoryException {
        Session session = server.login("admin","admin".toCharArray());

        DocumentManager manager = ((HippoWorkspace)session.getWorkspace()).getDocumentManager();
        Document document = manager.getDocument("authors","Jan Smit");

        assertNotNull(document);

        assertTrue("document is of wrong type: "+document.getClass().getName(), document instanceof AuthorDocument);
        AuthorDocument author = (AuthorDocument) document;
        assertTrue(author.authorId == SampleWorkflowSetup.newAuthorId);

        session.logout();
    }
}
