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
import java.util.Optional;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.Privilege;

import org.apache.cxf.message.Exchange;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.pagecomposer.jaxrs.api.ChannelManagerPrivileges;
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

        if (privilegesAllowed == null) {
            return Optional.empty();
        }

        // the privilege is currently checked against the *live hstConfigPath* and against the *preview hstConfigPath* if
        // there exists a preview (if no preview present, only the live is checked) :
        // the privileges need to be on both live and preview (if preview present) config path, otherwise not allowed

        try {
            final Session session = RequestContextProvider.get().getSession();

            final Privilege[] privileges = ChannelManagerPrivileges.createPrivileges(privilegesAllowed.value());

            final String liveConfigurationPath = getPageComposerContextService().getEditingLiveConfigurationPath();

            if (!session.getAccessControlManager().hasPrivileges(liveConfigurationPath, privileges)) {
                return Optional.of(String.format("Method '%s' is not allowed to be invoked since current user does not have " +
                        "the right privileges", method.getName()));
            }

            if (previewChannel == null) {
                return Optional.empty();
            }

            if (!session.getAccessControlManager().hasPrivileges(previewChannel.getHstConfigPath(), privileges)) {
                return Optional.of(String.format("Method '%s' is not allowed to be invoked since current user does not have " +
                        "the right privileges", method.getName()));
            }
            return Optional.empty();

        } catch (RepositoryException e) {
            throw new IllegalStateException("Exception while trying to find whether user has privilege.", e);
        }

    }

}
