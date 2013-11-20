/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.reviewedactions;

import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.AbstractCmsEditor;
import org.hippoecm.frontend.editor.EditorHelper;
import org.hippoecm.frontend.editor.HippostdPublishableEditor;
import org.hippoecm.frontend.editor.IEditorContext;
import org.hippoecm.frontend.editor.IEditorFactory;
import org.hippoecm.frontend.model.JcrHelper;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.update.NodeUpdateVisitor;
import org.onehippo.repository.update.UpdaterRegistry;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HippostdEditorFactoryPlugin extends Plugin implements IEditorFactory {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(HippostdEditorFactoryPlugin.class);

    public HippostdEditorFactoryPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        context.registerService(this, config.getString(IEditorFactory.SERVICE_ID, IEditorFactory.class.getName()));
    }

    @Override
    public AbstractCmsEditor<Node> newEditor(IEditorContext manager, IModel<Node> nodeModel, IEditor.Mode mode, IPluginConfig parameters)
            throws EditorException {
        Node node = nodeModel.getObject();
        try {
            if (JcrHelper.isNodeType(node, HippoNodeType.NT_HANDLE)) {
                update(node);
                Set<Node> docs = EditorHelper.getDocuments(node);
                if (docs.size() == 0) {
                    throw new EditorException("Document has been deleted");
                }
                Node doc = docs.iterator().next();
                if (JcrHelper.isNodeType(doc, HippoStdNodeType.NT_PUBLISHABLE)) {
                    HippostdPublishableEditor editor = new HippostdPublishableEditor(manager, getPluginContext(),
                            parameters, nodeModel);
                    editor.start();
                    return editor;
                }
            } else if (node.isNodeType(JcrConstants.NT_VERSION) && JcrHelper.isNodeType(node, HippoStdNodeType.NT_PUBLISHABLE)) {
                HippostdPublishableEditor editor = new HippostdPublishableEditor(manager, getPluginContext(),
                        parameters, new JcrNodeModel(node));
                editor.start();
                return editor;
            }
        } catch (RepositoryException ex) {
            log.error("Error creating editor for document");
        }
        return null;
    }

    private void update(final Node item) {
        final UpdaterRegistry updaterRegistry = HippoServiceRegistry.getService(UpdaterRegistry.class);
        if (updaterRegistry != null) {
            try {
                final List<NodeUpdateVisitor> updaters = updaterRegistry.getUpdaters(item);
                for (NodeUpdateVisitor updater : updaters) {
                    try {
                        updater.doUpdate(item);
                    } finally {
                        updater.destroy();
                    }
                }
            } catch (RepositoryException e) {
                log.error("Error updating document", e);
            }
        }
    }
}
