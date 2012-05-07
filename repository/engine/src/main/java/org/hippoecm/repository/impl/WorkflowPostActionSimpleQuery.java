/*
 *  Copyright 2011 Hippo.
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

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.WorkflowEventWorkflow;
import org.hippoecm.repository.standardworkflow.WorkflowEventsWorkflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This class is not part of a public accessible API or extensible interface */
class WorkflowPostActionSimpleQuery implements WorkflowPostActions {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id: ";

    static final Logger log = LoggerFactory.getLogger(WorkflowPostActionSimpleQuery.class);

    WorkflowManagerImpl workflowManager;
    String sourceIdentity;
    boolean isDocumentResult;
    Node wfSubject;
    Node wfNode;
    Set<String> preconditionSet;
    String workflowCategory;
    String workflowMethod;

    WorkflowPostActionSimpleQuery(WorkflowManagerImpl workflowManager, Node wfSubject, boolean isDocumentResult, Node wfNode, String workflowCategory, String workflowMethod) throws RepositoryException {
        this.workflowManager = workflowManager;
        this.sourceIdentity = wfSubject.getIdentifier();
        this.wfSubject = wfSubject;
        this.isDocumentResult = isDocumentResult;
        this.wfNode = wfNode;
        this.workflowCategory = workflowCategory;
        this.workflowMethod = workflowMethod;
        if (wfNode.hasNode("hipposys:eventdocument")) {
            // TODO
        } else if (wfNode.hasProperty("hipposys:eventdocument")) {
            this.wfSubject = wfNode.getProperty("hipposys:eventdocument").getNode();
        }
        if (wfNode.hasNode("hipposys:eventprecondition")) {
            Query preQuery = workflowManager.rootSession.getWorkspace().getQueryManager().getQuery(wfNode.getNode("hipposys:eventprecondition"));
            preconditionSet = evaluateQuery(preQuery, null);
        } else {
            preconditionSet = new HashSet<String>();
        }
    }

    private Set<String> evaluateQuery(Query query, String resultIdentity) throws RepositoryException {
        Set<String> result = new HashSet<String>();
        ValueFactory valueFactory = workflowManager.rootSession.getValueFactory();
        Map<String,Value> bindValues = new HashMap<String,Value>();
        bindValues.put("subject", valueFactory.createValue(sourceIdentity));
        if (isDocumentResult && resultIdentity != null) {
            bindValues.put("result", valueFactory.createValue(resultIdentity));
        }
        bindValues.put("workflowCategory", valueFactory.createValue(workflowCategory));
        bindValues.put("workflowMethod", valueFactory.createValue(workflowMethod));
        for (String bindVariableName : query.getBindVariableNames()) {
            if (bindValues.containsKey(bindVariableName)) {
                query.bindValue(bindVariableName, bindValues.get(bindVariableName));
            }
        }
        QueryResult queryResult = query.execute();
        String selectorName = null;
        String[] selectorNames = queryResult.getSelectorNames();
        if (selectorNames != null && selectorNames.length > 0) {
            selectorName = selectorNames[0];
        }
        RowIterator rows = queryResult.getRows();
        while (rows.hasNext()) {
            while (rows.hasNext()) {
                Row row = rows.nextRow();
                String id;
                if (selectorName != null) {
                    id = row.getNode(selectorName).getIdentifier();
                } else {
                    id = row.getNode().getIdentifier();
                }
                result.add(id);
            }
        }
        return result;
    }

    public void execute(Object returnObject) {
        String resultIdentity = null;
        if (isDocumentResult) {
            if (returnObject != null) {
                resultIdentity = ((Document)returnObject).getIdentity();
            }
        }
        try {
            Query postQuery = (wfNode.hasNode("hipposys:eventpostcondition") ? workflowManager.rootSession.getWorkspace().getQueryManager().getQuery(wfNode.getNode("hipposys:eventpostcondition")) : null);
            Set<String> postconditionSet = null;
            if (postQuery != null) {
                postconditionSet = evaluateQuery(postQuery, (resultIdentity == null ? "" : resultIdentity));
                String conditionOperator = "post\\pre";
                if (wfNode.hasProperty("hipposys:eventconditionoperator")) {
                    conditionOperator = wfNode.getProperty("hipposys:eventconditionoperator").getString();
                }
                if (conditionOperator.equals("post\\pre")) {
                    postconditionSet.removeAll(preconditionSet);
                    if (postconditionSet.isEmpty()) {
                        return;
                    }
                } else {
                    log.warn("workflow event operator " + conditionOperator + " unrecognized");
                }
            }
            Workflow workflow = workflowManager.getWorkflowInternal(wfNode, wfSubject);
            if (workflow instanceof WorkflowEventWorkflow) {
                WorkflowEventWorkflow event = (WorkflowEventWorkflow)workflow;
                if (event instanceof WorkflowEventsWorkflow) {
                    ((WorkflowEventsWorkflow)event).setWorkflowCategory(workflowCategory);
                    ((WorkflowEventsWorkflow)event).setWorkflowMethod(workflowMethod);
                }
                try {
                    if (postconditionSet != null) {
                        final Iterator<String> postconditionSetIterator = postconditionSet.iterator();
                        event.fire(new Iterator<Document>() {
                            public boolean hasNext() {
                                return postconditionSetIterator.hasNext();
                            }
                            public Document next() {
                                String id = postconditionSetIterator.next();
                                try {
                                    Node node = workflowManager.rootSession.getNodeByIdentifier(id);
                                    if (node.isNodeType("hippo:handle")) {
                                        if (node.hasNode(node.getName())) {
                                            id = node.getNode(node.getName()).getIdentifier();
                                        }
                                    }
                                } catch (RepositoryException ex) {
                                    // deliberate ignore of error, possible because document has been deleted, denied, but id is still relevant
                                }
                                return new Document(id);
                            }
                            public void remove() {
                                throw new UnsupportedOperationException();
                            }
                        });
                    } else {
                        if (isDocumentResult) {
                            event.fire((Document)returnObject);
                        } else {
                            event.fire();
                        }
                    }
                } catch (WorkflowException ex) {
                    log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                } catch (MappingException ex) {
                    log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                } catch (RemoteException ex) {
                    log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        }
    }

    public void dispose() {
    }
}
