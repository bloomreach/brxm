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
package org.hippoecm.frontend.plugins.reporting;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportPlugin extends Plugin {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ReportPlugin.class);

    public ReportPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public void start() {
        Node reportNode = getReportNode();
        if (reportNode == null) {
            log.warn("Failed to  create report: cannot locate report node");
        } else {
            IPluginContext context = getPluginContext();
            String modelId = getPluginConfig().getString("report.resultset.model");
            ReportModel reportModel = new ReportModel(new JcrNodeModel(reportNode));
            ModelReference modelService = new ModelReference(modelId, reportModel);
            modelService.init(context);

            IClusterConfig renderer = getReportRenderer(reportNode);
            if (renderer != null) {
                context.newCluster(renderer, null).start();
            } else {
                log.error("Failed to  create report: cannot create report plugin");
            }
        }
    }

    // privates

    private IClusterConfig getReportRenderer(Node reportNode) {
        JavaClusterConfig clusterConfig;
        try {
            Node rendererNode = reportNode.getNode(ReportingNodeTypes.PLUGIN);
            JcrNodeModel rendererNodeModel = new JcrNodeModel(rendererNode);
            IPluginConfig pluginConfig = new JcrPluginConfig(rendererNodeModel);
            clusterConfig = new JavaClusterConfig();
            clusterConfig.addPlugin(pluginConfig);

        } catch (RepositoryException e) {
            log.error(e.getMessage());
            clusterConfig = null;
        }
        return clusterConfig;
    }

    private Node getReportNode() {
        String reportId = getPluginConfig().getString("report.input.node");
        Node node = null;
        try {
            if (reportId != null) {
                Session session = UserSession.get().getJcrSession();
                node = session.getNodeByIdentifier(reportId);
                if (!node.isNodeType(ReportingNodeTypes.NT_REPORT)) {
                    node = null;
                }
            }
        } catch (ItemNotFoundException e) {
            log.info("Report node not found with id: " + reportId);
        } catch (RepositoryException e) {
            log.error("Error while fetching report node with id: " + reportId, e);
        }
        return node;
    }

}
