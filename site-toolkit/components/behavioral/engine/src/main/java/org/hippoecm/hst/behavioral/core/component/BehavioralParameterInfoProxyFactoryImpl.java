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

import org.hippoecm.hst.behavioral.BehavioralProfile;
import org.hippoecm.hst.behavioral.util.BehavioralUtils;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.HstParameterInfoInvocationHandler;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactory;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactoryImpl;
import org.hippoecm.hst.core.component.HstParameterValueConverter;
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

    // TODO THRESHOLD should come from somewhere else
    final static double THRESHOLD = 0.1D;
    
    @Override
    protected HstParameterInfoInvocationHandler createHstParameterInfoInvocationHandler(
            ComponentConfiguration componentConfig, HstRequest request,
            HstParameterValueConverter parameterValueConverter) {
        
        HstParameterInfoInvocationHandler parameterInfoInvocationHandler = new ParameterInfoInvocationHandler(componentConfig, request, parameterValueConverter) {

            @Override
            public String getParameterValue(String parameterName, ComponentConfiguration config, HstRequest req) {
              String parameterValue = null;
              
              BehavioralProfile profile = BehavioralUtils.getBehavioralProfile(req);
              
              if(profile != null && !profile.getPersonaScores().isEmpty() && profile.getPersonaScores().get(0).getScore() > THRESHOLD) {
                  // get highest scoring personaId
                  String prefixedParameterName = profile.getPersonaScores().get(0).getPersonaId() + HstComponentConfiguration.PARAMETER_PREFIX_NAME_DELIMITER +parameterName; 
                  parameterValue = config.getParameter(prefixedParameterName, req.getRequestContext().getResolvedSiteMapItem());
              }
              if(parameterValue == null) {
                  parameterValue = config.getParameter(parameterName, req.getRequestContext().getResolvedSiteMapItem()); 
              }
              return parameterValue;
            }
        };
        
        return parameterInfoInvocationHandler;
    }

    
    
}
