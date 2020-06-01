/*
 * Copyright 2014-2020 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;
import org.hippoecm.hst.util.HstSiteMapUtils;

public class SiteMenuItemLink {

    private final LinkType linkType;
    private final String link;
    private final String pathInfo;
    private final String mountPath;

    public SiteMenuItemLink(final LinkType linkType, final String link,
                            final String pathInfo, final String mountPath) {
        this.linkType = linkType;
        this.link = link;
        this.pathInfo = pathInfo;
        this.mountPath = mountPath;
    }

    public SiteMenuItemLink(HstSiteMenuItemConfiguration item, Mount mount) {
        final String externalLink = item.getExternalLink();
        final String siteMapItemPath = item.getSiteMapItemPath();
        mountPath = mount.getMountPath();
        if (externalLink != null && siteMapItemPath == null) {
            link = externalLink;
            linkType = LinkType.EXTERNAL;
            pathInfo = link;
        } else if (siteMapItemPath != null && externalLink == null) {
            final String path = HstSiteMapUtils.getPath(mount, siteMapItemPath);
            link = siteMapItemPath;
            linkType = LinkType.SITEMAPITEM;
            pathInfo = path;
        } else {
            link = null;
            linkType = LinkType.NONE;
            pathInfo = null;
        }
    }

    public LinkType getLinkType() {
        return linkType;
    }

    public String getLink() {
        return link;
    }

    public String getPathInfo() {
        return pathInfo;
    }

    public String getMountPath() {
        return mountPath;
    }

}
