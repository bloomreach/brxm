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
package org.hippoecm.frontend.plugins.admin.menu;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.AbstractMenuPlugin;
import org.hippoecm.frontend.plugin.JcrEvent;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugins.admin.menu.delete.DeleteDialog;
import org.hippoecm.frontend.plugins.admin.menu.export.ExportDialog;
import org.hippoecm.frontend.plugins.admin.menu.move.MoveDialog;
import org.hippoecm.frontend.plugins.admin.menu.node.NodeDialog;
import org.hippoecm.frontend.plugins.admin.menu.property.PropertyDialog;
import org.hippoecm.frontend.plugins.admin.menu.rename.RenameDialog;
import org.hippoecm.frontend.plugins.admin.menu.reset.ResetDialog;
import org.hippoecm.frontend.plugins.admin.menu.save.SaveDialog;

public class MenuPlugin extends AbstractMenuPlugin {
    private static final long serialVersionUID = 1L;

    public MenuPlugin(PluginDescriptor pluginDescriptor, final JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        addMenuOption("node-dialog", "node-dialog-link", NodeDialog.class.getName(), model);
        addMenuOption("delete-dialog", "delete-dialog-link", DeleteDialog.class.getName(), model);
        addMenuOption("move-dialog", "move-dialog-link", MoveDialog.class.getName(), model);
        addMenuOption("rename-dialog", "rename-dialog-link", RenameDialog.class.getName(), model);
        addMenuOption("export-dialog", "export-dialog-link", ExportDialog.class.getName(), model);
        addMenuOption("property-dialog", "property-dialog-link", PropertyDialog.class.getName(), model);
        addMenuOption("save-dialog", "save-dialog-link", SaveDialog.class.getName(), model);
        addMenuOption("reset-dialog", "reset-dialog-link", ResetDialog.class.getName(), model);

        add(new Label("path"));
    }
    
    
    public void update(AjaxRequestTarget target, JcrEvent jcrEvent) {
        if (jcrEvent.getModel() != null) {
            setModel(jcrEvent.getModel());
        }
        if (target != null) {
            target.addComponent(this);
        }
    }

}
