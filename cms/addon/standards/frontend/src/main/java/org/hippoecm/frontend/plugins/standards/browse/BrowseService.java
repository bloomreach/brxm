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
package org.hippoecm.frontend.plugins.standards.browse;

import java.util.Iterator;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.event.IRefreshable;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of IBrowseService that also exposes three model services,
 * for document, folder and search.  These represent the state of the "browser".
 * <p>
 * The IBrowseService interface should be used by plugins that do not form
 * part of the "browser".  The model services should be used by plugins that do.
 * <p>
 * The folder and document models are always JcrNodeModel instances, though the
 * nodes may not exist.  When the document node is null, this implies that no
 * document is selected from the folder.  Setting the folder node to null is
 * not supported.
 */
public class BrowseService implements IBrowseService<IModel<Node>>, IRefreshable, IDetachable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(BrowseService.class);

    private class DocumentModelService extends ModelReference<Node> {
        private static final long serialVersionUID = 1L;

        DocumentModelService(IPluginConfig config, IModel<Node> document) {
            super(config.getString("model.document"), document);
        }

        public void updateModel(IModel<Node> model) {
            super.setModel(model);
        }

        @Override
        public void setModel(IModel<Node> model) {
            if (model == null) {
                throw new IllegalArgumentException("invalid model null");
            }
            browse(model);
        }
    }

    private class FolderModelService extends ModelReference<Node> {
        private static final long serialVersionUID = 1L;

        FolderModelService(IPluginConfig config, IModel<Node> document) {
            super(config.getString("model.folder"), document);
        }

        public void updateModel(IModel<Node> model) {
            super.setModel(model);
        }

        @Override
        public void setModel(IModel<Node> model) {
            if (model == null) {
                throw new IllegalArgumentException("invalid folder model null");
            } else if (model.getObject() == null) {
                throw new IllegalArgumentException("invalid folder node null");
            }
            selectFolder(model);
        }

    }

    private class SearchModelService extends ModelReference<BrowserSearchResult> {
        private static final long serialVersionUID = 1L;

        private IObserver searchObserver;

        SearchModelService(IPluginConfig config) {
            super(config.getString("model.search"), new AbstractReadOnlyModel<BrowserSearchResult>() {
                private static final long serialVersionUID = 1L;

                @Override
                public BrowserSearchResult getObject() {
                    return null;
                }
            });
        }

        public void updateModel(final IModel<BrowserSearchResult> model) {
            if (searchObserver != null) {
                context.unregisterService(searchObserver, IObserver.class.getName());
                searchObserver = null;
            }
            super.setModel(model);
            if (model instanceof IObservable) {
                searchObserver = new IObserver() {
                    private static final long serialVersionUID = 1L;

                    public IObservable getObservable() {
                        return (IObservable) model;
                    }

                    public void onEvent(Iterator events) {
                        if (!active) {
                            setSearch(model);
                        }
                    }

                };
                context.registerService(searchObserver, IObserver.class.getName());
            }
        }

        @Override
        public void setModel(IModel<BrowserSearchResult> model) {
            if (model == null) {
                throw new IllegalArgumentException("invalid folder model null");
            }
            setSearch(model);
        }

    }

    private boolean active = false;
    private BrowserFolderHistory current;
    private String path;
    private BrowserHistory history;
    private DocumentModelService documentService;
    private FolderModelService folderService;
    private SearchModelService searchModelService;

    private final IPluginContext context;

    public BrowseService(final IPluginContext context, final IPluginConfig config, JcrNodeModel document) {

        this.context = context;
        history = new BrowserHistory();

        JcrNodeModel folder = new JcrNodeModel("/");
        if (document.getNode() != null) {
            if (BrowserHelper.isFolder(document)) {
                folder = document;
                document = new JcrNodeModel((Node) null);
            } else {
                folder = BrowserHelper.getParent(document);
                while (!BrowserHelper.isFolder(folder)) {
                    if (!BrowserHelper.isHandle(folder)) {
                        document = null;
                    }
                    folder = BrowserHelper.getParent(folder);
                }
            }
        }
        current = history.getFolderHistory(folder);
        current.setActiveDocument(document);

        context.registerService(this, config.getString(IBrowseService.BROWSER_ID, BrowseService.class.getName()));
        context.registerService(this, IRefreshable.class.getName());

        documentService = new DocumentModelService(config, document);
        documentService.init(context);

        folderService = new FolderModelService(config, folder);
        folderService.init(context);

        searchModelService = new SearchModelService(config);
        searchModelService.init(context);
    }

    public void selectFolder(IModel<Node> model) {
        current = history.getFolderHistory(model);
        updateModels();
    }

    public void setSearch(IModel<BrowserSearchResult> model) {
        active = true;
        try {
            current.setSearchResult(model);
            if (model.getObject() != null) {
                documentService.updateModel(current.getActiveDocument());
            }
            searchModelService.updateModel(model);
        } finally {
            active = false;
        }
    }

    /**
     * Use the supplied model of a Node (or Version) to set folder and document models.
     * When a Version is supplied from the version storage, the physical node is used.
     */
    public void browse(IModel<Node> model) {
        active = true;
        try {
            IModel<Node> document = getPhysicalNode(model);
            if (document.getObject() == null) {
                return;
            }

            IModel<Node> folder = null;
            if (BrowserHelper.isFolder(document)) {
                selectFolder(document);
                return;
            }

            current = history.getFolderHistoryForDocument(document);
            if (current == null) {
                folder = BrowserHelper.getParent(document);
                while (!BrowserHelper.isFolder(folder)) {
                    if (!BrowserHelper.isHandle(folder)) {
                        document = null;
                    }
                    folder = BrowserHelper.getParent(folder);
                }
                if (document == null) {
                    selectFolder(folder);
                    return;
                }
                current = history.getFolderHistory(folder);
            }
            current.setActiveDocument(document);
            updateModels();
        } finally {
            active = false;
        }
    }

    private void updateModels() {
        documentService.updateModel(current.getActiveDocument());
        folderService.updateModel(current.getFolder());
        searchModelService.updateModel(current.getSearchResult());
    }

    public void refresh() {
        if (path != null) {
            IModel<Node> nodeModel = documentService.getModel();
            if (nodeModel.getObject() == null) {
                nodeModel = folderService.getModel();
                if (nodeModel.getObject() == null) {
                    // detect move/delete of ancestor
                    nodeModel = new JcrNodeModel(path);
                    boolean hasChanged = false;
                    while (nodeModel.getObject() == null && path.length() > 0) {
                        path = path.substring(0, path.lastIndexOf('/'));
                        nodeModel = new JcrNodeModel(path);
                        hasChanged = true;
                    }
                    if (hasChanged && nodeModel != null) {
                        browse(nodeModel);
                    }
                }
            }
        }
    }

    public void detach() {
        IModel<Node> nodeModel = documentService.getModel();
        if (nodeModel == null || nodeModel.getObject() == null) {
            nodeModel = folderService.getModel();
        }
        if (nodeModel != null && nodeModel.getObject() != null) {
            path = new JcrItemModel(nodeModel.getObject()).getPath();
        } else {
            path = null;
        }

        history.detach();
        folderService.detach();
        documentService.detach();
    }

    // retrieve the physical node when the node is versioned
    private IModel<Node> getPhysicalNode(IModel<Node> model) {
        Node node = model.getObject();
        if (node != null) {
            try {
                if (node.isNodeType("nt:version")) {
                    Node frozen = node.getNode("jcr:frozenNode");
                    String uuid = frozen.getProperty("jcr:frozenUuid").getString();
                    try {
                        Node docNode = node.getSession().getNodeByUUID(uuid);
                        if (docNode.getDepth() > 0) {
                            Node parent = docNode.getParent();
                            if (parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                                return new JcrNodeModel(parent);
                            }
                        }
                        return new JcrNodeModel(docNode);
                    } catch (ItemNotFoundException infe) {
                        // node doesn't exist anymore.  If it's a document, the handle
                        // should still be available though.
                        if (frozen.hasProperty(HippoNodeType.HIPPO_PATHS)) {
                            Value[] ancestors = frozen.getProperty(HippoNodeType.HIPPO_PATHS).getValues();
                            if (ancestors.length > 1) {
                                uuid = ancestors[1].getString();
                                return new JcrNodeModel(node.getSession().getNodeByUUID(uuid));
                            }
                        }
                        throw infe;
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
        return model;
    }

}
