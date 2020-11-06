/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SiteMapTreeItemTest {

    private SiteMapPagesRepresentation createPages(final String[]... inputs) {
        List<SiteMapPageRepresentation> pages = new ArrayList<>();
        Arrays.stream(inputs).forEach(input ->
        {
            SiteMapPageRepresentation page = new SiteMapPageRepresentation();
            page.setName(input[0]);
            page.setPathInfo(input[1]);
            page.setRenderPathInfo(input[1]);
            page.setExperiencePage(false);
            page.setPageTitle(input[0]);
            pages.add(page);
        });
        SiteMapPagesRepresentation siteMapPagesRepresentation = new SiteMapPagesRepresentation();
        siteMapPagesRepresentation.setPages(pages);
        return siteMapPagesRepresentation;
    }


    @Test
    public void test_transformation_from_pages_to_tree() {

        SiteMapPagesRepresentation pages = createPages(
                new String[]{"root", "/"},
                new String[]{"News", "/news"},
                new String[]{"2019", "/news/2019"},
                new String[]{"2020", "/news/2020"},
                new String[]{"About","/about"},
                new String[]{"Address","/contact/address"},
                new String[]{"Contact","/contact"}
                );

        SiteMapTreeItem siteMapTreeItem = SiteMapTreeItem.transform(pages);

        assertThat(siteMapTreeItem.getPathInfo()).isEqualTo("/");

        LinkedHashMap<String, SiteMapTreeItem> children = siteMapTreeItem.getChildren();
        {
            List<String> childPathInfos = children.values().stream().map(item -> item.getPathInfo()).collect(Collectors.toList());
            assertThat(childPathInfos)
                    .containsExactly("/news", "/about", "/contact");
            List<String> childNames = children.values().stream().map(item -> item.getName()).collect(Collectors.toList());
            assertThat(childNames)
                    .containsExactly("News", "About", "Contact");
        }


        {
            SiteMapTreeItem news = children.get("news");
            LinkedHashMap<String, SiteMapTreeItem> newsChildren = news.getChildren();
            List<String> childPathInfos = newsChildren.values().stream().map(item -> item.getPathInfo()).collect(Collectors.toList());
            assertThat(childPathInfos)
                    .containsExactly("/news/2019", "/news/2020");
        }


        {
            SiteMapTreeItem contact = children.get("contact");
            LinkedHashMap<String, SiteMapTreeItem> contactChildren = contact.getChildren();
            List<String> childPathInfos = contactChildren.values().stream().map(item -> item.getPathInfo()).collect(Collectors.toList());
            assertThat(childPathInfos)
                    .containsExactly("/contact/address");
        }
    }


    @Test
    public void test_transformation_from_pages_to_tree_with_structural_items() {
        SiteMapPagesRepresentation pages = createPages(
                new String[]{"root", "/"},
                new String[]{"03", "/news/2019/12/03"},
                new String[]{"Uk","/contact/address/uk"}
        );

        SiteMapTreeItem siteMapTreeItem = SiteMapTreeItem.transform(pages);

        LinkedHashMap<String, SiteMapTreeItem> children = siteMapTreeItem.getChildren();
        {
            // pathInfo for 'news' and 'contact' should be null since no URL for those
            Collection<SiteMapTreeItem> values = children.values();

            List<String> childNames = values.stream().map(item -> item.getName()).collect(Collectors.toList());
            assertThat(childNames).containsExactly("news", "contact");

            List<String> childPathInfos = values.stream().map(item -> item.getPathInfo()).collect(Collectors.toList());
            assertThat(childPathInfos).containsExactly(null, null);

        }

        {
            LinkedHashMap<String, SiteMapTreeItem> newsChildren = children.get("news").getChildren();
            Collection<SiteMapTreeItem> values = newsChildren.values();
            List<String> childNames = values.stream().map(item -> item.getName()).collect(Collectors.toList());
            assertThat(childNames).containsExactly("2019");

            List<String> childPathInfos = values.stream().map(item -> item.getPathInfo()).collect(Collectors.toList());
            assertThat(childPathInfos).containsExactly(new String[] {null});

            SiteMapTreeItem dayItem = newsChildren.get("2019").getChildren().get("12").getChildren().get("03");
            assertThat(dayItem.getPathInfo()).isEqualTo("/news/2019/12/03");
        }

        assertThat(children.get("contact").getChildren().get("address").getChildren().get("uk").getPathInfo()).isEqualTo("/contact/address/uk");
    }


    @Test
    public void test_transformation_from_pages_to_tree_when_root_page_misses() {
        SiteMapPagesRepresentation pages = createPages(
                new String[]{"News", "/news"},
                new String[]{"2019", "/news/2019"},
                new String[]{"Uk","/contact/address/uk"}
        );
        SiteMapTreeItem siteMapTreeItem = SiteMapTreeItem.transform(pages);

        // no root page present, so pathInfo is null
        assertThat(siteMapTreeItem.getPathInfo()).isEqualTo(null);

        // make sure children are loaded as normal
        List<String> childNames = siteMapTreeItem.getChildren().values().stream().map(item -> item.getName()).collect(Collectors.toList());
        assertThat(childNames).containsExactly("News", "contact");
    }


    @Test
    public void test_transformation_from_pages_to_tree_with_null_pathInfo() {
        SiteMapPagesRepresentation pages = createPages(
                new String[]{"root", "/"},
                new String[]{"News", null}
        );
        SiteMapTreeItem siteMapTreeItem = SiteMapTreeItem.transform(pages);

        assertThat(siteMapTreeItem.getChildren().size())
                .as("Expected that items with 'null' path info are skipped")
                .isEqualTo(0);
    }
}
