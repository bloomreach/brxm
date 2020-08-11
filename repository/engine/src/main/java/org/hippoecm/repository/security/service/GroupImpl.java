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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.NodeNameCodec;
import org.hippoecm.repository.security.group.GroupManager;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.PropertyIterable;
import org.onehippo.repository.security.Group;

import com.google.common.collect.ImmutableSet;

import static org.hippoecm.repository.api.HippoNodeType.HIPPOSYS_DESCRIPTION;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_SYSTEM;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_USERROLES;
import static org.hippoecm.repository.api.HippoNodeType.NT_EXTERNALGROUP;

public final class GroupImpl extends AbstractSecurityNodeInfo implements Group {

    private static final Set<String> PROTECTED_PROPERTY_NAMES = ImmutableSet.of(HIPPO_SYSTEM);

    private final String id;
    private final boolean external;
    private final HashMap<String, Object> properties = new HashMap<>();
    private Set<String> userIds;
    private final Set<String> userRoles;

    GroupImpl(final Node node, final GroupManager groupManager) throws RepositoryException {
        this.id = NodeNameCodec.decode(node.getName());
        this.external = node.isNodeType(NT_EXTERNALGROUP);

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
        userIds = Collections.unmodifiableSet(groupManager.getMembers(node));
        userRoles = Collections.unmodifiableSet(new HashSet<>(JcrUtils.getStringListProperty(node, HIPPO_USERROLES, Collections.emptyList())));
    }

    protected Set<String> getProtectedPropertyNames() {
        return PROTECTED_PROPERTY_NAMES;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDescription() {
        return getProperty(HIPPOSYS_DESCRIPTION);
    }

    @Override
    public boolean isSystemGroup() {
        return (Boolean)properties.get(HIPPO_SYSTEM);
    }

    @Override
    public boolean isExternal() {
        return external;
    }

    @Override
    public Set<String> getMembers() {
        return userIds;
    }

    @Override
    public Set<String> getUserRoles() {
        return userRoles;
    }

    @Override
    public String getProperty(final String propertyName) {
        Object value = properties.get(propertyName);
        return value instanceof String ? (String)value : null;
    }

    @Override
    public String toString() {
        return "Group: " + getId();
    }

}
