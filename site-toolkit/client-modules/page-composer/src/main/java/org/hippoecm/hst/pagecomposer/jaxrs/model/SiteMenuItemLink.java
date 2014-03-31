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

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemenu.HstSiteMenuItemConfiguration;
import org.hippoecm.hst.util.HstSiteMapUtils;

public class SiteMenuItemLink {

    private final LinkType linkType;
    private final String link;
    private final String pathInfo;
    // mountPath + site map item path
    private final String renderPathInfo;

    public SiteMenuItemLink(final LinkType linkType, final String link,
                            final String pathInfo, final String mountPath) {
        this.linkType = linkType;
        this.link = link;
        this.pathInfo = pathInfo;
        renderPathInfo = mountPath + pathInfo;
    }

    public SiteMenuItemLink(HstSiteMenuItemConfiguration item, Mount mount) {
        final String externalLink = item.getExternalLink();
        final String siteMapItemPath = item.getSiteMapItemPath();
        if (externalLink != null && siteMapItemPath == null) {
            link = externalLink;
            linkType = LinkType.EXTERNAL;
            pathInfo = link;
            renderPathInfo = link;
        } else if (siteMapItemPath != null && externalLink == null) {
            final String path = HstSiteMapUtils.getPath(mount, siteMapItemPath);
            link = siteMapItemPath;
            linkType = LinkType.SITEMAPITEM;
            pathInfo = path;
            if (StringUtils.isEmpty(pathInfo)) {
                renderPathInfo = mount.getMountPath();
            } else {
                if (pathInfo.startsWith("/")) {
                    renderPathInfo = mount.getMountPath() + pathInfo;
                } else {
                    renderPathInfo = mount.getMountPath() + "/" + pathInfo;
                }
            }
        } else {
            link = null;
            linkType = LinkType.NONE;
            pathInfo = null;
            renderPathInfo = null;
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

    public String getRenderPathInfo() {
        return renderPathInfo;
    }
}
