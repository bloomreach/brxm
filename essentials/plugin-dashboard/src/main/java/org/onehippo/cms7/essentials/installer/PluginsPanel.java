package org.onehippo.cms7.essentials.installer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.onehippo.cms7.essentials.dashboard.DashboardPlugin;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.config.ProjectSettingsBean;
import org.onehippo.cms7.essentials.dashboard.ctx.DashboardPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.setup.ProjectSetupPlugin;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.onehippo.cms7.essentials.installer.panels.BodyPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class PluginsPanel extends Panel {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(PluginsPanel.class);

    private final List<Plugin> pluginList;

    public PluginsPanel(final String id, final PluginContext context, final List<Plugin> pluginList, final List<Plugin> mainPlugins) {
        super(id);

//        pluginList.addAll(mainPlugins);
        this.pluginList = pluginList;
        //pluginList.addAll(mainPlugins);

        List<ITab> tabs = new ArrayList<>();
        tabs.add(new AbstractTab(new Model<>("Installed")) {
            private static final long serialVersionUID = 1L;

            @Override
            public Panel getPanel(String panelId) {
                return new Installed(panelId);
            }
        });

        tabs.add(new AbstractTab(new Model<>("Find additional")) {
            private static final long serialVersionUID = 1L;
            @Override
            public Panel getPanel(String panelId) {
                return new FindAdditional(panelId);
            }
        });
//
        add(new EssentialsTabPanel<>("tabs", tabs));

    }


    /**
     * Panel representing the content panel for the first tab.
     */
    private static class Installed extends Panel {
        private static final long serialVersionUID = 1L;

        /**
         * Constructor
         *
         * @param id component id
         */
        public Installed(String id) {
            super(id);
        }
    }


    /**
     * Panel representing the empty panel
     */
    private static class EmptyPluginPanel extends Panel {
        private static final long serialVersionUID = 1L;
        /**
         * Constructor
         *
         * @param id component id
         */
        public EmptyPluginPanel(String id) {
            super(id);
        }
    }


    /**
     * Panel representing the content panel for the first tab.
     */
    public class FindAdditional extends Panel {
        private static final long serialVersionUID = 1L;
        private Plugin selectedPlugin;
        private Panel configuration;
        // private final Panel panel;

        /**
         * Constructor
         *
         * @param id component id
         */
        public FindAdditional(String id) {
            super(id);

//            Plugin plugin1 = new EssentialsPlugin() {
//
//                @Override
//                public String getType() {
//                    return "enterprise";
//                }
//
//                @Override
//                public String getName() {
//                    return "Relevance";
//                }
//            };
//
//            Plugin plugin2 = new EssentialsPlugin() {
//
//                @Override
//                public String getType() {
//                    return "enterprise";
//                }
//
//                @Override
//                public String getName() {
//                    return "Reporting";
//                }
//            };
//
//            Plugin plugin3 = new EssentialsPlugin() {
//
//                @Override
//                public String getType() {
//                    return "Hippo Certified";
//                }
//
//                @Override
//                public String getName() {
//                    return "Content Blocks";
//                }
//            };

            MultivaluedMap<String, Plugin> map = new MetadataMap<>();

            for (Plugin plugin : pluginList) {
                map.add(plugin.getType(), plugin);
            }

//            map.add(plugin2.getType(), plugin2);
//            map.add(plugin3.getType(), plugin3);

            configuration = new BodyPanel("body");
            add(configuration);

            add(new ListView<EntryWrapper>("type", convertToSerializableEntryModel(map.entrySet())) {

                private static final long serialVersionUID = 1L;

                @Override
                protected void populateItem(final ListItem<EntryWrapper> item) {
                    final EntryWrapper entryWrapper = item.getModelObject();
                    item.add(new Label("type-label", entryWrapper.getKey()));
                    item.add(new ListView<Plugin>("plugin", entryWrapper.getValue()) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        protected void populateItem(final ListItem<Plugin> item) {
                            final Plugin plugin = item.getModelObject();
                            AjaxLink<Void> link = new AjaxLink<Void>("plugin-link") {
                                private static final long serialVersionUID = 1L;
                                @Override
                                public void onClick(final AjaxRequestTarget target) {
                                    onPluginSelected(plugin, target);
                                    // add(new AttributeModifier("class", "selected"));
                                    //target.add(this);
                                   /* System.out.println("install mother fucker");
                                    final EmptyPanel empty = new EmptyPanel("plugin");
                                    empty.setOutputMarkupId(true);
                                    configuration.replace(empty);
                                    target.add(configuration);*/
                                }
                            };
                            link.setOutputMarkupId(true);

                            Label label = new Label("plugin-label", plugin.getName());

                            link.add(label);
                            item.add(link);


                        }
                    });
                }


            });

            configuration.replace(new EmptyPluginPanel("plugin"));
        }

//        private void delegateOnPluginClick(final AjaxRequestTarget target, final Plugin plugin) {
//            System.out.println("install mother fucker: " + plugin.getName());
//            final EmptyPanel empty = new EmptyPanel("plugin");
//            empty.setOutputMarkupId(true);
//            configuration.replace(empty);
//            target.add(configuration);
//        }

        /**
         * Triggered when menu item is clicked
         *
         * @param plugin plugin model of clicked item
         * @param target ajax target of clicked item
         */
        public void onPluginSelected(final Plugin plugin, final AjaxRequestTarget target) {
            log.info("Plugin selected:  {}", plugin);

            final PluginContext context = new DashboardPluginContext(GlobalUtils.createSession(), plugin);
            // inject project settings:
            final ProjectSettingsBean document = context.getConfigService().read(ProjectSetupPlugin.class.getName());
            if (document != null) {
                context.setBeansPackageName(document.getSelectedBeansPackage());
                context.setComponentsPackageName(document.getSelectedComponentsPackage());
                context.setRestPackageName(document.getSelectedRestPackage());
                context.setProjectNamespacePrefix(document.getProjectNamespace());
            }
            final String pluginClass = plugin.getPluginClass();

            selectedPlugin = plugin;


            final DashboardPlugin component = HomePage.instantiatePlugin(plugin, context, pluginClass);
            //add(feedbackPanel);
            log.debug("Created component {}", component);
            if (component != null) {
                configuration.replace(component);
                target.add(configuration);
            }

        }


    }

    /**
     * Serialization utils for wicket:
     */

    public List<EntryWrapper> convertToSerializableEntryModel(Collection<Map.Entry<String, List<Plugin>>> entrySet) {
        List<EntryWrapper> list = new ArrayList<>();
        if (entrySet != null && !entrySet.isEmpty()) {
            for (Map.Entry<String, List<Plugin>> entry : entrySet) {
                list.add(new EntryWrapper(entry));
            }
        }
        return list;
    }

    public static class EntryWrapper implements Serializable {

        private static final long serialVersionUID = 1L;
        private List<Plugin> value;
        private String key;

        public EntryWrapper(final Map.Entry<String, List<Plugin>> entry) {
            this.key = entry.getKey();
            this.value = entry.getValue();
        }

        public String getKey() {
            return key;
        }

        public List<Plugin> getValue() {
            return value;
        }
    }


}
