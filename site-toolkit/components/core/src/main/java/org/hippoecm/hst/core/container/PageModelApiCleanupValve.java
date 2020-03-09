/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.container.HstContainerRequestImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageModelApiCleanupValve extends AbstractBaseOrderableValve {

    final static Logger log = LoggerFactory.getLogger(PageModelApiCleanupValve.class);

    @Override
    public void invoke(final ValveContext context) throws ContainerException {
        final HttpServletRequest request = context.getServletRequest();
        if (!(request instanceof HstContainerRequestImpl)) {
            context.invokeNext();
            return;
        }

        final Exception createSessionStackTrace = ((HstContainerRequestImpl) request).getCreateSessionStackTrace();
        if (createSessionStackTrace != null) {
            if (log.isInfoEnabled()) {
                log.error("Stateless marked request {} should never create an http session but one was created. " +
                        "The http session will be invalidated again if still live to avoid congestion. Stacktrace: ", request, createSessionStackTrace);
            } else {
                log.error("Stateless marked request {} should never create an http session but one was created. " +
                        "The http session will be invalidated again if still live to avoid congestion. Change log level of {} to " +
                        "info to see the stacktrace that created the http session", request, PageModelApiCleanupValve.class);
            }
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
                log.info("Http session invalidated");
            } else {
                log.info("During request created http session was again invalidated");
            }
        }

    }
}
