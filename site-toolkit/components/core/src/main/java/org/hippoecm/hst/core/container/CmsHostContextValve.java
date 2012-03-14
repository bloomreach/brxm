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


/**
 * CmsHostContextValve sets an attribute on the request that indicates it is a request from a CMS host context
 */
public class CmsHostContextValve extends AbstractValve {

    @Override
    public void invoke(ValveContext context) throws ContainerException {

        context.getServletRequest().setAttribute(ContainerConstants.CMS_HOST_CONTEXT, Boolean.TRUE);
        context.getServletRequest().setAttribute(ContainerConstants.REQUEST_COMES_FROM_CMS, Boolean.TRUE);
        context.invokeNext();

    }
}

    


