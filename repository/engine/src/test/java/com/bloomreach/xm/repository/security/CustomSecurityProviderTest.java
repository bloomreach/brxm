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
package com.bloomreach.xm.repository.security;

import java.util.stream.Collectors;

import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.api.security.user.UserManager;
import org.hippoecm.repository.security.AbstractSecurityProvider;
import org.hippoecm.repository.security.SecurityProviderContext;
import org.hippoecm.repository.security.group.GroupManager;
import org.hippoecm.repository.security.user.DummyUserManager;
import org.hippoecm.repository.security.user.HippoUserManager;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class CustomSecurityProviderTest extends RepositoryTestCase {

    private static final String TEST_USER_ID = "custom-security-provider-user";

    public static final class CustomSecurityProvider extends AbstractSecurityProvider {

        @Override
        public void init(final SecurityProviderContext context) throws RepositoryException {
        }

        @Override
        public UserManager getUserManager() {
            return new DummyUserManager() {

                @Override
                public boolean authenticate(final SimpleCredentials creds) throws RepositoryException {
                    return true;
                }
                @Override
                public boolean isActive(final String rawUserId) throws RepositoryException {
                    return true;
                }
            };
        }

        @Override
        protected void syncUser(final SimpleCredentials creds, final HippoUserManager userMgr) throws RepositoryException {
            return;
        }

        @Override
        protected void syncGroup(final SimpleCredentials creds, final HippoUserManager userMgr, final GroupManager groupMgr) throws RepositoryException {
            return;
        }
    }


    @Test
    public void assert_custom_security_provider_login_syncs_ONCE_per_CREDENTIALS_instance() throws RepositoryException {
        SimpleCredentials credentials = new SimpleCredentials(TEST_USER_ID, "".toCharArray());
        // very annoying, the 'providerId' must for the purpose of this test to work without having to entirely
        // implement RepositorySecurityProvider (extending doesn't work nicely either) be non-existing...otherwise
        // it gets set to the existing provider 'custom-security-provider' which triggers
        // org.hippoecm.repository.security.SecurityManager.assignPrincipals with an existing custom provider and results
        // in all kind of unhandy code that most be implemented in the subclasses...for the purpose of what we want
        // to test here this really isn't needed

        credentials.setAttribute("providerId", "non-existing");
        try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(AbstractSecurityProvider.class).build()) {
            server.login(credentials);
            assertThat(interceptor.messages().collect(Collectors.toList()))
                    .contains(format("Sync user '%s'", TEST_USER_ID));
        }

        try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(AbstractSecurityProvider.class).build()) {
            server.login(credentials);
            assertThat(interceptor.messages().collect(Collectors.toList()))
                    .contains(format("Sync for user '%s' already done", TEST_USER_ID));
        }

        // new credentials object triggers new sync
        try (Log4jInterceptor interceptor = Log4jInterceptor.onInfo().trap(AbstractSecurityProvider.class).build()) {
            SimpleCredentials credentials2 = new SimpleCredentials(TEST_USER_ID, "".toCharArray());
            credentials2.setAttribute("providerId", "non-existing");
            server.login(credentials2);
            assertThat(interceptor.messages().collect(Collectors.toList()))
                    .contains(format("Sync user '%s'", TEST_USER_ID));
        }
    }


}
