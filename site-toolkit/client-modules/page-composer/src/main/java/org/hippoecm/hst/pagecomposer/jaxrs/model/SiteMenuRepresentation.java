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
import java.util.List;

import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;

public class SiteMenuRepresentation {

    private String id;
    private String name;
    private long lastModifiedTimestamp;
    private List<SiteMenuItemRepresentation> children = new ArrayList<>();

    public SiteMenuRepresentation() {
        super();
    }

    public SiteMenuRepresentation(HstSiteMenuConfiguration siteMenuConfiguration) throws IllegalArgumentException {
        if (!(siteMenuConfiguration instanceof CanonicalInfo)) {
            throw new IllegalArgumentException("Expected object of type CanonicalInfo");
        }
        id = ((CanonicalInfo) siteMenuConfiguration).getCanonicalIdentifier();
        name = siteMenuConfiguration.getName();

        // TODO last modified timestamp / etc etc
        lastModifiedTimestamp = Calendar.getInstance().getTimeInMillis();

        for (HstSiteMenuItemConfiguration item : siteMenuConfiguration.getSiteMenuConfigurationItems()) {
            children.add(new SiteMenuItemRepresentation(item));
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getLastModifiedTimestamp() {
        return lastModifiedTimestamp;
    }

    public void setLastModifiedTimestamp(final long lastModifiedTimestamp) {
        this.lastModifiedTimestamp = lastModifiedTimestamp;
    }

    public List<SiteMenuItemRepresentation> getChildren() {
        return children;
    }

    public void setChildren(final List<SiteMenuItemRepresentation> children) {
        this.children = children;
    }
}
