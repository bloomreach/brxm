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
package org.hippoecm.cmsprototype.frontend.plugins.list;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.repeater.data.table.AjaxFallbackDefaultDataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.JcrEvent;
import org.hippoecm.frontend.plugin.PluginEvent;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;

public class ListPlugin extends Plugin {

    private static final long serialVersionUID = 1L;
    
    AjaxFallbackDefaultDataTable dataTable;
    List columns;

    public ListPlugin(PluginDescriptor pluginDescriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        columns = new ArrayList();
        columns.add(new PropertyColumn(new Model("Name"), "name", "name"));
        columns.add(new PropertyColumn(new Model("Path"), "path"));

        dataTable = new AjaxFallbackDefaultDataTable("table", columns, new SortableJcrDataProvider(model), 10);
        add(dataTable);
    }

    public void update(AjaxRequestTarget target, PluginEvent event) {
        JcrNodeModel nodeModel = event.getNodeModel(JcrEvent.NEW_MODEL);
        if (nodeModel != null) {
            setModel(nodeModel);
            remove(dataTable);
            dataTable = new AjaxFallbackDefaultDataTable("table", columns, new SortableJcrDataProvider(nodeModel), 10);
            add(dataTable);
        }
        if (target != null) {
            target.addComponent(this);
        }
        

    }

    
    
    
}
