/**
 * Copyright 2011 Hippo
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
package org.onehippo.cms7.channelmanager.widgets;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogAction;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.editor.plugins.linkpicker.LinkPickerDialog;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.cms7.channelmanager.model.UuidFromPathModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Link picker dialog that returns the picked path via a 'picked' event to an ExtJS component.
 */
public class ExtLinkPicker extends Panel implements IModel<String> {

    private Logger log = LoggerFactory.getLogger(ExtLinkPicker.class);

    private final IPluginContext context;
    private final WebMarkupContainer container;
    private final Component picker;
    private String activeComponentId;
    private String pickedPath;

    public ExtLinkPicker(IPluginContext context, String id) {
        super(id);

        this.context = context;

        picker = new EmptyPanel("picker");
        picker.setOutputMarkupId(true);

        container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        container.add(picker);
        add(container);

        setOutputMarkupId(true);
    }

    @Override
    public String getObject() {
        return pickedPath;
    }

    @Override
    public void setObject(final String pickedPath) {
        this.pickedPath = pickedPath;

        if (this.activeComponentId == null) {
            log.warn("Picked '{}' but got not div id to invoke a callback on", this.pickedPath);
            return;
        }

        AjaxRequestTarget target = AjaxRequestTarget.get();
        if (target == null) {
            log.warn("Cannot invoke callback on div id '{}' for picked path '{}': no ajax request target available",
                    this.activeComponentId, this.pickedPath);
            return;
        }

        target.prependJavascript("Ext.getCmp('" + this.activeComponentId + "').fireEvent('picked', '" + this.pickedPath + "')");
        this.activeComponentId = null;
    }

    public void pickFolder(AjaxRequestTarget target, String componentId, IPluginConfig pickerConfig) {
        this.activeComponentId = componentId;

        final IDialogFactory dialogFactory = createDialogFactory(context, pickerConfig, new UuidFromPathModel(this));
        final IDialogService dialogService = context.getService(IDialogService.class.getName(), IDialogService.class);

        final DialogAction action = new DialogAction(dialogFactory, dialogService);
        action.execute();

        target.addComponent(container);
    }

    private static IDialogFactory createDialogFactory(final IPluginContext context, final IPluginConfig config, final IModel<String> model) {
        return new IDialogFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public AbstractDialog<String> createDialog() {
                return new LinkPickerDialog(context, config, model);
            }
        };
    }

}
