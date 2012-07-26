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
package org.hippoecm.frontend.plugins.gallery;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.gallery.upload.UploadDialog;
import org.hippoecm.frontend.service.render.RenderPlugin;

public class GalleryShortcutPlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    public GalleryShortcutPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        AjaxLink link = new AjaxLink("link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                IDialogService dialogService = getDialogService();
                UploadDialog dialog = new UploadDialog(GalleryShortcutPlugin.this.getPluginContext(),
                                                       GalleryShortcutPlugin.this.getPluginConfig(),
                                                       GalleryShortcutPlugin.this.getDefaultModel());
                dialogService.show(dialog);
            }
        };
        add(link);

        Label label = new Label("label");
        label.setDefaultModel(new StringResourceModel(config.getString("option.text"), this, null));
        link.add(label);
    }
}
