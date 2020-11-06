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

import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class SiteMapTreeItem {

    private String name;
    private String pageTitle;
    private String pathInfo;
    // mountPath + sitemap item path = renderPathInfo
    private String renderPathInfo;
    private boolean experiencePage;

    private final LinkedHashMap<String, SiteMapTreeItem> children = new LinkedHashMap<>();

    // no sorting needed: we keep the order of siteMapPagesRepresentation.getPages()
    public static SiteMapTreeItem transform(final SiteMapPagesRepresentation siteMapPagesRepresentation) {
        List<SiteMapPageRepresentation> pages = siteMapPagesRepresentation.getPages();

        final SiteMapTreeItem root = new SiteMapTreeItem("");

        pages.forEach(page -> {

            final String pathInfo = page.getPathInfo();
            if (StringUtils.isEmpty(pathInfo)) {
                return;
            }

            if (pathInfo.equals("/")) {
                // home page
                root.pathInfo = page.getPathInfo();
                root.renderPathInfo = page.getRenderPathInfo();
                root.name = page.getName();
                root.pageTitle = page.getPageTitle();
                root.experiencePage = page.isExperiencePage();
                return;
            }

            // merge the page into the SiteMapTreeItem and create any none existing parents (which might
            // get filled later on with a pathInfo by a SiteMapPageRepresentation and if not such SiteMapPageRepresentation
            // it will be a SiteMapTreeItem without pathInfo or renderPathInfo (which means won't be clickable in the CM)

            String[] elements = page.getPathInfo().split("/");
            SiteMapTreeItem current = root;
            SiteMapTreeItem next;
            int total = elements.length;
            // skip i = 0 since pathInfo always starts with '/' so first element is ""
            for (int i = 1; i < total; i++) {
                next = current.children.get(elements[i]);
                if (next == null) {
                    // if last element, create SiteMapTreeItem for 'page', if not last element, create a
                    // structural place holder SiteMapTreeItem and continue
                    if (i == total - 1 ) {
                        // last element
                        current.children.put(elements[i], new SiteMapTreeItem(page));
                    } else {
                        // structural element
                        SiteMapTreeItem siteMapTreeItem = new SiteMapTreeItem(elements[i]);
                        current.children.put(elements[i], siteMapTreeItem);
                        current = siteMapTreeItem;
                    }
                } else {
                    if (i == total - 1 ) {
                        // last element
                        // this item might have been added earlier as structural, now set the pathInfo, title, etc
                        next.name = page.getName();
                        next.pageTitle = page.getPageTitle();
                        next.pathInfo = page.getPathInfo();
                        next.renderPathInfo = page.getRenderPathInfo();
                        next.experiencePage = page.isExperiencePage();
                    } else {
                        current = next;
                    }
                }
            }
        });

        return root;
    }

    // structural item only, without pathInfo meaning not a clickable sitemap item
    private SiteMapTreeItem(final String name) {
        this.name = name;
    }

    private SiteMapTreeItem(final SiteMapPageRepresentation siteMapPageRepresentation) {
        this.name = siteMapPageRepresentation.getName();
        this.pageTitle = siteMapPageRepresentation.getPageTitle();
        this.pathInfo = siteMapPageRepresentation.getPathInfo();
        this.renderPathInfo = siteMapPageRepresentation.getRenderPathInfo();
        this.experiencePage = siteMapPageRepresentation.isExperiencePage();
    }

    private SiteMapTreeItem addChild(String name) {
        SiteMapTreeItem child = new SiteMapTreeItem(name);
        if (pathInfo.endsWith("/")) {
            children.put(pathInfo + name, child);
        } else {
            children.put(pathInfo + "/" + name, child);
        }
        return child;
    }

    private void addChild(SiteMapTreeItem child) {
        children.put(child.pathInfo, child);
    }

    public String getName() {
        return name;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public String getPathInfo() {
        return pathInfo;
    }

    public String getRenderPathInfo() {
        return renderPathInfo;
    }

    public boolean isExperiencePage() {
        return experiencePage;
    }

    public LinkedHashMap<String, SiteMapTreeItem> getChildren() {
        return children;
    }
}
