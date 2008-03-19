/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.plugins.template;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.DialogPageCreator;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.dialog.lookup.LookupDialog;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.ChannelFactory;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.template.model.TemplateModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkPickerPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    private JcrPropertyValueModel valueModel;

    private List<String> nodetypes = new ArrayList<String>();
    private DialogLink linkPicker;
    private Model linkText;

    static final Logger log = LoggerFactory.getLogger(LinkPickerPlugin.class);

    public LinkPickerPlugin(PluginDescriptor pluginDescriptor, IPluginModel pluginModel, Plugin parentPlugin) {
        super(pluginDescriptor, new TemplateModel(pluginModel), parentPlugin);

        TemplateModel tmplModel = (TemplateModel) getPluginModel();
        valueModel = tmplModel.getJcrPropertyValueModel();

        Channel channel = getTopChannel();
        ChannelFactory factory = getPluginManager().getChannelFactory();

        if (pluginDescriptor.getParameter("nodetypes") != null) {
            nodetypes.addAll(pluginDescriptor.getParameter("nodetypes"));
        }

        if (nodetypes.size() == 0) {
            log.debug("No configuration specified for filtering on nodetypes. No filtering will take place.");
        }

        Channel proxy = factory.createChannel();

        // add the dialog link before instantiating the dialog so that a ComponentReference can be created
        // in the dialog.
        final DialogWindow dialogWindow = new DialogWindow("dialog", tmplModel.getNodeModel(), channel, proxy);
        linkText = new Model(getValue());
        linkPicker = new DialogLink("value", linkText, dialogWindow, tmplModel.getNodeModel());
        linkPicker.setOutputMarkupId(true);
        add(linkPicker);

        LookupDialog lookupDialog = new LinkPickerDialog(dialogWindow, valueModel, nodetypes);
        dialogWindow.setPageCreator(new DialogPageCreator(lookupDialog));

        setOutputMarkupId(true);
    }

    @Override
    public void receive(Notification notification) {
        if ("select".equals(notification.getOperation())) {
            setPluginModel(new JcrNodeModel(notification.getModel()));
            notification.getContext().addRefresh(this);
        } else if ("flush".equals(notification.getOperation())) {
            linkText.setObject(getValue());
            notification.getContext().addRefresh(linkPicker);
        }
        super.receive(notification);
    }

    private String getValue() {
        String value = (String) valueModel.getObject();
        if (value == null || "".equals(value)) {
            value = "[...]";
        }
        return value;
    }
}
