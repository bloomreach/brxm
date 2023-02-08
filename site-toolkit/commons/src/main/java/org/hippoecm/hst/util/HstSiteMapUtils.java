/*
 *  Copyright 2008-2023 Bloomreach
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
package org.hippoecm.hst.util;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.util.JcrUtils;

import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.hippoecm.hst.configuration.HstNodeTypes.INDEX;

public class HstSiteMapUtils {

    private HstSiteMapUtils() {

    }

    /**
     * Returns string representation of the sitemap item path
     * @param siteMapItem
     * @return String representation of the path (without leading /)
     */
    public static String getPath(HstSiteMapItem siteMapItem) {
        StringBuilder path = new StringBuilder(siteMapItem.getValue());
        
        while (siteMapItem.getParentItem() != null) {
            siteMapItem = siteMapItem.getParentItem();
            path.insert(0, "/").insert(0, siteMapItem.getValue());
        }
        
        return path.toString();
    }

    /**
     * Returns string representation of the pathInfo of the child sitemap item appended with relPath
     * @param siteMapItem
     * @param relPath
     * @return string representation of the pathInfo of the child sitemap item appended with relPath
     */
    public static String getPath(HstSiteMapItem siteMapItem, String relPath) {
        StringBuilder path = new StringBuilder(siteMapItem.getValue());
        
        while (siteMapItem.getParentItem() != null) {
            siteMapItem = siteMapItem.getParentItem();
            path.insert(0, "/").insert(0, siteMapItem.getValue());
        }
        
        if (relPath == null) {
            return path.toString();
        }
        
        if (relPath.startsWith("/")) {
            path.append(relPath);
        } else {
            path.append("/").append(relPath);
        }
        
        return path.toString();
    }
    
    /**
     * Returns string representation of the sitemap item which is specified by either refId or path.
     * If <CODE>refIdOrPath</CODE> doesn't represent a refId, then it returns the <CODE>refIdOrPath</CODE> as sitemap item path.
     * @param mount
     * @param refIdOrPath
     * @return
     */
    public static String getPath(Mount mount, String refIdOrPath) {
        if (refIdOrPath == null) {
            return null;
        }

        if(mount.getHstSite() == null) {
            return refIdOrPath;
        }

        final HstSiteMap siteMap = mount.getHstSite().getSiteMap();

        if(siteMap == null) {
            return refIdOrPath;
        }
        
        HstSiteMapItem siteMapItemByRefId = siteMap.getSiteMapItemByRefId(refIdOrPath);
        
        if (siteMapItemByRefId != null) {
            return getPath(siteMapItemByRefId);
        }
        
        return refIdOrPath;
    }
    
    /**
     * Returns string representation of the sitemap item which is specified by either refId or path
     * If <CODE>refIdOrRelPath</CODE> doesn't represent a refId, then its return will be equivalent to {@link #getPath(HstSiteMapItem siteMapItem, String relPath)}.
     * @param mount
     * @param siteMapItem
     * @param refIdOrRelPath
     * @return
     */
    public static String getPath(Mount mount, HstSiteMapItem siteMapItem, String refIdOrRelPath) {
        if (refIdOrRelPath != null) {
            HstSiteMapItem siteMapItemByRefId = mount.getHstSite().getSiteMap().getSiteMapItemByRefId(refIdOrRelPath);
            
            if (siteMapItemByRefId != null) {
                return getPath(siteMapItemByRefId);
            }
        }
        
        if (siteMapItem == null) {
            return null;
        }
        
        return getPath(siteMapItem, refIdOrRelPath);
    }

    public static String getPageTitle(final HstLink hstLink, final Node handleNode) throws RepositoryException {
        final String handleDisplayName = JcrUtils.getDisplayNameQuietly(handleNode);
        if (hstLink != null && !hstLink.isNotFound() && hstLink.representsIndex()) {
            final HstSiteMapItem indexItem = hstLink.getHstSiteMapItem().getChild(INDEX);
            if (indexItem != null && substringAfterLast(indexItem.getRelativeContentPath(), "/").equals(handleDisplayName)) {
                // the 'index' document its display name is the same as the path in the relative content path for the
                // sitemap item: this means that the pageTitle is typically something like 'index'. In this case, do not
                // use the display name from the index document but instead fallback to the folder display name as this
                // makes more sense than having eg 'index' as pageTitle for the 'folder' in the SiteMap tree
                return JcrUtils.getDisplayNameQuietly(handleNode.getParent());
            }
        }
        return handleDisplayName;
    }

}
