/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.List;

import org.hippoecm.hst.core.ResourceLifecycleManagement;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;

/**
 * InitializationValve
 * 
 * @version $Id$
 */
public class InitializationValve extends AbstractValve
{
    protected List<ResourceLifecycleManagement> resourceLifecycleManagements;
    
    public void setResourceLifecycleManagements(List<ResourceLifecycleManagement> resourceLifecycleManagements) {
        this.resourceLifecycleManagements = resourceLifecycleManagements;
    }
    
    @Override
    public void invoke(ValveContext context) throws ContainerException
    {
        HstMutableRequestContext requestContext = (HstMutableRequestContext)context.getRequestContext();
        
        requestContext.setServletRequest(context.getServletRequest());
        requestContext.setServletResponse(context.getServletResponse());
        
        if (requestContext.getURLFactory() == null) {
            requestContext.setURLFactory(getUrlFactory());
        }
        
        if (requestContext.getHstLinkCreator() == null) {
            requestContext.setLinkCreator(getLinkCreator());
        }
        
        if (requestContext.getSiteMapMatcher() == null) {
            requestContext.setSiteMapMatcher(getSiteMapMatcher());
        }
        
        if (requestContext.getHstQueryManagerFactory() == null) {
            requestContext.setHstQueryManagerFactory(getHstQueryManagerFactory());
        }
        
        if (this.resourceLifecycleManagements != null) {
            for (ResourceLifecycleManagement resourceLifecycleManagement : this.resourceLifecycleManagements) {
                resourceLifecycleManagement.disposeAllResources();
                resourceLifecycleManagement.setActive(true);
            }
            // because the requestContext can already have a jcr session at this moment which is now disposed  (returned to pool) again, we explicitly 
            // set it to null in the HstMutableRequestContext to be sure it gets a new one when requestContext#getSession is called
            requestContext.setSession(null);
        }
        
        // continue
        context.invokeNext();
    }
}
