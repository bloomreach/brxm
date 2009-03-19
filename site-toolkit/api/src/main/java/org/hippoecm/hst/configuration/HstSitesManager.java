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
 * The manager for HstSites in a certain scope. Typically, there will be a seperate HstSitesManager for for example 
 * a live and a preview environment (scope is live or preview). There can be any number of scopes. The number of scopes
 * is the number of HstSitesManagers.
 *
 */
public interface HstSitesManager {

    /**
     * Sets the sitesContentPath to the specified value. The sitesContentPath is the entry point for a HstSitesManager where it can find
     * all its sites which it manages. The  sitesContentPath is relative to the root node.
     * Typically the sitesContentPath will be for example preview or live, though, you can have any path where your HstSitesManager will
     * look for its sites it has to manage. 
     * 
     * @param sitesContentPath
     */
    void setSitesContentPath(String sitesContentPath);
    
    /**
     * Returns sitesContentPath relative to the root for this <code>HstSitesManager</code>
     * @return the sitesContentPath for this <code>HstSitesManager</code>
     */
    String getSitesContentPath();
    
    /**
     * Returns a HstSites object containing all the HstSite's this HstSiteManager is managing. 
     * @return the all the <code>HstSites</code> this HstSitesManager manages
     */
    HstSites getSites();
    
    /**
     * Invalidates this HstSitesManager. Typically this invalidate is called after a received event indicating that for example
     * the backing configuration has been changed.
     */
    void invalidate();
    
}
