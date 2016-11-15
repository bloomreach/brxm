/*
 *  Copyright 2014-2016 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.internal.CanonicalInfo;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.util.HstSiteMapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.INDEX;

public class SiteMapPagesRepresentation {

    private static final Logger log = LoggerFactory.getLogger(SiteMapPagesRepresentation.class);

    private String id;
    private String host;
    private String mount;
    private List<SiteMapPageRepresentation> pages = new ArrayList<>();

    public SiteMapPagesRepresentation represent(final HstSiteMap siteMap,
                                                final Mount mount,
                                                final String previewConfigurationPath) throws IllegalArgumentException {
        id = ((CanonicalInfo)siteMap).getCanonicalIdentifier();
        host = mount.getVirtualHost().getHostName();
        this.mount = mount.getMountPath();
        final String homePagePathInfo = HstSiteMapUtils.getPath(mount, mount.getHomePage());
        for (HstSiteMapItem child : siteMap.getSiteMapItems()) {
            addPages(child, null, homePagePathInfo, previewConfigurationPath);
        }
        Collections.sort(pages, (o1, o2) -> o1.getPathInfo().compareTo(o2.getPathInfo()));
        // move homepage to first location
        return this;
    }

    private void addPages(final HstSiteMapItem siteMapItem,
                          final SiteMapPageRepresentation parent,
                          final String homePagePathInfo,
                          final String previewConfigurationPath) {
        if (!siteMapItem.isExplicitElement()) {
            // wildcards are not the pages we want to expose
            log.debug("Skip '{}' from page overview because only explicit non-wildcard sitemap items can be shown as 'pages'" +
                    " in the channel manager", siteMapItem);
            return;
        }
        if (INDEX.equals(siteMapItem.getValue())) {
            log.debug("Skip '{}' from page overview because '{}' items do not need to be shown as pages since the " +
                    "parent item shows the same page", siteMapItem, INDEX);
            return;
        }
        if (siteMapItem.isContainerResource() || siteMapItem.isHiddenInChannelManager()) {
            log.debug("Skip '{}' from page overview because represents container resource or is marked " +
                    "explicitly to be hidden in channel manager", siteMapItem);
            return;
        }
        final SiteMapPageRepresentation siteMapPageRepresentation = new SiteMapPageRepresentation();
        pages.add(siteMapPageRepresentation);
        if (parent == null) {
            siteMapPageRepresentation.represent(siteMapItem, null, mount, homePagePathInfo, previewConfigurationPath);
        } else {
            siteMapPageRepresentation.represent(siteMapItem, parent.getId(), mount, homePagePathInfo, previewConfigurationPath);
        }
        for (HstSiteMapItem child : siteMapItem.getChildren()) {
            addPages(child, siteMapPageRepresentation, homePagePathInfo, previewConfigurationPath);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public String getMount() {
        return mount;
    }

    public void setMount(final String mount) {
        this.mount = mount;
    }

    public List<SiteMapPageRepresentation> getPages() {
        return pages;
    }

    public void setPages(final List<SiteMapPageRepresentation> pages) {
        this.pages = pages;
    }
}
