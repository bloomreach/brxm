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

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;

public interface HstBaseSiteMapItem {

    
    public String getComponentLocation();
    
    /**
     * The repositorty content location this sitemap item uses. If the path does not start with a "/", the path is taken
     * relative to the content context base. Using relative paths is preferred. 
     * 
     * @return String repository path, relative to the content context base or absolute to the jcr root node when it starts with a "/"
     */
    public String getDataSource();
    
    /**
     * Returns the url part a sitemap item matches on. This is normally its nodename. When a property 'hst:urlname' is found, this 
     * value is used instead of the nodename. This enables for example a different language having a language specific url space
     * @return String urlpart for this sitemap item
     */
    public String getUrlPartName();
    
    /**
     * Returns the url of the sitemap item, which is the urlPartName of this item + the url of the parent SiteMapItemService 
     */
    public String getUrl();
    
    /**
     * @return whether this sitemap item is repository based
     */
    public boolean isRepositoryBased();
    
    public HstSiteMapItem getParent();

    public HstComponentConfiguration getComponentService();
}
