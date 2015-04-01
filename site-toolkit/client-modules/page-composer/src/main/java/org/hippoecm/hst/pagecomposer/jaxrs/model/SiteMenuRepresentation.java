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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuConfiguration;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;

public class SiteMenuRepresentation {

    private String id;
    private String name;
    private long lastModifiedTimestamp;

    private String siteContentIdentifier;
    private String siteMapIdentifier;

    private List<SiteMenuItemRepresentation> children = new ArrayList<>();

    public SiteMenuRepresentation() {
        super();
    }

    public SiteMenuRepresentation(final HstSiteMenuConfiguration siteMenuConfiguration, final Mount mount,
                                  final String siteContentIdentifier) throws IllegalArgumentException {
        if (!(siteMenuConfiguration instanceof CanonicalInfo)) {
            throw new IllegalArgumentException("Expected siteMenuConfiguration of type CanonicalInfo but was " +
                    siteMenuConfiguration.getClass().getName());
        }
        id = ((CanonicalInfo) siteMenuConfiguration).getCanonicalIdentifier();
        name = siteMenuConfiguration.getName();
        this.siteContentIdentifier = siteContentIdentifier;

        final HstSiteMap siteMap = mount.getHstSite().getSiteMap();
        if (!(siteMap instanceof CanonicalInfo)) {
            throw new IllegalArgumentException("Expected siteMap of type CanonicalInfo but was " +
                    siteMap.getClass().getName());
        }

        siteMapIdentifier = ((CanonicalInfo)siteMap).getCanonicalIdentifier();

        // TODO last modified timestamp / etc etc
        lastModifiedTimestamp = Calendar.getInstance().getTimeInMillis();

        for (HstSiteMenuItemConfiguration item : siteMenuConfiguration.getSiteMenuConfigurationItems()) {
            children.add(new SiteMenuItemRepresentation(item, mount));
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @XmlAttribute(name = "title")
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

    public String getSiteContentIdentifier() {
        return siteContentIdentifier;
    }

    public void setSiteContentIdentifier(final String siteContentIdentifier) {
        this.siteContentIdentifier = siteContentIdentifier;
    }

    public String getSiteMapIdentifier() {
        return siteMapIdentifier;
    }

    public void setSiteMapIdentifier(final String siteMapIdentifier) {
        this.siteMapIdentifier = siteMapIdentifier;
    }

    @XmlElement(name = "items")
    public List<SiteMenuItemRepresentation> getChildren() {
        return children;
    }

    public void setChildren(final List<SiteMenuItemRepresentation> children) {
        this.children = children;
    }
}
