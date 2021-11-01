/*
 * Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Node;

import org.hippoecm.hst.pagecomposer.jaxrs.model.treepicker.AbstractTreePickerRepresentation;
import org.junit.Test;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_NAME;
import static org.hippoecm.repository.api.HippoNodeType.NT_NAMED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CommonFolderContentHippoDocumentResourceTest extends AbstractTestTreePickerRepresentation {

    @Test
    public void common_folder_representation_assertions() throws Exception {
        // request for the homepage and set the homepage as REQUEST_CONFIG_NODE_IDENTIFIER hence 'true'
        // homepage has pathInfo = ""
        AbstractTreePickerRepresentation representation = createRootContentRepresentation("", getCommonFolderConfigIdentifier());

        assertEquals(representation.getPickerType(), "documents");
        assertEquals("common", representation.getDisplayName());
        assertFalse(representation.isCollapsed());
        assertEquals(2, representation.getItems().size());

        final AbstractTreePickerRepresentation homePageRepresentation = representation.getItems().get(0);

        assertEquals("document",homePageRepresentation.getType());

        assertEquals("live", homePageRepresentation.getState());
        assertEquals(homePageRepresentation.getPickerType(), "documents");

        final AbstractTreePickerRepresentation aboutFolderRepresentation = representation.getItems().get(1);

        assertEquals(aboutFolderRepresentation.getPickerType(), "documents");
        assertEquals("folder",aboutFolderRepresentation.getType());
        // only common should be expanded
        assertTrue(aboutFolderRepresentation.isCollapsed());

        assertEquals("Children of about folder should not have been loaded yet", 0, aboutFolderRepresentation.getItems().size());
    }

    @Test
    public void assert_homepage_representation_has_pathInfo_home_and_not_empty() throws Exception {

        // request for the homepage and set the homepage as REQUEST_CONFIG_NODE_IDENTIFIER hence 'true'
        // homepage has pathInfo = ""
        AbstractTreePickerRepresentation representation = createRootContentRepresentation("", getCommonFolderConfigIdentifier());
        assertEquals(2, representation.getItems().size());

        final AbstractTreePickerRepresentation homePageRepresentation = representation.getItems().get(0);
        assertEquals("Home Page", homePageRepresentation.getDisplayName());
        assertEquals("home", homePageRepresentation.getPathInfo());

    }


    @Test
    public void assert_translation_is_used_in_displayName() throws Exception {
        try {
            final Node commonFolder = session.getNodeByIdentifier(getCommonFolderConfigIdentifier());
            commonFolder.addMixin(NT_NAMED);
            commonFolder.setProperty(HIPPO_NAME, "Common Folder");
            session.save();
            Thread.sleep(100);
            AbstractTreePickerRepresentation representation = createRootContentRepresentation("", getCommonFolderConfigIdentifier());
            assertEquals("Common Folder", representation.getDisplayName());
            assertEquals(2, representation.getItems().size());
        } finally {
            final Node commonFolder = session.getNodeByIdentifier(getCommonFolderConfigIdentifier());
            commonFolder.removeMixin(NT_NAMED);
            session.save();
            // give time for jcr events to evict model
            Thread.sleep(100);
        }
    }

}
