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

import javax.jcr.Repository;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.jcr.LazySession;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;

/**
 * SubjectBasedSessionValveImpl
 */
public class SubjectBasedSessionValve extends AbstractBaseOrderableValve {
    
    public static final String SUBJECT_BASED_SESSION_ATTR_NAME = SubjectBasedSessionValve.class.getName() + ".session";
    
    protected Repository subjectBasedRepository;
    
    public void setSubjectBasedRepository(Repository subjectBasedRepository) {
        this.subjectBasedRepository = subjectBasedRepository;
    }
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HttpServletRequest servletRequest = (HttpServletRequest) context.getServletRequest();
        HstRequestContext requestContext = (HstRequestContext) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
        ResolvedMount resolvedMount = requestContext.getResolvedMount();
        boolean subjectBasedSession = resolvedMount.isSubjectBasedSession();
        boolean sessionStateful = resolvedMount.isSessionStateful();
        
        if (subjectBasedSession) {
            if (requestContext.getSubject() == null) {
                log.debug("Subject based session cannot be set because no subject is found.");
            } else {
                // we could include the session userId in the cachekey, but instead we mark the
                // request as uncacheable for subjectbased request rendering
                markRequestUncacheable(context);
                setSubjectSession(context, requestContext, sessionStateful);
            }
        }
        context.invokeNext();
    }

    private void markRequestUncacheable(final ValveContext context) {
        context.getPageCacheContext().markUncacheable("Page response marked as uncacheable " +
                "because subjectBasedSession request rendering.");
    }

    protected void setSubjectSession(ValveContext valveContext, HstRequestContext requestContext, boolean sessionStateful) throws ContainerException {
        LazySession lazySession = null;
        
        if (sessionStateful) {
            HttpSession httpSession = valveContext.getServletRequest().getSession(false);
            lazySession = (httpSession != null ? (LazySession) httpSession.getAttribute(SUBJECT_BASED_SESSION_ATTR_NAME) : (LazySession) null);
            
            if (lazySession != null) {
                boolean isLive = false;
                
                try {
                    isLive = lazySession.isLive();
                } catch (Exception e) {
                    log.error("Error during checking lazy session", e);
                }
                
                if (!isLive) {
                    try {
                        lazySession.logout();
                    } catch (Exception e) {
                        log.warn("Exception logging out lazySession", e);
                    } finally {
                        lazySession = null;
                    }
                }
            }
        } else {
            lazySession = (LazySession) requestContext.getAttribute(SUBJECT_BASED_SESSION_ATTR_NAME);
        }
        
        if (lazySession == null) {
            try {
                lazySession = (LazySession) subjectBasedRepository.login();
            } catch (Exception e) {
                throw new ContainerException("Failed to create session based on subject. Cause '"+e.toString()+"'", e);
            }
        }

        if (sessionStateful) {
            valveContext.getServletRequest().getSession(true).setAttribute(SUBJECT_BASED_SESSION_ATTR_NAME, lazySession);
            
        } else {
            requestContext.setAttribute(SUBJECT_BASED_SESSION_ATTR_NAME, lazySession);
        }
        
        ((HstMutableRequestContext) requestContext).setSession(lazySession);
    }
}
