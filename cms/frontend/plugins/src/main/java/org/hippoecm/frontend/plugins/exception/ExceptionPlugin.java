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
package org.hippoecm.frontend.plugins.exception;

import org.hippoecm.frontend.dialog.DialogPageCreator;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.model.ExceptionModel;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Notification;

public class ExceptionPlugin extends Plugin {
    private static final long serialVersionUID = 1L;
    private DialogWindow dialogWindow;
    private ExceptionDialog exceptionDialog;

    public ExceptionPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, new JcrNodeModel(model), parentPlugin);

        dialogWindow = new DialogWindow("exception", new JcrNodeModel(model), null, null);
        add(dialogWindow);

        exceptionDialog = new ExceptionDialog(dialogWindow);
        dialogWindow.setPageCreator(new DialogPageCreator(exceptionDialog));
    }

    @Override
    public void receive(Notification notification) {
        if ("exception".equals(notification.getOperation())) {

            ExceptionModel repositoryExceptionModel = null;
            if (notification.getModel() instanceof ExceptionModel) {
                repositoryExceptionModel = (ExceptionModel) notification.getModel();
                exceptionDialog.setExceptionMessage(repositoryExceptionModel);
                notification.getContext().addRefresh(dialogWindow, "show");
            }

        }
        super.receive(notification);
    }
}
