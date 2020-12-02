/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.hippoecm.hst.configuration.components.DynamicParameter;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration.Type;
import org.hippoecm.hst.core.request.ComponentConfiguration;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.core.util.PropertyParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ComponentConfigurationImpl
 * 
 * @version $Id$
 */
public class ComponentConfigurationImpl implements ComponentConfiguration {

    private final static Logger log = LoggerFactory.getLogger(ComponentConfigurationImpl.class);
    
    private final HstComponentConfiguration componentConfiguration;
    private final List<String> parameterNames;
    
    public ComponentConfigurationImpl(HstComponentConfiguration compConfig) {
        this.componentConfiguration = compConfig;
        if (componentConfiguration.getParameters() == null) {
            this.parameterNames = Collections.emptyList();
        } else {
            this.parameterNames = Collections.unmodifiableList(new ArrayList<String>(componentConfiguration.getParameters().keySet()));
        }
    }

    public List<String> getParameterNames() {
        return parameterNames;
    }
    
    public Map<String,String> getParameters(ResolvedSiteMapItem hstResolvedSiteMapItem) {
        Map<String,String> parameters = new HashMap<>();
        PropertyParser pp = new PropertyParser(hstResolvedSiteMapItem.getParameters());
        for(Entry<String, String> entry: componentConfiguration.getParameters().entrySet()) {
            String parsedParamValue = (String)pp.resolveProperty(entry.getKey(), entry.getValue());
            parameters.put(entry.getKey(), parsedParamValue);
        }
        componentConfiguration.getDynamicComponentParameters().forEach(param -> {
            if (parameters.containsKey(param.getName())) {
                // already from parameter names/values populated
                return;
            }
            Object defaultValue = param.getDefaultValue();
            if (defaultValue == null) {
                return;
            }
            String parsedParamValue = (String)pp.resolveProperty(param.getName(), String.valueOf(defaultValue));
            parameters.put(param.getName(), parsedParamValue);
        });
        return parameters;
    }
    
   
    public String getParameter(String name, ResolvedSiteMapItem hstResolvedSiteMapItem) {
        String paramValue = componentConfiguration.getParameter(name);
        if (paramValue == null) {
            Optional<DynamicParameter> dynamicComponentParameter = componentConfiguration.getDynamicComponentParameter(name);
            if (!dynamicComponentParameter.isPresent()) {
                return null;
            }
            Object defaultValue = dynamicComponentParameter.get().getDefaultValue();
            if (defaultValue == null) {
                return null;
            }
            paramValue = String.valueOf(defaultValue);
        }
        PropertyParser pp = new PropertyParser(hstResolvedSiteMapItem.getParameters());
        String parsedParamValue = (String)pp.resolveProperty(name, paramValue);
        log.debug("Return value '{}' for property '{}'", parsedParamValue, name);
        return parsedParamValue;
    }
    
    public Map<String,String> getLocalParameters(ResolvedSiteMapItem hstResolvedSiteMapItem) {
        Map<String,String> parameters = new HashMap<String, String>();
        PropertyParser pp = new PropertyParser(hstResolvedSiteMapItem.getParameters());
        for(Entry<String, String> entry: componentConfiguration.getLocalParameters().entrySet()) {
            String parsedParamValue = (String)pp.resolveProperty(entry.getKey(), entry.getValue());
            parameters.put(entry.getKey(), parsedParamValue);
        }
        return parameters;
    }
    
    public String getLocalParameter(String name, ResolvedSiteMapItem hstResolvedSiteMapItem) {
        String paramValue = componentConfiguration.getLocalParameter(name);
        PropertyParser pp = new PropertyParser(hstResolvedSiteMapItem.getParameters());
        String parsedParamValue = (String)pp.resolveProperty(name, paramValue);
        log.debug("Return value '{}' for property '{}'", parsedParamValue, name);
        return parsedParamValue;
    }
    
    public Map<String, String> getRawParameters() {
        return componentConfiguration.getParameters();
    }
    
    public Map<String, String> getRawLocalParameters() {
        return componentConfiguration.getLocalParameters();
    }
    
    public String getRenderPath() {
        return componentConfiguration.getRenderPath();
    }
    
    public String getServeResourcePath() {
        return componentConfiguration.getServeResourcePath();
    }

    public String getCanonicalPath() {
        return componentConfiguration.getCanonicalStoredLocation();
    }

    public String getCanonicalIdentifier() {
        return componentConfiguration.getCanonicalIdentifier();
    }
    
    public String getXType() {
        return componentConfiguration.getXType();
    }

    public String getCType() {
        return componentConfiguration.getCType();
    }

    public Type getComponentType() {
        return componentConfiguration.getComponentType();
    }

    public String getParametersInfoClassName() {
        return componentConfiguration.getParametersInfoClassName();
    }

    public HstComponentConfiguration getComponentConfiguration() {
        return componentConfiguration;
    }

    @Override
    public List<DynamicParameter> getDynamicComponentParameters() {
        return componentConfiguration.getDynamicComponentParameters();
    }

    @Override
    public Optional<DynamicParameter> getDynamicComponentParameter(String name) {
        return componentConfiguration.getDynamicComponentParameter(name);
    }

    @Override
    public String toString() {
        return "ComponentConfigurationImpl{" +
                "componentConfiguration=" + componentConfiguration +
                ", parameterNames=" + parameterNames +
                '}';
    }
}
