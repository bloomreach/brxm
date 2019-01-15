/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.forge.selection.frontend.utils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.onehippo.forge.selection.frontend.Namespace;
import org.onehippo.forge.selection.frontend.model.ListItem;
import org.onehippo.forge.selection.frontend.model.ValueList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrUtils {

    private static final Logger log = LoggerFactory.getLogger(JcrUtils.class);

    public static ValueList getValueList(final String name, final Locale locale, final Session session) {

        final ValueList valuelist = new ValueList();

        if ((name == null) || (name.equals(""))) {
            log.debug("No node name (uuid or path) configured, returning empty value list");
        }
        else {
            Node sourceNode = getSourceNode(name, session);
            if (sourceNode != null) {

                if (locale != null) {
                    sourceNode = getSourceNodeVariant(sourceNode, locale);
                }

                try {
                    log.debug("Start filling map with values from node " + sourceNode.getPath());

                    final NodeIterator iterator = sourceNode.getNodes( Namespace.Type.VALUE_LIST_ITEM);
                    log.debug("Items in the list: {}", iterator.getSize());
                    while (iterator.hasNext()) {
                        final Node node = iterator.nextNode();
                        final String key = node.getProperty(Namespace.Property.KEY).getValue().getString();
                        final String label = node.getProperty(Namespace.Property.LABEL).getValue().getString();

                        valuelist.add(new ListItem(key, label));

                        log.debug("Adding key: {} with value: {} ", key, label);
                    }
                } catch (RepositoryException e) {
                    log.error("RepositoryException occurred while trying to fill the list of values: {}",
                            e.getMessage());
                }
            }
        }

        return valuelist;
    }

    /**
     * Gets the configured JCR node that holds the values for the Value list.
     *
     * @param nodeSource path to the source node
     * @return {@link Node}
     */
    public static Node getSourceNode(final String nodeSource, final Session session) {
        Node valueNode;
        try {
            log.debug("Trying to get node from: {}", nodeSource);

            if (nodeSource.startsWith("/")) {
                final String relativePath = nodeSource.substring(1);
                if (!session.getRootNode().hasNode(relativePath)) {
                    log.warn("Value list node not found at absolute path {}", nodeSource);
                    return null;
                }
                valueNode = session.getRootNode().getNode(relativePath);
            }
            else {
                // assume nodeSource is a uuid
                valueNode = session.getNodeByIdentifier(nodeSource);
            }

            log.debug("Nodetype of valueNode: {}", valueNode.getPrimaryNodeType().getName());
            if (valueNode.getPrimaryNodeType().getName().equals(HippoNodeType.NT_HANDLE)) {
                valueNode = valueNode.getNode(valueNode.getName());
                log.debug("Nodetype of valueNode below configured handle: {}", 
                        valueNode.getPrimaryNodeType().getName());
            }
            log.debug("Found node with name: {}", valueNode.getName());

            return valueNode;

        } catch (IllegalArgumentException e) {
            log.warn("IllegalArgumentException: provided source " + nodeSource + " is probably not a valid node UUID",
                    e);
        } catch (ItemNotFoundException e) {
            log.warn("ItemNotFoundException occurred while trying to get value list node by source " + nodeSource, e);
        } catch (RepositoryException e) {
            log.error("RepositoryException occurred while trying to get value list node by source " + nodeSource, e);
        }
        return null;
    }

    /**
     * Get a possible variant of a source node specified by locale.
     * */
    public static Node getSourceNodeVariant(final Node sourceNode, final Locale locale) {
        Node variant = sourceNode;

        // Check if the document exists in the preferred locale
        try {
            if (sourceNode.hasProperty(HippoTranslationNodeType.ID)) {
                final String id = sourceNode.getProperty(HippoTranslationNodeType.ID).getString();
                final String xpath = "//element(*," + HippoTranslationNodeType.NT_TRANSLATED + ")["
                        + HippoTranslationNodeType.ID + " = '" + id + "' and "
                        + HippoTranslationNodeType.LOCALE + " = '"  + locale.toString() + "']";
                final Query query = sourceNode.getSession().getWorkspace().getQueryManager().createQuery(xpath, "xpath");
                final NodeIterator nodeIterator = query.execute().getNodes();
                if (nodeIterator.hasNext()) {
                    variant = nodeIterator.nextNode();
                    log.debug("Using '{}' valuelist translation: '{}'", locale, variant.getPath());
                } else {
                    log.debug("'{}' valuelist translation of '{}' does not exist", locale, variant.getPath());
                }
            }
        } catch (RepositoryException e) {
            log.warn("RepositoryException getting source node variant", e);
        }

        return variant;
    }

    public static List<String> getValueListNames(final Session session) {
        log.debug("Locating value lists.");
        final LinkedList<String> valueLists = new LinkedList<>();
        try {
            final QueryManager qm = session.getWorkspace().getQueryManager();
            @SuppressWarnings("deprecation")
            final Query query = qm.createQuery("//element(*,"+ Namespace.Type.VALUE_LIST +")", Query.XPATH);
            final NodeIterator iterator = query.execute().getNodes();
            log.debug("Items in the list: {}", iterator.getSize());
            while (iterator.hasNext()) {
                final Node node = iterator.nextNode();
                final Node parent = node.getParent();
                if (parent.isNodeType(HippoNodeType.NT_HANDLE) && parent.isNodeType("mix:referenceable")) {
                    final String identifier = parent.getIdentifier();
                    valueLists.add(identifier);
                    log.debug("Adding identifier: {}", identifier);
                } else if (log.isDebugEnabled()) {
                    log.debug("Skipping {}, parent is not a referenceable handle", node.getPath());
                }
            }
        } catch (RepositoryException e) {
            log.error("RepositoryException occurred while trying to obtain names of value lists: {}", e.getMessage());
        }

        return Collections.unmodifiableList(valueLists);
    }


    private JcrUtils() {}
}
