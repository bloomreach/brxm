/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.hst.demo.container;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.container.HstComponentWindowCreationFilter;
import org.hippoecm.hst.core.request.HstRequestContext;

public class ExampleComponentWindowCreationFilter implements HstComponentWindowCreationFilter {

    @Override
    public boolean skipComponentWindow(HstRequestContext requestContext, HstComponentConfiguration compConfig)
            throws HstComponentException {
        
        if("true".equals(compConfig.getParameter("ExampleComponentWindowCreationFilter.skip"))) {
            return true;
        }
        
        return false;
    }

}
