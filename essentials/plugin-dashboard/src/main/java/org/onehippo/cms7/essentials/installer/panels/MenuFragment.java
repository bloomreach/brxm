/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.installer.panels;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.Model;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.installer.InstallablePlugin;
import org.onehippo.cms7.essentials.installer.HomePage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
public class MenuFragment extends Fragment {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(MenuFragment.class);

    public MenuFragment(final String id, final String markupId, final MarkupContainer markupProvider, final HomePage dashboard, final List<Plugin> pluginList, final List<Plugin> mainPlugins) {
        super(id, markupId, markupProvider);
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
        if (mainPlugins.size() == 0) {
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

                if (model instanceof InstallablePlugin) {
                    if (!((InstallablePlugin) model).isInstalled()) {
                        plugin.setVisible(false);
                    }
                }

            }
        };
        menuItemsPluginListView.setOutputMarkupId(true);
        add(menuItemsPluginListView);


        add(new AjaxLink<Plugin>("dashboardLink", null) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(final AjaxRequestTarget target) {
                dashboard.onSetupSelected(target);
            }
        });

        add(new AjaxLink<Plugin>("marketplaceLink", null) {
            private static final long serialVersionUID = 1L;

            //final Plugin model = plugin.getModelObject();
            @Override
            public void onClick(final AjaxRequestTarget target) {
                dashboard.onMarketplaceSelected(target);
            }
        });

    }


}
