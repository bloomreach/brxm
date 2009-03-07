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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentConfigurationImpl implements ComponentConfiguration{

    private final static Logger log = LoggerFactory.getLogger(ComponentConfigurationImpl.class);
    
    public Map<String, Object> unmodifiablePropertiesMap;
    
    public ComponentConfigurationImpl(Map<String, Object> properties) {
        this.unmodifiablePropertiesMap = properties;
    }

    public Object getResolvedProperty(String name, ResolvedSiteMapItem hstResolvedSiteMapItem) {
        
        Object o = unmodifiablePropertiesMap.get(name);
        PropertyParser pp = new PropertyParser(hstResolvedSiteMapItem.getResolvedProperties());
        Object oparsed = pp.resolveProperty(name, o);
        log.debug("Return value '{}' for property '{}'", oparsed, name);
        return oparsed;
    }
}
