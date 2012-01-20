/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.hst.behavioral.core.component;

import java.lang.reflect.InvocationHandler;
import java.util.Map;
import java.util.TreeMap;

import org.hippoecm.hst.behavioral.BehavioralProfile;
import org.hippoecm.hst.behavioral.util.BehavioralUtils;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactory;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactoryImpl;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.ComponentConfiguration;

/**
 * The BehavioralParameterInfoProxyFactoryImpl extends the HstParameterInfoProxyFactoryImpl by returning a different parameterInfoInvocationHandler : namely
 * one that first checks if there is a prefixed parameter name available before checking the default parameter name. 
 * 
 * For example, if the current persona is 'developer' , this invocation handler first tries to fetch a parametername that is
 * prefixed by 'developer' : If that one is present, that parametervalue is returned. Otherwise, a fallback to the default non prefixed parametername is done
 *
 */
public class BehavioralParameterInfoProxyFactoryImpl extends HstParameterInfoProxyFactoryImpl implements HstParameterInfoProxyFactory {
    
    @Override
    protected InvocationHandler createHstParameterInfoInvocationHandler(
            ComponentConfiguration componentConfig, HstRequest request) {
        
        InvocationHandler parameterInfoInvocationHandler = new ParameterInfoInvocationHandler(componentConfig, request) {

            @Override
            public String getParameterValue(final String parameterName, ComponentConfiguration config, HstRequest req) {
                String prefixedParameterName = parameterName;

                Map<String,String[]> parameterMap = req.getParameterMap();
                String[] personas = parameterMap.get("variant");
                if (personas != null && personas.length > 0) {
                    String persona = personas[0];
                    for (Map.Entry<String,String> entry : getParameterNames(config, parameterName).entrySet()) {
                        String prefix = entry.getKey();
                        if (prefix.equals(persona)) {
                            prefixedParameterName = entry.getValue();
                            break;
                        }
                    }
                } else {
                    BehavioralProfile profile = BehavioralUtils.getBehavioralProfile(req);
                    if (profile != null && profile.hasPersona()) {
                        for (Map.Entry<String,String> entry : getParameterNames(config, parameterName).entrySet()) {
                            String prefix = entry.getKey();
                            if (profile.isPersona(prefix)) {
                                prefixedParameterName = entry.getValue();
                            }
                        }
                    }
                }
                String value = config.getParameter(prefixedParameterName, req.getRequestContext().getResolvedSiteMapItem());
                if (value == null || value.isEmpty()) {
                    // fallback semantics should be the same as fallback to annotated value:
                    // if prefixed value is empty then use the default value
                    value = config.getParameter(parameterName, req.getRequestContext().getResolvedSiteMapItem());
                }
                return value;
            }
        };
        
        return parameterInfoInvocationHandler;
    }

    Map<String, String> getParameterNames(ComponentConfiguration config, String paramName) {
        Map<String, String> names = new TreeMap<String, String>();
        for (String name : config.getParameterNames()) {
            int offset = name.indexOf(HstComponentConfiguration.PARAMETER_PREFIX_NAME_DELIMITER);
            if (offset != -1) {
                if (name.substring(offset + 1).equals(paramName)) {
                    names.put(name.substring(0, offset), name);
                }
            }
        }
        return names;
    }

}
