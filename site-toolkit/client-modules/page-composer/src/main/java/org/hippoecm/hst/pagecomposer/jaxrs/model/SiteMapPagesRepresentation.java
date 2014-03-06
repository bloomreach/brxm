/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.util.HstSiteMapUtils;

public class SiteMapPagesRepresentation {

    private String id;
    private List<SiteMapPageRepresentation> pages = new ArrayList<>();

    public SiteMapPagesRepresentation represent(SiteMapRepresentation siteMapRepresentation, final Mount mount) throws IllegalArgumentException {
        id = siteMapRepresentation.getId();
        for (SiteMapItemRepresentation siteMapItemRepresentation : siteMapRepresentation.getChildren()) {
            addPages(siteMapItemRepresentation, null, HstSiteMapUtils.getPath(mount, mount.getHomePage()));
        }
        Collections.sort(pages, new Comparator<SiteMapPageRepresentation>() {
            @Override
            public int compare(final SiteMapPageRepresentation o1, final SiteMapPageRepresentation o2) {
                return o1.getPathInfo().compareTo(o2.getPathInfo());
            }
        });

        // move homepage to first location

        return this;
    }

    private void addPages(final SiteMapItemRepresentation siteMapItemRepresentation,
                          final SiteMapPageRepresentation parent,
                          final String homePagePathInfo) {
        if (!siteMapItemRepresentation.isExplicitElement()) {
            // wildcards are not the pages we want to expose
            return;
        }
        final SiteMapPageRepresentation siteMapPageRepresentation = new SiteMapPageRepresentation();
        pages.add(siteMapPageRepresentation);
        if (parent == null) {
            siteMapPageRepresentation.represent(siteMapItemRepresentation, null, null, homePagePathInfo);
        } else {
            siteMapPageRepresentation.represent(siteMapItemRepresentation, parent.getId(), parent.getPathInfo(), homePagePathInfo);
        }
        for (SiteMapItemRepresentation child : siteMapItemRepresentation.getChildren()) {
            addPages(child, siteMapPageRepresentation, homePagePathInfo);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<SiteMapPageRepresentation> getPages() {
        return pages;
    }

    public void setPages(final List<SiteMapPageRepresentation> pages) {
        this.pages = pages;
    }
}
