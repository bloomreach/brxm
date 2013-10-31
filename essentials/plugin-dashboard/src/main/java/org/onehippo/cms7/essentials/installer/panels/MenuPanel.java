package org.onehippo.cms7.essentials.installer.panels;

import java.util.List;

import com.google.common.base.Strings;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.onehippo.cms7.essentials.dashboard.installer.InstallablePlugin;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.installer.HomePage;

/**
 * @author Jeroen Reijn
 */
public class MenuPanel extends Panel {

    private static final long serialVersionUID = 1L;

    public MenuPanel(final String id, final HomePage dashboard, final List<Plugin> pluginList, final List<Plugin> mainPlugins) {

        super(id);
        setOutputMarkupId(true);
        //############################################
        // MAIN ITEMS
        //############################################
        final ListView<Plugin> mainPluginsListView = new ListView<Plugin>("mainPlugins", mainPlugins) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<Plugin> plugin) {
                final Plugin model = plugin.getModelObject();
                final AjaxLink<Void> link = new AjaxLink<Void>("item") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(final AjaxRequestTarget ajaxRequestTarget) {
                        dashboard.onPluginSelected(model, ajaxRequestTarget);
                    }
                };
                plugin.add(link);
                link.add(new Label("itemLabel", model.getName()));
                if (!Strings.isNullOrEmpty(model.getIcon())) {
                    link.add(new AttributeModifier("class", new Model<>(model.getIcon())));
                }
            }
        };
        if(mainPlugins.size()==0) {
            mainPluginsListView.setVisible(false);
        }
        add(mainPluginsListView);

        //############################################
        // NORMAL PLUGINS
        //############################################


        final ListView<Plugin> menuItemsPluginListView = new ListView<Plugin>("menuItems", pluginList) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<Plugin> plugin) {
                final Plugin model = plugin.getModelObject();

                final AjaxLink<Void> link = new AjaxLink<Void>("item") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(final AjaxRequestTarget ajaxRequestTarget) {
                        dashboard.onPluginSelected(plugin.getModelObject(), ajaxRequestTarget);
                    }
                };
                if (plugin.getModel().getObject().equals(dashboard.getSelectedPlugin())) {
                    plugin.add(new AttributeAppender("class", Model.of("active"), " "));
                }
                plugin.add(link);
                link.add(new Label("itemLabel", plugin.getModelObject().getName()));

                if(model instanceof InstallablePlugin) {
                    if(!((InstallablePlugin) model).isInstalled()){
                        plugin.setVisible(false);
                    }
                }

            }
        };
        menuItemsPluginListView.setOutputMarkupId(true);
        add(menuItemsPluginListView);


        add(new AjaxLink<Plugin>("dashboardLink",null) {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(final AjaxRequestTarget target) {
                dashboard.onSetupSelected(target);
            }
        });

        add(new AjaxLink<Plugin>("marketplaceLink",null) {
            private static final long serialVersionUID = 1L;

            //final Plugin model = plugin.getModelObject();
            @Override
            public void onClick(final AjaxRequestTarget target) {
                dashboard.onMarketplaceSelected(target);
            }
        });

    }

}
