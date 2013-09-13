package org.onehippo.cms7.essentials.installer.panels;

import java.util.List;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.PageableListView;
import org.apache.wicket.markup.html.navigation.paging.PagingNavigator;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.installer.PluginDetails;
import org.onehippo.cms7.essentials.installer.PluginHelper;

/**
 * @author Jeroen Reijn
 */
public class InstalledPluginsPanel extends Panel {

    private static final long serialVersionUID = 1L;

    public InstalledPluginsPanel(final String id) {
        super(id);
        final PageableListView<Plugin> listView;

        final List<Plugin> pluginList = PluginHelper.getPluginsFromServletContext(WebApplication.get().getServletContext());

        add(listView = new PageableListView<Plugin>("essentials", new PropertyModel<List<Plugin>>(this,"essentials"), 4)
        {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(final ListItem<Plugin> listItem)
            {
                final Plugin plugin = listItem.getModelObject();
                listItem.add(PluginDetails.link("details", plugin, getLocalizer().getString("noPluginTitle", this)));
                listItem.add(moveUpLink("moveUp", listItem));
                listItem.add(moveDownLink("moveDown", listItem));
                listItem.add(removeLink("remove", listItem));
            }
        });
        add(new PagingNavigator("navigator", listView));

    }

    public List<Plugin> getPlugins() {
        return PluginHelper.getPluginsFromServletContext(WebApplication.get().getServletContext());
    }

}
