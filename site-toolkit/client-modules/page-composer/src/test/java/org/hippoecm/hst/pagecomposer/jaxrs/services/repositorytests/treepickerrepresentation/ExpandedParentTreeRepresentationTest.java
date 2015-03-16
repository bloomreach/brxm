/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

import javax.ws.rs.core.Response;

import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.treepicker.AbstractTreePickerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.treepicker.DocumentTreePickerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.treepicker.SiteMapTreePickerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.HippoDocumentResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ExpandedParentTreeRepresentationTest extends AbstractTestTreePickerRepresentation {

    @Test
    public void representation_for_siteMapPathInfo_returns_tree_containing_ancestors_with_siblings_and_direct_children() throws Exception {

        // about-us sitemap item as relative contentpath 'common/aboutfolder/about-us' and that path should be expanded

        AbstractTreePickerRepresentation representation =
                createExpandedTreeContentRepresentation("", getRootContentConfigIdentifier(), "about-us");

        assertTrue(representation instanceof DocumentTreePickerRepresentation);
        aboutUsRepresentationAssertions(representation);

    }

    private void aboutUsRepresentationAssertions(final AbstractTreePickerRepresentation representation) {
        assertEquals("unittestproject",representation.getNodeName());

        assertEquals(representation.getPickerType(), "documents");
        assertFalse(representation.isCollapsed());
        assertEquals("expected 'common' and 'News' folder", 2, representation.getItems().size());

        // news folder should not be expanded!
        assertEquals("Folder News' should not be loaded/expanded ", 0, representation.getItems().get(1).getItems().size());
        assertTrue(representation.getItems().get(1).isCollapsed());

        // 'common' should be expanded
        final AbstractTreePickerRepresentation commonFolderRepresentation = representation.getItems().get(0);

        assertEquals(commonFolderRepresentation.getPickerType(), "documents");
        assertEquals("Folder 'common' should loaded/expanded ", 2, commonFolderRepresentation.getItems().size());
        assertFalse(commonFolderRepresentation.isCollapsed());
        assertFalse("Folder 'common' should not be able to be matched in sitemap ",commonFolderRepresentation.isSelectable());

        // ordered folder, home page first
        assertEquals("Home Page", commonFolderRepresentation.getItems().get(0).getDisplayName());
        assertTrue("Documents should never be expanded", commonFolderRepresentation.getItems().get(0).isCollapsed());
        assertFalse("Document 'Home Page' is not the selected one according 'about-us' sitemapPathInfo",
                commonFolderRepresentation.getItems().get(0).isSelected());

        final AbstractTreePickerRepresentation aboutFolderRepresentation = commonFolderRepresentation.getItems().get(1);
        assertEquals("aboutfolder", aboutFolderRepresentation.getDisplayName());
        assertFalse("Folder 'aboutfolder' should not be able to be matched in sitemap ", aboutFolderRepresentation.isSelectable());
        assertFalse("'aboutfolder' should be expanded", aboutFolderRepresentation.isCollapsed());

        final AbstractTreePickerRepresentation aboutDocumentRepresentation = aboutFolderRepresentation.getItems().get(0);

        assertEquals(aboutDocumentRepresentation.getPickerType(), "documents");
        assertEquals("About Us", aboutDocumentRepresentation.getDisplayName());
        assertTrue(aboutDocumentRepresentation.isSelectable());

        assertTrue("Document 'About Us' is *selected* one according 'about-us' sitemapPathInfo",
                aboutDocumentRepresentation.isSelected());
        assertTrue("The selected item should not be expanded", aboutDocumentRepresentation.isCollapsed());
    }

    @Test
    public void representation_for_siteMapPathInfo_that_can_be_matched_to_siteMapItem_default_but_not_to_valid_content_results_in_root_content_presentation() throws Exception {

        // 'matching_to_default_siteMapItem' will match _default_ which does not have a relative content path and as a result,
        // should return 'root content representation' since a wildcard site map item cannot be picked in the sitemap representation either
        AbstractTreePickerRepresentation representation =
                createExpandedTreeContentRepresentation("", getRootContentConfigIdentifier(), "matching_to_default_siteMapItem");

        assertTrue(representation instanceof DocumentTreePickerRepresentation);
        rootContentRepresentationAssertions(representation);
    }

    @Test
    public void representation_for_siteMapPathInfo_that_can_be_matched_in_sitemap_to_explicit_item_without_relative_contentpath_results_in_sitemap_representation() throws Exception {

        AbstractTreePickerRepresentation representation =
                createExpandedTreeContentRepresentation("", getRootContentConfigIdentifier(), "contact");


        assertFalse("contact sitemap pathinfo matches sitemap item that does not have a relative content path, hence a " +
                "fallback to SiteMapTreePickerRepresentation should be done", representation instanceof DocumentTreePickerRepresentation);

        assertTrue(representation instanceof SiteMapTreePickerRepresentation);

        assertEquals(representation.getPickerType(), "pages");

        // TODO HSTTWO-3225
    }

    @Test
    public void representation_for_siteMapPathInfo_that_cannot_be_matched_in_sitemap_results_in_root_content_presentation() throws Exception {

        AbstractTreePickerRepresentation representation =
                createExpandedTreeContentRepresentation("", getRootContentConfigIdentifier(), "path/that/cannot/be/matched");

        rootContentRepresentationAssertions(representation);
        assertTrue(representation instanceof DocumentTreePickerRepresentation);
    }

    @Test
    public void representation_for_siteMapPathInfo_for_non_root_content_request_config_identifier_throws_clientException() throws Exception {

        mockNewRequest(session, "localhost", "about-us", getCommonFolderConfigIdentifier());
        final HippoDocumentResource resource = createHippoDocumentResource();
        final Response response = resource.get("about-us");

        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        final ExtResponseRepresentation representation = (ExtResponseRepresentation) response.getEntity();
        assertEquals(ClientError.INVALID_UUID.toString(), representation.getErrorCode());
    }

    @Test
    public void representation_for_unmatchable_siteMapPathInfo_for_non_root_content_request_config_identifier_throws_clientException() throws Exception {

        mockNewRequest(session, "localhost", "about-us", getCommonFolderConfigIdentifier());
        final HippoDocumentResource resource = createHippoDocumentResource();
        final Response response = resource.get("path/that/cannot/be/matched");

        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
        final ExtResponseRepresentation representation = (ExtResponseRepresentation) response.getEntity();
        assertEquals(ClientError.INVALID_UUID.toString(), representation.getErrorCode());
    }

    @Test
    public void representation_for_siteMapPathInfo_to_selecetd_news_folder() throws Exception {

        // news sitemap item as relative contentpath 'News' and that path should be expanded but 'News' folder
        // representation is still collapsed

        AbstractTreePickerRepresentation representation =
                createExpandedTreeContentRepresentation("", getRootContentConfigIdentifier(), "news");


        assertTrue(representation instanceof DocumentTreePickerRepresentation);

        assertEquals("unittestproject",representation.getNodeName());
        assertFalse(representation.isCollapsed());
        assertEquals("expected 'common' and 'News' folder", 2, representation.getItems().size());

        // 'common' should not be expanded
        final AbstractTreePickerRepresentation commonFolderRepresentation = representation.getItems().get(0);
        assertEquals("common", commonFolderRepresentation.getDisplayName());
        assertTrue(commonFolderRepresentation.isCollapsed());

        // news folder should be selected *but* not expanded!!
        final AbstractTreePickerRepresentation newsRepresentation = representation.getItems().get(1);
        assertEquals("News", newsRepresentation.getDisplayName());
        assertTrue("News folder should be selected", newsRepresentation.isSelected());
        assertTrue("News folder should be collapsed since selected", newsRepresentation.isCollapsed());
        assertEquals("Folder News' should not be expanded ", 0, newsRepresentation.getItems().size());

    }
}
