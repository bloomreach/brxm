/*
 * Copyright 2015-2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.treepickerrepresentation;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.pagecomposer.jaxrs.model.treepicker.AbstractTreePickerRepresentation;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.Test;

import static org.hippoecm.repository.HippoStdNodeType.NT_DIRECTORY;
import static org.hippoecm.repository.HippoStdNodeType.NT_FOLDER;
import static org.junit.Assert.assertEquals;

public class RootContentHippoDocumentResourceTest extends AbstractTestTreePickerRepresentation {

    @Test
    public void root_content_representation_assertions() throws Exception {
        // request for the homepage but do not set the homepage as REQUEST_CONFIG_NODE_IDENTIFIER hence 'false'
        AbstractTreePickerRepresentation representation = createRootContentRepresentation("", getRootContentConfigIdentifier());
        rootContentRepresentationAssertions(representation);

        assertEquals(getRootContentConfigIdentifier(), representation.getId());
        assertEquals("unittestproject", representation.getDisplayName());
    }

    private void addContent() throws RepositoryException {
        JcrUtils.copy(session, "/unittestcontent/documents/unittestproject/common",
                "/unittestcontent/documents/unittestproject/bbb");

        // two documents
        JcrUtils.copy(session, "/unittestcontent/documents/unittestproject/common/homepage",
                "/unittestcontent/documents/unittestproject/doc1");
        JcrUtils.copy(session, "/unittestcontent/documents/unittestproject/common/aboutfolder/about-us",
                "/unittestcontent/documents/unittestproject/doc2");

        JcrUtils.copy(session, "/unittestcontent/documents/unittestproject/common",
                "/unittestcontent/documents/unittestproject/aaa");
        JcrUtils.copy(session, "/unittestcontent/documents/unittestproject/common",
                "/unittestcontent/documents/unittestproject/ccc");
    }

    private void cleanupContent(final String nodePath, final String originalPrimaryType) throws RepositoryException {
        session.getNode(nodePath).setPrimaryType(originalPrimaryType);
        session.getNode("/unittestcontent/documents/unittestproject/bbb").remove();
        session.getNode("/unittestcontent/documents/unittestproject/doc1").remove();
        session.getNode("/unittestcontent/documents/unittestproject/doc2").remove();
        session.getNode("/unittestcontent/documents/unittestproject/aaa").remove();
        session.getNode("/unittestcontent/documents/unittestproject/ccc").remove();
    }


    @Test
    public void representation_shows_folders_and_documents_by_jcr_order_for_ordered_folders() throws Exception {

        final String absRootContentPath = "/unittestcontent/documents/unittestproject";
        final String originalPrimaryType = session.getNode(absRootContentPath).getPrimaryNodeType().getName();

        try {
            /**
             * unittestcontent/documents/unittestproject is of type hippostd:directory which is an *unordered* cms folder,
             * hence we need to set the primary typp to hippostd:folder which is "ordered"
             */
            session.getNode(absRootContentPath).setPrimaryType(NT_FOLDER);

            addContent();

            session.save();
            Thread.sleep(100);

            // request for the homepage but do not set the homepage as REQUEST_CONFIG_NODE_IDENTIFIER hence 'false'
            AbstractTreePickerRepresentation representation = createRootContentRepresentation("", getRootContentConfigIdentifier());

            assertEquals("common", representation.getItems().get(0).getDisplayName());
            assertEquals("experiences", representation.getItems().get(1).getDisplayName());
            assertEquals("News", representation.getItems().get(2).getDisplayName());
            assertEquals("bbb", representation.getItems().get(3).getDisplayName());
            assertEquals("Home Page", representation.getItems().get(4).getDisplayName());
            assertEquals("doc1", representation.getItems().get(4).getNodeName());
            assertEquals("About Us", representation.getItems().get(5).getDisplayName());
            assertEquals("doc2", representation.getItems().get(5).getNodeName());
            assertEquals("aaa", representation.getItems().get(6).getDisplayName());
            assertEquals("ccc", representation.getItems().get(7).getDisplayName());
        } finally {
            cleanupContent(absRootContentPath, originalPrimaryType);
            session.save();
            // give time for jcr events to evict model
            Thread.sleep(100);
        }
    }


    @Test
    public void representation_shows_folders_first_then_documents_both_ordered_on_displayName_for_jcr_unordered_folders() throws Exception {

        final String absRootContentPath = "/unittestcontent/documents/unittestproject";
        final String originalPrimaryType = session.getNode(absRootContentPath).getPrimaryNodeType().getName();

        try {
            /**
             * unittestcontent/documents/unittestproject is must now be of type hippostd:directory which is an *unordered* cms folder
             */
            session.getNode(absRootContentPath).setPrimaryType(NT_DIRECTORY);

            addContent();

            session.save();
            Thread.sleep(100);

            // request for the homepage but do not set the homepage as REQUEST_CONFIG_NODE_IDENTIFIER hence 'false'
            AbstractTreePickerRepresentation representation = createRootContentRepresentation("", getRootContentConfigIdentifier());

            assertEquals("aaa", representation.getItems().get(0).getDisplayName());
            assertEquals("bbb", representation.getItems().get(1).getDisplayName());
            assertEquals("ccc", representation.getItems().get(2).getDisplayName());
            assertEquals("common", representation.getItems().get(3).getDisplayName());
            assertEquals("experiences", representation.getItems().get(4).getDisplayName());
            assertEquals("News", representation.getItems().get(5).getDisplayName());
            assertEquals("About Us", representation.getItems().get(6).getDisplayName());
            assertEquals("doc2", representation.getItems().get(6).getNodeName());
            assertEquals("Home Page", representation.getItems().get(7).getDisplayName());
            assertEquals("doc1", representation.getItems().get(7).getNodeName());
        } finally {
            cleanupContent(absRootContentPath, originalPrimaryType);
            session.save();
            // give time for jcr events to evict model
            Thread.sleep(100);
        }
    }


}