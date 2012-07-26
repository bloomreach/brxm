/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.editor.impl;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.editor.model.JcrTypeInfo;
import org.hippoecm.editor.model.JcrTypeVersion;
import org.hippoecm.frontend.editor.AbstractCmsEditor;
import org.hippoecm.frontend.editor.IEditorContext;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TemplateTypeEditor extends AbstractCmsEditor<Node> {
    private static final long serialVersionUID = 1L;


    private static final Logger log = LoggerFactory.getLogger(TemplateTypeEditor.class);

    private JcrNodeModel nodeTypeModel;

    TemplateTypeEditor(IEditorContext manager, IPluginContext context, IPluginConfig config, IModel<Node> model,
            Mode mode) throws EditorException {
        super(manager, context, config, model, mode);
    }

    @Override
    public void start() throws EditorException {
        Node node = getModel().getObject();
        if (node != null) {
            try {
                JcrTypeInfo info = new JcrTypeInfo(node);
                JcrTypeVersion draft = info.getDraft();
                this.nodeTypeModel = new JcrNodeModel(draft.getTypeNode());
            } catch (RepositoryException e) {
                throw new EditorException("Could not find draft to display");
            }
        }
        super.start();
    }

    @Override
    public void refresh() {
        Node node = getModel().getObject();
        if (node == null) {
            try {
                close();
            } catch (EditorException e) {
                log.error("Could not close editor for null node", e);
            }
            return;
        }
        try {
            JcrTypeInfo info = new JcrTypeInfo(node);
            JcrTypeVersion draft = info.getDraft();
            JcrNodeModel draftModel = new JcrNodeModel(draft.getTypeNode());
            if (!draftModel.equals(nodeTypeModel)) {
                stop();

                nodeTypeModel = null;
                start();
                return;
            }

            if (node.hasNode(HippoNodeType.HIPPO_PROTOTYPES)) {
                NodeIterator prototypes = node.getNode(HippoNodeType.HIPPO_PROTOTYPES).getNodes();
                while (prototypes.hasNext()) {
                    Node prototype = prototypes.nextNode();
                    if (prototype.isNodeType("nt:unstructured")) {
                        // any mode is fine
                        return;
                    }
                }

                // no draft present
                setMode(Mode.VIEW);
            }
        } catch (EditorException e) {
            log.warn("Could not set editor mode", e);
        } catch (RepositoryException e) {
            log.error("Error determining node type", e);
        }
    }

    @Override
    public void detach() {
        if (nodeTypeModel != null) {
            nodeTypeModel.detach();
        }
        super.detach();
    }
}
