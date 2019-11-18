/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.security.PermitAll;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.Privilege;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.message.Exchange;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.pagecomposer.jaxrs.api.annotation.ChannelAgnostic;
import org.hippoecm.hst.pagecomposer.jaxrs.api.annotation.PrivilegesAllowed;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelPrivilegesAllowedInvokerPreprocessor extends PrivilegesAllowedInvokerPreprocessor {

    private static final Logger log = LoggerFactory.getLogger(ChannelPrivilegesAllowedInvokerPreprocessor.class);

    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    protected Optional<String> isForbiddenOperationContext(final Exchange exchange,
                                                           final Method method, Set<String> privilegesAllowed) {

        if (method.getAnnotation(ChannelAgnostic.class) != null) {
            log.info("Method '{}' is channel agnostic so passes ChannelPrivilegesAllowedInvokerPreprocessor preprocesser",
                    method.getName());
            return Optional.empty();
        }
        
        try {
            final Session session = RequestContextProvider.get().getSession();
            if (!getPageComposerContextService().isRenderingMountSet()) {
                return Optional.of(String.format("Method '%s' is not allowed to be invoked no channel selected yet", method.getName()));
            }

            final Channel previewChannel = getPageComposerContextService().getEditingPreviewChannel();

            // the privilege is currently checked against the *live hstConfigPath* and against the *preview hstConfigPath* if
            // there exists a preview (if no preview present, only the live is checked) :
            // the privileges need to be on both live and preview (if preview present) config path, otherwise not allowed

            final String liveConfigurationPath = getPageComposerContextService().getEditingLiveConfigurationPath();
            final Privilege[] livePrivileges = session.getAccessControlManager().getPrivileges(liveConfigurationPath);

            final Set<String> intersection = getIntersection(privilegesAllowed, livePrivileges);

            if (intersection.isEmpty()) {
                return Optional.of(String.format("Method '%s' is not allowed to be invoked since current user does not have " +
                        "the right privileges", method.getName()));
            }

            final Privilege[] previewPrivileges = session.getAccessControlManager().getPrivileges(previewChannel.getHstConfigPath());

            final Set<String> finalIntersection = getIntersection(intersection, previewPrivileges);

            if (finalIntersection.isEmpty()) {
                return Optional.of(String.format("Method '%s' is not allowed to be invoked since current user does not have " +
                        "the right privileges", method.getName()));
            }
            return Optional.empty();

        } catch (RepositoryException e) {

            throw new IllegalStateException("Exception while trying to find whether user has privilege.", e);
        }
    }


}
