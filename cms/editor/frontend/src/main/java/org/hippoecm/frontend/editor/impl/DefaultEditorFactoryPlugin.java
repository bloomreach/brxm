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
package org.hippoecm.frontend.editor.impl;

import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.AbstractCmsEditor;
import org.hippoecm.frontend.editor.EditorHelper;
import org.hippoecm.frontend.editor.IEditorContext;
import org.hippoecm.frontend.editor.IEditorFactory;
import org.hippoecm.frontend.model.JcrHelper;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.repository.api.HippoNodeType;

/**
 * Factory for Node {@link IEditor}s.  Use this class to create an editor for any Node.
 * <p>
 * This will likely be turned into a service at some point.  
 */
public final class DefaultEditorFactoryPlugin extends Plugin implements IEditorFactory {

    private static final long serialVersionUID = 1L;

    public DefaultEditorFactoryPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        context.registerService(this, config.getString(IEditorFactory.SERVICE_ID, IEditorFactory.class.getName()));
    }

    public IEditor<Node> newEditor(IEditorContext manager, IModel<Node> nodeModel, IEditor.Mode mode,
            IPluginConfig parameters) throws EditorException {
        Node node = nodeModel.getObject();
        AbstractCmsEditor<Node> editor;
        try {
            if (JcrHelper.isNodeType(node, HippoNodeType.NT_HANDLE)) {
                Set<Node> docs = EditorHelper.getDocuments(node);
                if (docs.size() == 0) {
                    throw new EditorException("Document has been deleted");
                }
                Node doc = docs.iterator().next();
                editor = newEditor(manager, nodeModel, mode, doc, parameters);
            } else {
                editor = newEditor(manager, nodeModel, mode, node, parameters);
            }
        } catch (RepositoryException e) {
            throw new EditorException("Could not determine type of editor required", e);
        }
        editor.start();
        return editor;
    }

    private AbstractCmsEditor<Node> newEditor(IEditorContext manager, IModel<Node> nodeModel, IEditor.Mode mode,
            Node node, IPluginConfig parameters) throws RepositoryException, EditorException {
        if (node.isNodeType(HippoNodeType.NT_TEMPLATETYPE)) {
            return new TemplateTypeEditor(manager, getPluginContext(), parameters, nodeModel, mode);
        } else {
            return new DefaultCmsEditor(manager, getPluginContext(), parameters, nodeModel, mode);
        }
    }

}
