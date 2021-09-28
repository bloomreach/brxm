/*
 *  Copyright 2021 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.core.internal.HstMutableRequestContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.request.HstRequestContextImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestTypeContextValve extends AbstractBaseOrderableValve {

    private final static Logger log = LoggerFactory.getLogger(RequestTypeContextValve.class);

    private HstRequestContextImpl.HstRequestType hstRequestType;

    public void setType(final HstRequestContextImpl.HstRequestType  hstRequestType) {
        this.hstRequestType = hstRequestType;
    }

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        final HstRequestContext requestContext = context.getRequestContext();

        if (hstRequestType == null) {
            log.warn("RequestTypeContextValve is configured without 'hstRequestType'");
            context.invokeNext();
            return;
        }

        if (!(requestContext instanceof HstMutableRequestContext)) {
            log.warn("Expected requestContext of type HstMutableRequestContext");
            context.invokeNext();
            return;
        }

        log.debug("Setting HST Request type to '{}'", hstRequestType);
        ((HstMutableRequestContext)requestContext).setHstRequestType(hstRequestType);

        context.invokeNext();
    }

}

