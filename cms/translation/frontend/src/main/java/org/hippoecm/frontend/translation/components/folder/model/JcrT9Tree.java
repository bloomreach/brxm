/*
 *  Copyright 2010-2019 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.translation.components.folder.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.translation.HippoTranslationNodeType;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.translations.DocumentTranslationsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrT9Tree extends T9Tree {

    static final Logger log = LoggerFactory.getLogger(JcrT9Tree.class);

    private static final String ROOT_PATH = "/content/documents";

    private final Node targetNode;
    private final Map<String, T9Node> nodes = new HashMap<String, T9Node>();

    public JcrT9Tree(Node node) {
        this.targetNode = node;
    }

    @Override
    public List<T9Node> getChildren(String id) {
        try {
            Node node = this.targetNode.getSession().getNodeByIdentifier(id);
            List<T9Node> results = new LinkedList<T9Node>();
            for (NodeIterator children = node.getNodes(); children.hasNext();) {
                Node child = children.nextNode();
                if (child.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                    results.add(getT9Node(child));
                }
            }
            return results;
        } catch (ItemNotFoundException ex) {
            log.debug("Invalid id {}", id);
        } catch (RepositoryException ex) {
            throw new RuntimeException("Error retrieving node", ex);
        }
        return Collections.emptyList();
    }

    @Override
    public T9Node getNode(String id) {
        try {
            return getT9Node(targetNode.getSession().getNodeByIdentifier(id));
        } catch (ItemNotFoundException ex) {
            log.debug("Invalid id {}", id);
        } catch (RepositoryException ex) {
            throw new RuntimeException("Error retrieving node", ex);
        }
        return null;
    }

    @Override
    public T9Node getRoot() {
        try {
            return getT9Node(targetNode.getSession().getNode(ROOT_PATH));
        } catch (PathNotFoundException ex) {
            log.debug("Could not find root path {}", ROOT_PATH);
        } catch (RepositoryException ex) {
            log.error("Error retrieving root T9Node");
        }
        throw new RuntimeException("Error retrieving root node");
    }

    @Override
    public List<T9Node> getSiblings(String t9Id) {
        try {
            final Set<T9Node> nodes = new HashSet<>();
            final DocumentTranslationsService documentTranslationsService = HippoServiceRegistry.getService(DocumentTranslationsService.class);
            final Map<UUID, String> translations = documentTranslationsService.getTranslations(t9Id);

            final Session session = targetNode.getSession();
            translations.entrySet().forEach(entry -> {
                try {
                    nodes.add(getT9Node(session.getNodeByIdentifier(entry.getKey().toString())));
                } catch (ItemNotFoundException e) {
                    log.info("session cannot read translated node {}", entry.getKey().toString());
                } catch (RepositoryException e) {
                    log.error("Exception while fetching translated node", e);
                }
            });

            return new ArrayList<>(nodes);
        } catch (RepositoryException e) {
            throw new RuntimeException("Unable to retrieve siblings for " + t9Id, e);
        }
    }

    private T9Node getT9Node(Node node) throws RepositoryException {
        String id = node.getIdentifier();
        if (!nodes.containsKey(id)) {
            nodes.put(id, createT9Node(node));
        }
        return nodes.get(id);
    }

    private T9Node createT9Node(Node node) throws RepositoryException {
        Node parent = node.getParent();
        if (parent.getPath().startsWith(ROOT_PATH)) {
            T9Node t9Node = new T9Node(getT9Node(parent), node.getIdentifier());
            String name = ((HippoNode) node).getDisplayName();
            t9Node.setName(name);
            if (node.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                t9Node.setT9id(node.getProperty(HippoTranslationNodeType.ID).getString());
                t9Node.setLang(node.getProperty(HippoTranslationNodeType.LOCALE).getString());
            }
            return t9Node;
        }
        return new T9Node(node.getIdentifier());
    }

}
