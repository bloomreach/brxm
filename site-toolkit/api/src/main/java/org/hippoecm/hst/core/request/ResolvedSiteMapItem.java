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
package org.hippoecm.hst.core.request;

import java.util.Properties;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.sitemap.HstSiteMap;


public interface ResolvedSiteMapItem {
    
    /**
     * Returns a property from the siteMapItem configuration but possible variables ( $1 or $2 etc ) replaced with the current value
     * 
     * @param name
     * @return property Object 
     */
    Object getProperty(String name);
    
    Properties getProperties();
    
    HstSiteMap getHstSiteMap();
    
    HstComponentConfiguration getHstComponentConfiguration();
}
