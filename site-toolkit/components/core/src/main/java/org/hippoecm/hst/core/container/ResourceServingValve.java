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
package org.hippoecm.hst.core.container;

import javax.servlet.ServletRequest;

import org.hippoecm.hst.core.component.HstComponentException;

public class ResourceServingValve extends AbstractValve {
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {

        if (!context.getServletResponse().isCommitted() && isResourceRequest()) {
            
            HstComponentWindow window = findResourceServingWindow(context.getRootComponentWindow());
            
            if (window != null) {
                ServletRequest servletRequest = context.getServletRequest();
                HstComponentInvoker invoker = getComponentInvoker();
                invoker.invokeBeforeServeResource(context.getServletConfig(), servletRequest, context.getServletResponse());
                invoker.invokeServeResource(context.getServletConfig(), servletRequest, context.getServletResponse());

                if (window.hasComponentExceptions() && log.isWarnEnabled()) {
                    for (HstComponentException hce : window.getComponentExceptions()) {
                        log.warn("Component exceptions found: " + hce.getMessage(), hce);
                    }
                    window.clearComponentExceptions();
                }
            }
        }
        
        // continue
        context.invokeNext();
    }

    private HstComponentWindow findResourceServingWindow(HstComponentWindow rootComponentWindow) {
        // TODO Auto-generated method stub
        return null;
    }
}
