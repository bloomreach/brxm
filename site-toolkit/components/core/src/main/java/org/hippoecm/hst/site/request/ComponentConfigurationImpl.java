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
package org.hippoecm.hst.site.request;

import java.util.Map;

import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;

public class ComponentConfigurationImpl implements ComponentConfiguration{

    public Map<String, Object> unmodifiablePropertiesMap;
    
    public ComponentConfigurationImpl(Map<String, Object> properties) {
        this.unmodifiablePropertiesMap = properties;
    }

    public Object getProperty(String name, ResolvedSiteMapItem hstMatchedSiteMapItem) {
        // TODO IF an expression exists, like ${year}, try to fetch hstMatchedSiteMapItem.getProperty("year") as this should 
        // have been substituted during creation of the MatchingSiteMapItem
        
        return unmodifiablePropertiesMap.get(name);
    }

}
