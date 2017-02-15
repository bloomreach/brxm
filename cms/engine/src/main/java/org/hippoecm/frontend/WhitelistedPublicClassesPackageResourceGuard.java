/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.wicket.markup.html.SecurePackageResourceGuard;
import org.hippoecm.frontend.util.WebApplicationHelper;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A resource guard which grants access when a user is logged in, or when the class of the resource is whitelisted.
 * When either of these criteria is met, access is granted by the super class ({@link SecurePackageResourceGuard}).
 * Otherwise access is denied.
 */
public class WhitelistedPublicClassesPackageResourceGuard extends SecurePackageResourceGuard {

    private static final Logger log = LoggerFactory.getLogger(WhitelistedPublicClassesPackageResourceGuard.class);

    private final Set<String> classNamePrefixes;

    public WhitelistedPublicClassesPackageResourceGuard() {
        this.classNamePrefixes = new HashSet<>();
    }

    public void addClassNamePrefixes(String... prefixes) {
        if (prefixes != null) {
            classNamePrefixes.addAll(Arrays.asList(prefixes));
        }
    }

    @Override
    public boolean accept(final Class<?> scope, final String absolutePath) {
        if (isUserLoggedIn() || isWhitelisted(scope)) {
            return super.accept(scope, absolutePath);
        }
        log.error("Public access denied to non-whitelisted (static) package resource: {}", absolutePath);
        return false;
    }

    private boolean isUserLoggedIn() {
        final HttpServletRequest servletRequest = WebApplicationHelper.retrieveWebRequest().getContainerRequest();
        final HttpSession httpSession = servletRequest.getSession(false);

        if (httpSession == null) {
            return false;
        }

        final CmsSessionContext cmsSessionContext = CmsSessionContext.getContext(httpSession);
        return cmsSessionContext != null;
    }

    private boolean isWhitelisted(final Class<?> scope) {
        final String scopeClassName = scope.getCanonicalName();
        for (String prefix : classNamePrefixes) {
            if (scopeClassName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
