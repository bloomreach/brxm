/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.impl;

import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoQuery;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentManagerImpl implements DocumentManager {

    private final Logger log = LoggerFactory.getLogger(DocumentManagerImpl.class);

    private Session session;
    private Node configuration;

    public DocumentManagerImpl(Session session) {
        this.session = session;
        try {
            configuration = session.getNode("/" + HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.DOCUMENTS_PATH);
        } catch(RepositoryException e) {
            log.error("Document manager configuration failed: " + e);
        }
    }

    public Session getSession() {
        return session;
    }

    public Document getDocument(String category, String identifier) throws RepositoryException {
        final Node queryNode;
        try {
            queryNode = configuration.getNode(category);
        } catch(PathNotFoundException ex) {
            log.warn("No such documents category: " + category);
            return null;
        }

        QueryResult result;
        final Query query = session.getWorkspace().getQueryManager().getQuery(queryNode);
        if (query instanceof HippoQuery) {
            final HippoQuery hippoQuery = (HippoQuery) query;
            if (hippoQuery.getArgumentCount() > 0) {
                final Map<String, String> arguments = new TreeMap<String, String>();
                for (final String argument : hippoQuery.getArguments()) {
                    arguments.put(argument, identifier);
                }
                result = hippoQuery.execute(arguments);
            } else {
                result = hippoQuery.execute();
            }
        } else {
            String[] bindVariableNames = query.getBindVariableNames();
            for (int i = 0; bindVariableNames != null && i < bindVariableNames.length; i++) {
                query.bindValue(bindVariableNames[i], session.getValueFactory().createValue(identifier));
            }
            result = query.execute();
        }
        RowIterator iter = result.getRows();
        String selectorName = (result.getSelectorNames().length > 1 ? result.getSelectorNames()[result.getSelectorNames().length - 1] : null);
        if (iter.hasNext()) {
            Node resultNode = null;
            while (iter.hasNext()) {
                Row resultRow = iter.nextRow();
                Node node = (selectorName != null ? resultRow.getNode(selectorName) : resultRow.getNode());
                if (node != null) {
                    if (resultNode == null || node.getPath().length() > resultNode.getPath().length()) {
                        resultNode = node;
                    }
                }
            }
            if (resultNode != null) {
                if(queryNode.isNodeType(HippoNodeType.NT_OCMQUERY) || queryNode.isNodeType(HippoNodeType.NT_WORKFLOW)) {
                    final String className = JcrUtils.getStringProperty(queryNode, HippoNodeType.HIPPO_CLASSNAME, Document.class.getName());
                    try {
                        final Class clazz = Class.forName(className);
                        final Document document = (Document)clazz.newInstance();
                        document.initialize(resultNode);
                        return document;
                    } catch (ClassNotFoundException e) {
                        log.error("Cannot create document of type " + className + ": " + e);
                    } catch (InstantiationException e) {
                        log.error("Failed to create document of type " + className + ": " + e);
                    } catch (IllegalAccessException e) {
                        log.error("Failed to create document of type " + className + ": " + e);
                    }
                } else {
                    return new Document(resultNode);
                }
            }
        }

        return null;
    }
}
