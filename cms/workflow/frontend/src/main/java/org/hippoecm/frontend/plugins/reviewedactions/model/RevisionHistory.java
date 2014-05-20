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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import javax.jcr.RepositoryException;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The revision history of a document.  This history is linear, i.e. JCR merging
 * is not handled.  Initialize this object with a document node.
 * <p>
 * True to the limitations in the current versioning implementation, the history
 * only consists of published variants.
 */
public class RevisionHistory implements IDetachable {

    private static final long serialVersionUID = -562404992641520380L;

    static final Logger log = LoggerFactory.getLogger(RevisionHistory.class);

    private final WorkflowDescriptorModel wdm;

    private transient List<Revision> list;

    public RevisionHistory(WorkflowDescriptorModel nodeModel) {
        this.wdm = nodeModel;
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

    @Override
    public void detach() {
        list = null;
        wdm.detach();
    }

    private void load() {
        if (list == null) {
            list = new LinkedList<>();
            try {
                DocumentWorkflow workflow = getWorkflow();
                if (workflow != null) {
                    final SortedMap<Calendar, Set<String>> versions = workflow.listVersions();
                    int index = versions.size();
                    for (Map.Entry<Calendar, Set<String>> entry : versions.entrySet()) {
                        list.add(new Revision(this, entry.getKey(), entry.getValue(), --index, new JcrNodeModel(wdm.getNode())));
                    }
                }
                Collections.reverse(list);
            } catch (RepositoryException | WorkflowException | RemoteException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public DocumentWorkflow getWorkflow() throws RepositoryException {
        return wdm.getWorkflow();
    }

}
