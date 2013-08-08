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

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.core.jcr.LazySession;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;

/**
 * A JCRSessionStatefulConcurrencyValve makes sure that from this valve on, requests that have a sessionstateful jcr session, in other words,
 * a jcr session tied to their {@link HttpSession}, will be processed synchronized. 
 * 
 * Note that jcr session from a session pool are *never* sessionstateful, and should never be processed synchronized as the session pools internal
 * housekeeping will be blocked as well then. 
 * 
 */
public class JCRSessionStatefulConcurrencyValve extends AbstractBaseOrderableValve {
    

    // default max refresh interval for refresh on lazy session is 5 minutes
    protected long maxRefreshIntervalOnLazySession = 300000;
    
    public void setMaxRefreshIntervalOnLazySession(long maxRefreshIntervalOnLazySession) {
        this.maxRefreshIntervalOnLazySession = maxRefreshIntervalOnLazySession;
    }
    
   
    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HttpServletRequest servletRequest = context.getServletRequest();
        HstRequestContext requestContext = (HstRequestContext) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
        ResolvedMount resolvedMount = requestContext.getResolvedMount();
        boolean subjectBasedSession = resolvedMount.isSubjectBasedSession();
        boolean sessionStateful = resolvedMount.isSessionStateful();
        
        if (!(subjectBasedSession && sessionStateful)) {
            context.invokeNext();
            return;
        }

        // only when subject based session and session stateful, we need synchronized processing
        Session session;
        try {
            session = requestContext.getSession();
        } catch (LoginException e) {
            throw new ContainerException(e);
        } catch (RepositoryException e) {
            throw new ContainerException(e);
        }

        if (session instanceof LazySession) {
            synchronized (session) {
                // by default the jcr session which is sessionStateful is a LazySession instance. We need to refresh it here *in* the
                // synchronized block: After synchronization, it is not allowed that another request refreshes this session!

                LazySession lazySession = (LazySession) session;
                if (maxRefreshIntervalOnLazySession > 0L) {
                    // First check whether the maxRefreshInterval has passed
                    if (System.currentTimeMillis() - lazySession.lastRefreshed() > maxRefreshIntervalOnLazySession) {
                        refreshSession(lazySession);
                    } else {
                        // if not refreshed, check whether there was a repository event that marked the lazySession as 'dirty'
                        long refreshPendingTimeMillis = lazySession.getRefreshPendingAfter();
                        if (refreshPendingTimeMillis > 0L && lazySession.lastRefreshed() < refreshPendingTimeMillis) {
                            refreshSession(lazySession);
                        }
                    }
                } else {
                    // if maxRefreshIntervalOnLazySession <= 0, we always instantly refresh. This is bad for performance
                    refreshSession(lazySession);
                }
                context.invokeNext();
            }
            return;
        } else {
            context.invokeNext();
            return;
        }

    }
    
    /**
     * refreshes a jcr session without keeping changes : use localRefresh as automatic LazySession refreshinh should not
     * propagate to database in clustered setups
     * @param session
     * @throws ContainerException
     */
    private void refreshSession(LazySession session) {
        // HSTTWO-1337: Hippo Repository requires to check isLive() before logout(), refresh(), etc.
        if (session.isLive()) {
            session.localRefresh();
        }
    }
}
