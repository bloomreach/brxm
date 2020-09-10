/*
 *  Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Optional;

import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.MessageContentsList;
import org.hippoecm.hst.jaxrs.cxf.InvokerPreprocessor;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ResponseRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.slf4j.Logger;

public abstract class AbstractInvokerPreProcessor implements InvokerPreprocessor {

    private PageComposerContextService pageComposerContextService;

    public void setPageComposerContextService(final PageComposerContextService pageComposerContextService) {
        this.pageComposerContextService = pageComposerContextService;
    }

    public PageComposerContextService getPageComposerContextService() {
        return pageComposerContextService;
    }

    @Override
    public Object preprocoess(final Exchange exchange, final Object request) {

        try {
            final Optional<String> forbiddenOperation = isForbiddenOperation(exchange);
            if (forbiddenOperation.isPresent()) {
                final ResponseRepresentation<Void> entity = ResponseRepresentation.<Void>builder()
                        .setSuccess(false)
                        .setMessage(forbiddenOperation.get())
                        .setErrorCode(ClientError.FORBIDDEN.name())
                        .build();

                return new MessageContentsList(Response.status(Response.Status.FORBIDDEN).entity(entity).build());
            }
        } catch (Exception e) {
            if (getLogger().isDebugEnabled()) {
                getLogger().warn("Exception happened during InvokerPreprocessor#preprocoess.", e);
            } else {
                getLogger().warn("Exception happened during InvokerPreprocessor#preprocoess : {}", e.toString());
            }
            return new MessageContentsList(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        }
        return null;

    }

    public abstract Logger getLogger();

    /**
     * @return if it returns {@link Optional#empty()}, the operation is allowed, and if present, the optional contains the
     * forbidden message
     * @param exchange
     */
    public abstract Optional<String> isForbiddenOperation(final Exchange exchange);


    public Method getMethod(final Exchange exchange) {
        final OperationResourceInfo operationResourceInfo = exchange.get(OperationResourceInfo.class);
        return operationResourceInfo.getMethodToInvoke();
    }

}
