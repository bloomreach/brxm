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

import java.io.IOException;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.jcr.LazySession;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * StatefulSessionValve
 * 
 * @version $Id$
 */
public class StatefulSessionValve extends AbstractValve {
    
    
    public static final String SESSION_ATTR_NAME = StatefulSessionValve.class.getName() + ".session";
    
    protected Repository subjectBasedStatefulRepository;
    
    public void setSubjectBasedStatefulRepository(Repository subjectBasedStatefulRepository) {
        this.subjectBasedStatefulRepository = subjectBasedStatefulRepository;
    }
    
    @Override
    public void invoke(ValveContext context) throws ContainerException {
        HttpServletRequest servletRequest = (HttpServletRequest) context.getServletRequest();
        HttpServletResponse servletResponse = (HttpServletResponse) context.getServletResponse();
        HstRequestContext requestContext = (HstRequestContext) servletRequest.getAttribute(ContainerConstants.HST_REQUEST_CONTEXT);
        
        if (requestContext.getResolvedSiteMount().isSessionStateful()) {
            if (requestContext.getSubject() != null) {
                HttpSession httpSession = context.getServletRequest().getSession(false);
                LazySession lazySession = (httpSession != null ? (LazySession) httpSession.getAttribute(SESSION_ATTR_NAME) : (LazySession) null);
                
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
                        } catch (Exception ignore) {
                            ;
                        } finally {
                            lazySession = null;
                        }
                    }
                }
                
                if (lazySession == null) {
                    try {
                        lazySession = (LazySession) subjectBasedStatefulRepository.login();
                        context.getServletRequest().getSession(true).setAttribute(SESSION_ATTR_NAME, lazySession);
                    } catch (Exception e) {
                        throw new ContainerException("Failed to create session based on subject.", e);
                    }
                }
                
                long refreshPendingTimeMillis = lazySession.getRefreshPendingAfter();
                
                if (refreshPendingTimeMillis > 0L && lazySession.lastRefreshed() < refreshPendingTimeMillis) {
                    try {
                        lazySession.refresh(false);
                    } catch (RepositoryException e) {
                        throw new ContainerException("Failed to refresh session.", e);
                    }
                }
                
                ((HstMutableRequestContext) requestContext).setSession(lazySession);
            } else {
                try {
                    servletResponse.sendError(403, "Authentication required.");
                } catch (IOException ioe) {
                    if (log.isDebugEnabled()) {
                        log.warn("Failed to send error code.", ioe);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Failed to send error code. {}", ioe.toString());
                    }
                }
                return;
            }
        }
        
        context.invokeNext();
    }
}
