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

import java.util.List;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.core.ResourceLifecycleManagement;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.jcr.LazySession;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * CleanupValve
 * 
 * @version $Id$
 */
public class CleanupValve extends AbstractValve
{
    protected List<ResourceLifecycleManagement> resourceLifecycleManagements;
    protected boolean refreshLazySession = true;
    protected long maxRefreshIntervalOnLazySession;
    
    public void setResourceLifecycleManagements(List<ResourceLifecycleManagement> resourceLifecycleManagements) {
        this.resourceLifecycleManagements = resourceLifecycleManagements;
    }
    
    public void setRefreshLazySession(boolean refreshLazySession) {
        this.refreshLazySession = refreshLazySession;
    }
    
    public void setMaxRefreshIntervalOnLazySession(long maxRefreshIntervalOnLazySession) {
        this.maxRefreshIntervalOnLazySession = maxRefreshIntervalOnLazySession;
    }
    
    @Override
    public void invoke(ValveContext context) throws ContainerException
    {
        if (this.resourceLifecycleManagements != null) {
            for (ResourceLifecycleManagement resourceLifecycleManagement : this.resourceLifecycleManagements) {
                resourceLifecycleManagement.disposeAllResources();
            }
        }
        
        HttpServletRequest servletRequest = context.getServletRequest();
        
        HttpSession httpSession = servletRequest.getSession(false);
        LazySession lazySession = (httpSession != null ? (LazySession) httpSession.getAttribute(StatefulSessionValve.SESSION_ATTR_NAME) : (LazySession) null);
        
        if (lazySession != null && refreshLazySession) {
            try {
                if (maxRefreshIntervalOnLazySession > 0L) {
                    if (System.currentTimeMillis() - lazySession.lastRefreshed() > maxRefreshIntervalOnLazySession) {
                        lazySession.refresh(false);
                    }
                } else {
                    lazySession.refresh(false);
                }
            } catch (RepositoryException e) {
                log.error("Failed to refresh session.", e);
            }
        }
        
        HstRequestContext requestContext = context.getRequestContext();
        
        if (requestContext != null) {
        	// ensure Session isn't tried to reuse again (it has been returned to the pool anyway)
            ((HstMutableRequestContext) requestContext).setSession(null);
            
            if (servletRequest.getAttribute(ContainerConstants.HST_FORWARD_PATH_INFO) == null) {
                getRequestContextComponent().release(requestContext);
            }
        }
        
        // continue
        context.invokeNext();
    }
}
