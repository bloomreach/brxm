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

    private IModel folder;
    private ModelService<JcrNodeModel> folderService;
    private DocumentModelService documentService;

    private IPluginContext ctx;
    private IPluginConfig cfg;

    public BrowseService(final IPluginContext context, final IPluginConfig config) {

        ctx = context;
        cfg = config;

        context.registerService(this, config.getString(IBrowseService.BROWSER_ID, BrowseService.class.getName()));

        if (config.getString("model.document") != null) {
            documentService = new DocumentModelService(config);
            documentService.init(context);
        } else {
            log.error("no document model service (model.document) specified");
        }

        if (config.getString("model.folder") != null) {
            String path = config.getString("model.folder.root", "/");
            folderService = new ModelService<JcrNodeModel>(config.getString("model.folder"), new JcrNodeModel(path));
            folderService.init(context);
            context.registerService(new IModelListener() {
                private static final long serialVersionUID = 1L;

                public void updateModel(IModel model) {
                    selectFolder(model);
                }

            }, config.getString("model.folder"));

            // model didn't exist for super constructor, so set it explicitly

            selectFolder(folderService.getModel());
        } else {
            log.error("no folder model service (model.folder) specified");
        }
    }

    public void selectFolder(IModel model) {
        if (model != null && !model.equals(folder)) {
            folder = model;

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

    private class DocumentModelService extends ModelService<JcrNodeModel> {
        private static final long serialVersionUID = 1L;

        DocumentModelService(IPluginConfig config) {
            super(config.getString("model.document"), new JcrNodeModel((Node) null));
        }

        public void updateModel(JcrNodeModel model) {
            super.setModel(model);
        }

        @Override
        public void setModel(JcrNodeModel model) {
            selectDocument(model);
        }
    }

    public void setFolderModel(JcrNodeModel nodeModel) {
        folderService.setModel(nodeModel);
    }

    public void updateDocumentModel(JcrNodeModel nodeModel) {
        documentService.updateModel(nodeModel);
    }

    public void browse(IModel model) {
        if (model instanceof JcrNodeModel) {
            JcrNodeModel nodeModel = (JcrNodeModel) model;
            try {
                Node node = nodeModel.getNode();
                // walk up until a folder or a handle has been found
                // FIXME dependency on hippostd: types ought not be necessary
                while (!node.isNodeType("hippostd:folder") && !node.isNodeType("hippostd:directory")
                        && !node.isNodeType(HippoNodeType.NT_HANDLE)) {
                    node = node.getParent();
                    nodeModel = new JcrNodeModel(node);
                }
                if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                    setFolderModel(nodeModel.getParentModel());
                    updateDocumentModel(nodeModel);
                } else {
                    setFolderModel(nodeModel);
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
    }

    public void detach() {
        cfg.detach();
        folder.detach();
        folderService.detach();
        documentService.detach();
    }

}
