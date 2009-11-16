/*
 *  Copyright 2009 Hippo.
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
    * classes should try to return the 'best' match, unless <code>canonical</code> is true (then regardless the context, the same sitemap item must be returned). 
    * Typically, the 'best' match would be a match that resolves to a <code>SiteMapItem</code> containing a relative content path that is the most specific for the current path. When two relative content path match equally well, then
    * the following steps define the order in which a sitemap item is preferred. 
    * 
    * 1) one of the N sitemap items is the same as the current ctx sitemapitem, this one is taken (SAME) : break;
    * 2) if one of the N sitemap items is a *descendant* sitemap item of the current ctx sitemap item, that one is taken : break;
    * 3) Take the matched sitemap items (List) with the first common (shared) ancestor with the current ctx sitemap item : if List contains 1 item: return item: else continue;
    * 4) If (3) returns multiple matched items, return the ones that are the closest (wrt depth) to the common ancestor: if there is one, return item : else continue;
    * 5) If (4) returns 1 or more items, pick the first (we cannot distinguish better) 
    * 6) If still no best context hit found, we return the first matchingSiteMapItem in the matchingSiteMapItems list, as there they are all equally out of context
    * 
    * If <code>canonical</code> is <code>true</code> we return a <code>ResolvedLocationMapTreeItem</code> containing always the same <code>HstSiteMapItem</code>, regardless
    * the current context. This is useful if you want to add the canonical location of some webpage, which is highly appreciated by search engines, for example:
    * <link rel="canonical" href="http://www.hippoecm.org/news/2009/article.html" />. This becomes increasingly important when making use of faceted navigations where
    * the same document content can be shown on a website in many different contexts. 
    * 
    * @param path the path you want to match
    * @param hstSite the current <code>HstSite</code> a match is tried for
    * @param representsDocument a boolean indicating whether the path to be rewritten is belonging to a document
    * @param resolvedSiteMapItem the current context which is used when two relative content paths match equally well.
    * @param canonical if <code>true</code> the linkrewriting is done without respecting the current context. If <code>false</code>, the link is rewritten taking into 
    * account the current context when two or more sitemap items are equally suited.
    * @return the resolvedLocationMapTreeItem that contains a rewritten path and the hstSiteMapId which is the unique id of the
    * HstSiteMapItem that returned the best match. If no match can be made, <code>null</code> is returned 
    */ 
   //ResolvedLocationMapTreeItem match(String path, HstSite hstSite, boolean representsDocument, ResolvedSiteMapItem resolvedSiteMapItem, boolean canonical);
   
}
