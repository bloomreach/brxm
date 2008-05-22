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
package org.hippoecm.frontend.sa.plugin.root;

import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.impl.RenderPlugin;
import org.hippoecm.frontend.sa.service.render.RenderService;
import org.hippoecm.frontend.service.dialog.DialogService;

public class RootPlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    private DialogService dialogService;

    public RootPlugin() {
        for (String extension : new String[] { "content" }) {
            addExtensionPoint(extension);
        }
        dialogService = new DialogService();
        add(new EmptyPanel("dialog"));
    }

    @Override
    public void start(IPluginContext context) {
        super.start(context);

        dialogService.init(context, context.getProperties().getString(RenderService.DIALOG_ID), "dialog");
        replace(dialogService);
    }

    @Override
    public void stop() {
        replace(new EmptyPanel("dialog"));
        dialogService.destroy();

        super.stop();
    }
}
