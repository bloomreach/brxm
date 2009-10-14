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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.util.PropertyParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentConfigurationImpl implements ComponentConfiguration{

    private final static Logger log = LoggerFactory.getLogger(ComponentConfigurationImpl.class);
    
    public HstComponentConfiguration componentConfiguration;
    
    public ComponentConfigurationImpl(HstComponentConfiguration compConfig) {
        this.componentConfiguration = compConfig;
    }

    public Map<String,String> getParameters(ResolvedSiteMapItem hstResolvedSiteMapItem) {
        Map<String,String> parameters = new HashMap<String, String>();
        PropertyParser pp = new PropertyParser(hstResolvedSiteMapItem.getParameters());
        for(Entry<String, String> entry: componentConfiguration.getParameters().entrySet()) {
            String parsedParamValue = (String)pp.resolveProperty(entry.getKey(), entry.getValue());
            parameters.put(entry.getKey(), parsedParamValue);
        }
        return parameters;
    }
    
    public String getParameter(String name, ResolvedSiteMapItem hstResolvedSiteMapItem) {
        String paramValue = componentConfiguration.getParameter(name);
        PropertyParser pp = new PropertyParser(hstResolvedSiteMapItem.getParameters());
        String parsedParamValue = (String)pp.resolveProperty(name, paramValue);
        log.debug("Return value '{}' for property '{}'", parsedParamValue, name);
        return parsedParamValue;
    }
    
    public String getRenderPath() {
        return componentConfiguration.getRenderPath();
    }
    
    public String getServeResourcePath() {
        return componentConfiguration.getServeResourcePath();
    }
    
}
