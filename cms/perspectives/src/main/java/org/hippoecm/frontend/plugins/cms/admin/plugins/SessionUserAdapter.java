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
package org.hippoecm.frontend.plugins.cms.admin.plugins;

import java.util.List;

import javax.jcr.RepositoryException;

import org.hippoecm.frontend.plugins.cms.admin.SecurityManagerHelper;
import org.hippoecm.frontend.plugins.cms.admin.groups.DetachableGroup;
import org.hippoecm.frontend.plugins.cms.admin.groups.Group;
import org.hippoecm.frontend.plugins.cms.admin.users.User;
import org.onehippo.repository.security.SessionUser;

import com.bloomreach.xm.repository.security.ChangePasswordManager;

/**
 * Adapter class which extends {@link User} to wrap a {@link SessionUser} and optionally allowing to change the current
 * user its password through the {@link ChangePasswordManager},
 * providing reduced and adapted access to the standard {@link User} methods which would require direct readwrite access
 * to the underlying repository user (node).
 * <p>
 *     This adapter only provides readonly access to {@link SessionUser} data, overrides a few {@link User} methods to
 *     delegate to the {@link ChangePasswordManager} (if provided), and throws UnsupportedOperationException for the
 *     other {@link User} methods.
 * </p>
 * <p>
 *     Usage of this class is only intended for the {@link ChangePasswordShortcutPlugin}, until a further (future)
 *     refactoring/replacement of the {@link }User} through a RepositorySecurityManager management based solution.
 * </p>
 */
class SessionUserAdapter extends User {

    private final boolean canChangePassword;

    SessionUserAdapter(final SessionUser user, final ChangePasswordManager changePasswordManager)
            throws RepositoryException {
        super(user);
        this.canChangePassword = changePasswordManager != null;
        if (changePasswordManager != null) {
            setPasswordMaxAge(changePasswordManager.getPasswordMaxAgeMs());
            setPasswordLastModified(changePasswordManager.getPasswordLastModified());
        }
    }

    public void savePassword(final char[] currentPassword, final char[] newPassword) throws RepositoryException {
        if (!canChangePassword) {
            throw new UnsupportedOperationException();
        }
        final ChangePasswordManager changePasswordManager = SecurityManagerHelper.getChangePasswordManager();
        changePasswordManager.setPassword(currentPassword, newPassword);
        setPasswordLastModified(changePasswordManager.getPasswordLastModified());
    }

    @Override
    public boolean checkPassword(final char[] password) {
        if (!canChangePassword) {
            throw new UnsupportedOperationException();
        }
        try {
            return SecurityManagerHelper.getChangePasswordManager().checkPassword(password);
        } catch (RepositoryException e) {
            return false;
        }
    }

    @Override
    public boolean isPreviousPassword(final char[] password, final int numberOfPreviousPasswords) throws RepositoryException {
        if (!canChangePassword) {
            throw new UnsupportedOperationException();
        }
        return SecurityManagerHelper.getChangePasswordManager().checkNewPasswordUsedBefore(password, numberOfPreviousPasswords);
    }

    @Override
    public void savePassword(final String password) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setActive(final boolean active) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUsername(final String username) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFirstName(final String firstName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLastName(final String lastName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setEmail(final String email) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DetachableGroup> getLocalMemberships() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DetachableGroup> getLocalMemberships(final boolean excludeSystemUsers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Group> getLocalMembershipsAsListOfGroups() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Group> getLocalMembershipsAsListOfGroups(final boolean excludeSystemUsers) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DetachableGroup> getExternalMemberships() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void create() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void create(final String securityProviderName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void save() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAllGroupMemberships() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addUserRole(final String userRole) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeUserRole(final String userRole) {
        throw new UnsupportedOperationException();
    }
}
