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
package org.hippoecm.frontend.plugin.root;

import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.plugin.render.RenderPlugin;
import org.hippoecm.frontend.service.dialog.DialogService;
import org.hippoecm.frontend.service.render.RenderService;

public class RootPlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    private DialogService dialogService;

    public RootPlugin() {
        for (String extension : new String[] { "browser", "content" }) {
            addExtensionPoint(extension);
        }
        dialogService = new DialogService();
        add(new EmptyPanel("dialog"));
    }

    @Override
    public void start(PluginContext context) {
        super.start(context);

        dialogService.init(context, context.getProperties().get(RenderService.DIALOG_ID).getStrings().get(0), "dialog");
        replace(dialogService);
    }

    @Override
    public void stop() {
        replace(new EmptyPanel("dialog"));
        dialogService.destroy();

        super.stop();
    }
}
