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

package org.hippoecm.frontend.plugins.development.content;

import java.util.Collection;
import java.util.LinkedList;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;

public class CreateFoldersShortcutPlugin extends RenderPlugin{
    private static final long serialVersionUID = 1L;

    String folder = "/content/documents/news";
    Collection<String> selectedTypes = new LinkedList<String>();
    int minLength = 20;
    int maxLength = 35;
    int amount = 5;
    
    public CreateFoldersShortcutPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        
        add(new AjaxLink("link") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                IDialogService dialogService = getDialogService();
                dialogService.show(new CreateFoldersShortcutPlugin.Dialog());
            }
        });
    }
    
    public class Dialog extends AbstractDialog {
        private static final long serialVersionUID = 1L;

        String folder = "/";

        public Dialog() {
            setOkLabel(new StringResourceModel("start-create-folders-label", CreateFoldersShortcutPlugin.this, null));

            add(new RequiredTextField("folder", new PropertyModel(this, "folder")));

        }

        public IModel getTitle() {
            return new StringResourceModel("create-folders-label", CreateFoldersShortcutPlugin.this, null);
        }

    }

    
}
