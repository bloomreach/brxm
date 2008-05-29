package org.hippoecm.frontend.plugins.cms.management;

import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugin.channel.Notification;
import org.hippoecm.frontend.plugins.standards.list.AbstractListingPlugin;
import org.hippoecm.frontend.plugins.standards.list.datatable.CustomizableDocumentListingDataTable;

public abstract class QueryListPlugin extends AbstractListingPlugin {
    private static final long serialVersionUID = 1L;
    
    private FlushableSortableDataProvider dataProvider;
    
    public QueryListPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
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
        if(notification.getOperation().equals("flush")) {
            flushDataProvider();
        }
        super.receive(notification);
    }
    
    private ISortableDataProvider getDataProvider() {
        if(dataProvider == null) {
            dataProvider = createDataProvider();
        }
        return dataProvider;
    }
    
    protected void flushDataProvider() {
        dataProvider.flush();
    }
    
    protected abstract FlushableSortableDataProvider createDataProvider();
    
}
