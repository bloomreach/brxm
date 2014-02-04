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
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.Operation;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstConfigurationUtils;

public class PostLockValidator extends AbstractLockValidator {

    private final String id;
    private final Operation operation;
    private String itemNodeType;
    private String rootNodeType;

    public PostLockValidator(final String id,
                             final Operation operation,
                             final String itemNodeType,
                             final String rootNodeType){
        this.id = id;
        this.operation = operation;
        this.itemNodeType = itemNodeType;
        this.rootNodeType = rootNodeType;

    }

    @Override
    public void validate() throws RuntimeException {
        try {
            HstRequestContext requestContext = RequestContextProvider.get();
            final Session session = requestContext.getSession();
            session.refresh(true);
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


}
