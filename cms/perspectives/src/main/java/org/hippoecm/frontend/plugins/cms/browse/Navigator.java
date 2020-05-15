/*
 *  Copyright 2010-2020 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.Strings;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.behaviors.OpenRootFolderBehavior;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ObservableModel;
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

    private static final Logger log = LoggerFactory.getLogger(Navigator.class);

    public static final String CLUSTER_NAME = "cluster.name";
    public static final String CLUSTER_PARAMETERS = "cluster.config";
    public static final String SELECTED_SECTION_MODEL = "selected.section.model";

    private final DocumentCollectionView docView;
    private final SectionViewer sectionViewer;
    private final ObservableModel<String> sectionModel;

    private boolean clusterStarted;

    public Navigator(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        // pretend that cluster has already been started to prevent it's creation in the BrowseService constructor
        clusterStarted = true;

        final JcrNodeModel defaultRootPath = new JcrNodeModel(config.getString("model.default.path", "/"));

        final boolean isPicker = Strings.isEqual(getVariation(), "picker");
        final ObservableModel<LastVisited> lastVisitedModel = !isPicker
                ? ObservableModel.from(context, LastVisited.MODEL_ID)
                : null;
        final BrowseService browseService = new BrowseService(context, config, defaultRootPath, lastVisitedModel) {

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

        add(new OpenRootFolderBehavior() {
            @Override
            protected void onOpenRootFolder(final AjaxRequestTarget target) {
                browseService.browse(defaultRootPath);
            }
        });

        final IModel<DocumentCollection> collectionModel = browseService.getCollectionModel();
        docView = new DocumentCollectionView("documents", context, config, collectionModel, this) {
            @Override
            protected String getExtensionPoint() {
                return config.getString("extension.list");
            }
        };
        add(docView);

        sectionModel = ObservableModel.from(context, Navigator.SELECTED_SECTION_MODEL);

        final BrowserSections sections = browseService.getSections();
        sectionViewer = new SectionViewer("sections", sections, this) {
            @Override
            protected void onSectionChange(final String sectionName) {
                super.onSectionChange(sectionName);
                sectionModel.setObject(sectionName);
            }
        };
        add(sectionViewer);
    }

    @Override
    public void render(final PluginRequestTarget target) {
        if (!clusterStarted && isActive()) {
            startCluster();
        }
        super.render(target);
        docView.render(target);
        sectionViewer.render(target);
    }

    private void startCluster() {
        clusterStarted = true;

        final IPluginConfig config = getPluginConfig();
        final String clusterName = config.getString(CLUSTER_NAME);
        if (clusterName != null) {
            final IPluginContext context = getPluginContext();
            final IPluginConfigService pluginConfigService = context.getService(IPluginConfigService.class.getName(),
                    IPluginConfigService.class);

            final IClusterConfig cluster = pluginConfigService.getCluster(clusterName);
            if (cluster == null) {
                log.warn("Unable to find cluster '" + clusterName + "'. Does it exist in repository?");
            } else {
                final IPluginConfig parameters = config.getPluginConfig(CLUSTER_PARAMETERS);
                final IClusterControl control = context.newCluster(cluster, parameters);
                control.start();
            }
        }
    }

    @Override
    protected void onDetach() {
        sectionModel.detach();
        super.onDetach();
    }
}
