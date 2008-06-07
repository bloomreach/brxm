/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.plugins.standardworkflow;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.legacy.dialog.DialogLink;
import org.hippoecm.frontend.legacy.model.IPluginModel;
import org.hippoecm.frontend.legacy.plugin.Plugin;
import org.hippoecm.frontend.legacy.plugin.PluginDescriptor;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.plugins.standardworkflow.dialogs.ExtendedFolderDialog;
import org.hippoecm.frontend.plugins.standardworkflow.dialogs.FolderDialog;
import org.hippoecm.frontend.plugins.standardworkflow.dialogs.PrototypeDialog;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class PrototypeWorkflowPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PrototypeWorkflowPlugin.class);

    public PrototypeWorkflowPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        Model linkText = null; // null if cannot add document

        try {
            Node node = ((WorkflowsModel)getModel()).getNodeModel().getNode();
            String path = node.getProperty(HippoNodeType.HIPPO_PROTOTYPE).getString().trim();
            if(path.length() > 0) {
                Node prototype = node.getSession().getRootNode().getNode(path.substring(1));
                String name = prototype.getParent().getName();
                if(name.contains(":"))
                    name = name.substring(name.indexOf(":") + 1);
                linkText = new Model("Add " + name);
            } else {
                if(log.isDebugEnabled()) {
                    log.debug("folder " + ((WorkflowsModel)getModel()).getNodeModel().getNode().getPath() +
                              " did not define any default document to work on");
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }

        if(linkText != null) {
            add(new DialogLink("addDocument-dialog", linkText, PrototypeDialog.class, (WorkflowsModel) getModel(),
                               getTopChannel(), getPluginManager().getChannelFactory()));
            add(new DialogLink("addFolder-dialog", new Model("Add folder"), FolderDialog.class, (WorkflowsModel) getModel(),
                               getTopChannel(), getPluginManager().getChannelFactory()));
        } else {
            add(new EmptyPanel("addDocument-dialog"));
            add(new EmptyPanel("addFolder-dialog"));
        }
        add(new DialogLink("addExtendedFolder-dialog", new Model("Add folder of type..."),
                           ExtendedFolderDialog.class, (WorkflowsModel) getModel(),
                           getTopChannel(), getPluginManager().getChannelFactory()));
    }
}
