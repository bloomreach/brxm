/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.annotations.Persistable;
import org.hippoecm.hst.core.component.HstComponentMetadata;
import org.hippoecm.hst.core.component.HstRequestImpl;
import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.jcr.LazySession;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for providing a persistable session to {@link HstRequestContext}
 * whenever an operation annotated by {@link Persistable} is inovked.
 * Note that the persistable session was given due to the {@link Persistable} annotated operation call,
 * the persistable session must be removed after the operation invocation.
 */
public class PersistableSessionAroundAdvice {

    private static Logger log = LoggerFactory.getLogger(PersistableSessionAroundAdvice.class);

    private static final String PERSISTABLE_SESSION_ATTR = PersistableSessionAroundAdvice.class.getName() + ".persistableSession";

    public PersistableSessionAroundAdvice() {
    }

    public Object invoke(ProceedingJoinPoint call) throws Throwable {
        HstRequestContext requestContext = RequestContextProvider.get();

        if (requestContext == null) {
            return call.proceed();
        }

        Object [] args = call.getArgs();
        String invokerMethodName = call.getSignature().getName();
        String compMethodName = StringUtils.replaceOnce(invokerMethodName, "invoke", "do");

        if (args.length < 2 || !(args[1] instanceof HstRequestImpl)) {
            return call.proceed();
        }

        HstComponentMetadata compMetadata = ((HstRequestImpl) args[1]).getComponentWindow().getComponentMetadata();

        if (compMetadata == null || !compMetadata.hasMethodAnnotatedBy(Persistable.class.getName(), compMethodName)) {
            return call.proceed();
        }

        Session existingSession = requestContext.getSession(false);

        // if there exists subject based session, do nothing...
        if (existingSession instanceof LazySession) {
            return call.proceed();
        }

        // in order to use the same persistable session for the same request cycle if available,
        // check if theres exists in request context attributes first.
        Session persistableSession = (Session) requestContext.getAttribute(PERSISTABLE_SESSION_ATTR);

        if (persistableSession == null) {
            try {
                Repository repository = HstServices.getComponentManager().getComponent(Repository.class.getName());
                Credentials persistableCredentials = requestContext.getContextCredentialsProvider().getWritableCredentials(requestContext);
                persistableSession = repository.login(persistableCredentials);
                requestContext.setAttribute(PERSISTABLE_SESSION_ATTR, persistableSession);
            } catch (Exception e) {
                log.warn("Failed to get a persistableSession", e);
            }
        }

        if (persistableSession == null) {
            return call.proceed();
        }

        try {
            ((HstMutableRequestContext) requestContext).setSession(persistableSession);
            return call.proceed();
        } finally {
            ((HstMutableRequestContext) requestContext).setSession(existingSession);
        }
    }

}
