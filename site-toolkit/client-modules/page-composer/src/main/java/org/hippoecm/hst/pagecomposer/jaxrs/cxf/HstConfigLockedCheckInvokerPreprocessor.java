/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.cxf;


import java.lang.reflect.Method;

import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.MessageContentsList;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.jaxrs.cxf.InvokerPreprocessor;
import org.hippoecm.hst.pagecomposer.jaxrs.api.annotation.IgnoreLock;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ExtResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstConfigLockedCheckInvokerPreprocessor implements InvokerPreprocessor {

    private static final Logger log = LoggerFactory.getLogger(HstConfigLockedCheckInvokerPreprocessor.class);

    private PageComposerContextService pageComposerContextService;

    public void setPageComposerContextService(final PageComposerContextService pageComposerContextService) {
        this.pageComposerContextService = pageComposerContextService;
    }

    @Override
    public Object preprocoess(final Exchange exchange, final Object request) {
        final Channel channel = pageComposerContextService.getEditingPreviewChannel();
        if (channel == null) {
            return null;
        }
        if (!channel.isConfigurationLocked()) {
            log.debug("Channel configuration for '{}' not locked.", channel.getId());
            return null;
        }
        try {
            if (isForbiddenOperation(exchange)) {
                ExtResponseRepresentation entity = new ExtResponseRepresentation();
                entity.setMessage("Method is forbidden when channel has its configuration locked.");
                entity.setSuccess(false);
                entity.setErrorCode(ClientError.FORBIDDEN.name());
                return new MessageContentsList(Response.status(Response.Status.FORBIDDEN).entity(entity).build());
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.warn("Exception happened during InvokerPreprocessor#preprocoess.", e);
            } else {
                log.warn("Exception happened during InvokerPreprocessor#preprocoess : {}", e.toString());
            }
            return new MessageContentsList(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        }
        return null;

    }

    private boolean isForbiddenOperation(final Exchange exchange) {

        final OperationResourceInfo operationResourceInfo = exchange.get(OperationResourceInfo.class);
        final Method method = operationResourceInfo.getMethodToInvoke();

        GET get = method.getAnnotation(GET.class);
        if (get != null) {
            log.debug("GET operation is allowed for locked hst configuration");
            return false;
        }

        HEAD head = method.getAnnotation(HEAD.class);
        if (head != null) {
            log.debug("HEAD operation is allowed for locked hst configuration");
            return false;
        }

        IgnoreLock ignoreLock = method.getAnnotation(IgnoreLock.class);
        if (ignoreLock != null) {
            log.debug("Method '{}' is allowed for locked hst configuration because it contains the annotation 'IgnoreLock'",
                    method.getName());
            return false;
        }

        PUT put = method.getAnnotation(PUT.class);
        if (put != null) {
            log.debug("PUT operation is allowed for locked hst configuration");
            return true;
        }

        POST post = method.getAnnotation(POST.class);
        if (post != null) {
            log.debug("POST operation is allowed for locked hst configuration");
            return true;
        }

        DELETE delete = method.getAnnotation(DELETE.class);
        if (delete != null) {
            log.debug("DELETE operation is allowed for locked hst configuration");
            return true;
        }

        log.info("Expected on method '{}' at least one of the annotations GET, HEAD, PUT, POST or DELETE. Since " +
                "the method is not annotated with HEAD or GET, the method is not allowed for locked hst configuration nodes",
                method.getName());
        return true;
    }
}
