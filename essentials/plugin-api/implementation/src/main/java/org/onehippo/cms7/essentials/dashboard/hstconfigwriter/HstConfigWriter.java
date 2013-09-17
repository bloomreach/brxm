/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.dashboard.hstconfigwriter;


import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.onehippo.cms7.essentials.dashboard.hstconfigwriter.model.ConfigNode;
import org.onehippo.cms7.essentials.dashboard.hstconfigwriter.model.HstConfigProperty;
import org.onehippo.cms7.essentials.dashboard.hstconfigwriter.model.ctx.Context;
import org.onehippo.cms7.essentials.dashboard.hstconfigwriter.model.types.ConfigType;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: HstConfigWriter.java 171565 2013-07-24 14:38:15Z mmilicevic $"
 */
public class HstConfigWriter {

    private static Logger log = LoggerFactory.getLogger(HstConfigWriter.class);
    private final Node rootNode;
    private final Node componentNode;
    private final Node pageNode;
    private final Node templateNode;
    private final Node menuNode;
    private final Node sitemapNode;
    private final Node catalogNode;
    private final Session session;
    private final Context context;


    public HstConfigWriter(final Session session, final Node root, final Context context) {
        rootNode = root;
        this.session = session;
        this.context = context;
        try {
            componentNode = root.getNode(ConfigType.COMPONENT.getPath());
            pageNode = root.getNode(ConfigType.PAGE.getPath());
            templateNode = root.getNode(ConfigType.TEMPLATE.getPath());
            menuNode = root.getNode(ConfigType.MENU.getPath());
            sitemapNode = root.getNode(ConfigType.SITEMAP.getPath());
            catalogNode = root.getNode(ConfigType.CATALOG.getPath());
        } catch (RepositoryException e) {
            throw new IllegalArgumentException(e);
        }
        log.debug("Context settings: {}", context);
    }

    public Node getNode(final ConfigNode configNode) throws RepositoryException {
        final Node root = rootNode.getNode(configNode.getType().getPath());
        return root.getNode(configNode.getPath());
    }

    public void write(final ConfigNode configNode) throws RepositoryException {
        log.info("Writing config node {}", configNode);
        if (configNode == null) {
            return;
        }
        final Node root = rootNode.getNode(configNode.getType().getPath());
        if (exists(root, configNode)) {
            log.info("Node already exists");
            if (context.isMerge()) {
                log.info("@merging node properties");
                final Node node = root.getNode(configNode.getPath());
                merge(node, configNode);
            }
        } else {
            writeNode(configNode, root);
        }
        saveSession();
    }

    public void remove(final ConfigNode configNode) throws RepositoryException {
        log.info("Removing config node {}", configNode);
        if (configNode == null) {
            return;
        }
        final Node root = rootNode.getNode(configNode.getType().getPath());
        final String path = configNode.getPath();
        if (root.hasNode(path)) {
            root.getNode(path).remove();
            saveSession();
        }
    }

    private void merge(final Node node, final ConfigNode configNode) throws RepositoryException {
        final List<HstConfigProperty> properties = configNode.getProperties();
        for (HstConfigProperty property : properties) {
            final String name = property.getName();
            if (!node.hasProperty(name)) {
                log.debug("@MERGE: adding {}", property);
                node.setProperty(name, property.getValue());
            } else {
                log.debug("@MERGE: exists {}", property);
            }
        }

        writeChildNodes(configNode, node);
    }

    private Node writeNode(final ConfigNode configNode, final Node parent) throws RepositoryException {
        final Node node;
        if (exists(parent, configNode)) {
            node = parent.getNode(configNode.getName());
        } else {
            node = parent.addNode(configNode.getName(), configNode.getPrimaryType());
        }
        merge(node, configNode);
        return node;
    }

    private void writeChildNodes(final ConfigNode configNode, final Node node) throws RepositoryException {
        final List<ConfigNode> childNodes = configNode.getChildNodes();
        for (ConfigNode childNode : childNodes) {
            final String path = childNode.getPath();
            if (node.hasNode(path)) {
                merge(node.getNode(path), childNode);
            } else {
                writeNode(childNode, node);
            }
        }
    }

    private void saveSession() throws RepositoryException {
        log.debug("@ saving session @");
        if (context.isDebug()) {
            log.warn("DEBUG mode not saving changes");
            GlobalUtils.refreshSession(session, false);
        } else {
            try {
                session.save();
            } catch (RepositoryException e) {
                log.error("Error saving  session", e);
                GlobalUtils.refreshSession(session, false);
            }

        }
    }

    private boolean exists(final Node parent, final ConfigNode configNode) throws RepositoryException {
        return parent.hasNode(configNode.getName());
    }
}
