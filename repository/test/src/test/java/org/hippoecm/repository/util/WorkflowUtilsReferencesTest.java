/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.util;

import java.util.List;
import java.util.Set;

import javax.jcr.Node;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WorkflowUtilsReferencesTest extends RepositoryTestCase {

    private String[] content = {
            "/test/folder", "hippostd:folder",
            "/test/folder/doc1", "hippo:handle",
            "/test/folder/doc1/doc1", "hippo:testdocument",

            "/test/folder/doc2", "hippo:handle",
            "/test/folder/doc2/doc2", "hippo:testdocument",
                "hippo:availability", "preview",
            "/test/folder/doc2/doc2/link", "hippo:mirror",
                "hippo:docbase", "",

            "/test/folder/doc3", "hippo:handle",
            "/test/folder/doc3/doc3", "hippo:testdocument",
                "hippo:availability", "preview",
            "/test/folder/doc3/doc3/link", "hippo:facetselect",
                "hippo:docbase", "",
                "hippo:values", null,
                "hippo:facets", null,
                "hippo:modes", null,

            "/test/folder/doc4", "hippo:handle",
            "/test/folder/doc4/doc4", "hippo:testdocument",
                "hippo:availability", "live",
            "/test/folder/doc4/doc4/link", "hippo:mirror",
                "hippo:docbase", "",

            "/test/folder/doc5", "hippo:handle",
            "/test/folder/doc5/doc5", "hippo:testdocument",
                "hippo:availability", "live",
            "/test/folder/doc5/doc5/link", "hippo:facetselect",
                "hippo:docbase", "",
                "hippo:values", null,
                "hippo:facets", null,
                "hippo:modes", null,

            "/test/folder/doc6", "hippo:handle",
            "/test/folder/doc6/doc6", "hippo:testdocument",
                "hippostd:state", "unpublished",
            "/test/folder/doc6/doc6/link1", "hippo:mirror",
                "hippo:docbase", "",
            "/test/folder/doc6/doc6/link2", "hippo:mirror",
                "hippo:docbase", "",
            "/test/folder/doc6/doc6/link3", "hippo:mirror",
                "hippo:docbase", "",
    };

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp(true);
        final Node root = session.getRootNode();
        root.addNode("test");
        build(content, session);
        session.save();
    }

    @Test
    public void getReferringDocumentsUnpublished() throws Exception {
        // doc1 is referenced by doc2 - doc5
        final Node doc1Handle = session.getNode("/test/folder/doc1");
        final Node doc2Handle = session.getNode("/test/folder/doc2");
        final Node doc2Link = session.getNode("/test/folder/doc2/doc2/link");
        doc2Link.setProperty("hippo:docbase", doc1Handle.getIdentifier());
        final Node doc3Handle = session.getNode("/test/folder/doc3");
        final Node doc3Link = session.getNode("/test/folder/doc3/doc3/link");
        doc3Link.setProperty("hippo:docbase", doc1Handle.getIdentifier());
        final Node doc4Handle = session.getNode("/test/folder/doc4");
        final Node doc4Link = session.getNode("/test/folder/doc4/doc4/link");
        doc4Link.setProperty("hippo:docbase", doc1Handle.getIdentifier());
        final Node doc5Handle = session.getNode("/test/folder/doc5");
        final Node doc5Link = session.getNode("/test/folder/doc5/doc5/link");
        doc5Link.setProperty("hippo:docbase", doc1Handle.getIdentifier());

        // doc6 references doc2, doc3 and doc4
        final Node doc6Handle = session.getNode("/test/folder/doc6");
        final Node doc6Link1 = session.getNode("/test/folder/doc6/doc6/link1");
        doc6Link1.setProperty("hippo:docbase", doc2Handle.getIdentifier());
        final Node doc6Link2 = session.getNode("/test/folder/doc6/doc6/link2");
        doc6Link2.setProperty("hippo:docbase", doc3Handle.getIdentifier());
        final Node doc6Link3 = session.getNode("/test/folder/doc6/doc6/link3");
        doc6Link3.setProperty("hippo:docbase", doc4Handle.getIdentifier());

        session.save();

        final Set<String> referringUnpublishedDocuments = WorkflowUtils.getReferringDocuments(doc1Handle, true).keySet();
        assertEquals("Number of referring documents is not correct", referringUnpublishedDocuments.size(), 2);
        assertTrue("Doc2 is not returned as a referring unpublished document", referringUnpublishedDocuments.contains(doc2Handle.getIdentifier()));
        assertTrue("Doc3 is not returned as a referring unpublished document", referringUnpublishedDocuments.contains(doc3Handle.getIdentifier()));

        final Set<String> referringPublishedDocuments = WorkflowUtils.getReferringDocuments(doc1Handle, false).keySet();
        assertEquals("Number of referring documents is not correct", referringPublishedDocuments.size(), 2);
        assertTrue("Doc4 is not returned as a referring published document", referringPublishedDocuments.contains(doc4Handle.getIdentifier()));
        assertTrue("Doc5 is not returned as a referring published document", referringPublishedDocuments.contains(doc5Handle.getIdentifier()));

        final List<String> referencesToUnpublishedDocuments = WorkflowUtils.getReferencesToUnpublishedDocuments(doc6Handle, session);
        assertEquals("Number of referring documents is not correct", referencesToUnpublishedDocuments.size(), 2);
        assertTrue("Doc2 is not returned as a referring unpublished document", referencesToUnpublishedDocuments.contains(doc2Handle.getIdentifier()));
        assertTrue("Doc3 is not returned as a referring unpublished document", referencesToUnpublishedDocuments.contains(doc3Handle.getIdentifier()));
        assertFalse("Doc4 is returned as a referring published document", referencesToUnpublishedDocuments.contains(doc4Handle.getIdentifier()));

    }

}
