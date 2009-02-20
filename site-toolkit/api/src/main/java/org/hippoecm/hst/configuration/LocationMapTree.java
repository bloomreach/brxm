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
package org.hippoecm.hst.configuration;


/**
 * Interface for inverted HstSiteMapItem tree where the tree is driven by the relativeContentPath's 
 * of all HstSiteMapItem's instead of the tree based on the HstSiteMapItem hierarchy.
 * 
 */
public interface LocationMapTree {

   LocationMapTreeItem getTreeItem(String name);

   /**
    * 
    * @param path
    * @return best matching LocationMapTreeItem and null if no matching one is found
    */
   LocationMapTreeItem find(String path);
   
   String getCanonicalSiteContentPath();
}
