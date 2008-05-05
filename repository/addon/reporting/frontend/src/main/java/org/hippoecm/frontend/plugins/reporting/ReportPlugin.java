/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.plugins.reporting;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.PluginFactory;
import org.hippoecm.frontend.plugin.config.PluginConfig;
import org.hippoecm.frontend.plugin.config.PluginRepositoryConfig;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ReportPlugin.class);

    public ReportPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        Node reportNode = getReportNode();
        if (reportNode == null) {
            setPluginModel(model);
            add(new Label("report", "Failed to  create report: cannot locate report node"));            
        } else {
            setPluginModel(new ReportModel(new JcrNodeModel(reportNode)));
            Plugin report = createReport("report", reportNode);
            if (report == null) {
                add(new Label("report", "Failed to  create report: cannot create report plugin"));
            } else {
                add(report);
                return;
            }
        }
    }

    // privates

    private Node getReportNode() {
        String reportId = getDescriptor().getParameter("report").getStrings().get(0);
        Node node;
        try {
            if (reportId != null) {
                Session session = ((UserSession) getSession()).getJcrSession();
                node = session.getNodeByUUID(reportId);
                if (!node.isNodeType(ReportingNodeTypes.NT_REPORT)) {
                    node = null;
                }
            } else {
                node = null;
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
            node = null;
        }
        return node;
    }

    private Plugin createReport(String id, Node reportNode) {
	    PluginConfig pluginConfig = new PluginRepositoryConfig(reportNode);
	    PluginDescriptor pluginDescriptor = pluginConfig.getPlugin(ReportingNodeTypes.PLUGIN);
	    pluginDescriptor.setWicketId(id);
	
	    ReportModel reportModel = new ReportModel(new JcrNodeModel(reportNode));
	    PluginFactory pluginFactory = new PluginFactory(getPluginManager());
	    return pluginFactory.createPlugin(pluginDescriptor, reportModel, this);
    }

}
