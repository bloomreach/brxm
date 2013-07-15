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
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentManagerImpl implements DocumentManager, HippoSession.CloseCallback {

    private final Logger log = LoggerFactory.getLogger(DocumentManagerImpl.class);

    private Session session;
    private Node configuration;

    public DocumentManagerImpl(Session session) {
        this.session = session;
        try {
            configuration = session.getRootNode().getNode(HippoNodeType.CONFIGURATION_PATH + "/" +
                                                          HippoNodeType.DOCUMENTS_PATH);
        } catch(RepositoryException ex) {
            log.error("document manager configuration failed: "+ex.getMessage());
        }
        if (session instanceof HippoSession) {
            ((HippoSession)session).registerSessionCloseCallback(this);
        }
    }

    public void close() {
    }

    public Session getSession() {
        return session;
    }

    public Document getDocument(String category, String identifier) throws RepositoryException {
        try {
            Node queryNode = configuration.getNode(category);
            QueryResult result;
            Query query = session.getWorkspace().getQueryManager().getQuery(queryNode);
            if (query instanceof HippoQuery) {
                HippoQuery hippoQuery = (HippoQuery)session.getWorkspace().getQueryManager().getQuery(queryNode);
                if (hippoQuery.getArgumentCount() > 0) {
                    Map<String, String> arguments = new TreeMap<String, String>();
                    String[] queryArguments = hippoQuery.getArguments();
                    for (int i = 0; i < queryArguments.length; i++) {
                        arguments.put(queryArguments[i], identifier);
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
                if(queryNode.isNodeType(HippoNodeType.NT_OCMQUERY) || queryNode.isNodeType(HippoNodeType.NT_WORKFLOW)) {
                    try {
                        String className = queryNode.getProperty(HippoNodeType.HIPPO_CLASSNAME).getString();
                        Class clazz = Class.forName(className);
                        Document document = (Document)clazz.newInstance();
                        document.initialize(resultNode);
                        return document;
                    } catch (Exception e) {
                        // TODO
                        e.printStackTrace();
                        return null;
                    }
                } else {
                    return new Document(resultNode);
                }
            } else {
                return null;
            }
        } catch(PathNotFoundException ex) {
            log.warn(ex.getClass().getName()+": "+ex.getMessage(), ex);
            /* getDocument cannot and should not be used to create documents.
             * null is a valid way to check whether the document looked for exist,
             * as this is the only way for e.g. Workflow plugins to lookup
             * documents.
             */
            return null;
        }
    }
}
