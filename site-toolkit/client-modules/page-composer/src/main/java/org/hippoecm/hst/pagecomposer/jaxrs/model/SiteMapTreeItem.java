/*
 *  Copyright 2020-2022 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.apache.commons.lang3.StringUtils;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;

public class SiteMapTreeItem {

    private String id;
    private String name;
    private String pageTitle;
    private String pathInfo;
    // mountPath + sitemap item path = renderPathInfo
    private String renderPathInfo;
    private boolean experiencePage;
    private boolean expandable;
    private boolean structural;

    /**
     * <p>
     * We need a sorted map based in the pathInfo elements which are the keys
     * </p>
     */
    private final SortedMap<String, SiteMapTreeItem> children = new TreeMap<>();

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
                root.id = "/";
                root.name = page.getName();
                root.pageTitle = page.getPageTitle();
                root.experiencePage = page.isExperiencePage();
                root.expandable = page.isExpandable();
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
            int start = page.getPathInfo().startsWith("/") ? 1 : 0;
            for (int i = start; i < total; i++) {
                final String itemId = elements[i];
                next = current.children.get(itemId);
                if (next == null) {
                    // if last element, create SiteMapTreeItem for 'page', if not last element, create a
                    // structural place holder SiteMapTreeItem and continue
                    if (i == total - 1) {
                        // last element
                        current.children.put(itemId, new SiteMapTreeItem(itemId, page));
                        current.expandable = true;
                    } else {
                        // structural element
                        SiteMapTreeItem siteMapTreeItem = new SiteMapTreeItem(itemId);
                        current.children.put(itemId, siteMapTreeItem);
                        current.expandable = true;
                        current = siteMapTreeItem;
                    }
                } else {
                    if (i == total - 1) {
                        // last element
                        // this item might have been added earlier as structural, now set the pathInfo, title, etc
                        next.id = itemId;
                        next.name = page.getName();
                        next.pageTitle = page.getPageTitle();
                        next.pathInfo = page.getPathInfo();
                        next.renderPathInfo = page.getRenderPathInfo();
                        next.experiencePage = page.isExperiencePage();
                        next.expandable = page.isExpandable();
                    } else {
                        current = next;
                    }
                }
            }
        });

        return root;
    }

    public static void mergeFieldsFromTo(SiteMapTreeItem source, SiteMapTreeItem target) {
        if (!isEmpty(source.name)) {
            target.name = source.name;
        }
        if (!isEmpty(source.pageTitle)) {
            target.pageTitle = source.pageTitle;
        }
        if (!isEmpty(source.pathInfo)) {
            target.pathInfo = source.pathInfo;
        }
        if (!isEmpty(source.renderPathInfo)) {
            target.renderPathInfo = source.renderPathInfo;
        }
    }

    // structural item only, without pathInfo meaning not a clickable sitemap item
    private SiteMapTreeItem(final String name) {
        this.id = name;
        this.name = name;
    }

    public SiteMapTreeItem(final SiteMapPageRepresentation siteMapPageRepresentation) {
        this(getIdFromPathInfo(siteMapPageRepresentation.getPathInfo()), siteMapPageRepresentation);
    }

    public SiteMapTreeItem(final String id, final SiteMapPageRepresentation siteMapPageRepresentation) {
        this.id = id;
        this.name = siteMapPageRepresentation.getName();
        this.pageTitle = siteMapPageRepresentation.getPageTitle();
        this.pathInfo = siteMapPageRepresentation.getPathInfo();
        this.renderPathInfo = siteMapPageRepresentation.getRenderPathInfo();
        this.experiencePage = siteMapPageRepresentation.isExperiencePage();
        this.expandable = siteMapPageRepresentation.isExpandable();
        this.structural = siteMapPageRepresentation.isStructural();
    }

    public SiteMapTreeItem(final String id, final String name, final String pageTitle, final String pathInfo,
                           final String renderPathInfo, final boolean experiencePage, final boolean expandable) {
        this.id = id;
        this.name = name;
        this.pageTitle = pageTitle;
        this.pathInfo = pathInfo;
        this.renderPathInfo = renderPathInfo;
        this.experiencePage = experiencePage;
        this.expandable = expandable;
    }

    private static String getIdFromPathInfo(final String pathInfo) {
        if (StringUtils.isEmpty(pathInfo)) {
            return "/";
        }
        if (pathInfo.contains("/")) {
            return StringUtils.substringAfterLast(pathInfo, "/");
        }
        return pathInfo;
    }

    public String getId() {
        return id;
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

    public boolean isExpandable() {
        return expandable;
    }

    @JsonIgnore
    public boolean isStructural() {
        return structural;
    }

    public Collection<SiteMapTreeItem> getChildren() {
        return children.values();
    }

    public SiteMapTreeItem getChild(final String id) {
        return children.get(id);
    }

    public void addOrReplaceChild(SiteMapTreeItem child) {
        children.put(child.getId(), child);
    }

    public void removeChildren() {
        children.clear();
    }

    public void setExpandable(final boolean expandable) {
        this.expandable = expandable;
    }

    /**
     * <p>
     *     A copy (new instance) of this {@link SiteMapTreeItem} only with descendants below its direct children removed
     * </p>>
     */
    public SiteMapTreeItem shallowClone() {

        final SiteMapTreeItem shallowClone = new SiteMapTreeItem(id, name, pageTitle, pathInfo, renderPathInfo, experiencePage, expandable);
        getChildren().forEach(
                child -> shallowClone.addOrReplaceChild(new SiteMapTreeItem(child.id, child.name, child.pageTitle, child.pathInfo,
                        child.renderPathInfo, child.experiencePage, child.expandable))
        );
        return shallowClone;
    }
}
