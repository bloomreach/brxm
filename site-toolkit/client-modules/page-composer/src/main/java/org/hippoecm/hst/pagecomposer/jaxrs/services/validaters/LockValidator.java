/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services.validaters;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstConfigurationUtils;
import org.hippoecm.repository.util.NodeIterable;

public class LockValidator implements Validator {


    public enum Operation {
        CREATE, DELETE, MOVE, UPDATE
    }

    private final String id;
    private final Operation operation;
    private String itemNodeType;
    private String rootNodeType;

    public LockValidator(final String id,
                         final Operation operation,
                         final String itemNodeType,
                         final String rootNodeType){
        this.id = id;
        this.operation = operation;
        this.itemNodeType = itemNodeType;
        this.rootNodeType = rootNodeType;

    }

    @Override
    public void preValidate() throws RuntimeException {
        try {
            HstRequestContext requestContext = RequestContextProvider.get();
            final Session session = requestContext.getSession();
            final Node node = session.getNodeByIdentifier(id);
            if (!node.isNodeType(itemNodeType)) {
                throw new IllegalArgumentException("Expected node of type '"+itemNodeType+
                        "' but was '"+node.getPrimaryNodeType().getName()+"'");
            }

            // assert not self or ancestor locked
            if (isLockedDeep(node, rootNodeType)) {
                throw new IllegalStateException("'"+node.getPath()+"' is part of a deep lock. Cannot perform '"+operation+"'");
            }

            // id an descendant is locked, only Operation.ADD is allowed
            if (isLockedOperation(node, rootNodeType, operation)) {
                throw new IllegalStateException("'"+operation+"' is not allowed for '"+node.getPath()+"' due to locked descendant");
            }

        } catch (ItemNotFoundException e) {
            throw new IllegalStateException("No repository configuration node for id '"+id+"'");
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException("RepositoryException during pre-validate", e);
        }

    }

    @Override
    public void postValidate() throws RuntimeException {
        try {
            HstRequestContext requestContext = RequestContextProvider.get();
            final Session session = requestContext.getSession();
            session.refresh(false);
            final Node node = session.getNodeByIdentifier(id);
            if (!node.isNodeType(itemNodeType)) {
                throw new IllegalArgumentException("Expected node of type '"+itemNodeType+
                        "' but was '"+node.getPrimaryNodeType().getName()+"'");
            }

            // assert not self or ancestor locked
            if(isLockedDeep(node, rootNodeType)) {
                if(node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
                    // only remove if owned by you : an ancestor has been locked while you acquired the lock
                    String lockedBy = node.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString();
                    if (node.getSession().getUserID().equals(lockedBy)) {
                        node.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).remove();

                        // TODO remove other lock props
                        HstConfigurationUtils.persistChanges(session);
                    }
                }
                throw new IllegalStateException("Node at '"+node.getPath()+"' is part of a deep lock. Performed '"+operation+"' " +
                        "should had failed. Lock for '"+session.getUserID()+"' on '"+node.getPath()+"' is removed.");
            }

            // assert current user has locked the node (or ancestor)
            String lockedBy = getLockedDeepBy(node, rootNodeType);
            if (!node.getSession().getUserID().equals(lockedBy)) {
                throw new IllegalStateException("Node for '"+node.getPath()+"' should be locked by '"+node.getSession().getUserID()+"' but found to be locked" +
                        " by '"+lockedBy+"'.");
            }

        } catch (ItemNotFoundException e) {
            throw new IllegalStateException("No repository sitemap node for id '"+id+"'");
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException("RepositoryException during pre-validate", e);
        }
    }

    /**
     * returns the userID that contains the deep lock or <code>null</code> when no deep lock present
     */
    protected String getLockedDeepBy(final Node node, final String rootNodeType) throws RepositoryException {
        if (node == null) {
            return null;
        }

        if (node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
            String lockedBy = node.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString();
            return lockedBy;
        }
        // no need to check higher than sitemap
        if (node.isNodeType(rootNodeType)) {
            return null;
        }
        return getLockedDeepBy(node.getParent(), rootNodeType);
    }

    protected boolean isLockedDeep(final Node node, String rootNodeType) throws RepositoryException {
        String lockedBy = getLockedDeepBy(node, rootNodeType);
        if (lockedBy == null) {
             return false;
        }
        return lockedBy.equals(node.getSession().getUserID());
    }



    /**
     * @return whether there is a partial lock for <code>node</code> : A node is partial locked if it contains
     * descendants with deep locks. In this case, only {@link Operation#CREATE} is allowed
     */
    protected boolean isLockedOperation(final Node node, final String rootNodeType, final Operation operation) throws RepositoryException {
        if (operation == Operation.CREATE) {
            return false;
        }
        return hasDescendantLock(node);
    }

    protected boolean hasDescendantLock(final Node node) throws RepositoryException {
        if (node.hasProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY)) {
            String lockedBy = node.getProperty(HstNodeTypes.GENERAL_PROPERTY_LOCKED_BY).getString();
            if (!lockedBy.equals(node.getSession())) {
                return true;
            }
        }
        for (Node child : new NodeIterable(node.getNodes())) {
            boolean hasDescendantLock = hasDescendantLock(child);
            if (hasDescendantLock) {
                return true;
            }
        }
        return false;
    }


}
