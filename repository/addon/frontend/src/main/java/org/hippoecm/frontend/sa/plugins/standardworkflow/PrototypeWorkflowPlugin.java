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
package org.hippoecm.frontend.sa.plugins.standardworkflow;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.WorkflowsModel;
import org.hippoecm.frontend.sa.dialog.AbstractDialog;
import org.hippoecm.frontend.sa.dialog.DialogLink;
import org.hippoecm.frontend.sa.dialog.IDialogFactory;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.sa.plugin.workflow.WorkflowPlugin;
import org.hippoecm.frontend.sa.plugins.standardworkflow.dialogs.ExtendedFolderDialog;
import org.hippoecm.frontend.sa.plugins.standardworkflow.dialogs.FolderDialog;
import org.hippoecm.frontend.sa.plugins.standardworkflow.dialogs.PrototypeDialog;
import org.hippoecm.frontend.sa.service.IDialogService;
import org.hippoecm.frontend.sa.service.IViewService;
import org.hippoecm.frontend.sa.service.ServiceTracker;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrototypeWorkflowPlugin extends AbstractWorkflowPlugin {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PrototypeWorkflowPlugin.class);

    private ServiceTracker<IViewService> viewer;

    public PrototypeWorkflowPlugin() {
        viewer = new ServiceTracker<IViewService>(IViewService.class);

        add(new EmptyPanel("addDocument-dialog"));
        add(new EmptyPanel("addFolder-dialog"));

        add(new DialogLink("addExtendedFolder-dialog", new Model("Add folder of type..."), new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService dialogService) {
                return new ExtendedFolderDialog(PrototypeWorkflowPlugin.this, dialogService);
            }
        }));
    }

    @Override
    public void init(IPluginContext context, IPluginConfig properties) {
        super.init(context, properties);
        if (properties.get(WorkflowPlugin.VIEWER_ID) != null) {
            viewer.open(context, properties.getString(WorkflowPlugin.VIEWER_ID));
        }
    }

    @Override
    public void destroy() {
        viewer.close();
        super.destroy();
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        try {
            Node node = ((WorkflowsModel) getModel()).getNodeModel().getNode();
            String path = node.getProperty(HippoNodeType.HIPPO_PROTOTYPE).getString().trim();
            if (path.length() > 0) {
                Node prototype = node.getSession().getRootNode().getNode(path.substring(1));
                String name = prototype.getParent().getName();
                if (name.contains(":"))
                    name = name.substring(name.indexOf(":") + 1);
                final String title = "Add " + name;
                replace(new DialogLink("addDocument-dialog", new Model(title), new IDialogFactory() {
                    private static final long serialVersionUID = 1L;

                    public AbstractDialog createDialog(IDialogService dialogService) {
                        return new PrototypeDialog(PrototypeWorkflowPlugin.this, dialogService, title);
                    }
                }));
                replace(new DialogLink("addFolder-dialog", new Model("Add folder"), new IDialogFactory() {
                    private static final long serialVersionUID = 1L;

                    public AbstractDialog createDialog(IDialogService dialogService) {
                        return new FolderDialog(PrototypeWorkflowPlugin.this, dialogService);
                    }
                }));
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("folder " + ((WorkflowsModel) getModel()).getNodeModel().getNode().getPath()
                            + " did not define any default document to work on");
                }
                replace(new EmptyPanel("addDocument-dialog"));
                replace(new EmptyPanel("addFolder-dialog"));
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
    }

    public void select(JcrNodeModel nodeModel) {
        IViewService view = viewer.getService();
        if (view != null) {
            view.view(nodeModel);
        }
    }
}
