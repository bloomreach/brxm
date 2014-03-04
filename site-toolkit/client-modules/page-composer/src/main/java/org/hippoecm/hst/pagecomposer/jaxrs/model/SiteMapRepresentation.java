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
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;

public class SiteMapRepresentation {

    private String id;
    private List<SiteMapItemRepresentation> children = new ArrayList<>();

    public SiteMapRepresentation represent(HstSiteMap siteMap, String previewConfigurationPath) throws IllegalArgumentException {
        if (!(siteMap instanceof CanonicalInfo)) {
            throw new IllegalArgumentException("Expected object of type CanonicalInfo");
        }
        id = ((CanonicalInfo) siteMap).getCanonicalIdentifier();

        for (HstSiteMapItem childItem : siteMap.getSiteMapItems()) {
            SiteMapItemRepresentation child = new SiteMapItemRepresentation();
            child.represent(childItem, previewConfigurationPath);
            children.add(child);
        }
        Collections.sort(children, new Comparator<SiteMapItemRepresentation>() {
            @Override
            public int compare(final SiteMapItemRepresentation o1, final SiteMapItemRepresentation o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        return this;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<SiteMapItemRepresentation> getChildren() {
        return children;
    }

    public void setChildren(final List<SiteMapItemRepresentation> children) {
        this.children = children;
    }
}
