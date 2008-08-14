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
package org.hippoecm.frontend.plugins.cms.browse;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IPluginControl;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.ClusterConfigDecorator;
import org.hippoecm.frontend.plugin.config.impl.JcrClusterConfig;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.ITitleDecorator;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowserPerspective extends Perspective implements IBrowseService {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Logger log = LoggerFactory.getLogger(BrowserPerspective.class);

    private static final long serialVersionUID = 1L;

    public static final String VIEWERS = "browser.viewers";

    private ModelService modelService;
    private String viewerName;
    private IPluginControl viewer;
    private String listingTitle = "documents";

    public BrowserPerspective(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (config.getString(RenderPlugin.MODEL_ID) != null) {
            modelService = new ModelService(config.getString(RenderPlugin.MODEL_ID), new JcrNodeModel("/"));
            modelService.init(context);
        } else {
            log.error("no model service specified");
        }

        if (config.getString(IBrowseService.BROWSER_ID) != null) {
            context.registerService(this, config.getString(IBrowseService.BROWSER_ID));
        }

        add(new Label("listing.title", new PropertyModel(this, "listingTitle")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onComponentTag(ComponentTag tag) {
                super.onComponentTag(tag);
                tag.put("class", listingTitle);
            }
        });

        // model didn't exist for super constructor, so set it explicitly
        updateModel(modelService.getModel());
    }

    public void browse(IModel model) {
        modelService.setModel(model);
        focus(null);
    }

    @Override
    public void onModelChanged() {
        IModel model = getModel();
        if (model != null && model instanceof JcrNodeModel) {
            try {
                Node node = ((JcrNodeModel) model).getNode();
                Node root = node.getSession().getRootNode();
                Map<String, IClusterConfig> viewers = getViewers();
                boolean shown = false;
                do {
                    shown = showNode(node, viewers);
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
            redraw();
        }
    }

    protected boolean showNode(Node node, Map<String, IClusterConfig> viewers) throws RepositoryException {
        IPluginContext context = getPluginContext();
        IPluginConfig config = getPluginConfig();
        for (Map.Entry<String, IClusterConfig> entry : viewers.entrySet()) {
            String type = entry.getKey();
            if (node.isNodeType(type)) {
                if (!type.equals(viewerName)) {
                    if (viewer != null) {
                        viewer.stopPlugin();
                        viewer = null;
                        viewerName = null;
                    }

                    String myId = context.getReference(this).getServiceId();
                    IClusterConfig cluster = entry.getValue();
                    IClusterConfig decorated = new ClusterConfigDecorator(cluster, myId + ".viewer");
                    decorated.put("wicket.id", config.getString("extension.list"));
                    decorated.put("wicket.model", config.getString("wicket.model"));
                    viewer = context.start(decorated);
                    viewerName = type;

                    // find title as service in started cluster
                    // (could be more precise by just looking for decorators on the root renderservice)
                    ITitleDecorator title = context.getService(context.getReference(viewer).getServiceId(),
                            ITitleDecorator.class);
                    if (title != null) {
                        listingTitle = title.getTitle();
                    } else {
                        listingTitle = viewerName;
                    }
                    redraw();
                }
                return true;
            }
        }
        return false;
    }

    protected Map<String, IClusterConfig> getViewers() {
        IPluginConfig config = getPluginConfig();
        Map<String, IClusterConfig> viewers = new LinkedHashMap<String, IClusterConfig>();
        if (config.getString(BrowserPerspective.VIEWERS) != null) {
            String path = config.getString(BrowserPerspective.VIEWERS);
            try {
                Node node = ((UserSession) Session.get()).getJcrSession().getRootNode().getNode(path.substring(1));
                NodeIterator iter = node.getNodes();
                while (iter.hasNext()) {
                    Node child = iter.nextNode();
                    if (child.isNodeType("frontend:plugincluster")) {
                        viewers.put(child.getName(), new JcrClusterConfig(new JcrNodeModel(child)));
                    }
                }
            } catch (RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
        return viewers;
    }

}
