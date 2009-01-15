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

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IModelListener;
import org.hippoecm.frontend.model.IModelService;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IPluginControl;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.service.IBrowseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBrowseView implements IBrowseService, IDetachable {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Logger log = LoggerFactory.getLogger(AbstractBrowseView.class);

    public static final String VIEWERS = "browser.viewers";

    private String viewerName;
    private IPluginControl viewer;

    private IPluginContext context;
    private IPluginConfig config;
    private BrowseService browseService;

    protected AbstractBrowseView(IPluginContext context, IPluginConfig config, JcrNodeModel document) {
        this.config = config;
        this.context = context;
        
        if (document == null) {
            document = new JcrNodeModel(config.getString("model.folder.root", "/"));
        }

        browseService = new BrowseService(context, config, document);
        context.registerService(new IModelListener() {

            public void updateModel(IModel model) {
                onFolderChanged(model);
            }

        }, config.getString("model.folder"));

        @SuppressWarnings("unchecked")
        IModelService<IModel> modelService = context.getService(config.getString("model.folder"), IModelService.class);
        if (modelService != null) {
            IModel model = modelService.getModel();
            onFolderChanged(model);
        }
    }

    protected boolean showFolder(Node node, List<String> viewers) throws RepositoryException {
        for (String type : viewers) {
            if (node.isNodeType(type)) {
                if (!type.equals(viewerName)) {
                    if (viewer != null) {
                        viewer.stopPlugin();
                        viewer = null;
                        viewerName = null;
                    }

                    IPluginConfigService pluginConfig = context.getService(IPluginConfigService.class.getName(),
                            IPluginConfigService.class);
                    IClusterConfig cluster = pluginConfig.getCluster(config.getString(VIEWERS) + "/" + type);
                    cluster.put("wicket.id", getExtensionPoint());
                    cluster.put("model.folder", config.getString("model.folder"));
                    cluster.put("model.document", config.getString("model.document"));

                    IPluginConfig parameters = config.getPluginConfig("browser.options");
                    if (parameters != null) {
                        for (String override : cluster.getOverrides()) {
                            if (parameters.containsKey(override)) {
                                cluster.put(override, parameters.get(override));
                            }
                        }
                    }
                    viewer = context.start(cluster);
                    viewerName = type;

                    onShowFolder();
                }
                return true;
            }
        }
        return false;
    }

    protected void onFolderChanged(IModel model) {
        if (model instanceof JcrNodeModel) {
            boolean shown = false;
            try {
                Node node = ((JcrNodeModel) model).getNode();
                Node root = node.getSession().getRootNode();
                IPluginConfigService pluginConfig = context.getService(IPluginConfigService.class.getName(),
                        IPluginConfigService.class);
                do {
                    shown = showFolder(node, pluginConfig.listClusters(config.getString(VIEWERS)));
                    if (!node.isSame(root)) {
                        node = node.getParent();
                    } else {
                        break;
                    }
                } while (!shown);
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        } else {
            if (viewer != null) {
                viewer.stopPlugin();
                viewer = null;
                viewerName = null;
            }
        }
    }

    public void browse(IModel model) {
        browseService.browse(model);
        onBrowse();
    }

    public void detach() {
        browseService.detach();
    }

    protected abstract String getExtensionPoint();

    protected void onShowFolder() {
    }

    protected void onBrowse() {
    }

}
