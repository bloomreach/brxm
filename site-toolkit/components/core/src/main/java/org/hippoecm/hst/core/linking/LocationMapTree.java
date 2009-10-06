/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.core.linking;

import org.hippoecm.hst.configuration.HstSite;
import org.hippoecm.hst.core.linking.ResolvedLocationMapTreeItem;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;


/**
 * Expert: The <code>LocationMapTree</code> is the container for a tree of <code>LocationMapTreeItem</code>'s that are
 * used for internal linkrewriting. Typically it is created by aggregating all the relative content paths from 
 * all <code>SiteMapItem</code>'s, and create a mapping from this. The 
 * <code>match(String path, HstSite hstSite, boolean representsDocument)</code> tries to return the best matching
 * <code>ResolvedLocationMapTreeItem<code> possible for some absolute <code>path</code>. A path can only be rewritten
 * if it belongs to the scope of the <scope>HstSite</scope>. If the <code>path</code> cannot be matched, <code>null</code>
 * will be returned from the match.
 * 
 */
public interface LocationMapTree {

   /**
    * @param name the name of the locationMapTreeItem
    * @return the locatioMapTreeItem with this <code>name</code> and <code>null</code> if none exists with this name
    */
   LocationMapTreeItem getTreeItem(String name);

   /**
    * Tries to find the best match for the <code>path</code> within this <code>LocationMapTree</code> belonging to <code>HstSite</code>.
    * As it can easily happen that multiple <code>SiteMapItem</code>'s are suitable for to match the <code>path</code>, implementing
    * classes should try to return the 'best' match. Typically, the 'best' match would be a match that resolves to a <code>SiteMapItem</code>
    * containing a relative content path that is the most specific for the current path. When two relative content path match equally well, then
    * if there is a sitemap item that is either a child, self, sibbling, parent and sibblings of parents of the current ResolvedSiteMapItem's backing HstSiteMapItem, then this 
    * sitemap item is preferred. The precedence is first self, then children, then sibblings, then parents and then sibblings of parents
    *
    * @param path the path you want to match
    * @param hstSite the current <code>HstSite</code> a match is tried for
    * @param representsDocument a boolean indicating whether the path to be rewritten is belonging to a document
    * @param resolvedSiteMapItem the current context which is used when two relative content paths match equally well.
    * @return the resolvedLocationMapTreeItem that contains a rewritten path and the hstSiteMapId which is the unique id of the
    * HstSiteMapItem that returned the best match. If no match can be made, <code>null</code> is returned 
    */ 
   ResolvedLocationMapTreeItem match(String path, HstSite hstSite, boolean representsDocument, ResolvedSiteMapItem resolvedSiteMapItem);
   
}
