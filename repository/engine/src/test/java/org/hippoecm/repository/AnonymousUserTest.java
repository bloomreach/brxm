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
package org.hippoecm.repository;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.impl.RepositoryDecorator;
import org.junit.Test;
import org.onehippo.repository.InternalHippoRepository;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.junit.Assert.fail;

/**
 * Test verifying it is not possible to login, nor impersonate, as an anonymous user
 */
public class AnonymousUserTest extends RepositoryTestCase {

    @Test
    public void test_login_with_anonymous_no_null_or_empty_credentials_is_denied() throws RepositoryException {
        Session testSession;
        try {
            testSession = session.getRepository().login(new SimpleCredentials("anonymous", new char[0]));
            testSession.logout();
            fail("Login with anonymous userId should be denied");
        } catch (LoginException ignore) {
        }
        try {
            testSession = session.getRepository().login();
            testSession.logout();
            fail("Login without credentials should be denied");
        } catch (LoginException ignore) {
        }
        try {
            testSession = session.getRepository().login(new SimpleCredentials(null, new char[0]));
            testSession.logout();
            fail("Login with null userId should be denied");
        } catch (LoginException ignore) {
        }
        try {
            testSession = session.getRepository().login(new SimpleCredentials("", new char[0]));
            testSession.logout();
            fail("Login with empty userId should be denied");
        } catch (LoginException ignore) {
        }
    }

    @Test
    public void test_impersonate_with_anonymous_no_null_or_empty_credentials_is_denied() throws RepositoryException {
        Session testSession;
        try {
            testSession = session.impersonate(new SimpleCredentials("anonymous", new char[0]));
            testSession.logout();
            fail("Impersonate with anonymous userId should be denied");
        } catch (LoginException ignore) {
        }
        try {
            testSession = session.impersonate(null);
            testSession.logout();
            fail("Impersonate without credentials should be denied");
        } catch (RepositoryException ignore) {
        }
        try {
            testSession = session.impersonate(new SimpleCredentials(null, new char[0]));
            testSession.logout();
            fail("Impersonate with null userId should be denied");
        } catch (LoginException ignore) {
        }
        try {
            testSession = session.impersonate(new SimpleCredentials("", new char[0]));
            testSession.logout();
            fail("Impersonate with empty userId should be denied");
        } catch (LoginException ignore) {
        }
    }

    @Test
    public void test_system_impersonate_with_anonymous_no_null_or_empty_credentials_is_denied() throws RepositoryException {
        InternalHippoRepository internalRepository = (InternalHippoRepository) RepositoryDecorator.unwrap(server.getRepository());
        Session systemSession = internalRepository.createSystemSession();
        try {
            Session testSession;
            try {
                testSession = systemSession.impersonate(new SimpleCredentials("anonymous", new char[0]));
                testSession.logout();
                fail("System impersonate with anonymous userId should be denied");
            } catch (LoginException ignore) {
            }
            try {
                testSession = systemSession.impersonate(null);
                testSession.logout();
                fail("System impersonate without credentials should be denied");
            } catch (RepositoryException ignore) {
            }
            try {
                testSession = systemSession.impersonate(new SimpleCredentials(null, new char[0]));
                testSession.logout();
                fail("System impersonate with null userId should be denied");
            } catch (LoginException ignore) {
            }
            try {
                testSession = systemSession.impersonate(new SimpleCredentials("", new char[0]));
                testSession.logout();
                fail("System impersonate with empty userId should be denied");
            } catch (LoginException ignore) {
            }
        } finally {
            systemSession.logout();
        }
    }
}
