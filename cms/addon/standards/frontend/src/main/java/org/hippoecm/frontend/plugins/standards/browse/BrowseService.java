package org.hippoecm.frontend.plugins.standards.browse;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IModelListener;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowseService implements IBrowseService<IModel>, IDetachable {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(BrowseService.class);

    private class DocumentModelService extends ModelService<JcrNodeModel> {
        private static final long serialVersionUID = 1L;

        DocumentModelService(IPluginConfig config, JcrNodeModel document) {
            super(config.getString("model.document"), document);
        }

        public void updateModel(JcrNodeModel model) {
            super.setModel(model);
        }

        @Override
        public void setModel(JcrNodeModel model) {
            selectDocument(model);
        }
    }

    private JcrNodeModel folder;
    private ModelService<JcrNodeModel> folderService;
    private DocumentModelService documentService;

    private IPluginContext ctx;
    private IPluginConfig cfg;

    public BrowseService(final IPluginContext context, final IPluginConfig config, JcrNodeModel document) {

        ctx = context;
        cfg = config;

        document = findDocument(document);

        context.registerService(this, config.getString(IBrowseService.BROWSER_ID, BrowseService.class.getName()));

        if (config.getString("model.document") != null) {
            documentService = new DocumentModelService(config, document);
            documentService.init(context);
        } else {
            log.error("no document model service (model.document) specified");
        }

        if (config.getString("model.folder") != null) {
            folderService = new ModelService<JcrNodeModel>(config.getString("model.folder"), folder);
            folderService.init(context);
            context.registerService(new IModelListener() {
                private static final long serialVersionUID = 1L;

                public void updateModel(IModel model) {
                    selectFolder(model);
                }

            }, config.getString("model.folder"));
        } else {
            log.error("no folder model service (model.folder) specified");
        }
    }

    public void selectFolder(IModel model) {
        if (model != null && (model instanceof JcrNodeModel) && !model.equals(folder)) {
            folder = (JcrNodeModel) model;

            documentService.updateModel(new JcrNodeModel((Node) null));
        }
    }

    public void selectDocument(JcrNodeModel model) {
        if (model != null && model.getNode() != null) {
            try {
                if (model.getNode().isNodeType("hippostd:folder") || model.getNode().isNodeType("hippostd:directory")) {
                    folderService.setModel(model);
                    return;
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
        documentService.updateModel(model);
    }

    public void setFolderModel(JcrNodeModel nodeModel) {
        folderService.setModel(nodeModel);
    }

    public void updateDocumentModel(JcrNodeModel nodeModel) {
        documentService.updateModel(nodeModel);
    }

    public void browse(IModel model) {
        if (model instanceof JcrNodeModel) {
            JcrNodeModel document = findDocument((JcrNodeModel) model);
            if (folder != null) {
                if (document != null) {
                    try {
                        Node node = document.getNode();
                        if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                            updateDocumentModel(document);
                        }
                    } catch (RepositoryException ex) {
                        log.error(ex.getMessage());
                    }
                }
                setFolderModel(folder);
            } else {
                log.warn("No folder found for model {}", model);
            }
        } else {
            log.warn("Model {} is not an JcrNodeModel", model);
        }
    }

    public void detach() {
        folderService.detach();
        documentService.detach();
    }

    private JcrNodeModel findDocument(JcrNodeModel document) {
        if (isFolder(document)) {
            folder = document;
            document = null;
        } else {
            folder = document.getParentModel();
            while (!isFolder(folder)) {
                document = folder;
                folder = folder.getParentModel();
            }
        }
        return document;
    }

    private boolean isFolder(JcrNodeModel nodeModel) {
        if (nodeModel != null) {
            try {
                Node node = nodeModel.getNode();
                if (node.isNodeType("hippostd:folder") || node.isNodeType("hippostd:directory")) {
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
