/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.plugins.xinha.dialog.links;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.xinha.AbstractXinhaPlugin;
import org.hippoecm.frontend.plugins.xinha.dialog.AbstractXinhaDialog;
import org.hippoecm.frontend.plugins.xinha.services.links.ExternalXinhaLink;
import org.hippoecm.frontend.widgets.LabelledBooleanFieldWidget;
import org.hippoecm.frontend.widgets.RequiredTextFieldWidget;
import org.hippoecm.frontend.widgets.TextFieldWidget;

public class ExternalLinkDialog extends AbstractXinhaDialog<ExternalXinhaLink> {
    private static final long serialVersionUID = 1L;


    private static final String SIZE = "49";
    private boolean disableOpenInANewWindow;

    public ExternalLinkDialog(IPluginContext context, IPluginConfig config, IModel<ExternalXinhaLink> model) {
        super(model);

        if (config.containsKey(AbstractXinhaPlugin.DISABLE_OPEN_IN_A_NEW_WINDOW_CONFIG)) {
            disableOpenInANewWindow = config.getAsBoolean(AbstractXinhaPlugin.DISABLE_OPEN_IN_A_NEW_WINDOW_CONFIG);
        }

        final DropDownChoice<String> protocolsChoice = new DropDownChoice<String>("protocols", new PropertyModel<String>(model, "protocol"), ExternalXinhaLink.PROTOCOLS);
        protocolsChoice.add(new OnChangeAjaxBehavior() {
            protected void onUpdate(AjaxRequestTarget target) {
                // nothing, just update the model
            }
        });
        protocolsChoice.setOutputMarkupId(true);
        add(protocolsChoice);

        final TextFieldWidget addressTextField = new RequiredTextFieldWidget("href", new StringPropertyModel(model, "address")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(protocolsChoice);
                target.add(this);
            }
        };
        addressTextField.setOutputMarkupId(true);
        addressTextField.setSize(SIZE);
        setFocus(addressTextField);
        add(addressTextField);

        final TextFieldWidget titleTextField = new TextFieldWidget("title", new StringPropertyModel(model, ExternalXinhaLink.TITLE));
        titleTextField.setSize(SIZE);
        add(titleTextField);

        if (disableOpenInANewWindow) {
            add(new EmptyPanel("extra"));
        } else {
            Fragment fragment = new Fragment("extra", "popup", this);

            final LabelledBooleanFieldWidget targetField = new LabelledBooleanFieldWidget("popup",
                    new PropertyModel<Boolean>(model, "target"),
                    new StringResourceModel("labels.popup", this, null));
            fragment.add(targetField);
            add(fragment);
        }

    }

    @Override
    protected void onOk() {
        ExternalXinhaLink link = getModelObject();
        link.setHref(link.getProtocol() + link.getAddress());
    }

    @Override
    public IValueMap getProperties() {
        return new ValueMap("width=450,height=190");
    }
}
