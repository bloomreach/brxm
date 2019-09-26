/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.cxf.message.Exchange;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.pagecomposer.jaxrs.api.annotation.PrivilegesAllowed;
import org.onehippo.cms7.services.hst.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrivilegesAllowedInvokerPreprocessor extends AbstractInvokerPreProcessor {


    private static final Logger log = LoggerFactory.getLogger(PrivilegesAllowedInvokerPreprocessor.class);

    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public Optional<String> isForbiddenOperation(final Exchange exchange, final Channel previewChannel) {

        final Method method = getMethod(exchange);
        final PrivilegesAllowed privilegesAllowed = method.getAnnotation(PrivilegesAllowed.class);
        final PermitAll permitAll = method.getAnnotation(PermitAll.class);

        if (privilegesAllowed == null && permitAll == null) {
            final String message = String.format("Method '%s' is not annotated with @PrivilegesAllowed and neither with @PermitAll which " +
                    "is not allowed. Either permit all or specify which privilege is allowed.", method.getName());
            log.error(message);
            return Optional.of(message);
        } else if (privilegesAllowed != null && permitAll != null) {
            final String message = String.format("Method '%s' is annotated with @PrivilegesAllowed AND with @PermitAll which " +
                    "is not allowed. Either permit all or specify which privilege is allowed.", method.getName());
            log.error(message);
            return Optional.of(message);
        } else if (privilegesAllowed == null) {
            log.info("Method '{}' is permitted for all", method.getName());
            return Optional.empty();
        }

        if (previewChannel == null) {
            return Optional.of(String.format("Method '%s' is not allowed to be invoked no channel selected yet", method.getName()));
        }

        // the privilege is currently checked against the *live hstConfigPath* and against the *preview hstConfigPath* if
        // there exists a preview (if no preview present, only the live is checked) :
        // the privileges need to be on both live and preview (if preview present) config path, otherwise not allowed

        try {
            final Session session = RequestContextProvider.get().getSession();

            final String liveConfigurationPath = getPageComposerContextService().getEditingLiveConfigurationPath();
            final Privilege[] livePrivileges = session.getAccessControlManager().getPrivileges(liveConfigurationPath);

            final Set<String> privilegesAllowedSet = Arrays.stream(privilegesAllowed.value()).collect(Collectors.toSet());

            final Set<String> intersection = getIntersection(privilegesAllowedSet, livePrivileges);

            if (intersection.isEmpty()) {
                return Optional.of(String.format("Method '%s' is not allowed to be invoked since current user does not have " +
                        "the right privileges", method.getName()));
            }

            if (previewChannel == null) {
                return Optional.empty();
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

    private Set<String> getIntersection(final Set<String> privilegesAllowed, final Privilege[] privileges) {
        return Arrays.stream(privileges)
                .filter(privilege -> privilegesAllowed.contains(privilege.getName()))
                .map(privilege -> privilege.getName())
                .collect(Collectors.toSet());
    }

}
