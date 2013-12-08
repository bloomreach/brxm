/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.xinha.gui;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.eclipse.jface.text.projection.Fragment;
import org.onehippo.cms7.essentials.dashboard.DashboardPlugin;
import org.onehippo.cms7.essentials.dashboard.Plugin;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.MultiValueJcrUtils;
import org.onehippo.cms7.essentials.dashboard.utils.XmlUtils;
import org.onehippo.cms7.essentials.dashboard.utils.xml.XmlNode;
import org.onehippo.cms7.essentials.dashboard.utils.xml.XmlProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: XinhaPlugin.java 174001 2013-08-16 09:24:04Z mmilicevic $"
 */
public class XinhaPlugin extends DashboardPlugin {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(XinhaPlugin.class);

    public XinhaPlugin(final String id, final Plugin descriptor, final PluginContext context) {
        super(id, descriptor, context);

        final List<String> availableToolBarList = getAvailableList(Property.TOOLBAR);
        final List<String> currentlySelectedToolBarList = getSelectedList(Property.TOOLBAR);

        final List<String> availablePluginList = getAvailableList(Property.PLUGIN);
        final List<String> currentlySelectedPluginList = getSelectedList(Property.PLUGIN);

        final List<XinhaEntry> mergedToolbarList = merge(availableToolBarList, currentlySelectedToolBarList);
        //final XinhaPluginDataProvider xinhaToolbarDataProvider = new XinhaPluginDataProvider(mergedToolbarList);
        /*
        GridView<XinhaEntry> toolbar = new XinhaGridView("toolbar", xinhaToolbarDataProvider, Property.TOOLBAR);
        add(toolbar);

        final List<XinhaEntry> mergedPluginList = merge(availablePluginList, currentlySelectedPluginList);
        final XinhaPluginDataProvider xinhaPluginProvider = new XinhaPluginDataProvider(mergedPluginList);
        GridView<XinhaEntry> plugins = new XinhaGridView("plugins", xinhaPluginProvider, Property.PLUGIN);
        add(plugins);
        */

    }

    /*private class XinhaGridView extends GridView<XinhaEntry> {

        private static final long serialVersionUID = 1L;
        private final Property property;

        *//**
         * @param id           component id
         * @param dataProvider data provider
         *//*
        public XinhaGridView(final String id, final IDataProvider<XinhaEntry> dataProvider, final Property property) {
            super(id, dataProvider);
            this.property = property;
            setColumns(5);
        }

        @Override
        protected void populateEmptyItem(final Item<XinhaEntry> item) {
            item.add(new EmptyPanel("item"));
        }

        @Override
        protected void populateItem(final Item<XinhaEntry> item) {
            final XinhaEntry modelObject = item.getModelObject();
            final int index = item.getIndex();
            Fragment fragment = new Fragment("item", "xinha-box", XinhaPlugin.this);
            AjaxCheckBox checkBox = new AjaxCheckBox("checkbox", new Model<>(modelObject.getValue())) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    final Boolean value = getModelObject();
                    setItem(modelObject.getKey(), index, value, property);
                }
            };
            fragment.add(checkBox);
            fragment.add(new Label("label", modelObject.getKey()));
            item.add(fragment);
        }

    }*/

    private boolean setItem(String item, int index, boolean enable, Property property) {
        final Session session = getContext().getSession();
        try {
            if (session.itemExists("/hippo:namespaces/hippostd/html/editor:templates/_default_/root")) {
                final Node currentXinhaConfigurationNode = session.getNode("/hippo:namespaces/hippostd/html/editor:templates/_default_/root");
                if (enable) {
                    MultiValueJcrUtils.addMultiValuePropertyValue(currentXinhaConfigurationNode, property.getProperty(), item, true, index);
                } else {
                    MultiValueJcrUtils.deleteMultiValuePropertyValue(currentXinhaConfigurationNode, property.getProperty(), item);
                }
                session.save();
                return true;
            }
        } catch (RepositoryException e) {
            log.error("Exception while trying to set item.", e);
        }
        return false;
    }


    public List<String> getAvailableList(Property property) {
        final InputStream resource = getClass().getResourceAsStream("/root.xml");
        final XmlNode xmlNode = XmlUtils.parseXml(resource);
        final XmlProperty toolbar = xmlNode.getXmlPropertyByName(property.getProperty());
        return new ArrayList<>(toolbar.getValues());
    }

    public List<String> getSelectedList(Property property) {
        final Session session = getContext().getSession();
        final List<String> list = new ArrayList<>();
        try {
            if (session.itemExists("/hippo:namespaces/hippostd/html/editor:templates/_default_/root")) {
                final Node currentXinhaConfigurationNode = session.getNode("/hippo:namespaces/hippostd/html/editor:templates/_default_/root");
                final Value[] values = currentXinhaConfigurationNode.getProperty(property.getProperty()).getValues();
                for (Value value : values) {
                    list.add(value.getString());
                }
            }
        } catch (RepositoryException e) {
            log.error("Repository exception while trying to retrieve the current xinha plug-ins. {}", e);
        }
        return list;
    }

    /*private static class XinhaPluginDataProvider implements IDataProvider<XinhaEntry> {

        private static final long serialVersionUID = 1L;
        private List<XinhaEntry> entries;

        private XinhaPluginDataProvider(List<XinhaEntry> entries) {
            this.entries = entries;
        }

        @Override
        public Iterator<XinhaEntry> iterator(final long first, final long count) {
            return entries.iterator();
        }

        @Override
        public long size() {
            return entries.size();
        }

        @Override
        public IModel<XinhaEntry> model(final XinhaEntry object) {
            return new Model<>(object);
        }


        @Override
        public void detach() {
        }
    }*/

    private enum Property {
        TOOLBAR("Xinha.config.toolbar"), PLUGIN("Xinha.plugins");

        private String property;

        Property(final String property) {
            this.property = property;
        }

        public String getProperty() {
            return property;
        }
    }

    public List<XinhaEntry> merge(List<String> available, List<String> selected) {
        List<XinhaEntry> xinhaEntries = new ArrayList<>();
        for (String av : available) {
            xinhaEntries.add(new XinhaEntry(av, selected.contains(av)));
        }
        return xinhaEntries;
    }

    private static class XinhaEntry implements Serializable {

        private static final long serialVersionUID = 1L;
        private String key;
        private Boolean value;

        private XinhaEntry(String key, Boolean value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public Boolean getValue() {
            return value;
        }
    }


}
