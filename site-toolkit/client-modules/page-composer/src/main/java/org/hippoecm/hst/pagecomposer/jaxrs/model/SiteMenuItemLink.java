/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.pagecomposer.jaxrs.model;

import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;

public class SiteMenuItemLink {

    private final LinkType linkType;
    private final String link;

    public SiteMenuItemLink(final LinkType linkType, final String link) {
        this.linkType = linkType;
        this.link = link;
    }

    public SiteMenuItemLink(HstSiteMenuItemConfiguration item) {
        final String externalLink = item.getExternalLink();
        final String siteMapItemPath = item.getSiteMapItemPath();
        if (externalLink != null && siteMapItemPath == null) {
            link = externalLink;
            linkType = LinkType.EXTERNAL;
        } else if (siteMapItemPath != null && externalLink == null) {
            link = siteMapItemPath;
            linkType = LinkType.SITEMAPITEM;
        } else {
            link = null;
            linkType = LinkType.NONE;
        }
        // TODO (meggermont): what should happen if externalLink and siteMapItem are bot not null?
    }

    public LinkType getLinkType() {
        return linkType;
    }

    public String getLink() {
        return link;
    }
}
