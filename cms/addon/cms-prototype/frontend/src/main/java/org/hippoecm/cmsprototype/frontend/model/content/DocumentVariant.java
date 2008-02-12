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

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.hippoecm.cmsprototype.frontend.model.exception.ModelWrapException;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A variant of a document. The type of variation can be anything, e.g. language
 * or workflow state.
 * 
 * A DocumentVariant's JCR node is of node type "hippo:document", and typically has
 * a node of type "hippo:handle" as an ancestor (the {@link Document}). 
 *
 */
public class DocumentVariant extends NodeModelWrapper {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(DocumentVariant.class);

    // Default labels
    // TODO: needs i18n
    public static final String NO_STATE = "no workflow";
    private static final String NO_LANGUAGE = "all languages";

    public DocumentVariant(JcrNodeModel nodeModel) throws ModelWrapException {
        super(nodeModel);
        try {
            if (nodeModel.getNode().isNodeType(HippoNodeType.NT_REQUEST)) {
                // find document variant associated with request object
                Session session = nodeModel.getNode().getSession();
                if (nodeModel.getNode().hasProperty("document")) {
                    String docUUID = nodeModel.getNode().getProperty("document").getString();
                    setChainedModel(new JcrNodeModel(session.getNodeByUUID(docUUID)));
                } else {
                    throw new ModelWrapException("Request object has no document associated.");
                }
            } else if (!nodeModel.getNode().isNodeType(HippoNodeType.NT_DOCUMENT)) {
                throw new ModelWrapException("Node is not a document variant.");
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

    public String getState() {
        String result;
        try {
            if (nodeModel.getNode().hasProperty("hippostd:state")) {
                result = nodeModel.getNode().getProperty("hippostd:state").getString();
            } else {
                result = NO_STATE;
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
            result = e.getMessage();
        }
        return result;
    }

    public String getLanguage() {
        String result;
        try {
            if (nodeModel.getNode().hasProperty("hippostd:language")) {
                result = nodeModel.getNode().getProperty("hippostd:language").getString();
            } else {
                result = NO_LANGUAGE;
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
            result = e.getMessage();
        }
        return result;
    }

    /**
     * @return The Document this DocumentVariant is a variant of, or null if not found.
     */
    public Document getDocument() {
        try {
            return new Document(nodeModel);
        } catch (ModelWrapException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof DocumentVariant == false) {
            return false;
        }
        if (this == object) {
            return true;
        }
        DocumentVariant variant = (DocumentVariant) object;
        return new EqualsBuilder().append(nodeModel, variant.nodeModel).isEquals();
    }

}
