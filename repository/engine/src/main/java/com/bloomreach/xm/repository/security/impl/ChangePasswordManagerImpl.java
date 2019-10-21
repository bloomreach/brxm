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
package com.bloomreach.xm.repository.security.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;

import org.apache.jackrabbit.util.ISO9075;
import org.hippoecm.repository.PasswordHelper;
import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bloomreach.xm.repository.security.ChangePasswordManager;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PASSWORD;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PASSWORDLASTMODIFIED;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PASSWORDMAXAGEDAYS;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PREVIOUSPASSWORDS;
import static org.onehippo.repository.security.SecurityConstants.CONFIG_SECURITY_PATH;

public class ChangePasswordManagerImpl implements ChangePasswordManager {

    private static final Logger log = LoggerFactory.getLogger(ChangePasswordManager.class);

    private static final String USER_QUERY = "//element(*, hipposys:user)[fn:name()='{}']";

    protected final RepositorySecurityManagerImpl repositorySecurityManager;
    private final String passwordMaxAgeDaysPath;

    public ChangePasswordManagerImpl(RepositorySecurityManagerImpl repositorySecurityManager) {
        this.repositorySecurityManager = repositorySecurityManager;
        this.passwordMaxAgeDaysPath = CONFIG_SECURITY_PATH + "/" + HIPPO_PASSWORDMAXAGEDAYS;
    }

    private Long getPassworMaxAgeDaysValue() {
        try {
            return JcrUtils.getLongProperty(repositorySecurityManager.getSystemSession(), passwordMaxAgeDaysPath, null);
        } catch (RepositoryException e) {
            // try once more, just to be a little bit more resilient
            try {
                return JcrUtils.getLongProperty(repositorySecurityManager.getSystemSession(), passwordMaxAgeDaysPath, null);
            } catch (RepositoryException ignore) {
                return null;
            }
        }
    }

    @Override
    public long getPasswordMaxAgeDays() throws RepositoryException {
        repositorySecurityManager.checkClosed();
        Long passwordMaxAge = getPassworMaxAgeDaysValue();
        return passwordMaxAge != null ? passwordMaxAge : -1L;
    }

    @Override
    public long getPasswordMaxAgeMs() throws RepositoryException {
        repositorySecurityManager.checkClosed();
        Long passwordMaxAge = getPassworMaxAgeDaysValue();
        return passwordMaxAge != null ? passwordMaxAge * ONEDAYMS : -1L;
    }

    private Node getUserNodeFromSystemSession() throws RepositoryException {
        final String userId = repositorySecurityManager.getHippoSession().getUser().getId();
        final String encodedUserId = ISO9075.encode(NodeNameCodec.encode(userId, true));
        final String userQuery = USER_QUERY.replace("{}", encodedUserId);
        Query q = repositorySecurityManager.getSystemSession().getWorkspace().getQueryManager().createQuery(userQuery, Query.XPATH);
        NodeIterator nodeIter = q.execute().getNodes();
        if (nodeIter.hasNext()) {
            return nodeIter.nextNode();
        } else {
            throw new ItemNotFoundException("User '"+userId+"' no longer exists");
        }
    }

    private boolean checkPassword(final char[] password, final String hashedPassword) {
        try {
            return PasswordHelper.checkHash(password, hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            log.error("Unknown algorithm for password", e);
        } catch (UnsupportedEncodingException e) {
            log.error("Unsupported encoding for password", e);
        }
        return false;
    }

    private String createPasswordHash(final char[] password) throws RepositoryException {
        try {
            return PasswordHelper.getHash(password);
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RepositoryException("Unable to create a password hash ", e);
        }
    }

    @Override
    public Calendar getPasswordLastModified() throws RepositoryException {
        repositorySecurityManager.checkClosed();
        final Node userNodeFromSystemSession = getUserNodeFromSystemSession();
        return JcrUtils.getDateProperty(userNodeFromSystemSession, HIPPO_PASSWORDLASTMODIFIED, null);
    }

    @Override
    public boolean checkPassword(final char[] password) throws RepositoryException {
        repositorySecurityManager.checkClosed();
        final Node userNode = getUserNodeFromSystemSession();
        final String currentPasswordHash = JcrUtils.getStringProperty(userNode, HIPPO_PASSWORD, null);
        if (currentPasswordHash != null) {
            return checkPassword(password, currentPasswordHash);
        }
        return false;
    }

    @Override
    public boolean checkNewPasswordUsedBefore(final char[] newPassword, final int numberOfPreviousPasswordsToCheck)
            throws RepositoryException {
        repositorySecurityManager.checkClosed();
        final Node userNodeFromSystemSession = getUserNodeFromSystemSession();
        final String currentPasswordHash = JcrUtils.getStringProperty(userNodeFromSystemSession, HIPPO_PASSWORD, null);
        if (currentPasswordHash != null && checkPassword(newPassword, currentPasswordHash)) {
            return true;
        }
        if (newPassword == null || newPassword.length == 0) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        if (numberOfPreviousPasswordsToCheck > 0) {
            final List<String> previousPasswordHashes =
                    JcrUtils.getStringListProperty(userNodeFromSystemSession, HIPPO_PREVIOUSPASSWORDS, Collections.emptyList());
            for (int i = 0, numPasswords = previousPasswordHashes.size();
                 i < numberOfPreviousPasswordsToCheck && i < numPasswords;
                 i++) {
                if (checkPassword(newPassword, previousPasswordHashes.get(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void setPassword(final char[] currentPassword, final char[] newPassword)
            throws IllegalArgumentException, RepositoryException {
        repositorySecurityManager.checkClosed();
        final Node userNodeFromSystemSession = getUserNodeFromSystemSession();
        final String currentPasswordHash = JcrUtils.getStringProperty(userNodeFromSystemSession, HIPPO_PASSWORD, null);
        if (currentPasswordHash != null && checkPassword(newPassword, currentPasswordHash)) {
            throw new IllegalArgumentException("Incorrect current password");
        }
        if (newPassword == null || newPassword.length < 4) {
            throw new IllegalArgumentException("New password must not be empty and at least 4 characters long");
        }
        if (Arrays.equals(newPassword, currentPassword)) {
            throw new IllegalArgumentException("New password is the same as the current password");
        }
        final String newPasswordHash = createPasswordHash(newPassword);
        try {
            if (currentPasswordHash != null) {
                List<String> previousPasswords =
                        JcrUtils.getStringListProperty(userNodeFromSystemSession, HIPPO_PREVIOUSPASSWORDS, new ArrayList<>());
                // save current password (hash) in previous passwords
                previousPasswords.add(0, currentPasswordHash);
                userNodeFromSystemSession.setProperty(HIPPO_PREVIOUSPASSWORDS, previousPasswords.toArray(new String[0]));
            }
            // set password last changed date
            userNodeFromSystemSession.setProperty(HIPPO_PASSWORDLASTMODIFIED, Calendar.getInstance());
            // set new password
            userNodeFromSystemSession.setProperty(HIPPO_PASSWORD, newPasswordHash);
            userNodeFromSystemSession.getSession().save();
        } catch (RepositoryException e) {
            // make sure to always clear the system session state
            try {
                repositorySecurityManager.getSystemSession().refresh(false);
            } catch (RepositoryException ignore) {
            }
            throw e;
        }
    }
}
