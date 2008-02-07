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

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Channel;
import org.hippoecm.frontend.plugin.channel.ChannelFactory;
import org.hippoecm.frontend.plugins.admin.linkpicker.LinkPickerDialog;
import org.hippoecm.frontend.template.model.FieldModel;
import org.hippoecm.repository.api.HippoNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkPickerPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(LinkPickerPlugin.class);

    public LinkPickerPlugin(PluginDescriptor pluginDescriptor, IPluginModel pluginModel, Plugin parentPlugin) {
        
        super(pluginDescriptor, new FieldModel(pluginModel, parentPlugin.getPluginManager().getTemplateEngine()),
                parentPlugin);
        FieldModel model = (FieldModel)getPluginModel();  
        
        add(new Label("name", model.getDescriptor().getName()));
        
        Channel incoming = pluginDescriptor.getIncoming();
        ChannelFactory factory = getPluginManager().getChannelFactory();
        HippoNode node = model.getNodeModel().getNode();
        String value = "[...]";
        try {
            value = (node.hasProperty("hippo:docbase") && !"".equals(node.getProperty("hippo:docbase")) ) ? node.getProperty("hippo:docbase").getString() : value ;
        } catch (ValueFormatException e) {
            e.printStackTrace();
        } catch (PathNotFoundException e) {
            e.printStackTrace();
        } catch (RepositoryException e) {
            e.printStackTrace();
        }
        
        //Panel infoPanel = new LookupDialogDefaultInfoPanel("info", nodeModel);
        
        DialogLink linkPicker = new DialogLink("value", value , LinkPickerDialog.class, model.getNodeModel(), incoming, factory);
        add(linkPicker);

        setOutputMarkupId(true);
    }

}
