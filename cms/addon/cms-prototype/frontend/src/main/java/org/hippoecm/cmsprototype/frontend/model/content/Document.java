/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.cmsprototype.frontend.model.content;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.hippoecm.cmsprototype.frontend.model.exception.ModelWrapException;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A document in the repository, possibly containing one or more document
 * variants. This model class wraps the {@link JcrNodeModel} representing the
 * document's node in the JCR repository.
 * 
 * A Document's JCR node is of node type "hippo:handle". If instantiated with a
 * node of any other type, the handle to which the node belongs will be
 * searched for. A {@link ModelWrapException} is thrown in case the handle is not found.
 *
 */
public class Document extends NodeModelWrapper {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(Document.class);

    public Document(JcrNodeModel nodeModel) throws ModelWrapException {
        super(nodeModel);
        try {
            if (!nodeModel.getNode().isNodeType(HippoNodeType.NT_HANDLE)) {
                JcrNodeModel handle = findHandle(nodeModel);
                if (handle != null) {
                    this.nodeModel = handle;
                }
                else {
                    throw new ModelWrapException("Node is not a handle, and does not descend from a handle.");
                }
            }
        } catch (RepositoryException e) {
            throw new ModelWrapException(e);
        }
    }

    public String getName() {
        try {
            return nodeModel.getNode().getDisplayName();
        } catch (RepositoryException e) {
            log.error(e.getMessage());
            return e.getMessage();
        }
    }


    public List<DocumentVariant> getVariants() {
        List<DocumentVariant> list = new ArrayList<DocumentVariant>();
        try {
            for (NodeIterator iter = nodeModel.getNode().getNodes(); iter.hasNext();) {
                HippoNode docNode = (HippoNode) iter.next();
                if (docNode.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                    list.add(new DocumentVariant(new JcrNodeModel(docNode)));
                }
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        } catch (ModelWrapException e) {
            log.error(e.getMessage());
        }
        return list;
    }
    
    
    /**
     * Finds the handle to which nodeModel belongs.
     * @param   model   Any JcrNodeModel
     * @return  nodeModel's first ancestor-or-self of type "hippo:handle", or null if not found
     */
    private JcrNodeModel findHandle(JcrNodeModel model) {
        try {
            while (model != null && !model.getNode().isNodeType(HippoNodeType.NT_HANDLE)) {
                model = model.getParentModel();
            }
            return model;
        } catch (RepositoryException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Document == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        Document variant = (Document) object;
        return new EqualsBuilder().append(nodeModel, variant.nodeModel).isEquals();
    }

}
