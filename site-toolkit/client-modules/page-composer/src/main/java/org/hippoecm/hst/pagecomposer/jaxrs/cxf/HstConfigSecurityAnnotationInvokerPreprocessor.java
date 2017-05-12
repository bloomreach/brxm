/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

import java.security.Principal;

import javax.jcr.RepositoryException;
import javax.ws.rs.core.SecurityContext;

import org.apache.cxf.message.Exchange;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.jaxrs.cxf.SecurityAnnotationInvokerPreprocessor;
import org.hippoecm.hst.pagecomposer.jaxrs.security.SecurityModel;

public class HstConfigSecurityAnnotationInvokerPreprocessor extends SecurityAnnotationInvokerPreprocessor {

    private SecurityModel securityModel;

    public void setSecurityModel(SecurityModel securityModel) {
        this.securityModel = securityModel;
    }

    @Override
    protected SecurityContext getSecurityContext(final Exchange exchange) {
        final SecurityContext delegatee = super.getSecurityContext(exchange);
        return new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                try {
                    return securityModel.getUserPrincipal(RequestContextProvider.get().getSession());
                } catch (RepositoryException e) {
                    throw new IllegalStateException("Exception while getting user principal.", e);
                }
            }

            @Override
            public boolean isUserInRole(final String role) {
                try {
                    return securityModel.isUserInRule(RequestContextProvider.get().getSession(), role);
                } catch (RepositoryException e) {
                    throw new IllegalStateException("Exception while trying to find whether user is " +
                            "in role.", e);
                }
            }

            @Override
            public boolean isSecure() {
                return delegatee.isSecure();
            }

            @Override
            public String getAuthenticationScheme() {
                return delegatee.getAuthenticationScheme();
            }
        };

    }

}
