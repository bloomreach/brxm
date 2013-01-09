/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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

}
