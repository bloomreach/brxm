package org.onehippo.cms7.essentials.installer.panels;

import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.rest.client.RestClient;
import org.onehippo.cms7.essentials.rest.model.PluginRestful;
import org.onehippo.cms7.essentials.rest.model.RestfulList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

/**
 * @author Jeroen Reijn
 */
public class MarketplacePanel extends Panel {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(MarketplacePanel.class);

    public MarketplacePanel(final String id, final Iterable<Plugin> allPlugins, final EventBus eventBus) {
        super(id);

        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);
        //add search bar
        //add list overview
        // plugins:
        // TODO make proper REST cal
        final RestfulList<PluginRestful> pluginList = new RestClient("TODO").getPlugins();
        final List<PluginRestful> plugins = pluginList.getItems();

        final ListView<PluginRestful> pluginListView = new ListView<PluginRestful>("plugins", plugins) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<PluginRestful> item) {
                final IModel<PluginRestful> model = item.getModel();
                final PluginRestful plugin = model.getObject();
                item.add(new Label("title", plugin.getTitle()));
                final Label description = new Label("description", plugin.getIntroduction());
                description.setEscapeModelStrings(false);
                item.add(description);
                final Label vendor = new Label("vendor", plugin.getVendor().getName());
                item.add(vendor);
            }
        };

        add(pluginListView);

    }

}
