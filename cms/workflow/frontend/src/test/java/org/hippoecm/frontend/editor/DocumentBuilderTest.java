/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.editor;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.service.EditorException;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.*;

public class DocumentBuilderTest extends RepositoryTestCase {


    String[] draftPublishedUnpublished = new String[] {
            "/test", "nt:unstructured",
            "/test/content", "hippostd:folder",
            "jcr:mixinTypes", "mix:referenceable",
            "/test/content/document", "hippo:handle",
            "jcr:mixinTypes", "mix:referenceable",
            "/test/content/document/document", "frontendtest:document",
            "jcr:mixinTypes", "mix:versionable",
            "hippostd:state", "unpublished",
            "/test/content/document/document", "frontendtest:document",
            "jcr:mixinTypes", "mix:versionable",
            "hippostd:state", "draft",
            "/test/content/document/document", "frontendtest:document",
            "jcr:mixinTypes", "mix:versionable",
            "hippostd:state", "published"
    };

    String[] draftPublishedUnpublishedTransferable = new String[] {
            "/test", "nt:unstructured",
            "/test/content", "hippostd:folder",
            "jcr:mixinTypes", "mix:referenceable",
            "/test/content/document", "hippo:handle",
            "jcr:mixinTypes", "mix:referenceable",
            "/test/content/document/document", "frontendtest:document",
            "jcr:mixinTypes", "mix:versionable",
            "hippostd:state", "unpublished",
            "/test/content/document/document", "frontendtest:document",
            "jcr:mixinTypes", "mix:versionable",
            "hippostd:state", "draft",
            "hippostd:transferable","true",
            "/test/content/document/document", "frontendtest:document",
            "jcr:mixinTypes", "mix:versionable",
            "hippostd:state", "published"
    };


    String[] draftPublishedUnpublishedTransferableHolder = new String[] {
            "/test", "nt:unstructured",
            "/test/content", "hippostd:folder",
            "jcr:mixinTypes", "mix:referenceable",
            "/test/content/document", "hippo:handle",
            "jcr:mixinTypes", "mix:referenceable",
            "/test/content/document/document", "frontendtest:document",
            "jcr:mixinTypes", "mix:versionable",
            "hippostd:state", "unpublished",
            "/test/content/document/document", "frontendtest:document",
            "jcr:mixinTypes", "mix:versionable",
            "hippostd:state", "draft",
            "hippostd:transferable","true",
            "hippostd:holder","userId",
            "/test/content/document/document", "frontendtest:document",
            "jcr:mixinTypes", "mix:versionable",
            "hippostd:state", "published"
    };


    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

    }

    @Test
    public void DraftPublishedUnpublished() throws RepositoryException, EditorException {
        build(draftPublishedUnpublished, session);
        final DocumentImpl actual = DocumentBuilder
                .create()
                .node(session.getNode("/test/content/document")).build();
        final DocumentImpl expected = new DocumentImpl();
        expected.setUnpublished("/test/content/document/document");
        expected.setPublished("/test/content/document/document[3]");
        expected.setDraft("/test/content/document/document[2]");
        assertEquals(expected, actual);
    }

    @Test
    public void DraftPublishedUnpublishedTransferable() throws RepositoryException, EditorException {
        build(draftPublishedUnpublishedTransferable, session);
        final DocumentImpl actual = DocumentBuilder
                .create()
                .node(session.getNode("/test/content/document")).build();
        final DocumentImpl expected = new DocumentImpl();
        expected.setUnpublished("/test/content/document/document");
        expected.setPublished("/test/content/document/document[3]");
        expected.setDraft("/test/content/document/document[2]");
        expected.setTransferable(true);
        assertEquals(expected, actual);
    }

    @Test
    public void DraftPublishedUnpublishedTransferableHolder() throws RepositoryException, EditorException {
        build(draftPublishedUnpublishedTransferableHolder, session);
        final DocumentImpl actual = DocumentBuilder
                .create()
                .userId("userId")
                .node(session.getNode("/test/content/document")).build();
        final DocumentImpl expected = new DocumentImpl();
        expected.setUnpublished("/test/content/document/document");
        expected.setPublished("/test/content/document/document[3]");
        expected.setDraft("/test/content/document/document[2]");
        expected.setTransferable(true);
        expected.setHolder(true);
        assertEquals(expected, actual);
    }

}
