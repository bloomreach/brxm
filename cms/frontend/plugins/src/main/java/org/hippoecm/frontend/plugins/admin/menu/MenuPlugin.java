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

import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.ChannelFactory;
import org.hippoecm.frontend.plugins.admin.menu.copy.CopyDialog;
import org.hippoecm.frontend.plugins.admin.menu.delete.DeleteDialog;
import org.hippoecm.frontend.plugins.admin.menu.export.ExportDialog;
import org.hippoecm.frontend.plugins.admin.menu.move.MoveDialog;
import org.hippoecm.frontend.plugins.admin.menu.node.NodeDialog;
import org.hippoecm.frontend.plugins.admin.menu.property.PropertyDialog;
import org.hippoecm.frontend.plugins.admin.menu.rename.RenameDialog;
import org.hippoecm.frontend.plugins.admin.menu.reset.ResetDialog;
import org.hippoecm.frontend.plugins.admin.menu.save.SaveDialog;

public class MenuPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    public MenuPlugin(PluginDescriptor pluginDescriptor, final IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, new JcrNodeModel(model), parentPlugin);

        JcrNodeModel jcrModel = (JcrNodeModel) getModel();
        Channel incoming = pluginDescriptor.getIncoming();
        ChannelFactory factory = getPluginManager().getChannelFactory();
        add(new DialogLink("node-dialog", new Model("Add Node"), NodeDialog.class, jcrModel, incoming, factory));
        add(new DialogLink("delete-dialog", new Model("Delete Node"), DeleteDialog.class, jcrModel, incoming, factory));
        add(new DialogLink("move-dialog", new Model("Move Node"), MoveDialog.class, jcrModel, incoming, factory));
        add(new DialogLink("copy-dialog", new Model("Copy Node"), CopyDialog.class, jcrModel, incoming, factory));
        add(new DialogLink("rename-dialog", new Model("Rename Node"), RenameDialog.class, jcrModel, incoming, factory));
        add(new DialogLink("export-dialog", new Model("Export Node"), ExportDialog.class, jcrModel, incoming, factory));
        add(new DialogLink("property-dialog", new Model("Add Property"), PropertyDialog.class, jcrModel, incoming, factory));
        add(new DialogLink("save-dialog", new Model("Save"), SaveDialog.class, jcrModel, incoming, factory));
        add(new DialogLink("reset-dialog", new Model("Reset"), ResetDialog.class, jcrModel, incoming, factory));
    }

}
