/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.cms.edit;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IRefreshable;
import org.hippoecm.frontend.plugin.IPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorManagerPlugin implements IPlugin, IEditorManager, IRefreshable, IDetachable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(EditorManagerPlugin.class);

    private EditorFactory editorFactory;
    private BrowserObserver browser;

    private List<AbstractCmsEditor<JcrNodeModel>> editors;
    transient boolean active = false;

    public EditorManagerPlugin(final IPluginContext context, final IPluginConfig config) {
        editorFactory = new EditorFactory(this, context, config);
        browser = new BrowserObserver(this, context, config);

        editors = new LinkedList<AbstractCmsEditor<JcrNodeModel>>();
        context.registerService(this, IRefreshable.class.getName());

        // register editor
        context.registerService(this, config.getString("editor.id"));
    }

    public void detach() {
        browser.detach();
        for (AbstractCmsEditor<JcrNodeModel> editor : editors) {
            editor.detach();
        }
    }

    @SuppressWarnings("unchecked")
    public AbstractCmsEditor<JcrNodeModel> getEditor(IModel model) {
        if (model instanceof JcrNodeModel) {
            JcrNodeModel editModel = getEditorModel((JcrNodeModel) model);
            for (IEditor editor : editors) {
                if (editor.getModel().equals(editModel)) {
                    return (AbstractCmsEditor<JcrNodeModel>) editor;
                }
            }
        } else {
            log.warn("Unknown model type", model);
        }
        return null;
    }

    public AbstractCmsEditor<JcrNodeModel> openEditor(IModel model) throws ServiceException {
        if (active) {
            throw new ServiceException("Cannot create editors recursively");
        }

        JcrNodeModel nodeModel = getEditorModel((JcrNodeModel) model);

        checkEditorDoesNotExist(nodeModel);

        try {
            active = true;

            return createEditor(nodeModel, IEditor.Mode.EDIT);
        } finally {
            active = false;
        }
    }

    public AbstractCmsEditor<JcrNodeModel> openPreview(IModel model) throws ServiceException {
        JcrNodeModel nodeModel = getEditorModel((JcrNodeModel) model);

        checkEditorDoesNotExist(nodeModel);

        return createEditor(nodeModel, IEditor.Mode.VIEW);
    }

    protected void checkEditorDoesNotExist(IModel model) throws ServiceException {
        for (IEditor editor : editors) {
            if (editor.getModel().equals(model)) {
                throw new ServiceException("editor already exists");
            }
        }
    }

    protected AbstractCmsEditor<JcrNodeModel> createEditor(JcrNodeModel model, IEditor.Mode mode) throws ServiceException {
        try {
            AbstractCmsEditor<JcrNodeModel> editor = editorFactory.newEditor(model, mode);
            editor.start();
    
            editors.add(editor);
            editor.focus();
    
            onFocus(editor);
            return editor;
        } catch (CmsEditorException ex) {
            log.error(ex.getMessage());
            throw new ServiceException("Initialization failed", ex);
        }
    }

    // validate existence of all open documents
    public void refresh() {
        active = true;
        try {
            List<AbstractCmsEditor<JcrNodeModel>> copy = new LinkedList<AbstractCmsEditor<JcrNodeModel>>(editors);
            Iterator<AbstractCmsEditor<JcrNodeModel>> iter = copy.iterator();
            while (iter.hasNext()) {
                AbstractCmsEditor<JcrNodeModel> editor = iter.next();
                editor.refresh();
            }
        } finally {
            active = false;
        }
    }

    // callback methods for editor events

    void onFocus(AbstractCmsEditor<?> editor) {
        if (editor.getModel() instanceof JcrNodeModel) {
            JcrNodeModel nodeModel = (JcrNodeModel) editor.getModel();
            browser.setModel(nodeModel);
        }
    }

    void onClose(AbstractCmsEditor<?> editor) {
        if (editor.getModel() instanceof JcrNodeModel) {
            JcrNodeModel model = (JcrNodeModel) editor.getModel();
            if (model != null) {
                // cleanup internals
                editors.remove(editor);
                if (editors.size() == 0 && model.equals(browser.getModel())) {
                    browser.setModel(new JcrNodeModel((Node) null));
                }
            }
        }
    }

    // internal

    private JcrNodeModel getEditorModel(JcrNodeModel nodeModel) {
        Node node = nodeModel.getNode();
        try {
            if (node.getDepth() > 0 && node.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                return new JcrNodeModel(node.getParent());
            }
        } catch (RepositoryException ex) {
            log.error("error resolving editor model", ex);
        }
        return nodeModel;
    }
}
