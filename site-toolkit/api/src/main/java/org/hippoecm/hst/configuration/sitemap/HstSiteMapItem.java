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
package org.hippoecm.hst.configuration.sitemap;

import java.util.List;
import java.util.Map;
 

public interface HstSiteMapItem {


    public static final String WILDCARD = "_default_";
    public static final String ANY = "_any_";
    
    String getId(); // optional but needs to be unique within the containing HstSiteMap
    String getValue();
    String getPath(); // returns the full path from the item
    boolean isWildCard();
    boolean isAny();
    boolean isVisible();
    boolean isRepositoryBased();
    String getRelativeContentPath();
    String getComponentConfigurationId();
    List<String> getRoles();  
    
    /**
     * This method returns a copied Map of the configured properties, such that if you would change an Object or
     * set a new Object with a different key, does not result in a different Map when you call this method again.
     * 
     * @return Map of all properties as they are configured
     */
    Map<String, Object> getProperties();
    
    List<HstSiteMapItem> getChildren();
    HstSiteMapItem getChild(String value);
    
    String getParameter(String name);
    Map<String, String> getParameters();
    
    /**
     * Returns parent HstSiteMapItem and null when the item does not have a parent
     * @return HstSiteMapItem 
     */
    HstSiteMapItem getParentItem();
    HstSiteMap getHstSiteMap();
}
