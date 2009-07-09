/*
 *  Copyright 2009 Hippo.
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

package org.hippoecm.hst.plugins.frontend;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryManager;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugins.standards.perspective.Perspective;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.hst.plugins.frontend.editor.context.HstContext;
import org.hippoecm.repository.api.HippoQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstEditorPerspective extends Perspective {
    private static final long serialVersionUID = 1L;
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(HstEditorPerspective.class);

    private static final String EDITOR_ROOT = "editor.root";
    private static final String NAMESPACES_ROOT = "namespaces.root";

    private String previewUrlRoot;
    private String previewUrl = "";

    WebMarkupContainer previewContainer;
    boolean previewShow;

    public HstEditorPerspective(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        String rootModelPath = config.getString(EDITOR_ROOT, null);

        previewUrlRoot = config.getString("preview.url.root", "http://127.0.0.1:8085/site/preview/");
        previewShow = config.getBoolean("preview.show");

        add(previewContainer = new Preview());

        if (rootModelPath != null) {
            javax.jcr.Session jcrSession = ((UserSession) getSession()).getJcrSession();
            try {
                String relPath = rootModelPath.substring(1);
                if (!jcrSession.getRootNode().hasNode(relPath)
                        || !jcrSession.getRootNode().getNode(relPath).isNodeType("hst:sites")) {
                    rootModelPath = null;
                }
            } catch (RepositoryException e) {
                log.warn("Could not find root node at {0}, will try default instead", rootModelPath);
                rootModelPath = null;
            }
        }

        if (rootModelPath == null) {
            NodeIterator siteIterator = null;
            //Do a query for all hst:sites where the content node has a property state:unpublished
            String query = "//element(*, hst:site)[hst:content/@hippo:values = 'unpublished']";
            javax.jcr.Session session = ((UserSession) Session.get()).getJcrSession();
            try {
                QueryManager queryManager = ((UserSession) Session.get()).getQueryManager();
                HippoQuery hippoQuery = (HippoQuery) queryManager.createQuery(query, "xpath");
                session.refresh(true);
                hippoQuery.setLimit(1);
                siteIterator = hippoQuery.execute().getNodes();
            } catch (RepositoryException e) {
                log.error("Error executing query[" + query + "]", e);
            }

            if (siteIterator.hasNext()) {
                try {
                    rootModelPath = siteIterator.nextNode().getParent().getPath();
                } catch (RepositoryException e) {
                    log.error("Error retrieving first hst:sites node from query result", e);
                }
            }
        }

        if (rootModelPath == null) {
            throw new IllegalStateException(
                    "HstEditorPerspective has failed to find a valid root node of primary type hst:sites");
        }

        JcrNodeModel root = new JcrNodeModel(rootModelPath);

        HstContext hstContext = new HstContext(root, config.getString(NAMESPACES_ROOT));
        context.registerService(hstContext, HstContext.class.getName());

        new ViewController(context, config, root) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getExtensionPoint() {
                return config.getString("extension.editor");
            }
        };

        IPluginConfigService pluginConfig = context.getService(IPluginConfigService.class.getName(),
                IPluginConfigService.class);
        IClusterConfig cluster = pluginConfig.getCluster(config.getString("cluster.name"));
        IPluginConfig parameters = new JavaPluginConfig(config.getPluginConfig("cluster.options"));

        parameters.put("path", hstContext.config.getPath());
        parameters.put("content.path", hstContext.content.getPath());
        parameters.put("sitemap.path", hstContext.sitemap.getPath());
        context.newCluster(cluster, parameters).start();
    }

    public void openPreviewUrl(AjaxRequestTarget target, String string) {
        previewUrl = string;
        target.addComponent(previewContainer);
    }

    class Preview extends WebMarkupContainer {

        public Preview() {
            super("previewFrame");
            setOutputMarkupId(true);
        }

        @Override
        protected void onComponentTag(ComponentTag tag) {
            if (previewShow) {
                tag.put("src", previewUrlRoot + previewUrl);
            }
            super.onComponentTag(tag);
        }

    }

}
