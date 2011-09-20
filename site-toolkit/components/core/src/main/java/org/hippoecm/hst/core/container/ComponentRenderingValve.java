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
package org.hippoecm.hst.core.container;

import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * ComponentRenderingValve
 */
public class ComponentRenderingValve extends AbstractValve {

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HstRequestContext requestContext = context.getRequestContext();
        String componentRenderingWindowReferenceNamespace = requestContext.getBaseURL().getComponentRenderingWindowReferenceNamespace();

        if (componentRenderingWindowReferenceNamespace != null) {

            HstComponentWindow window = findComponentWindow(context.getRootComponentWindow(), componentRenderingWindowReferenceNamespace);
            if(window.getComponentInfo().isStandalone()) {
                // set the current window as the root window because the backing componentInfo is standalone
                context.setRootComponentWindow(window);
            } else {
                // the component is not standalone: All HstComponent's should have their doBeforeRender called,
                // but only the renderer/dispatcher of the found window should be invoked
                
                // TODO
            }
        }
        context.invokeNext();
    }
}
