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

import org.hippoecm.hst.container.HstContainerRequestImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageModelApiInitializationValve extends AbstractBaseOrderableValve {

    private static final Logger log = LoggerFactory.getLogger(PageModelApiInitializationValve.class);
    private boolean statelessRequestValidation;

    public void setStatelessRequestValidation(final boolean statelessRequestValidation) {
        this.statelessRequestValidation = statelessRequestValidation;
    }

    @Override
    public void invoke(final ValveContext context) throws ContainerException {
        HttpServletRequest request = context.getServletRequest();
        if (logErrorForCreatedHttpSession(request)) {
            ((HstContainerRequestImpl) request).setStatelessRequestValidation(true);
        }

        context.invokeNext();
    }

    private boolean logErrorForCreatedHttpSession(final HttpServletRequest servletRequest) {
        if (!(servletRequest instanceof HstContainerRequestImpl)) {
            return false;
        }

        HstContainerRequestImpl req = (HstContainerRequestImpl) servletRequest;
        if (req.getSession(false) == null) {
            return statelessRequestValidation;
        }
        log.debug("Request {} already has an http session, no need to report prohibited http session creation in " +
                "Page Model Api request", req);
        return false;

    }
}
