/*
 * Copyright 2014-2014 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend;

import static org.hippoecm.frontend.util.WebApplicationHelper.HIPPO_AUTO_LOGIN_COOKIE_BASE_NAME;

import org.apache.wicket.core.request.handler.PageProvider;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler;
import org.apache.wicket.core.request.handler.RenderPageRequestHandler.RedirectPolicy;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.cycle.AbstractRequestCycleListener;
import org.apache.wicket.request.cycle.IRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.util.lang.Exceptions;
import org.hippoecm.frontend.session.InvalidSessionException;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.util.WebApplicationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default {@link IRequestCycleListener} implementation to handle {@link RepositoryRuntimeException}s.
 * If a {@link RepositoryRuntimeException} occurs, then this listener redirects to either home page
 * or {@link NoRepositoryAvailablePage} depending on the exception type.
 */
public class RepositoryRuntimeExceptionHandlingRequestCycleListener extends AbstractRequestCycleListener {

    private static Logger log = LoggerFactory.getLogger(RepositoryRuntimeExceptionHandlingRequestCycleListener.class);

    @Override
    public IRequestHandler onException(RequestCycle cycle, Exception ex) {
        IRequestHandler handler = null;
        RepositoryRuntimeException rrEx = Exceptions.findCause(ex, RepositoryRuntimeException.class);

        if (rrEx != null) {
            handler = createRequestHandler(rrEx);

            if (handler != null) {
                clearStates();
            }
        }

        return handler;
    }

    /**
     * Creates IRequestHandler based on the given <code>RepositoryRuntimeException</code>.
     * @param rrEx RepositoryRuntimeException
     * @return
     */
    protected IRequestHandler createRequestHandler(final RepositoryRuntimeException rrEx) {
        IRequestHandler handler = null;

        if (rrEx instanceof InvalidSessionException) {
            log.error("Creating RequestHandler for InvalidSessionException: {}", rrEx, rrEx);
            handler = new RenderPageRequestHandler(new PageProvider(WebApplication.get().getHomePage()), RedirectPolicy.AUTO_REDIRECT); 
        } else if (rrEx instanceof RepositoryUnavailableException) {
            log.error("Creating RequestHandler for RepositoryUnavailableException: {}", rrEx, rrEx);
            handler = new RenderPageRequestHandler(new PageProvider(NoRepositoryAvailablePage.class), RedirectPolicy.AUTO_REDIRECT);
        }

        return handler;
    }

    /**
     * Clear any user states other than user session.
     */
    protected void clearStates() {
        // Remove the Hippo Auto Login cookie
        WebApplicationHelper.clearCookie(WebApplicationHelper.getFullyQualifiedCookieName(HIPPO_AUTO_LOGIN_COOKIE_BASE_NAME));

        UserSession userSession = UserSession.get();

        if (userSession != null) {
            // Clears HTTP session states
            userSession.invalidate();
        }
    }
}
