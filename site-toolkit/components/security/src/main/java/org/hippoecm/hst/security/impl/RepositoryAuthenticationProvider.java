/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.security.impl;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.security.AuthenticationProvider;
import org.hippoecm.hst.security.Role;
import org.hippoecm.hst.security.TransientRole;
import org.hippoecm.hst.security.TransientUser;
import org.hippoecm.hst.security.User;
import org.hippoecm.repository.api.HippoSession;
import org.onehippo.repository.security.SessionUser;

import static java.util.stream.Collectors.toList;

/**
 * Repository based AuthenticationProvider which (only) uses and filters/maps the {@link SessionUser#getUserRoles()}
 * to produce the set of {@link Role}s of an authenticated user.
 * <p>
 *     The following configuration properties are available:
 * </p>
 * <ul>
 *     <li>
 *         {@link #setRequiredUserRole(String)}: when not empty both {@link #authenticate(String, char[])} and
 *         {@link #getRolesByUser(User)} will check and require the user to have at least this userrole (through
 *         {@link SessionUser#getUserRoles()}). Default: empty.
 *     </li>
 *     <li>
 *         {@link #setExcludedUserRolePrefixes(String)}: a String of one or more prefixes (delimited by
 *         {@link #setExcludedUserRolePrefixesDelimiter(String)}) to <em>exclude</em> the userroles which name starts
 *         with such prefix(es). When blank or null, no userrole name prefix exclusion will be applied. Default: empty.
 *     </li>
 *     <li>
 *         {@link #setExcludedUserRolePrefixesDelimiter(String)}: the delimiter (string) to split the prefixes provided
 *         through {@link #setExcludedUserRolePrefixes(String)}. Default: "," (comma).
 *     <li>
 *         {@link #setIncludedUserRolePrefix(String)}: the userrole name prefix required for all userroles to be
 *         included. When blank or null, all not-yet-excluded userroles will be included. Default: empty.
 *     </li>
 *     <li>
 *         {@link #setStripIncludedUserRolePrefix(boolean)}: when {@link #setIncludedUserRolePrefix(String)} is
 *         non-empty, strip that prefix from the included userroles when mapping to a {@link Role}. Default: true.
 *     </li>
 *     <li>
 *         {@link #setRolePrefix(String)}: When not empty all included userroles will be mapped to a {@link Role} with
 *         this prefix to their name. Default: empty.
 *     </li>
 *     <li>
 *         {@link #setDefaultRoleName(String)}: When not empty, adds a predefined {@link Role} with this name to the
 *         returned {@link Role}s from {@link #getRolesByUser(User)}.
 *     </li>
 * </ul>
 * <p>
 *     This provider is a general replacement for the now (v14) deprecated {@link JcrAuthenticationProvider} and
 *     {@link HippoAuthenticationProvider} which use the no longer recommended solution of defining auxiliary
 *     hipposys:authrole nodes under a specific security domain, which hipposys:role property is then used as
 *     'source' for the mapped user roles. While that solution still works, it is now deprecated and likely be
 *     removed with v15+.
 * </p>
 */
public class RepositoryAuthenticationProvider implements AuthenticationProvider {

    private final Repository systemRepository;
    private final Credentials systemCreds;
    private final Repository userAuthRepository;
    private String requiredUserRole;
    private List<String> excludedUserRolePrefixesList = Collections.emptyList();
    private String excludedUserRolePrefixesDelimiter = ",";
    private String includedUserRolePrefix = "";
    private boolean stripIncludedUserRolePrefix = true;
    private String rolePrefix = "";
    private String defaultRoleName;

    public RepositoryAuthenticationProvider(final Repository systemRepository, final Credentials systemCreds,
                                            final Repository userAuthRepository) {
        this.systemRepository = systemRepository;
        this.systemCreds = systemCreds;
        this.userAuthRepository = userAuthRepository;
    }

    public void setRequiredUserRole(final String requiredUserRole) {
        if (!StringUtils.isBlank(requiredUserRole)) {
            this.requiredUserRole = requiredUserRole;
        } else {
            this.requiredUserRole = null;
        }
    }

    @SuppressWarnings("unused")
    protected String getRequiredUserRole() {
        return requiredUserRole;
    }

    public void setExcludedUserRolePrefixes(final String excludedUserRolePrefixes) {
        if (!StringUtils.isBlank(excludedUserRolePrefixes)) {
            this.excludedUserRolePrefixesList =
                    Arrays.stream(excludedUserRolePrefixes.split(excludedUserRolePrefixesDelimiter))
                            .filter(r -> !StringUtils.isBlank(r))
                            .collect(Collectors.collectingAndThen(toList(), Collections::unmodifiableList));
        } else {
            this.excludedUserRolePrefixesList = Collections.emptyList();
        }
    }

    @SuppressWarnings("unused")
    protected List<String> getExcludedUserRolePrefixesList() {
        return excludedUserRolePrefixesList;
    }

    public void setExcludedUserRolePrefixesDelimiter(final String excludedUserRolePrefixesDelimiter) {
        if (!StringUtils.isBlank(excludedUserRolePrefixesDelimiter)) {
            this.excludedUserRolePrefixesDelimiter = excludedUserRolePrefixesDelimiter;
        } else {
            this.excludedUserRolePrefixesDelimiter = "";
        }
    }

    @SuppressWarnings("unused")
    protected String getExcludedUserRolePrefixesDelimiter() {
        return excludedUserRolePrefixesDelimiter;
    }

    public void setIncludedUserRolePrefix(final String includedUserRolePrefix) {
        if (includedUserRolePrefix != null) {
            this.includedUserRolePrefix = includedUserRolePrefix;
        } else {
            this.includedUserRolePrefix = "";
        }
    }

    @SuppressWarnings("unused")
    protected String getIncludedUserRolePrefix() {
        return includedUserRolePrefix;
    }

    public void setStripIncludedUserRolePrefix(final boolean stripIncludedUserRolePrefix) {
        this.stripIncludedUserRolePrefix = stripIncludedUserRolePrefix;
    }

    @SuppressWarnings("unused")
    protected boolean isStripIncludedUserRolePrefix() {
        return stripIncludedUserRolePrefix;
    }

    public void setRolePrefix(final String rolePrefix) {
        if (!StringUtils.isBlank(rolePrefix)) {
            this.rolePrefix = rolePrefix;
        } else {
            this.rolePrefix = "";
        }
    }

    @SuppressWarnings("unused")
    protected String getRolePrefix() {
        return rolePrefix;
    }

    public void setDefaultRoleName(final String defaultRoleName) {
        if (!StringUtils.isBlank(defaultRoleName)) {
            this.defaultRoleName = defaultRoleName;
        } else {
            this.defaultRoleName = null;
        }
    }

    @SuppressWarnings("unused")
    protected String getDefaultRoleName() {
        return defaultRoleName;
    }

    @Override
    public TransientUser authenticate(final String userName, final char[] password) throws SecurityException {
        HippoSession session = null;
        try {
            session = (HippoSession)userAuthRepository.login(new SimpleCredentials(userName, password));
            if (requiredUserRole == null || session.isUserInRole(requiredUserRole)) {
                return new TransientUser(session.getUser().getId(), session.getUser());
            }
            throw new SecurityException("Access denied");
        } catch (RepositoryException e) {
            throw new SecurityException(e.getLocalizedMessage(), e);
        } finally {
            if (session != null) {
                try {
                    session.logout();
                } catch (Exception ignore) {
                }
            }
        }
    }

    /*
     * @param username the username to get the roles for
     * @return list of roles
     * @throws SecurityException in case something goes wrong
     * @deprecated Note that this method will need to be kept, but just made private in v15 since
     * getRolesByUser(final User user) will still need to be able to fetch the roles by user name
     */
    @Override
    @Deprecated
    public Set<Role> getRolesByUsername(final String username) throws SecurityException {
        Session session = null;
        HippoSession userSession = null;
        try {
            session = systemRepository.login(systemCreds);
            userSession = (HippoSession)session.impersonate(new SimpleCredentials(username, new char[0]));
            if (userSession.isSystemSession()) {
                throw new SecurityException("Not allowed to impersonate a system user");
            }
            return getRolesByUser(userSession.getUser());
        } catch (RepositoryException e) {
            throw new SecurityException(e.getLocalizedMessage(), e);
        } finally {
            if (userSession != null) {
                try {
                    userSession.logout();
                } catch (Exception ignore) {
                }
            }
            if (session != null) {
                try {
                    session.logout();
                } catch (Exception ignore) {
                }
            }
        }
    }

    /**
     * <p>
     *     If the provided user is an instance of {@link TransientUser}, <em>and</em> its
     *     {@link TransientUser#getUserObject()} is an instance of org.onehippo.repository.security.SessionUser,
     *     <em>then</em> this method will delegate to {@link #getRolesByUser(SessionUser)} and no further login
     *     is needed! Otherwise it delegates to {@link #getRolesByUsername(String)} which first will do (again?) a
     *     login to retrieve the {@link SessionUser} which <em>thereafter</em> delegates to {@link #getRolesByUser(SessionUser)}.
     * </p>
     * @param user the user to get the roles for
     * @return list of roles
     * @throws SecurityException in case something goes wrong
     */
    @Override
    public Set<Role> getRolesByUser(final User user) throws SecurityException {
        if (user instanceof TransientUser) {
            Serializable userObject = ((TransientUser) user).getUserObject();
            if (userObject instanceof SessionUser) {
                return getRolesByUser((SessionUser)userObject);
            }
        }
        return getRolesByUsername(user.getName());
    }

    /**
     * @param user the user to get the roles for
     * @return list of roles
     * @throws SecurityException in case something goes wrong
     */
    public Set<Role> getRolesByUser(final SessionUser user) throws SecurityException {
        if (requiredUserRole != null && !user.getUserRoles().contains(requiredUserRole)) {
            throw new SecurityException("Access denied");
        }

        final HashSet<Role> roles = user.getUserRoles().stream()
                // filter out excluded prefixes, if not empty
                .filter(r -> excludedUserRolePrefixesList.isEmpty() || excludedUserRolePrefixesList.stream().noneMatch(r::startsWith))
                // and those not having the userrole prefix
                .filter((r -> r.startsWith(includedUserRolePrefix)))
                // strip the userrole prefix if needed
                .map(r -> stripIncludedUserRolePrefix ? r.substring(includedUserRolePrefix.length()) : r)
                // prepend the role prefix
                .map(r -> rolePrefix + r)
                // make them a Role
                .map(TransientRole::new).collect(Collectors.toCollection(HashSet::new));
        // add the defaultRoleName if needed (will be rejected by the set if already added)
        if (defaultRoleName != null) {
            roles.add(new TransientRole(defaultRoleName));
        }
        return roles;
    }
}
