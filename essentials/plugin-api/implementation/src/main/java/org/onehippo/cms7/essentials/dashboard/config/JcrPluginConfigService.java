/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.config;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

/**
 * Service used to read/write plugin configuration(s)
 *
 * @version "$Id: JcrPluginConfigService.java 174393 2013-08-20 13:46:56Z mmilicevic $"
 */
public class JcrPluginConfigService implements PluginConfigService {

    public static final String CONFIG_PATH = "dashboard/plugins";
    public static final String NS_FOLDER = "dashboard:folder";
    public static final String NS_DOCUMENT = "dashboard:document";
    private static Logger log = LoggerFactory.getLogger(JcrPluginConfigService.class);
    private final PluginContext context;
    private final Session session;

    public JcrPluginConfigService(final PluginContext context) {
        this.context = context;
        this.session = context.getSession();
    }

    public PluginContext getContext() {
        return context;
    }

    @Override
    public void write(final ConfigDocument document) {
        log.debug("Writing document: {}", document);
        log.debug("Writing node: {}", context);
        try {
            final Node configRoot = getConfigRoot();
            processNode(document, configRoot);
            session.save();
        } catch (RepositoryException e) {
            log.error("Error writing configuration", e);
            refreshSession();
        }

    }

    @Override
    public ConfigDocument read(final String pluginClass) {
        final String path = getFullConfigPath(pluginClass);
        return getConfigDocument(path);
    }



    @Override
    public ConfigDocument read() {
        final String path = getFullConfigPath(context.getDescriptor().getPluginClass());
        return getConfigDocument(path);
    }

    private void populateProperties(final ConfigDocument document, final Node documentNode) throws RepositoryException {
        final PropertyIterator properties = documentNode.getProperties();
        while (properties.hasNext()) {
            final Property property = properties.nextProperty();
            final String name = property.getName();
            log.debug("reading property:  {}", name);
            if(name.startsWith("jcr:")){
                continue;
            }
            final ConfigProperty prop = new PluginConfigProperty(name);
            final Value[] values = property.getValues();
            for (Value value : values) {
                prop.addValue(value.getString());
            }
            document.addProperty(prop);
        }
    }

    private String getFullConfigPath(final CharSequence pluginClass) {

        final List<String> configList = Lists.newLinkedList(Splitter.on('/').split(CONFIG_PATH));
        configList.addAll(Lists.newLinkedList(Splitter.on('.').split(pluginClass)));
        return Joiner.on('/').join(configList);
    }

    @SuppressWarnings("HippoHstCallNodeRefreshInspection")
    private void refreshSession() {
        try {
            session.refresh(false);
        } catch (RepositoryException e) {
            log.error("Error refreshing session", e);
        }
    }

    private ConfigDocument getConfigDocument(final String path) {
        try {
            // NOTE: added null check so we can test dashboard without repository (CMS) running
            if(session==null){
                return null;
            }
            final Node rootNode = session.getRootNode();
            if (rootNode.hasNode(path)) {
                final Node folderNode = rootNode.getNode(path);
                if (folderNode.hasNodes()) {
                    final Node documentNode = folderNode.getNodes().nextNode();
                    final ConfigDocument document = new PluginConfigDocument(documentNode.getName());
                    populateProperties(document, documentNode);
                    return document;
                }
            }
        } catch (RepositoryException e) {
            log.error("Error reading config document", e);
        }


        return null;
    }

    private void processNode(final ConfigDocument document, final Node node) throws RepositoryException {
        final String name = document.getName();
        final Node documentNode;
        if (!node.hasNode(name)) {
            documentNode = node.addNode(name, NS_DOCUMENT);
        } else {
            documentNode = node.getNode(name);
        }
        // write properties:
        final List<ConfigProperty> properties = document.getProperties();
        for (ConfigProperty property : properties) {
            log.debug("Writing property {}", property);
            documentNode.setProperty(property.getPropertyName(), property.getPropertyValuesArray());

        }


    }

    private Node getConfigRoot() throws RepositoryException {
        final Node rootNode = session.getRootNode();
        Node parent = rootNode;
        if (!rootNode.hasNode(CONFIG_PATH)) {
            final Iterable<String> nodeParts = Splitter.on('/').split(CONFIG_PATH);
            parent = populateParts(parent, nodeParts);
        } else {
            parent = rootNode.getNode(CONFIG_PATH);
        }
        // make plugin root:
        final String pluginClass = context.getDescriptor().getPluginClass();

        log.info("Processing plugin class: {}", pluginClass);
        final Iterable<String> classParts = Splitter.on('.').split(pluginClass);
        parent = populateParts(parent, classParts);
        return parent;


    }

    private Node populateParts(Node parent, final Iterable<String> nodeParts) throws RepositoryException {
        for (String nodePart : nodeParts) {
            if (!parent.hasNode(nodePart)) {
                parent = parent.addNode(nodePart, NS_FOLDER);
                log.debug("Created folder: {}", parent.getPath());
            } else {
                parent = parent.getNode(nodePart);
            }
        }
        return parent;
    }


}
