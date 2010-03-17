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
package org.hippoecm.frontend.editor;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DefaultCmsEditor extends AbstractCmsEditor<Node> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Logger log = LoggerFactory.getLogger(DefaultCmsEditor.class);

    DefaultCmsEditor(IEditorContext manager, IPluginContext context, IPluginConfig config, IModel<Node> model, Mode mode)
            throws EditorException {
        super(manager, context, config, model, mode);

        Node node = model.getObject();
        if (node != null) {
            try {
                if (node.isNodeType("nt:version") && Mode.EDIT == mode) {
                    throw new EditorException("Cannot edit version");
                }
            } catch (RepositoryException e) {
                throw new EditorException("Error determining node type", e);
            }
        }
    }

    @Override
    protected IModel<Node> getEditorModel() throws EditorException {
        IModel<Node> model = super.getEditorModel();
        try {
            Node node = model.getObject();
            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                if (node.hasNode(node.getName())) {
                    return new JcrNodeModel(node.getNode(node.getName()));
                } else {
                    return null;
                }
            } else if (node.isNodeType("nt:version")) {
                return new JcrNodeModel(node.getNode("jcr:frozenNode"));
            } else {
                return model;
            }
        } catch (RepositoryException ex) {
            throw new EditorException("cannot obtain proper editable document from handle", ex);
        }
    }

    @Override
    public void refresh() {
        JcrNodeModel nodeModel = (JcrNodeModel) getModel();

        // close editor if model no longer exists
        if (!nodeModel.getItemModel().exists()) {
            try {
                close();
            } catch (EditorException ex) {
                log.warn("failed to close editor for non-existing document");
            }
        }
    }

}
