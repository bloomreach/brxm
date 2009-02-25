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

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

public class ComponentConfigurationImpl implements ComponentConfiguration{

    public Map<String, Object> unmodifiablePropertiesMap;
    
    public ComponentConfigurationImpl(Map<String, Object> properties) {
        this.unmodifiablePropertiesMap = properties;
    }

    public Object getResolvedProperty(String name, ResolvedSiteMapItem hstResolvedSiteMapItem) {
        // TODO IF an expression exists, like ${year}, try to fetch hstMatchedSiteMapItem.getProperty("year") as this should 
        // have been substituted during creation of the MatchingSiteMapItem
        
        //hstResolvedSiteMapItem.getProperties()
        
        Object o = unmodifiablePropertiesMap.get(name);
        PropertyParser pp = new PropertyParser(hstResolvedSiteMapItem.getProperties());
        
        return pp.getResolvedProperty(o);
    }

    protected class PropertyParser extends PropertyPlaceholderConfigurer {
        
        private Properties properties;
        
        protected PropertyParser(Properties properties){
            super();
            this.properties = properties;
        }
        
        protected Object getResolvedProperty(Object o) {
            if(o == null || properties == null) {
                return o;
            }
            
            if(o instanceof String) {
                // replace possible expressions
                return this.parseStringValue((String)o, properties, new HashSet());
            }
            if(o instanceof String[]) {
                // replace possible expressions in every String
                String[] unparsed = (String[])o;
                String[] parsed = new String[unparsed.length];
                for(int i = 0 ; i < unparsed.length ; i++) {
                    parsed[i] = this.parseStringValue(unparsed[i], properties, new HashSet());
                }
                return parsed;
            }
            return o;
        }
        
    }
}
