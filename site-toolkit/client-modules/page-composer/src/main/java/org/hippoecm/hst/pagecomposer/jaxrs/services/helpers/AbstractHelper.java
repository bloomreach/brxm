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

import org.apache.commons.lang.StringUtils;
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
     * if the <code>node</code> is already locked for the user <code>node.getSession()</code> this method does not do anything.
     * If there is no lock yet, a lock for the current session userID gets set on the node. If there is already a lock by another
     * user a IllegalStateException is thrown,
     */
    public void acquireLock(final Node node) throws RepositoryException {
        if (hasSelfOrDescendantLockBySomeOneElse(node)) {
            throw new IllegalStateException("Node '"+node.getPath()+"' cannot be locked due to a descendant locked node by " +
                    "someone else.");
        }
        String selfOrAncestorLockedBy = getSelfOrAncestorLockedBy(node);
        if (selfOrAncestorLockedBy != null) {
            if (selfOrAncestorLockedBy.equals(node.getSession().getUserID())) {
                log.debug("Node '{}' already locked", node.getSession().getUserID());
                return;
            }
            throw new IllegalStateException("Node '"+node.getPath()+"' cannot be locked due to an ancestor locked node by " +
                    "someone else.");
        }

        if (!node.isNodeType(HstNodeTypes.MIXINTYPE_HST_EDITABLE)) {
            node.addMixin(HstNodeTypes.MIXINTYPE_HST_EDITABLE);
        }

        final Session session = node.getSession();
        log.info("Node '{}' gets a lock for user '{}'.", node.getPath(), session.getUserID());
        node.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY, session.getUserID());
        Calendar now = Calendar.getInstance();
        if (!node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON)) {
            node.setProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_ON, now);
        }
        node.setProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED_BY, session.getUserID());
    }


    protected void setProperty(final Node jcrNode, final String propName, final String propValue) throws RepositoryException {
        if (StringUtils.isEmpty(propValue)) {
            removeProperty(jcrNode, propName);
        } else {
            jcrNode.setProperty(propName, propValue);
        }
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

    /**
     * returns the userID that contains the deep lock or <code>null</code> when no deep lock present
     */
    protected String getSelfOrAncestorLockedBy(final Node node) throws RepositoryException {
        if (node == null || node.isSame(node.getSession().getRootNode())) {
            return null;
        }

        if (node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
            String lockedBy = node.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString();
            return lockedBy;
        }

        if (node.isNodeType(HstNodeTypes.NODETYPE_HST_WORKSPACE)) {
            return null;
        }
        return getSelfOrAncestorLockedBy(node.getParent());
    }

    protected boolean hasSelfOrAncestorLockBySomeOneElse(final Node node) throws RepositoryException {
        String selfOrAncestorLockedBy = getSelfOrAncestorLockedBy(node);
        if (selfOrAncestorLockedBy == null) {
            return false;
        }
        return node.getSession().getUserID().equals(selfOrAncestorLockedBy);
    }

    protected boolean hasSelfOrDescendantLockBySomeOneElse(final Node node) throws RepositoryException {
        if (node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
            String lockedBy = node.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString();
            if (!lockedBy.equals(node.getSession().getUserID())) {
                return true;
            }
        }
        for (Node child : new NodeIterable(node.getNodes())) {
            boolean hasDescendantLock = hasSelfOrDescendantLockBySomeOneElse(child);
            if (hasDescendantLock) {
                return true;
            }
        }
        return false;
    }

}
