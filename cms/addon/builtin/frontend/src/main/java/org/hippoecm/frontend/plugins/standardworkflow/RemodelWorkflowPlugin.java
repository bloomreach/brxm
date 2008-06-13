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

import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.IServiceReference;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.workflow.AbstractWorkflowPlugin;
import org.hippoecm.frontend.plugins.standardworkflow.dialogs.CreateTypeDialog;
import org.hippoecm.frontend.plugins.standardworkflow.dialogs.RemodelDialog;
import org.hippoecm.frontend.service.IJcrService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemodelWorkflowPlugin extends AbstractWorkflowPlugin {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RemodelWorkflowPlugin.class);

    public RemodelWorkflowPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        
        IJcrService jcrService = context.getService(IJcrService.class.getName(), IJcrService.class);
        final IServiceReference<IJcrService> jcrRef = context.getReference(jcrService);

        add(new DialogLink("remodelRequest-dialog", new Model("Update content"), new IDialogFactory() {
            private static final long serialVersionUID = 1L;
            public AbstractDialog createDialog(IDialogService dialogService) {
                return new RemodelDialog(RemodelWorkflowPlugin.this, getDialogService(), jcrRef);
            }
        }, getDialogService()));
        
        add(new DialogLink("createTypeRequest-dialog", new Model("Create new type"), new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            public AbstractDialog createDialog(IDialogService dialogService) {
                return new CreateTypeDialog(RemodelWorkflowPlugin.this, getDialogService(), jcrRef);
            }
            
        }, getDialogService()));
    }
}
