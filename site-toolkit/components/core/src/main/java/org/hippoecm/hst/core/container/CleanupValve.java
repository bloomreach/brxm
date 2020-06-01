/*
 *  Copyright 2008-2016 Hippo B.V. (http://www.onehippo.com)
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

import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.ResourceLifecycleManagement;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.jcr.LazySession;
import org.hippoecm.hst.core.jcr.SessionSecurityDelegation;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;

/**
 * CleanupValve
 */
public class CleanupValve extends AbstractBaseOrderableValve {

    protected SessionSecurityDelegation sessionSecurityDelegation;

    /**
     * @deprecated since 4.1.0 resourceLifecycleManagements does not need to be set any more on the CleanupValve
     */
    @Deprecated
    public void setResourceLifecycleManagements(List<ResourceLifecycleManagement> resourceLifecycleManagements) {
        log.info("CleanupValve#setResourceLifecycleManagements has been deprecated. Not 'resourceLifecycleManagements' " +
                "is needed to be set via spring wiring any more");
    }

    public void setSessionSecurityDelegation(SessionSecurityDelegation sessionSecurityDelegation) {
        this.sessionSecurityDelegation = sessionSecurityDelegation;
    }

    @Override
    public void invoke(ValveContext context) throws ContainerException
    {

        HttpServletRequest servletRequest = context.getServletRequest();
        HstRequestContext requestContext = (HstRequestContext) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
        ResolvedMount resolvedMount = requestContext.getResolvedMount();
        boolean subjectBasedSession = resolvedMount.isSubjectBasedSession();
        boolean sessionStateful = resolvedMount.isSessionStateful();

        if (subjectBasedSession) {
            clearSubjectSession(context, requestContext, sessionStateful);
        }

        // ensure Session isn't tried to reuse again (it has been returned to the pool anyway)
        ((HstMutableRequestContext) requestContext).setSession(null);

        if (sessionSecurityDelegation.sessionSecurityDelegationEnabled()) {
            sessionSecurityDelegation.cleanupSessionDelegates(requestContext);
        }

        // continue
        context.invokeNext();
    }
    
    protected void clearSubjectSession(ValveContext valveContext, HstRequestContext requestContext, boolean sessionStateful) throws ContainerException {
        LazySession lazySession = null;
        
        if (sessionStateful) {
            /*
             *  We do not log out or refresh session stateful jcr session: This is done by either:
             *  1) The JCRSessionStatefulConcurrencyValve refreshes the jcr session when needed
             *  2) When the HttpSession container the jcr session is invalidated (unbinded), the LazySession logs itself out in the finally part
             */
        } else {
            lazySession = (LazySession) requestContext.getAttribute(SubjectBasedSessionValve.SUBJECT_BASED_SESSION_ATTR_NAME);
            
            if (lazySession != null) {
                try {
                    if (lazySession.isLive()) {
                        lazySession.logout();
                    }
                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Failed to logout session.", e);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Failed to logout session. " + e);
                    }
                }
                
                requestContext.removeAttribute(SubjectBasedSessionValve.SUBJECT_BASED_SESSION_ATTR_NAME);
            }
        }
    }
}
