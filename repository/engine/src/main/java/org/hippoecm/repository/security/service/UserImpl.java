/*
 *  Copyright 2013-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.security.service;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.security.group.GroupManager;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.PropertyIterable;
import org.onehippo.repository.security.User;

import com.google.common.collect.ImmutableSet;

import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_EMAIL;
import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_FIRSTNAME;
import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_LASTNAME;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_ACTIVE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_LASTLOGIN;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PASSKEY;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PASSWORD;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PASSWORDLASTMODIFIED;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_SYSTEM;

public final class UserImpl extends AbstractSecurityNodeInfo implements User {

    private static final Set<String> PROTECTED_PROPERTY_NAMES = ImmutableSet.of(
            HIPPO_PASSWORD,
            HIPPO_PASSKEY,
            HIPPO_PASSWORDLASTMODIFIED,
            HIPPO_SYSTEM,
            HIPPO_ACTIVE,
            HIPPO_LASTLOGIN
    );

    private final String id;
    private final HashMap<String, Object> properties = new HashMap<>();
    private Set<String> groupIds;
    private GroupManager groupManager;

    public UserImpl(final Node node, final GroupManager groupManager) throws RepositoryException {
        this.id = NodeNameCodec.decode(node.getName());
        // load and store the String value of all node properties which are:
        // - not multiple value
        // - of type String|Boolean}Date|Double|Long
        // - and not skipped (either to be hidden, or to be loaded with the predefined/interface non-String value below)
        for (Property p : new PropertyIterable(node.getProperties())) {
            if (isInfoProperty(p)) {
                properties.put(p.getName(), p.getString());
            }
        }
        // load and store the non-string type values for predefined/interface properties
        properties.put(HIPPO_SYSTEM, JcrUtils.getBooleanProperty(node, HIPPO_SYSTEM, false));
        properties.put(HIPPO_ACTIVE, JcrUtils.getBooleanProperty(node, HIPPO_ACTIVE, true));
        properties.put(HIPPO_LASTLOGIN, JcrUtils.getDateProperty(node, HIPPO_LASTLOGIN, null));
        this.groupManager = groupManager;
    }

    protected Set<String> getProtectedPropertyNames() {
        return PROTECTED_PROPERTY_NAMES;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isSystemUser() {
        return (Boolean)properties.get(HIPPO_SYSTEM);
    }

    @Override
    public boolean isActive() {
        return (Boolean)properties.get(HIPPO_ACTIVE);
    }

    @Override
    public String getFirstName() {
        return getProperty(HIPPOSYS_FIRSTNAME);
    }

    @Override
    public String getLastName() {
        return getProperty(HIPPOSYS_LASTNAME);
    }

    @Override
    public String getEmail() {
        return getProperty(HIPPOSYS_EMAIL);
    }

    @Override
    public Calendar getLastLogin() {
        return (Calendar)properties.get(HIPPO_LASTLOGIN);
    }

    @Override
    public String getProperty(final String propertyName) {
        Object value = properties.get(propertyName);
        return value instanceof String ? (String)value : null;
    }

    @Override
    public Set<String> getMemberships() {
        if (groupIds == null) {
            if (groupManager != null) {
                try {
                    groupIds = Collections.unmodifiableSet(groupManager.getMembershipIds(id));
                } catch (RepositoryException ignore) {
                    groupIds = Collections.emptySet();
                }
                groupManager = null;
            } else {
                groupIds = Collections.emptySet();
            }
        }
        return groupIds;
    }

    @Override
    public String toString() {
        return "User: " + getId();
    }
}
