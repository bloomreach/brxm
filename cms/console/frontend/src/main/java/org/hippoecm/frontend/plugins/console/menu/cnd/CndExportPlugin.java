/*
 *  Copyright 2011-2023 Bloomreach
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
package org.hippoecm.frontend.plugins.console.menu.cnd;

import javax.jcr.Node;

import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;

public class CndExportPlugin extends RenderPlugin<Node> {

    private static final long serialVersionUID = 1L;

    public CndExportPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        IDialogFactory factory = new IDialogFactory() {
            private static final long serialVersionUID = 1L;
            public Dialog createDialog() {
                return new CndExportDialog();
            }
        };
        add(new DialogLink("link", new Model<String>("CND Export"), factory, getDialogService()));
    }

}
