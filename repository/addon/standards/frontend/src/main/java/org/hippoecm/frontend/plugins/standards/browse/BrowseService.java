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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.model.event.IRefreshable;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of IBrowseService that also exposes two model services,
 * for document and folder, that represent the state of the "browser".
 * <p>
 * The IBrowseService interface should be used by plugins that do not form
 * part of the "browser".  The model services should be used by plugins that do.
 * <p>
 * The folder and document models are always JcrNodeModel instances, though the
 * nodes may not exist.  When the document node is null, this implies that no
 * document is selected from the folder.  Setting the folder node to null is
 * not supported.
 */
public class BrowseService implements IBrowseService<JcrNodeModel>, IRefreshable, IDetachable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(BrowseService.class);

    private class DocumentModelService extends ModelReference<JcrNodeModel> {
        private static final long serialVersionUID = 1L;

        DocumentModelService(IPluginConfig config, JcrNodeModel document) {
            super(config.getString("model.document"), document);
        }

        public void updateModel(JcrNodeModel model) {
            super.setModel(model);
        }

        @Override
        public void setModel(JcrNodeModel model) {
            if (model == null) {
                throw new IllegalArgumentException("invalid model null");
            }
            browse(model);
        }
    }

    private class FolderModelService extends ModelReference<JcrNodeModel> {
        private static final long serialVersionUID = 1L;

        FolderModelService(IPluginConfig config, JcrNodeModel document) {
            super(config.getString("model.folder"), document);
        }

        public void updateModel(JcrNodeModel model) {
            super.setModel(model);
        }

        @Override
        public void setModel(JcrNodeModel model) {
            if (model == null) {
                throw new IllegalArgumentException("invalid folder model null");
            } else if (model.getNode() == null) {
                throw new IllegalArgumentException("invalid folder node null");
            }
            selectFolder(model);
        }

    }
    
    private JcrNodeModel folder;
    private String path;
    private FolderModelService folderService;
    private DocumentModelService documentService;

    public BrowseService(final IPluginContext context, final IPluginConfig config, JcrNodeModel document) {

        document = findDocument(document);

        context.registerService(this, config.getString(IBrowseService.BROWSER_ID, BrowseService.class.getName()));
        context.registerService(this, IRefreshable.class.getName());

        documentService = new DocumentModelService(config, document);
        documentService.init(context);

        folderService = new FolderModelService(config, folder);
        folderService.init(context);
    }

    public void selectFolder(IModel model) {
        if (model != null && (model instanceof JcrNodeModel) && !model.equals(folder)) {
            folder = (JcrNodeModel) model;

            documentService.updateModel(new JcrNodeModel((Node) null));
            folderService.updateModel(folder);
        }
    }

    public void browse(JcrNodeModel model) {
        JcrNodeModel document = findDocument((JcrNodeModel) model);
        if (folder != null) {
            documentService.updateModel(document);
            folderService.updateModel(folder);
        } else {
            log.warn("No folder found for model {}", model);
        }
    }

    public void refresh() {
        if (path != null) {
            JcrNodeModel nodeModel = documentService.getModel();
            if (!nodeModel.getItemModel().exists()) {
                nodeModel = folderService.getModel();
                if (!nodeModel.getItemModel().exists()) {
                    // detect move/delete of ancestor
                    nodeModel = new JcrNodeModel(path);
                    boolean hasChanged = false;
                    while (!nodeModel.getItemModel().exists() && path.length() > 0) {
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
        JcrNodeModel nodeModel = documentService.getModel();
        if (!nodeModel.getItemModel().exists()) {
            nodeModel = folderService.getModel();
        }
        if (nodeModel.getItemModel().exists()) {
            path = nodeModel.getItemModel().getPath();
        }

        folderService.detach();
        documentService.detach();
    }

    private JcrNodeModel findDocument(JcrNodeModel document) {
        if (document.getNode() == null) {
            return document;
        } else if (isFolder(document)) {
            folder = document;
            document = new JcrNodeModel((Node) null);
        } else {
            folder = getParent(document);
            while (!isFolder(folder)) {
                document = folder;
                folder = getParent(document);
            }
        }
        return document;
    }

    private JcrNodeModel getParent(JcrNodeModel model) {
        JcrNodeModel parentModel = model.getParentModel();
        if (parentModel == null) {
            return new JcrNodeModel((Node) null);
        }
        try {
            // skip facetresult nodes in hierarchy
            Node parent = parentModel.getNode();
            if (parent.isNodeType(HippoNodeType.NT_FACETRESULT)) {
                return new JcrNodeModel(parent.getParent());
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return parentModel;
    }

    private boolean isFolder(JcrNodeModel nodeModel) {
        if (nodeModel.getNode() != null) {
            try {
                Node node = nodeModel.getNode();
                if (node.isNodeType("hippostd:folder") || node.isNodeType("hippostd:directory")
                        || node.isNodeType("hipposysedit:namespace") || node.isNodeType(HippoNodeType.NT_FACETBASESEARCH)) {
                    return true;
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
            return false;
        }
        return true;
    }
    
}
