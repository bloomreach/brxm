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
package com.bloomreach.xm.repository.security;

import java.util.Calendar;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.impl.RepositoryDecorator;
import org.hippoecm.repository.impl.SessionDecorator;
import org.junit.Test;
import org.onehippo.repository.InternalHippoRepository;
import org.onehippo.repository.security.SecurityConstants;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static com.bloomreach.xm.repository.security.ChangePasswordManager.ONEDAYMS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PASSWORD;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PASSWORDLASTMODIFIED;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PASSWORDMAXAGEDAYS;
import static org.hippoecm.repository.api.HippoNodeType.NT_EXTERNALUSER;
import static org.hippoecm.repository.api.HippoNodeType.NT_USER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.onehippo.repository.security.SecurityConstants.CONFIG_SECURITY_PATH;

public class ChangePasswordManagerImplTest extends RepositoryTestCase {

    private static final String TEST_USER_ID = "testuser";

    private Node securityConfigNode;
    private Node testUserNode;
    private HippoSession testSession;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        removeTestData();
        setupTestData();
    }

    @Override
    public void tearDown() throws Exception {
        removeTestData();
        if (testSession != null && testSession.isLive()) {
            testSession.logout();
        }
        testSession = null;
        super.tearDown();
    }

    private void removeTestData() throws RepositoryException {
        securityConfigNode = session.getNode(CONFIG_SECURITY_PATH);
        if (securityConfigNode.hasProperty(HIPPO_PASSWORDMAXAGEDAYS)) {
            securityConfigNode.getProperty(HIPPO_PASSWORDMAXAGEDAYS).remove();
            securityConfigNode = null;
        }
        if (testUserNode != null) {
            testUserNode.remove();
        }
        session.save();
    }

    private void setupTestData() throws RepositoryException {
        securityConfigNode = session.getNode(CONFIG_SECURITY_PATH);
        testUserNode = session.getNode(SecurityConstants.CONFIG_USERS_PATH).addNode(TEST_USER_ID, NT_USER);
        session.save();
    }

    @Test
    public void testChangePasswordManagerAccess() throws RepositoryException {
        // regular test user should be allowed access
        testSession = (HippoSession)session.impersonate(new SimpleCredentials(TEST_USER_ID, new char[0]));

        testUserNode.remove();
        testUserNode = session.getNode(SecurityConstants.CONFIG_USERS_PATH).addNode(TEST_USER_ID, NT_EXTERNALUSER);
        session.save();
        try {
            testSession = (HippoSession)session.impersonate(new SimpleCredentials(TEST_USER_ID, new char[0]));
            testSession.getWorkspace().getSecurityManager().getChangePasswordManager();
            fail("For an external user the ChangePasswordManager should not be accessible");
        } catch (AccessDeniedException ignore) {
        } finally {
            if (testSession != null && testSession.isLive()) {
                testSession.logout();
            }
        }

        try {
            testSession = (HippoSession) session.impersonate(new SimpleCredentials("workflowuser", new char[0]));
            testSession.getWorkspace().getSecurityManager().getChangePasswordManager();
            fail("For a user of type system the ChangePasswordManager should not be accessible");
        } catch (AccessDeniedException ignore) {
        } finally {
            if (testSession != null && testSession.isLive()) {
                testSession.logout();
            }
        }

        InternalHippoRepository internalHippoRepository = (InternalHippoRepository) RepositoryDecorator.unwrap(session.getRepository());
        try {
            testSession = SessionDecorator.newSessionDecorator(internalHippoRepository.createSystemSession());
            testSession.getWorkspace().getSecurityManager().getChangePasswordManager();
            fail("For a system user the ChangePasswordManager should not be accessible");
        } catch (AccessDeniedException ignore) {
        } finally {
            if (testSession != null && testSession.isLive()) {
                testSession.logout();
            }
        }
    }

    @Test
    public void testPaswordMaxAge() throws Exception {
        testSession = (HippoSession)session.impersonate(new SimpleCredentials(TEST_USER_ID, new char[0]));
        ChangePasswordManager changePasswordManager = testSession.getWorkspace().getSecurityManager().getChangePasswordManager();

        // default no password max age
        assertEquals(-1L, changePasswordManager.getPasswordMaxAgeDays());
        assertEquals(-1L, changePasswordManager.getPasswordMaxAgeMs());
        securityConfigNode.setProperty(HIPPO_PASSWORDMAXAGEDAYS, 1L);
        session.save();

        // config node with 1 day max age
        assertEquals(1L, changePasswordManager.getPasswordMaxAgeDays());
        assertEquals(ONEDAYMS, changePasswordManager.getPasswordMaxAgeMs());

        // update max age to 2 days
        securityConfigNode.setProperty(HIPPO_PASSWORDMAXAGEDAYS, 2);
        session.save();
        assertEquals(2L, changePasswordManager.getPasswordMaxAgeDays());
        assertEquals(2*ONEDAYMS, changePasswordManager.getPasswordMaxAgeMs());

        /* the hipposys:passwordmaxagedays property type actually allows both long and double properties (?)
           to be able to test/check standard Java double->long conversion with *no rounding* we first have to
           delete the existing type long property, before we can recreate it as type double
         */
        securityConfigNode.getProperty(HIPPO_PASSWORDMAXAGEDAYS).remove();

        // verify no rounding from double->long
        securityConfigNode.setProperty(HIPPO_PASSWORDMAXAGEDAYS, 3.9);
        session.save();
        assertEquals(3L, changePasswordManager.getPasswordMaxAgeDays());
        assertEquals(3*ONEDAYMS, changePasswordManager.getPasswordMaxAgeMs());

        // test no passwordmaxagedays property
        securityConfigNode.getProperty(HIPPO_PASSWORDMAXAGEDAYS).remove();
        session.save();
        assertEquals(-1L, changePasswordManager.getPasswordMaxAgeDays());
    }

    @Test
    public void testSetPassword() throws Exception {
        testSession = (HippoSession)session.impersonate(new SimpleCredentials(TEST_USER_ID, new char[0]));
        ChangePasswordManager changePasswordManager = testSession.getWorkspace().getSecurityManager().getChangePasswordManager();

        assertFalse(testUserNode.hasProperty(HIPPO_PASSWORD));
        try {
            changePasswordManager.setPassword(null, "abcd".toCharArray());
        } catch (IllegalArgumentException e) {
            fail("Setting a new password for the first time should ignore the current password parameter (null)");
        }
        assertTrue(testUserNode.hasProperty(HIPPO_PASSWORD));
        testUserNode.getProperty(HIPPO_PASSWORD).remove();
        session.save();

        try {
            changePasswordManager.setPassword(new char[0], "abcd".toCharArray());
        } catch (IllegalArgumentException e) {
            fail("Setting a new password for the first time should ignore the current password parameter (empty)");
        }
        assertTrue(testUserNode.hasProperty(HIPPO_PASSWORD));
        testUserNode.getProperty(HIPPO_PASSWORD).remove();
        session.save();

        try {
            changePasswordManager.setPassword("random".toCharArray(), "abcd".toCharArray());
        } catch (IllegalArgumentException e) {
            fail("Setting a new password for the first time should ignore the current password parameter (random)");
        }
        assertTrue(testUserNode.hasProperty(HIPPO_PASSWORD));
        testUserNode.getProperty(HIPPO_PASSWORD).remove();
        session.save();

        try {
            changePasswordManager.setPassword(new char[0], null);
            fail("Setting a new password should not be null");
        } catch (IllegalArgumentException ignore) {
        }
        assertFalse(testUserNode.hasProperty(HIPPO_PASSWORD));

        try {
            changePasswordManager.setPassword(new char[0], new char[0]);
            fail("Setting a new password should not be empty");
        } catch (IllegalArgumentException ignore) {
        }
        assertFalse(testUserNode.hasProperty(HIPPO_PASSWORD));

        try {
            changePasswordManager.setPassword(new char[0], "abc".toCharArray());
            fail("Setting a new password should be at least 4 characters long");
        } catch (IllegalArgumentException ignore) {
        }
        assertFalse(testUserNode.hasProperty(HIPPO_PASSWORD));

        if (testUserNode.hasProperty(HIPPO_PASSWORDLASTMODIFIED)) {
            testUserNode.getProperty(HIPPO_PASSWORDLASTMODIFIED).remove();
            session.save();
        }

        changePasswordManager.setPassword("abcd".toCharArray(), "random".toCharArray());

        try {
            changePasswordManager.setPassword("random".toCharArray(), "random".toCharArray());
            fail("A new password should be different from the current password");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void testPasswordLastModified() throws Exception {
        testSession = (HippoSession)session.impersonate(new SimpleCredentials(TEST_USER_ID, new char[0]));
        ChangePasswordManager changePasswordManager = testSession.getWorkspace().getSecurityManager().getChangePasswordManager();

        assertNull(changePasswordManager.getPasswordLastModified());

        long now = System.currentTimeMillis();
        changePasswordManager.setPassword(new char[0], "abcd".toCharArray());
        Calendar passwordLastModified = changePasswordManager.getPasswordLastModified();
        assertNotNull(passwordLastModified);
        assertTrue(passwordLastModified.getTimeInMillis() > now);

        changePasswordManager.setPassword("abcd".toCharArray(), "random".toCharArray());
        Calendar updatedPasswordLastModified = changePasswordManager.getPasswordLastModified();
        assertNotNull(updatedPasswordLastModified);
        assertTrue(updatedPasswordLastModified.getTimeInMillis() > passwordLastModified.getTimeInMillis());
    }

    @Test
    public void testCheckPassword() throws Exception {
        testSession = (HippoSession)session.impersonate(new SimpleCredentials(TEST_USER_ID, new char[0]));
        ChangePasswordManager changePasswordManager = testSession.getWorkspace().getSecurityManager().getChangePasswordManager();

        assertFalse(testUserNode.hasProperty(HIPPO_PASSWORD));
        assertFalse(changePasswordManager.checkPassword(null));
        assertFalse(changePasswordManager.checkPassword("random".toCharArray()));

        changePasswordManager.setPassword(new char[0], "random".toCharArray());
        assertTrue(changePasswordManager.checkPassword("random".toCharArray()));

        try {
            changePasswordManager.checkPassword(new char[0]);
            fail("Checking an existing password with empty value should not be supported");
        } catch (IllegalArgumentException ignore) {
        }
        // checking against a blank password should be allowed
        assertFalse(changePasswordManager.checkPassword(" ".toCharArray()));
    }

    @Test
    public void testCheckNewPasswordUsedBefore() throws Exception {
        testSession = (HippoSession)session.impersonate(new SimpleCredentials(TEST_USER_ID, new char[0]));
        ChangePasswordManager changePasswordManager = testSession.getWorkspace().getSecurityManager().getChangePasswordManager();

        assertFalse(testUserNode.hasProperty(HIPPO_PASSWORD));
        try {
            changePasswordManager.checkNewPasswordUsedBefore(null, 0);
            fail("Checking a new password without value should not be supported");
        } catch (IllegalArgumentException ignore) {
        }
        try {
            changePasswordManager.checkNewPasswordUsedBefore(new char[0], 0);
            fail("Checking an empty new password should not be supported");
        } catch (IllegalArgumentException ignore) {
        }
        assertFalse(changePasswordManager.checkNewPasswordUsedBefore("abcd".toCharArray(), 0));

        changePasswordManager.setPassword(new char[0], "random".toCharArray());
        assertFalse(changePasswordManager.checkNewPasswordUsedBefore("abcd".toCharArray(), 0));
        assertFalse(changePasswordManager.checkNewPasswordUsedBefore("abcd".toCharArray(), 100));
        assertTrue(changePasswordManager.checkNewPasswordUsedBefore("random".toCharArray(), 0));

        changePasswordManager.setPassword("random".toCharArray(), "abcd".toCharArray());
        assertTrue(changePasswordManager.checkNewPasswordUsedBefore("abcd".toCharArray(), 0));
        assertFalse(changePasswordManager.checkNewPasswordUsedBefore("random".toCharArray(), 0));
        assertTrue(changePasswordManager.checkNewPasswordUsedBefore("random".toCharArray(), 1));

        changePasswordManager.setPassword("abcd".toCharArray(), "foobar".toCharArray());
        assertFalse(changePasswordManager.checkNewPasswordUsedBefore("random".toCharArray(), 1));
        assertTrue(changePasswordManager.checkNewPasswordUsedBefore("random".toCharArray(), 2));
    }
}
