/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_NAME;
import static org.hippoecm.repository.api.HippoNodeType.NT_NAMED;
import static org.junit.Assert.assertEquals;

public class HippoNodeDisplayNameTest extends RepositoryTestCase {

    private String[] content = new String[]{
            "/test", "nt:unstructured",
            "/test/Document1", "hippo:handle",
            "/test/Document1/Document1", "hippo:document"
    };

    private static final String DOCUMENT_DISPLAYNAME = "Display Name";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        build(content, session);
        session.save();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testUnNamedHandleDisplayName() throws RepositoryException {
        HippoNode handle = (HippoNode)session.getNode("/test/Document1");
        assertEquals("Document1", handle.getDisplayName());
    }

    @Test
    public void testUnNamedHandleDocumentDisplayName() throws RepositoryException {
        HippoNode document = (HippoNode)session.getNode("/test/Document1/Document1");
        assertEquals("Document1", document.getDisplayName());
    }

    @Test
    public void testNamedHandleDisplayName() throws RepositoryException {
        HippoNode handle = (HippoNode)session.getNode("/test/Document1");
        handle.addMixin(NT_NAMED);
        handle.setProperty(HIPPO_NAME, DOCUMENT_DISPLAYNAME);
        assertEquals(DOCUMENT_DISPLAYNAME, handle.getDisplayName());
    }

    @Test
    public void testNamedHandleDocumentDisplayName() throws RepositoryException {
        HippoNode handle = (HippoNode)session.getNode("/test/Document1");
        handle.addMixin(NT_NAMED);
        handle.setProperty(HIPPO_NAME, DOCUMENT_DISPLAYNAME);
        assertEquals(DOCUMENT_DISPLAYNAME, ((HippoNode)handle.getNode("Document1")).getDisplayName());
    }
}

