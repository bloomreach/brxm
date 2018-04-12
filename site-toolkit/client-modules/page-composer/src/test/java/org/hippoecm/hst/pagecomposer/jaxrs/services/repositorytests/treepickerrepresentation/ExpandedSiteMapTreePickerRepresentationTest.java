/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.treepickerrepresentation;

import java.util.ArrayList;
import java.util.List;

import org.hippoecm.hst.pagecomposer.jaxrs.model.treepicker.AbstractTreePickerRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.treepicker.AbstractTreePickerRepresentation.PickerType;
import org.hippoecm.hst.pagecomposer.jaxrs.model.treepicker.AbstractTreePickerRepresentation.Type;
import org.hippoecm.hst.pagecomposer.jaxrs.model.treepicker.SiteMapTreePickerRepresentation;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExpandedSiteMapTreePickerRepresentationTest extends AbstractTestTreePickerRepresentation {

    private static final List<String> LEAFS = new ArrayList<>();
    static {
        LEAFS.add("/hst:hst/hst:configurations/unittestproject/hst:sitemap/about-us");
        LEAFS.add("/hst:hst/hst:configurations/unittestproject/hst:sitemap/handler_browser_redirecttest");
        LEAFS.add("/hst:hst/hst:configurations/unittestproject/hst:sitemap/handler_nooptest");
        LEAFS.add("/hst:hst/hst:configurations/unittestproject/hst:sitemap/handler_sitemapitem_redirecttest");
        LEAFS.add("/hst:hst/hst:configurations/unittestproject/hst:sitemap/home");
        LEAFS.add("/hst:hst/hst:configurations/unittestproject/hst:sitemap/multiplehandler_example");
        LEAFS.add("/hst:hst/hst:configurations/unittestproject/hst:sitemap/news");
        LEAFS.add("/hst:hst/hst:configurations/unittestproject/hst:sitemap/newsCtxOnly");
        LEAFS.add("/hst:hst/hst:configurations/unittestproject/hst:sitemap/newswith_linkrewriting_excluded");
        LEAFS.add("/hst:hst/hst:configurations/unittestproject/hst:sitemap/search");
        LEAFS.add("/hst:hst/hst:configurations/unittestproject/hst:sitemap/uniquelinks");
        LEAFS.add("/hst:hst/hst:configurations/unittestproject/hst:sitemap/alsonews/news2");
    }

    @Test
    public void representation_for_non_existing_siteMapPathInfo_returns_collapsed_tree() throws Exception {
        final AbstractTreePickerRepresentation representation =
                createExpandedSiteMapRepresentation("", getSiteMapIdentifier(), "non-existing");

        assertRootSiteMapTreePickerRepresentation(representation);
        assertSiteMapTreePickerRepresentationItems(representation.getItems());
    }

    @Test
    public void representation_for_siteMapPathInfo_returns_collapsed_tree_with_selected_item() throws Exception {
        final AbstractTreePickerRepresentation representation =
                createExpandedSiteMapRepresentation("", getSiteMapIdentifier(), "about-us");

        assertRootSiteMapTreePickerRepresentation(representation);

        final int aboutUsIndex = 0;
        final List<AbstractTreePickerRepresentation> items = representation.getItems();
        final AbstractTreePickerRepresentation aboutUs = items.get(aboutUsIndex);

        assertEquals("about-us", aboutUs.getPathInfo());
        assertSelectedSiteMapTreePickerRepresentation(aboutUs);

        items.remove(aboutUsIndex);
        assertSiteMapTreePickerRepresentationItems(items);
    }

    @Test
    public void representation_for_siteMapPathInfo_returns_tree_containing_ancestors_with_siblings_and_direct_children() throws Exception {
        final AbstractTreePickerRepresentation representation =
                createExpandedSiteMapRepresentation("", getSiteMapIdentifier(), "alsonews/news2");

        assertRootSiteMapTreePickerRepresentation(representation);

        final int alsoNewsIndex = 2;
        final List<AbstractTreePickerRepresentation> items = representation.getItems();
        final AbstractTreePickerRepresentation alsoNews = items.get(alsoNewsIndex);

        assertEquals("alsonews", alsoNews.getPathInfo());
        assertSiteMapTreePickerRepresentation(alsoNews, false, 1);

        final int news2Index = 0;
        final AbstractTreePickerRepresentation news2 = alsoNews.getItems().get(news2Index);

        assertEquals("alsonews/news2", news2.getPathInfo());
        assertSelectedSiteMapTreePickerRepresentation(news2);

        items.remove(alsoNewsIndex);
        assertSiteMapTreePickerRepresentationItems(items);
    }

    private static void assertRootSiteMapTreePickerRepresentation(final AbstractTreePickerRepresentation representation) {
        assertTrue(representation instanceof SiteMapTreePickerRepresentation);
        assertEquals("unittestproject", representation.getNodeName());
        assertEquals("unittestproject", representation.getDisplayName());
        assertEquals("/hst:hst/hst:configurations/unittestproject/hst:sitemap", representation.getNodePath());
        assertEquals(PickerType.PAGES.getName(), representation.getPickerType());
        assertEquals(Type.PAGE.getName(), representation.getType());
        assertFalse(representation.isCollapsed());
        assertFalse(representation.isSelected());
        assertFalse(representation.isSelectable());
        assertFalse(representation.isLeaf());
        assertTrue(representation.isExpandable());
        assertEquals(18, representation.getItems().size());
    }

    private static void assertSiteMapTreePickerRepresentationItems(final List<AbstractTreePickerRepresentation> items) {
        for (final AbstractTreePickerRepresentation item : items) {
            assertSiteMapTreePickerRepresentation(item);
        }
    }

    private static void assertSiteMapTreePickerRepresentation(final AbstractTreePickerRepresentation item) {
        assertSiteMapTreePickerRepresentation(item, false, 0);
    }

    private static void assertSelectedSiteMapTreePickerRepresentation(final AbstractTreePickerRepresentation item) {
        assertSiteMapTreePickerRepresentation(item, true, 0);
    }

    private static void assertSiteMapTreePickerRepresentation(final AbstractTreePickerRepresentation item,
                                                              final boolean selected, final int numberOfChildItems) {
        assertEquals(PickerType.PAGES.getName(), item.getPickerType());
        assertEquals(Type.PAGE.getName(), item.getType());

        assertEquals(numberOfChildItems, item.getItems().size());
        if (numberOfChildItems > 0) {
            assertFalse(item.isCollapsed());
        } else {
            assertTrue(item.isCollapsed());
        }

        assertTrue(item.isSelectable());
        assertEquals(selected, item.isSelected());

        if (isLeaf(item)) {
            assertTrue(item.isLeaf());
            assertFalse(item.isExpandable());
        } else {
            assertFalse(item.isLeaf());
            assertTrue(item.isExpandable());
        }
    }

    private static boolean isLeaf(final AbstractTreePickerRepresentation item) {
        return LEAFS.contains(item.getNodePath());
    }
}
