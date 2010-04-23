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
package org.hippoecm.repository.sample;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.junit.*;
import static org.junit.Assert.*;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.HippoWorkspace;

public class SamplePersistencyTest {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private HippoRepository server;

    @Before
    public void setUp() throws RepositoryException, IOException {
        server = HippoRepositoryFactory.getHippoRepository();
        SampleWorkflowSetup.commonStart(server);
    }

    @After
    public void tearDown() throws RepositoryException {
        SampleWorkflowSetup.commonEnd(server);
        server.close();
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
