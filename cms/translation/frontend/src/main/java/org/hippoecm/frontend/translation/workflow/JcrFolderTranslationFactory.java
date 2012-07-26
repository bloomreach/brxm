/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.translation.workflow;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.translation.components.document.FolderTranslation;
import org.hippoecm.repository.api.HippoNodeType;

public final class JcrFolderTranslationFactory {

    private JcrFolderTranslationFactory() {
    }

    public static FolderTranslation createFolderTranslation(Node original, Node node) throws RepositoryException {
        if (original == null && node == null) {
            throw new IllegalArgumentException("Both source and target folders are null");
        }

        Node reference;
        if (original == null) {
            reference = node;
        } else {
            reference = original;
        }
        FolderTranslation ft = new FolderTranslation(reference.getIdentifier());
        if (reference.isNodeType(HippoNodeType.NT_DOCUMENT)) {
            ft.setType("folder");
        } else {
            ft.setType("doc");
        }

        if (original != null) {
            ft.setUrl(original.getName());
            ft.setName(new NodeTranslator(new JcrNodeModel(original)).getNodeName().getObject());
        }

        if (node != null) {
            ft.setUrlfr(node.getName());
            ft.setNamefr(new NodeTranslator(new JcrNodeModel(node)).getNodeName().getObject());
        }

        return ft;
    }

}
