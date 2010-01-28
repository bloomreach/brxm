/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.plugins.cms.browse.section;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.browse.model.DocumentCollection;
import org.hippoecm.frontend.plugins.cms.browse.service.IBrowserSection;
import org.hippoecm.frontend.plugins.standards.browse.BrowserHelper;
import org.hippoecm.frontend.plugins.standards.browse.BrowserSearchResult;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowsingSectionPlugin extends RenderPlugin<DocumentCollection> implements IBrowserSection {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(BrowsingSectionPlugin.class);

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

    private String rootPath;
    private FolderModelService folderService;
    private DocumentCollection collection;

    public BrowsingSectionPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        this.rootPath = config.getString("model.folder.root", "/");

        collection = new DocumentCollection();

        folderService = new FolderModelService(config, new JcrNodeModel((Node) null));
    }

    @Override
    public void onStart() {
        folderService.init(getPluginContext());
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        folderService.destroy();
    }

    public void selectFolder(IModel<Node> model) {
        folderService.updateModel(model);
        collection.setFolder(model);
    }

    public void select(IModel<Node> folder) {
        while (!BrowserHelper.isFolder(folder)) {
            folder = BrowserHelper.getParent(folder);
        }

        folderService.updateModel(folder);
        collection.setFolder(folder);
    }

    public DocumentCollection getCollection() {
        return collection;
    }

    public IModel<Node> getFolder() {
        return folderService.getModel();
    }

    public IModel<BrowserSearchResult> getSearchResult() {
        return null;
    }

    public boolean contains(IModel<Node> nodeModel) {
        try {
            return nodeModel.getObject().getPath().startsWith(rootPath);
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        return false;
    }

    public IModel<String> getTitle() {
        return new StringResourceModel(getPluginConfig().getString("title", getPluginConfig().getName()), this, null);
    }

}
