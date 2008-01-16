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

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.NodeModelWrapper;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentVariant extends NodeModelWrapper {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(DocumentVariant.class);

    // Default labels
    // TODO: needs i18n
    private static final String NO_STATE = "no workflow";
    private static final String NO_LANGUAGE = "all languages";

    public DocumentVariant(JcrNodeModel nodeModel) {
        super(nodeModel);
    }

    public String getName() {
        try {
            return nodeModel.getNode().getDisplayName();
        } catch (RepositoryException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    public String getState() {
        try {
            if (nodeModel.getNode().hasProperty(HippoNodeType.HIPPO_STATE)) {
                return nodeModel.getNode().getProperty(HippoNodeType.HIPPO_STATE).getString();
            }
            else {
                return NO_STATE;
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
            return e.getMessage();
        }
    }

    public String getLanguage() {
        try {
            if (nodeModel.getNode().hasProperty(HippoNodeType.HIPPO_LANGUAGE)) {
                return nodeModel.getNode().getProperty(HippoNodeType.HIPPO_LANGUAGE).getString();
            }
            else {
                return NO_LANGUAGE;
            }
        } catch (RepositoryException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }
    
    public Document getDocument() {
        JcrNodeModel parentModel = nodeModel.getParentModel();
        if (parentModel != null) {
            return new Document(parentModel);
        }
        else {
            return null;
        }
    }
}
