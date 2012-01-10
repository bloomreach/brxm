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
package org.hippoecm.hst.behavioral.core.container;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.behavioral.BehavioralService;
import org.hippoecm.hst.behavioral.core.component.BehavioralParameterInfoProxyFactoryImpl;
import org.hippoecm.hst.behavioral.util.BehavioralUtils;
import org.hippoecm.hst.core.component.HstParameterInfoProxyFactory;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.Valve;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.request.HstRequestContext;


public class BehavioralUpdateValve implements Valve {

    private final static HstParameterInfoProxyFactory PARAMETER_INFO_PROXY_FACTORY =  new BehavioralParameterInfoProxyFactoryImpl();
    
    @Override
    public void destroy() {
    }

    @Override
    public void initialize() throws ContainerException {
    }

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HttpServletRequest servletRequest = context.getServletRequest();
        HttpServletResponse servletResponse = context.getServletResponse();
        HstRequestContext requestContext = context.getRequestContext();
        
        
        if (requestContext.getBaseURL().getActionWindowReferenceNamespace() != null
                || requestContext.getBaseURL().getResourceWindowReferenceNamespace() != null) {
            
            // for action or resource URLs we do not update BehavioralData
            context.invokeNext();
            return;
        }
        
        BehavioralService bs = BehavioralUtils.getBehavioralService();
        bs.updateBehavioralData(servletRequest, servletResponse);

        // now we set a different parameterInfoProxyFactory on the HstRequestContext because we want 
        // to depending on the current persona fetch a prefixed parameter name if it is available
        ((HstMutableRequestContext)context.getRequestContext()).setParameterInfoProxyFactory(PARAMETER_INFO_PROXY_FACTORY);
        
        // continue
        context.invokeNext();
    }

}
