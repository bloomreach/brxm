/*
 *  Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.linking;

import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;

public class ResolvedLocationMapTreeItemImpl implements ResolvedLocationMapTreeItem {

    private static final long serialVersionUID = 1L;

    private final String path;
    private final HstSiteMapItem siteMapItem;
    private final boolean representsDocument;
    
    public ResolvedLocationMapTreeItemImpl(final String path, final HstSiteMapItem siteMapItem, final boolean representsDocument){
        this.path = path;
        // siteMapItem can be null!
        this.siteMapItem = siteMapItem;
        this.representsDocument = representsDocument;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public HstSiteMapItem getSiteMapItem() {
        return siteMapItem;
    }

    @Override
    public boolean representsDocument() {
        return representsDocument;
    }

    @Override
    public String toString() {
        return "ResolvedLocationMapTreeItemImpl{" +
                "path='" + path + '\'' +
                ", siteMapItem=" + siteMapItem +
                ", representsDocument=" + representsDocument +
                '}';
    }
}
