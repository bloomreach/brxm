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
package org.hippoecm.frontend.plugin;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.dialog.DynamicDialogFactory;
import org.hippoecm.frontend.model.JcrNodeModel;

/**
 * Abstract plugin for a menu with any number of menu options. The menu option are
 * respresented as dialog links opening a dialog window.
 */
public abstract class AbstractMenuPlugin extends Plugin {

    public AbstractMenuPlugin(PluginDescriptor pluginDescriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);
    }

    /**
     * Adds a menu option to the current plugin (in the form of a dialog link)
     * @param dialogId html id for the dialog window of this menu option
     * @param dialogLinkId html id for the dialog link of this menu option
     * @param dialogClassName class name of the dialog window for this menu option
     * @param model the current model
     */
    protected void addMenuOption(String dialogId, String dialogLinkId, String dialogClassName, JcrNodeModel model) {
        final DialogWindow dialog = new DialogWindow(dialogId, model);
        dialog.setPageCreator(new DynamicDialogFactory(dialog, dialogClassName));
        add(dialog);
        add(dialog.dialogLink(dialogLinkId));
    }

    public void update(final AjaxRequestTarget target, final PluginEvent event) {
        JcrNodeModel newModel = event.getNodeModel(JcrEvent.NEW_MODEL);
        if (newModel != null) {
            setNodeModel(newModel);
        }
    }
}
