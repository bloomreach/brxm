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

import java.util.Map;

import org.hippoecm.hst.core.component.HstComponent;

/**
 * The <code>HstSites</code> is the entry point for a hst2 configuration for multiple HstSite's in a certain scope 
 * (for example live or preview). Through this object you can access some specific <code>HstSite</code> by <code>{@link #getSite(String name)}</code>
 * all all <code>HstSite</code>'s held by this <code>HstSites</code> object.
 * 
 * <p/>
 * NOTE: As {@link HstComponent} instances can access <code>HstSites</code> instances but should not be able to modify them, 
 * implementations must make sure that through the api a <code>HstSites</code> instance cannot be changed. Returned List and Map
 * should be therefor unmodifiable. 
 */
public interface HstSites {
    
    /**
     * @return Returns the absolute sitesContentPath for this <code>HstSites</code> object
     */
    String getSitesContentPath();

    /**
     * Implementations should return an unmodifiable map, for example {@link java.util.Collections$UnmodifiableMap} to avoid 
     * client code changing configuration
     * @return a map containing all the <code>HstSite</code>'s belonging to this <code>HstSites</code> object
     */
    Map<String, HstSite> getSites();

    /**
     * @return Returns the HstSite object corresponding to the unique <code>name</code> within this <code>HstSites</code> and <code>null</code> if no <code>HstSite</code>
     * exists with this <code>name</code> in this <code>HstSites</code> object
     */
    HstSite getSite(String name);
}