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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.pagecomposer.jaxrs.model.TreePickerRepresentation;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CommonFolderContentHippoDocumentResourceTest extends AbstractHippoDocumentResourceTest {

    @Test
    public void common_folder_representation_assertions() throws Exception {
        // request for the homepage and set the homepage as REQUEST_CONFIG_NODE_IDENTIFIER hence 'true'
        // homepage has pathInfo = ""
        TreePickerRepresentation representation = createRootContentRepresentation("", getCommonFolderRequestConfigIdentifier());

        assertEquals("common", representation.getDisplayName());
        assertEquals(2, representation.getItems().size());

        final TreePickerRepresentation homePageRepresentation = representation.getItems().get(0);
        assertFalse(homePageRepresentation.isFolder());
        assertEquals("live", homePageRepresentation.getState());

        final TreePickerRepresentation aboutFolderRepresentation = representation.getItems().get(1);
        assertTrue(aboutFolderRepresentation.isFolder());

        assertEquals("Children of about folder should not have been loaded yet", 0, aboutFolderRepresentation.getItems().size());
    }

    @Test
    public void assert_homepage_representation_has_pathInfo_home_and_not_empty() throws Exception {

        // request for the homepage and set the homepage as REQUEST_CONFIG_NODE_IDENTIFIER hence 'true'
        // homepage has pathInfo = ""
        TreePickerRepresentation representation = createRootContentRepresentation("", getCommonFolderRequestConfigIdentifier());
        assertEquals(2, representation.getItems().size());

        final TreePickerRepresentation homePageRepresentation = representation.getItems().get(0);
        assertEquals("Home Page", homePageRepresentation.getDisplayName());
        assertEquals("home", homePageRepresentation.getPathInfo());

    }


    @Test
    public void assert_translation_node_is_skipped_but_used_in_displayName() throws Exception {
        try {
            final Node commonFolder = session.getNodeByIdentifier(getCommonFolderRequestConfigIdentifier());
            commonFolder.addMixin(HippoNodeType.NT_TRANSLATED);
            final Node translation = commonFolder.addNode("hippo:translation", HippoNodeType.NT_TRANSLATION);
            translation.setProperty(HippoNodeType.HIPPO_LANGUAGE, "en");
            translation.setProperty(HippoNodeType.HIPPO_MESSAGE, "Common Folder");
            session.save();

            TreePickerRepresentation representation = createRootContentRepresentation("", getCommonFolderRequestConfigIdentifier());
            assertEquals("Common Folder", representation.getDisplayName());
            // translation node does not result in extra child representation
            assertEquals(2, representation.getItems().size());
        } finally {
            final Node commonFolder = session.getNodeByIdentifier(getCommonFolderRequestConfigIdentifier());
            commonFolder.removeMixin(HippoNodeType.NT_TRANSLATED);
            commonFolder.getNode("hippo:translation").remove();
            session.save();
        }
    }

}
