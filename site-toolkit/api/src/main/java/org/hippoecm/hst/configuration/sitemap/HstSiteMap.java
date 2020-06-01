/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.component.HstComponent;

/**
 * A <code>HstSiteMap</code> contains a list of (root) <code>HstSiteMapItem</code> objects which themselves might contain additional 
 * <code>HstSiteMapItem</code> children and so on. As a <code>HstSiteMapItem</code> might have an id, which needs to be unique 
 * within the <code>HstSiteMap</code>, through which a direct lookup of such a <code>HstSiteMapItem</code> is also possible
 * 
 * <p/>
 * NOTE: As {@link HstComponent} instances can access <code>HstSiteMap</code> instances but should not be able to modify them, 
 * implementations must make sure that through the api a <code>HstSiteMap</code> instance cannot be changed. Returned List and Map
 * should be therefor unmodifiable. 
 */
public interface HstSiteMap {
    
    /**
     * Return the {@link HstSite} this <code>HstSiteMap</code> belongs to. 
     * @return the site this <code>HstSiteMap</code> belongs to
     */
    HstSite getSite();
    
    /**
     * The list of <code>SiteMapItem</code>'s that are <code>root</code> items. They represent the first paths of the urls.
     * Implementations should return an unmodifiable list, for example {@link java.util.Collections$UnmodifiableList} to avoid 
     * client code changing configuration
     * @return a List of all root <code>SiteMapItem</code>'s
     */
    List<HstSiteMapItem> getSiteMapItems();

    /**
     * Return the child <code>HstSiteMapItem</code> that has the corresponding <code>value</code> ({@link HstSiteMapItem#getValue()} ) 
     * and <code>null</code> otherwise
     * @param value the value of the child <code>HstSiteMapItem</code> as it would be returned by {@link HstSiteMapItem#getValue()}
     * @return Returns the HstSiteMapItem object corresponding to the unique <code>value</code> and <code>null</code> if no <code>HstSiteMapItem</code>
     * exists with this <code>value</code> in this <code>HstSiteMapItem</code> object. 
     */
    HstSiteMapItem getSiteMapItem(String value);
    
    /**
     * Return the child <code>HstSiteMapItem</code> that has the corresponding <code>id</code> ({@link HstSiteMapItem#getId()} ) 
     * and <code>null</code> otherwise
     * @param id the id of the child <code>HstSiteMapItem</code> as it would be return by {@link HstSiteMapItem#getId()} 
     * @return Returns the HstSiteMapItem object corresponding to the unique <code>id</code> and <code>null</code> if no <code>HstSiteMapItem</code>
     * exists with this <code>refId</code> in this <code>HstSiteMapItem</code> object. 
     */
    HstSiteMapItem getSiteMapItemById(String id);
    
    /**
     * Return the child <code>HstSiteMapItem</code> that has the corresponding <code>refId</code> ({@link HstSiteMapItem#getRefId()} ) 
     * and <code>null</code> otherwise
     * @param refId the refId of the child <code>HstSiteMapItem</code> as it would be return by {@link HstSiteMapItem#getRefId()} 
     * @return Returns the HstSiteMapItem object corresponding to the unique <code>refId</code> and <code>null</code> if no <code>HstSiteMapItem</code>
     * exists with this <code>refId</code> in this <code>HstSiteMapItem</code> object. 
     */
    HstSiteMapItem getSiteMapItemByRefId(String refId);
    
  }
