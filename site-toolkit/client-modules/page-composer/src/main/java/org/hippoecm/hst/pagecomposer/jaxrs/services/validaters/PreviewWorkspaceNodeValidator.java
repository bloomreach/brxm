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

import java.util.UUID;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.core.request.HstRequestContext;

public class PreviewWorkspaceNodeValidator implements Validator {

    final String id;
    final String requiredNodeType;

    public PreviewWorkspaceNodeValidator(final String id){
        this(id, null);
    }

    public PreviewWorkspaceNodeValidator(final String id, final String requiredNodeType){
        this.id = id;
        this.requiredNodeType = requiredNodeType;
    }

    @Override
    public void validate() throws RuntimeException {
        try {
            HstRequestContext requestContext = RequestContextProvider.get();
            final Session session = requestContext.getSession();
            try {
                UUID.fromString(id);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Not a valid uuid '"+id+"'");
            }
            final Node node = session.getNodeByIdentifier(id);
            if (requiredNodeType != null && !node.isNodeType(requiredNodeType)) {
                throw new IllegalArgumentException("Required node of type '"+requiredNodeType+"' but node '"+node.getPath()+"' of " +
                        "type '"+node.getPrimaryNodeType().getName()+"' found." );
            }

            if (!isPreviewWorkspaceNode(node)) {
                throw new IllegalArgumentException("Required workspace node but '"+node.getPath()+"' is not part of hst:workspace");
            }

        } catch (ItemNotFoundException e) {
            throw new IllegalStateException("No repository configuration node for id '"+id+"'");
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException("RepositoryException during pre-validate", e);
        }

    }

    private boolean isPreviewWorkspaceNode(final Node node) throws RepositoryException {
        Node cr = node;
        Node root = cr.getSession().getRootNode();
        while (!cr.isSame(root)) {
            if (cr.isNodeType(HstNodeTypes.NODETYPE_HST_WORKSPACE) && cr.getParent().getName().endsWith("-preview")) {
                return true;
            }
            cr = cr.getParent();
        }
        return false;
    }

}
