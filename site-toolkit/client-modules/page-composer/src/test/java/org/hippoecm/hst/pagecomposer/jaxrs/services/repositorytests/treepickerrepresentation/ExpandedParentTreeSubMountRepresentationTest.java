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

import org.hippoecm.hst.pagecomposer.jaxrs.model.treepicker.AbstractTreePickerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.treepicker.DocumentTreePickerRepresentation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExpandedParentTreeSubMountRepresentationTest extends AbstractTestTreePickerRepresentation {

    @Test
    public void representation_for_siteMapPathInfo_for_subMount_in_pathInfo_works() throws Exception {

        // about-us sitemap item as relative contentpath 'common/aboutfolder/about-us' and that path should be expanded
        final String subSiteRootContentConfigIdentifier = session.getNode("/unittestcontent/documents/unittestsubproject").getIdentifier();

        AbstractTreePickerRepresentation representation =
                createExpandedTreeContentRepresentation("/subsite", subSiteRootContentConfigIdentifier, "news");

        assertTrue(representation instanceof DocumentTreePickerRepresentation);
        subSiteNewsRepresentationAssertions(representation);

    }

    private void subSiteNewsRepresentationAssertions(final AbstractTreePickerRepresentation representation) {
        assertEquals("unittestsubproject",representation.getNodeName());
        assertEquals("/unittestcontent/documents/unittestsubproject",representation.getNodePath());

        assertEquals("documents", representation.getPickerType());
        assertFalse(representation.isCollapsed());
        assertEquals("expected 'common' and 'News' folder", 2, representation.getItems().size());


        // 'common' older should not be expanded and not selected
        final AbstractTreePickerRepresentation commonFolderRepresentation = representation.getItems().get(0);
        assertEquals("documents", commonFolderRepresentation.getPickerType());
        assertEquals("Folder 'common' should not be loaded/expanded ", 0, commonFolderRepresentation.getItems().size());
        assertTrue(commonFolderRepresentation.isCollapsed());

        // news folder should not be expanded (though selected)
        assertEquals("Folder News' should not be loaded/expanded ", 0, representation.getItems().get(1).getItems().size());
        assertTrue(representation.getItems().get(1).isCollapsed());
        assertTrue(representation.getItems().get(1).isSelected());

    }
}
