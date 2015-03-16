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

import org.hippoecm.hst.pagecomposer.jaxrs.model.TreePickerRepresentation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class SiteMapItemTreePickerRepresentationTest extends AbstractTreePickerRepresentationTest {

    @Test
    public void siteMapItem_treePicker_representation() throws Exception {
        TreePickerRepresentation representation = createSiteMapItemRepresentation("", getSiteMapItemIdentifier("about-us"));
        assertEquals("pages", representation.getPickerType());
        assertEquals("page", representation.getType());

        assertEquals("about-us", representation.getPathInfo());
        assertFalse("about sitemap item is not expandable", representation.isExpandable());
        assertTrue("about sitemap item selectable", representation.isSelectable());
        assertEquals(0, representation.getItems().size());
        assertTrue(representation.isLeaf());

    }



    @Test
    public void invisible_siteMapItem_treePicker_representation_results_siteMapItem_nonetheless() throws Exception {
        // if for some reason the UUID points to, say /news/**.html, this is normally not possibly to pick via the sitemap,
        // but we then show the representation nonetheless to at least give webmasters the possibility to select a different one

        TreePickerRepresentation representation = createSiteMapItemRepresentation("", getSiteMapItemIdentifier("news/_any_"));
        assertEquals("news/_any_", representation.getPathInfo());
        assertFalse(representation.isExpandable());
        assertTrue(representation.isCollapsed());
        assertEquals(0, representation.getItems().size());
        assertTrue(representation.isLeaf());
    }
}
