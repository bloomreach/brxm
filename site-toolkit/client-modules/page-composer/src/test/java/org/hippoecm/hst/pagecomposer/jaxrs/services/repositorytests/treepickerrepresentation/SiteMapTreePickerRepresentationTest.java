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

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.model.TreePickerRepresentation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SiteMapTreePickerRepresentationTest extends AbstractTreePickerRepresentationTest {

    @Test
    public void siteMap_treePicker_representation() throws Exception {

        TreePickerRepresentation representation = createSiteMapRepresentation("", getSiteMapIdentifier());

        assertEquals("pages", representation.getPickerType());
        assertEquals("page", representation.getType());
        assertTrue("Root sitemap is expandable", representation.isExpandable());
        assertFalse("Root sitemap is expanded", representation.isCollapsed());
        assertFalse("Root sitemap is not selectable", representation.isSelectable());
        assertFalse("Root sitemap is never selected", representation.isSelected());

        assertNull("Root sitemap has path info null", representation.getPathInfo());

        for (TreePickerRepresentation child : representation.getItems()) {
            assertFalse("Only explicit sitemap items can be picked", child.getPathInfo().contains("_default_"));
            assertFalse("Only explicit sitemap items can be picked", child.getPathInfo().contains("_any_"));
            assertFalse("Page not found item cannot be picked", child.getPathInfo().contains("pagenotfound"));
            assertEquals("Tree must be lazily loaded", 0, child.getItems().size());

            assertTrue("Every sitemap item is collapsed", child.isCollapsed());
            assertFalse(child.isSelected());
            assertTrue("Every explicit sitemap item is selectable", child.isSelectable());

            if (child.getPathInfo().equals("about-us")) {
                // about-us has no children
                assertFalse(child.isExpandable());
            }

            if (child.getPathInfo().equals("news")) {
                // news has only wildcard childreb
                assertFalse(child.isExpandable());
            }

            if (child.getPathInfo().equals("contact")) {
                // contact has thankyou child
                assertTrue(child.isExpandable());
            }

        }

    }

    @Test
    public void siteMap_treePicker_child_representations_are_sorted_on_displayName() throws Exception {
        TreePickerRepresentation representation = createSiteMapRepresentation("", getSiteMapIdentifier());
        TreePickerRepresentation prev = null;
        for (TreePickerRepresentation child : representation.getItems()) {
            if (prev != null) {
                assertTrue(prev.getDisplayName().compareTo(child.getDisplayName()) <= 0);
            }
            prev = child;
        }
    }

    @Test
    public void siteMapItem_marked_as_hidden_in_channel_mngr_not_part_of_representation() throws Exception {
        // mark homepage to be hidden from pages in channel manager
        final Node home = session.getNode("/hst:hst/hst:configurations/unittestproject/hst:sitemap/home");
        home.setProperty(HstNodeTypes.SITEMAPITEM_PROPERTY_HIDDEN_IN_CHANNEL_MANAGER, true);
        session.save();

        TreePickerRepresentation representation = createSiteMapRepresentation("", getSiteMapIdentifier());
        for (TreePickerRepresentation child : representation.getItems()) {
            assertFalse("'home' should be skipped", "home".equals(child.getPathInfo()));
        }
    }

}