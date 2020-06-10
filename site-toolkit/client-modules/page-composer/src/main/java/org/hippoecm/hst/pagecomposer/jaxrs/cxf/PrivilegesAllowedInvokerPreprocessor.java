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
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.security.PermitAll;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.Privilege;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.message.Exchange;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.pagecomposer.jaxrs.api.annotation.PrivilegesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrivilegesAllowedInvokerPreprocessor extends AbstractInvokerPreProcessor {

    private static final Logger log = LoggerFactory.getLogger(PrivilegesAllowedInvokerPreprocessor.class);
    private boolean enabled = true;

    /**
     * This is a method only meant for integration tests to be able to temporarily switch of the
     * PrivilegesAllowedInvokerPreprocessor. Typically for the use case like asserting that deleting a non existing
     * channel results in a 404, however, this Preprocessor typically already does not allow the delete since for
     * example no 'previewChannel' is present. Therefor this setter is there to disable this PreProcessor
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public Optional<String> isForbiddenOperation(final Exchange exchange) {
        if (!enabled) {
            return Optional.empty();
        }

        final Method method = getMethod(exchange);
        final PrivilegesAllowed privilegesAllowed = method.getAnnotation(PrivilegesAllowed.class);
        final PermitAll permitAll = method.getAnnotation(PermitAll.class);

        if (privilegesAllowed == null && permitAll == null) {
            final String message = String.format("Method '%s' is not annotated with @PrivilegesAllowed and neither with @PermitAll which " +
                    "is not allowed. Either permit all or specify which privilege is allowed.", method.getName());
            getLogger().error(message);
            return Optional.of(message);
        } else if (privilegesAllowed != null && permitAll != null) {
            final String message = String.format("Method '%s' is annotated with @PrivilegesAllowed AND with @PermitAll which " +
                    "is not allowed. Either permit all or specify which privilege is allowed.", method.getName());
            getLogger().error(message);
            return Optional.of(message);
        } else if (privilegesAllowed == null) {
            getLogger().info("Method '{}' is permitted for all", method.getName());
            return Optional.empty();
        }

        try {
            final Session session = RequestContextProvider.get().getSession();

            final Set<String> privilegesAllowedSet = Arrays.stream(privilegesAllowed.value()).collect(Collectors.toSet());

            final String absPath = privilegesAllowed.absPath();

            if (StringUtils.isNotEmpty(absPath)) {
                final Privilege[] privileges = session.getAccessControlManager().getPrivileges(absPath);
                final Set<String> intersection = getIntersection(privilegesAllowedSet, privileges);
                if (intersection.isEmpty()) {
                    return Optional.of(String.format("Method '%s' is not allowed to be invoked since current user does not have " +
                            "the right privileges on path '%s'", method.getName(), absPath));
                }
                return Optional.empty();
            } else if (getPageComposerContextService().isExperiencePageRequest()) {
                // check the privileges on the handle node for the experience page to check whether the user has the
                // right privilege

                final Session cmsUser = getPageComposerContextService().getRequestContext().getSession();
                final String uuid = getPageComposerContextService().getExperiencePageHandleUUID();
                try {
                    final Node handle = cmsUser.getNodeByIdentifier(uuid);

                    final Privilege[] privileges = session.getAccessControlManager().getPrivileges(handle.getPath());
                    final Set<String> intersection = getIntersection(privilegesAllowedSet, privileges);
                    if (intersection.isEmpty()) {
                        return Optional.of(String.format("Method '%s' is not allowed to be invoked since current user does not have " +
                                "the right privileges on path '%s'", method.getName(), absPath));
                    }
                    return Optional.empty();
                } catch (ItemNotFoundException e) {
                    return Optional.of(String.format("Method '%s' is not allowed to be invoked since current user does " +
                                    "not have read access on the document",
                            method.getName(), privilegesAllowed));
                }
            } else {
                return isForbiddenOperationContext(exchange, method, privilegesAllowedSet);
            }

        } catch (RepositoryException e) {
            throw new IllegalStateException("Exception while trying to find whether user has privilege.", e);
        }

    }

    /**
     * To be subclassed if the privilege check is context aware instead of absolute path based. Context aware can for
     * example be based on some http session attribute (like what is the currently selected channel) By default this
     * PrivilegesAllowedInvokerPreprocessor returns that the method is forbidden, subclasses can
     * allow the method
     */
    protected Optional<String> isForbiddenOperationContext(final Exchange exchange,
                                                           final Method method, Set<String> privilegesAllowed) {
        return Optional.of(String.format("Method '%s' is not allowed to be invoked since current user does not have one of " +
                "the allowed privileges '%s' for the current context",
                method.getName(), privilegesAllowed));
    }

    protected Set<String> getIntersection(final Set<String> privilegesAllowed, final Privilege[] privileges) {
        return Arrays.stream(privileges)
                .filter(privilege -> privilegesAllowed.contains(privilege.getName()))
                .map(privilege -> privilege.getName())
                .collect(Collectors.toSet());
    }


}
