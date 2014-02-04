/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services.helpers;

import java.util.Calendar;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import com.google.common.collect.Iterables;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMENUITEM_PROPERTY_ROLES;

public abstract class AbstractHelper {

    private static final Logger log = LoggerFactory.getLogger(AbstractHelper.class);

    /**
     * @return the configuration object for <code>id</code> and <code>null</code> if not existing
     */
    public abstract <T> T getConfigObject(String id);

    protected void removeProperty(Node node, String name) throws RepositoryException {
        if (node.hasProperty(name)) {
            node.getProperty(name).remove();
        }
    }

    /**
     * if the <code>node</code> is already locked for the user <code>node.getSession()</code>, either explicitly or implicitly
     * through an ancestor, this method does not do anything. If there is no lock yet, a lock for the current session
     * userID gets set on the node except another user already has a lock, explicit or implicit
     */
    // TODO check lastModifiedTimestamp
    protected void acquireLock(final Node node) throws RepositoryException {
        if (isLockPresent(node)) {
            return;
        }
        node.addMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
        final Session session = node.getSession();
        log.info("Node '{}' gets a lock for user '{}'.", node.getPath(), session.getUserID());
        node.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY, session.getUserID());
        Calendar now = Calendar.getInstance();
        if (!node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON)) {
            node.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON, now);
        }
        node.setProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED_BY, session.getUserID());
        // remove all present descendant locks for current user.
        removeDescendantLocks(new NodeIterable(node.getNodes()));
    }

    private void removeDescendantLocks(final NodeIterable nodes) throws RepositoryException {
        for (Node node : nodes) {
            removeProperty(node, HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY);
            removeProperty(node, HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON);
        }
    }

    private boolean isLockPresent(final Node node) throws RepositoryException {
        if(node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON)) {
            String lockedBy = node.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON).getString();
            if (!node.getSession().getUserID().equals(lockedBy)) {
                throw new IllegalStateException("Locked by someone else");
            }
            return true;
        }

        if (node.isNodeType(HstNodeTypes.NODETYPE_HST_SITEMAP)) {
            return false;
        }
        return isLockPresent(node.getParent());
    }

    protected void setLocalParameters(final Node node, final Map<String, String> modifiedLocalParameters) throws RepositoryException {
        if (modifiedLocalParameters != null && !modifiedLocalParameters.isEmpty()) {
            final String[][] namesAndValues = mapToNameValueArrays(modifiedLocalParameters);
            node.setProperty(GENERAL_PROPERTY_PARAMETER_NAMES, namesAndValues[0], PropertyType.STRING);
            node.setProperty(GENERAL_PROPERTY_PARAMETER_VALUES, namesAndValues[1], PropertyType.STRING);
        } else if (modifiedLocalParameters != null && modifiedLocalParameters.isEmpty()) {
            removeProperty(node, GENERAL_PROPERTY_PARAMETER_NAMES);
            removeProperty(node, GENERAL_PROPERTY_PARAMETER_VALUES);
        }

    }

    protected void setRoles(final Node node, final Set<String> modifiedRoles) throws RepositoryException {
        if (modifiedRoles != null && !modifiedRoles.isEmpty()) {
            final String[] roles = Iterables.toArray(modifiedRoles, String.class);
            node.setProperty(SITEMENUITEM_PROPERTY_ROLES, roles, PropertyType.STRING);
        } else if (modifiedRoles != null && modifiedRoles.isEmpty()) {
            removeProperty(node, SITEMENUITEM_PROPERTY_ROLES);
        }
    }

    private String[][] mapToNameValueArrays(final Map<String, String> map) {
        final int size = map.size();
        final String[][] namesAndValues = {
                map.keySet().toArray(new String[size]),
                new String[size]
        };
        for (int i = 0; i < size; i++) {
            namesAndValues[1][i] = map.get(namesAndValues[0][i]);
        }
        return namesAndValues;
    }

}
