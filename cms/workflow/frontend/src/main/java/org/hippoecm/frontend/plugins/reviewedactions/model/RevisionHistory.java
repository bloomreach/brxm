/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.reviewedactions.model;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.ocm.JcrObject;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.VersionWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The revision history of a document.  This history is linear, i.e. JCR merging
 * is not handled.  Initialize this object with a document node.
 * <p>
 * True to the limitations in the current versioning implementation, the history
 * only consists of published variants.
 */
public class RevisionHistory extends JcrObject {

    private static final long serialVersionUID = -562404992641520380L;

    static final Logger log = LoggerFactory.getLogger(RevisionHistory.class);

    private transient List<Revision> list;

    public RevisionHistory(JcrNodeModel nodeModel) {
        super(nodeModel);
    }

    public List<Revision> getRevisions() {
        load();
        return list;
    }

    Revision getRevision(int index) {
        load();
        if (index >= 0 && index < list.size()) {
            return list.get(index);
        }
        return null;
    }

    VersionWorkflow getWorkflow() {
        try {
            Node document = getNode();
            WorkflowManager workflowManager = ((HippoWorkspace) getNode().getSession().getWorkspace())
                    .getWorkflowManager();
            return (VersionWorkflow) workflowManager.getWorkflow("versioning", document);
        } catch (RepositoryException ex) {
            log.error(ex.getMessage(), ex);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void processEvents(IObservationContext context, Iterator<? extends IEvent> events) {
    }

    @Override
    protected JcrNodeModel getNodeModel() {
        return super.getNodeModel();
    }

    @Override
    public void detach() {
        list = null;
        super.detach();
    }

    private void load() {
        if (list == null) {
            list = new LinkedList<>();

            try {
                Node subject = getNode();
                Node handle = null;
                if (subject.isNodeType("nt:frozenNode")) {
                    // use hippo:paths to find handle
                    if (subject.hasProperty(HippoNodeType.HIPPO_PATHS)) {
                        Value[] paths = subject.getProperty(HippoNodeType.HIPPO_PATHS).getValues();
                        if (paths.length > 1) {
                            String handleUuid = paths[1].getString();
                            handle = subject.getSession().getNodeByUUID(handleUuid);
                        }
                    }
                } else {
                    handle = subject.getParent();
                }

                VersionWorkflow workflow = getWorkflow();
                if (workflow != null) {
                    try {
                        SortedMap<Calendar, Set<String>> versions = workflow.list();
                        int index = 0;
                        for (Map.Entry<Calendar, Set<String>> entry : versions.entrySet()) {
                            list.add(new Revision(this, entry.getKey(), entry.getValue(), index++, new JcrNodeModel(handle)));
                        }
                    } catch (RemoteException | WorkflowException | MappingException ex) {
                        log.error(ex.getMessage(), ex);
                    }
                }
                Collections.reverse(list);
            } catch (RepositoryException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

}
