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
package org.hippoecm.frontend.plugins.cms.management;

import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugins.standards.list.AbstractListingPlugin;
import org.hippoecm.frontend.plugins.standards.list.datatable.CustomizableDocumentListingDataTable;

public abstract class FlushableListingPlugin extends AbstractListingPlugin {
    private static final long serialVersionUID = 1L;

    private SortableDataProvider dataProvider;

    public FlushableListingPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);
    }

    @Override
    protected CustomizableDocumentListingDataTable getTable(IPluginModel model) {
        CustomizableDocumentListingDataTable dataTable = new CustomizableDocumentListingDataTable("table", columns,
                getDataProvider(), pageSize, false);
        dataTable.addBottomPaging(viewSize);
        dataTable.addTopColumnHeaders();
        return dataTable;
    }

    @Override
    public void receive(Notification notification) {
        if (notification.getOperation().equals("flush")) {
            flushDataProvider();
        }
        super.receive(notification);
    }

    private ISortableDataProvider getDataProvider() {
        if (dataProvider == null) {
            dataProvider = createDataProvider();
        }
        return dataProvider;
    }

    protected void flushDataProvider() {
        dataProvider.detach();
    }

    protected abstract SortableDataProvider createDataProvider();

}
