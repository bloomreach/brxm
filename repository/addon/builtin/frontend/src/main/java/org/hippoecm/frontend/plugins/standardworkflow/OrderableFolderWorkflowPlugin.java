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

import org.apache.wicket.Component;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogAction;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standardworkflow.reorder.ReorderDialog;
import org.hippoecm.frontend.service.IJcrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderableFolderWorkflowPlugin extends FolderWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;

    private final static Logger log = LoggerFactory.getLogger(OrderableFolderWorkflowPlugin.class);

    public OrderableFolderWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        Node node = (Node) getModel().getObject();
        try {
            IJcrService jcrService = context.getService(IJcrService.class.getName(), IJcrService.class);
            final IServiceReference<IJcrService> jcrRef = context.getReference(jcrService);

            DialogAction action = new DialogAction(new IDialogFactory() {
                private static final long serialVersionUID = 1L;
    
                public AbstractDialog createDialog(IDialogService dialogService) {
                    return new ReorderDialog(OrderableFolderWorkflowPlugin.this, dialogService, jcrRef);
                }
            }, getDialogService());
    
            if (node.getNodes().getSize() < 2) {
                action.setEnabled(false);
            }
    
            addWorkflowAction("Reorder folder", "reorder_ico", null, action);
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
        }

    }

    @Override
    protected Component createDialogLinksComponent() {
        return new WorkflowActionComponentDropDownChoice(DIALOG_LINKS_COMPONENT_ID, templates);
    }

}
