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

import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.Valve;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;

public class ExampleComponentWindowFilteringValve  implements Valve {

    @Override
    public void initialize() throws ContainerException {
    }

    @Override
    public void destroy() {
    }


    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HstMutableRequestContext requestContext = (HstMutableRequestContext) context.getRequestContext();

        requestContext.addComponentWindowCreationFilters(new ExampleComponentWindowFilter());
        context.invokeNext();
    }

}
