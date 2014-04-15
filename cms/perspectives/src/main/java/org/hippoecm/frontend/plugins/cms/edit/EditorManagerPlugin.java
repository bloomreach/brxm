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
package org.hippoecm.frontend.plugins.cms.edit;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.editor.IEditorContext;
import org.hippoecm.frontend.editor.IEditorFactory;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IRefreshable;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditor.Mode;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.service.ServiceException;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorManagerPlugin extends Plugin implements IEditorManager, IRefreshable, IDetachable {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(EditorManagerPlugin.class);

    private IEditorFactory editorFactory;
    private BrowserObserver browser;

    private List<IEditor<Node>> editors;
    private transient boolean active = false;

    public EditorManagerPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        editorFactory = createEditorFactory(context, config);
        browser = new BrowserObserver(this, context, config);

        editors = new LinkedList<IEditor<Node>>();
        context.registerService(this, IRefreshable.class.getName());

        // register editor
        context.registerService(this, config.getString("editor.id"));
    }

    /**
     * create an editor factory that delegates to registered factories.
     * The returned editor factory behaves different from the interface: it will throw
     * an exception when no editor can be created. 
     */
    private IEditorFactory createEditorFactory(final IPluginContext context, final IPluginConfig config) {
        return new IEditorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public IEditor<Node> newEditor(IEditorContext manager, IModel<Node> nodeModel, Mode mode, IPluginConfig parameters)
                    throws EditorException {
                List<IEditorFactory> upstream = context.getServices(config.getString(IEditorFactory.SERVICE_ID,
                        IEditorFactory.class.getName()), IEditorFactory.class);
                for (ListIterator<IEditorFactory> iter = upstream.listIterator(upstream.size()); iter.hasPrevious();) {
                    IEditorFactory factory = iter.previous();
                    IEditor<Node> editor = factory.newEditor(manager, nodeModel, mode, parameters);
                    if (editor != null) {
                        return editor;
                    }
                }
                throw new EditorException("Could not find factory willing to create an editor");
            }

        };
    }

    public void detach() {
        browser.detach();
        for (IEditor<Node> editor : editors) {
            if (editor instanceof IDetachable) {
                ((IDetachable) editor).detach();
            }
        }
    }

    public IEditor<Node> getEditor(IModel<Node> model) {
        if (model instanceof JcrNodeModel) {
            JcrNodeModel editModel = getEditorModel((JcrNodeModel) model);
            for (IEditor<Node> editor : editors) {
                if (editor.getModel().equals(editModel)) {
                    return editor;
                }
            }
        } else {
            log.warn("Unknown model type", model);
        }
        return null;
    }

    public IEditor<Node> openEditor(IModel<Node> model) throws ServiceException {
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

    public IEditor<Node> openPreview(IModel<Node> model) throws ServiceException {
        JcrNodeModel nodeModel = getEditorModel((JcrNodeModel) model);

        checkEditorDoesNotExist(nodeModel);

        return createEditor(nodeModel, IEditor.Mode.VIEW);
    }

    protected void checkEditorDoesNotExist(IModel<Node> model) throws ServiceException {
        for (IEditor editor : editors) {
            if (editor.getModel().equals(model)) {
                throw new ServiceException("editor already exists");
            }
        }
    }

    protected IEditor<Node> createEditor(final IModel<Node> model, IEditor.Mode mode) throws ServiceException {
        try {
            IEditor<Node> editor = editorFactory.newEditor(new IEditorContext() {

                public IEditorManager getEditorManager() {
                    return EditorManagerPlugin.this;
                }

                public void onClose() {
                    EditorManagerPlugin.this.onClose(model);
                }

                public void onFocus() {
                    EditorManagerPlugin.this.onFocus(model);
                }

            }, model, mode, getPluginConfig().getPluginConfig("cluster.options"));

            editors.add(editor);
            editor.focus();

            focusBrowser(model);
            return editor;
        } catch (EditorException ex) {
            log.error(ex.getMessage());
            throw new ServiceException("Initialization failed", ex);
        }
    }

    // validate existence of all open documents
    public void refresh() {
        active = true;
        try {
            List<IEditor<Node>> copy = new LinkedList<IEditor<Node>>(editors);
            Iterator<IEditor<Node>> iter = copy.iterator();
            while (iter.hasNext()) {
                IEditor<Node> editor = iter.next();
                if (editor instanceof IRefreshable) {
                    ((IRefreshable) editor).refresh();
                }
            }
        } finally {
            active = false;
        }
    }

    // callback methods for editor events

    void onFocus(IModel<Node> model) {
        if (!active) {
            active = true;
            try {
                focusBrowser(model);
            } finally {
                active = false;
            }
        }
    }

    private void focusBrowser(IModel<Node> nodeModel) {
        browser.setModel((JcrNodeModel) nodeModel);
    }

    void onClose(IModel<Node> model) {
        if (model != null) {
            // cleanup internals
            editors.remove(getEditor(model));
            if (editors.size() == 0) {
                browser.setModel(new JcrNodeModel((Node) null));
            }
        }
    }

    // internal

    private JcrNodeModel getEditorModel(JcrNodeModel nodeModel) {
        Node node = nodeModel.getNode();
        if (node != null) {
            try {
                if (node.getDepth() > 0 && node.getParent().isNodeType(HippoNodeType.NT_HANDLE)) {
                    return new JcrNodeModel(node.getParent());
                }
            } catch (InvalidItemStateException iex) {
                try {
                    log.info("Item '{}' appears not to be existing anymore", node.getIdentifier());

                    // I am checking for debug level to avoid string concatenation if debug is not enabled
                    if (log.isDebugEnabled()) {
                        log.debug("Error happened when referencing item '" + node.getIdentifier() + "'", iex);
                    }
                } catch (RepositoryException rex) {
                    log.debug("Error resolving editor model", rex);
                }
            } catch (RepositoryException ex) {
                log.error("Error resolving editor model", ex);
            }
        }
        return nodeModel;
    }

}
