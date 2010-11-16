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

package org.hippoecm.frontend.plugins.xinha.dialog.links;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.IValueMap;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.xinha.dialog.AbstractXinhaDialog;
import org.hippoecm.frontend.plugins.xinha.services.links.ExternalXinhaLink;
import org.hippoecm.frontend.widgets.LabelledBooleanFieldWidget;
import org.hippoecm.frontend.widgets.RequiredTextFieldWidget;
import org.hippoecm.frontend.widgets.TextFieldWidget;

public class ExternalLinkDialog extends AbstractXinhaDialog<ExternalXinhaLink> {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final String SIZE = "49";

    public ExternalLinkDialog(IPluginContext context, IPluginConfig config, IModel<ExternalXinhaLink> model) {
        super(model);
        
        add(new DropDownChoice<String>("protocols",
                new PropertyModel<String>(model, "protocol"), ExternalXinhaLink.PROTOCOLS));

        TextFieldWidget widget;
        add(widget = new RequiredTextFieldWidget("href", new StringPropertyModel(model, "address")){
            private static final long serialVersionUID = 1L;
            
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.addComponent(ExternalLinkDialog.this);
            }
        });
        widget.setSize(SIZE);
        setFocus(widget);
        
        add(widget = new TextFieldWidget("title", new StringPropertyModel(model, ExternalXinhaLink.TITLE)));
        widget.setSize(SIZE);
        
        add(new LabelledBooleanFieldWidget("popup", new PropertyModel<Boolean>(model, "target"),
                new StringResourceModel("labels.popup", this, null)));

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
