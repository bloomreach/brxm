/*
 * Copyright 2022-2023 Bloomreach
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
package org.hippoecm.hst.pagecomposer.sitemap;

public class XPageSiteMapShallowItem extends XPageSiteMapBaseItem {

    boolean expandable;

    XPageSiteMapTreeItem parent;

    public XPageSiteMapShallowItem(final XPageSiteMapTreeItem source) {
        setName(source.getName());
        setPageTitle(source.getPageTitle());
        setPathInfo(source.getPathInfo());
        setAbsoluteJcrPath(source.getAbsoluteJcrPath());
        parent = source.getParent();
    }

    public XPageSiteMapShallowItem(final String pathInfo) {
        setPathInfo(pathInfo);
    }

    public boolean isExpandable() {
        return expandable;
    }

    public void setExpandable(final boolean expandable) {
        this.expandable = expandable;
    }

    public XPageSiteMapTreeItem getParent() {
        return parent;
    }
}
