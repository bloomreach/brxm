/*
 *  Copyright 2017-2019 Hippo B.V. (http://www.onehippo.com)
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

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;

import org.apache.cxf.message.Exchange;
import org.hippoecm.hst.jaxrs.cxf.InvokerPreprocessor;
import org.hippoecm.hst.pagecomposer.jaxrs.api.annotation.ChannelAgnostic;
import org.hippoecm.hst.pagecomposer.jaxrs.api.annotation.IgnoreLock;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstConfigLockedCheckInvokerPreprocessor extends AbstractInvokerPreProcessor implements InvokerPreprocessor {

    private static final Logger log = LoggerFactory.getLogger(HstConfigLockedCheckInvokerPreprocessor.class);

    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public Optional<String> isForbiddenOperation(final Exchange exchange) {

        final Method method = getMethod(exchange);

        ChannelAgnostic channelAgnostic = method.getAnnotation(ChannelAgnostic.class);
        if (channelAgnostic != null) {
            log.info("Method '{}' is channel agnostic so passes HstConfigLockedCheckInvokerPreprocessor preprocesser",
                    method.getName());
            return Optional.empty();
        }

        if (!getPageComposerContextService().isRenderingMountSet()) {
            getLogger().debug("No preview channel yet so also not (yet) locked");
            return Optional.empty();
        }

        final Channel previewChannel = getPageComposerContextService().getEditingPreviewChannel();

        if (!previewChannel.isConfigurationLocked()) {
            getLogger().debug("Channel configuration for '{}' not locked.", previewChannel.getId());
            return Optional.empty();
        }


        GET get = method.getAnnotation(GET.class);
        if (get != null) {
            log.debug("GET operation is allowed for locked hst configuration");
            return Optional.empty();
        }

        HEAD head = method.getAnnotation(HEAD.class);
        if (head != null) {
            log.debug("HEAD operation is allowed for locked hst configuration");
            return Optional.empty();
        }

        OPTIONS options = method.getAnnotation(OPTIONS.class);
        if (options != null) {
            log.debug("OPTIONS operation is allowed for locked hst configuration");
            return Optional.empty();
        }

        IgnoreLock ignoreLock = method.getAnnotation(IgnoreLock.class);
        if (ignoreLock != null) {
            log.debug("Method '{}' is allowed for locked hst configuration because it contains the annotation 'IgnoreLock'",
                    method.getName());
            return Optional.empty();
        }

        PUT put = method.getAnnotation(PUT.class);
        if (put != null) {
            log.debug("PUT operation is not allowed for locked hst configuration");
            return Optional.of("PUT operation is forbidden when channel has its configuration locked.");
        }

        POST post = method.getAnnotation(POST.class);
        if (post != null) {
            log.debug("POST operation is not allowed for locked hst configuration");
            return Optional.of("POST operation is forbidden when channel has its configuration locked.");
        }

        DELETE delete = method.getAnnotation(DELETE.class);
        if (delete != null) {
            log.debug("DELETE operation is not allowed for locked hst configuration");
            return Optional.of("DELETE operation is forbidden when channel has its configuration locked.");
        }

        log.info("Expected on method '{}' at least one of the annotations GET, HEAD, PUT, POST or DELETE. Since " +
                        "the method is not annotated with HEAD or GET, the method is not allowed for locked hst configuration nodes",
                method.getName());

        return Optional.of("Method without annotation GET, HEAD, PUT, POST or DELETE is forbidden when channel has its configuration locked.");
    }

}

