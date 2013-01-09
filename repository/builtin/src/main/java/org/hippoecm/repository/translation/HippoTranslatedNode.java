/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.translation;

import java.util.Set;
import java.util.TreeSet;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HippoTranslatedNode {

    private static final Logger log = LoggerFactory.getLogger(HippoTranslatedNode.class);

    private final Node node;

    public HippoTranslatedNode(final Node node) throws RepositoryException {
        this.node = node;
        if (!node.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
            throw new RepositoryException("Invalid node, it is not of type " + HippoTranslationNodeType.NT_TRANSLATED);
        }
    }

    public Set<String> getTranslations() throws RepositoryException {
        final Set<String> available = new TreeSet<String>();
        String id = node.getProperty(HippoTranslationNodeType.ID).getString();
        Query query = node.getSession().getWorkspace().getQueryManager().createQuery(
                "SELECT " + HippoTranslationNodeType.LOCALE
                        + " FROM " + HippoTranslationNodeType.NT_TRANSLATED
                        + " WHERE " + HippoTranslationNodeType.ID + "='" + id + "'",
                Query.SQL);
        final QueryResult result = query.execute();
        final RowIterator rowIterator = result.getRows();
        while (rowIterator.hasNext()) {
            final Row row = rowIterator.nextRow();
            final Value value = row.getValue(HippoTranslationNodeType.LOCALE);
            available.add(value.getString());
        }
        return available;
    }

    public Node getTranslation(String language) throws RepositoryException {
        String id = node.getProperty(HippoTranslationNodeType.ID).getString();
        Query query = node.getSession().getWorkspace().getQueryManager().createQuery(
                "SELECT * FROM " + HippoTranslationNodeType.NT_TRANSLATED
                        + " WHERE " + HippoTranslationNodeType.ID + "='" + id + "'"
                        + " AND " + HippoTranslationNodeType.LOCALE + "='" + language + "'",
                Query.SQL);
        final QueryResult result = query.execute();
        NodeIterator nodes = result.getNodes();
        if (!nodes.hasNext()) {
            throw new ItemNotFoundException("Folder was not translated to " + language);
        }
        if (nodes.getSize() > 1) {
            log.warn("More than one translated variant found for node " + id + " in language " + language);
        }
        return nodes.nextNode();
    }

    public boolean hasTranslation(String language) throws RepositoryException {
        String id = node.getProperty(HippoTranslationNodeType.ID).getString();
        Query query = node.getSession().getWorkspace().getQueryManager().createQuery(
                "SELECT * FROM " + HippoTranslationNodeType.NT_TRANSLATED
                        + " WHERE " + HippoTranslationNodeType.ID + "='" + id + "'"
                        + " AND " + HippoTranslationNodeType.LOCALE + "='" + language + "'",
                Query.SQL);
        final QueryResult result = query.execute();
        NodeIterator nodes = result.getNodes();
        return nodes.hasNext();
    }

    /**
     * returns farthest ancestor from rootSubject of type HippoTranslationNodeType.NT_TRANSLATED and returns null if all ancestors and rootSubject
     * are not of type HippoTranslationNodeType.NT_TRANSLATED
     *
     */
    public Node getFarthestTranslatedAncestor() throws RepositoryException {
        Node jcrRoot = node.getSession().getRootNode();
        Node current = node;
        Node highestAncestorOfTypeTranslated = null;
        while (!current.isSame(jcrRoot)) {
            if (current.isNodeType(HippoTranslationNodeType.NT_TRANSLATED)) {
                highestAncestorOfTypeTranslated = current;
            }
            current = current.getParent();
        }
        return highestAncestorOfTypeTranslated;
    }

    public Node getContainingFolder() throws RepositoryException {
        Node jcrRoot = node.getSession().getRootNode();
        Node parent = node.getParent();
        while (!parent.isSame(jcrRoot)) {
            if (parent.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                return parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    public String getLocale() throws RepositoryException {
        return node.getProperty(HippoTranslationNodeType.LOCALE).getString();
    }

}
