/*
 *  Copyright 2010-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.browse;

import javax.jcr.Node;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.plugins.cms.browse.model.BrowserSections;
import org.hippoecm.frontend.plugins.cms.browse.model.DocumentCollection;
import org.hippoecm.frontend.plugins.cms.browse.service.BrowseService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Navigator extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(Navigator.class);

    public static final String CLUSTER_NAME = "cluster.name";
    public static final String CLUSTER_PARAMETERS = "cluster.config";

    private DocumentCollectionView docView;
    private SectionViewer sectionViewer;

    private boolean clusterStarted;

    public Navigator(IPluginContext context, final IPluginConfig config) {
        super(context, config);

        // pretend that cluster has already been started to prevent it's creation in the BrowseService constructor
        clusterStarted = true;
        final BrowseService browseService = new BrowseService(context, config,
                new JcrNodeModel(config.getString("model.default.path", "/"))) {
            private static final long serialVersionUID = 1L;

            @Override
            public void browse(final IModel<Node> model) {
                if (!clusterStarted) {
                    startCluster();
                }
                super.browse(model);
            }

            @Override
            protected void onBrowse() {
                focus(null);
            }
        };
        clusterStarted = false;

        IModel<DocumentCollection> collectionModel = browseService.getCollectionModel();
        docView = new DocumentCollectionView("documents", context, config, collectionModel, this) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getExtensionPoint() {
                return config.getString("extension.list");
            }
        };
        add(docView);

        final BrowserSections sections = browseService.getSections();
        sectionViewer = new SectionViewer("sections", sections, this);
        add(sectionViewer);
    }

    @Override
    public void render(PluginRequestTarget target) {
        if (!clusterStarted && isActive()) {
            startCluster();
        }
        super.render(target);
        docView.render(target);
        sectionViewer.render(target);
    }

    private void startCluster() {
        clusterStarted = true;

        IPluginConfig config = getPluginConfig();
        String clusterName = config.getString(CLUSTER_NAME);
        if (clusterName != null) {
            IPluginContext context = getPluginContext();
            IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                    IPluginConfigService.class);

            IClusterConfig cluster = pluginConfigService.getCluster(clusterName);
            if (cluster == null) {
                log.warn("Unable to find cluster '" + clusterName + "'. Does it exist in repository?");
            } else {
                IPluginConfig parameters = config.getPluginConfig(CLUSTER_PARAMETERS);
                IClusterControl control = context.newCluster(cluster, parameters);
                control.start();
            }
        }
    }
}
