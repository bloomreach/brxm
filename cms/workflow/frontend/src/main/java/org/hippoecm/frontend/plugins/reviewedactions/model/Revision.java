/*
 *  Copyright 2009 Hippo.
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
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Session;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.ocm.JcrObject;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.VersionWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Revision extends JcrObject {

    private static final long serialVersionUID = -9076909924582742292L;

    static final Logger log = LoggerFactory.getLogger(Revision.class);

    JcrNodeModel versionModel;
    private JcrNodeModel handleModel;
    RevisionHistory history;
    Calendar date;
    Set<String> labels;
    int index;

    public Revision(RevisionHistory history, Calendar date, Set<String> labels, int index) {
        super(history.getNodeModel());

        this.history = history;
        this.date = date;
        this.labels = labels;
        this.index = index;
    }

    public Revision(RevisionHistory history, Calendar date, Set<String> labels, int index, JcrNodeModel versionModel,
            JcrNodeModel handleModel) {
        super(history.getNodeModel());

        this.history = history;
        this.date = date;
        this.labels = labels;
        this.index = index;
        this.versionModel = versionModel;
        this.handleModel = handleModel;
    }

    /**
     * The node model for the nt:version node that corresponds to this revision.
     */
    public IModel<Node> getDocument() {
        if (versionModel == null) {
            versionModel = new JcrNodeModel((Node) null);
            VersionWorkflow workflow = history.getWorkflow();
            if (workflow != null) {
                try {
                    Document doc = workflow.retrieve(date);
                    if (doc != null) {
                        versionModel = new JcrNodeModel(UserSession.get().getJcrSession().getNodeByUUID(
                                doc.getIdentity()).getParent());
                    }
                } catch (ItemNotFoundException e) {
                    log.error("Could not find version", e);
                } catch (RepositoryException e) {
                    log.error("Repository error", e);
                } catch (RemoteException e) {
                    log.error("Connection error", e);
                } catch (WorkflowException e) {
                    log.error("Workflow error", e);
                }
            }
        }
        return versionModel;
    }

    public JcrNodeModel getHandle() {
        return handleModel;
    }

    public int getRevisionNumber() {
        return index;
    }

    public Set<String> getLabels() {
        return labels;
    }

    public Date getCreationDate() {
        return date.getTime();
    }

    public Date getExpirationDate() {
        Revision successor = getSuccessor();
        if (successor != null) {
            return successor.getCreationDate();
        }
        return null;
    }

    public Revision getSuccessor() {
        return history.getRevision(index + 1);
    }

    public Revision getPredecessor() {
        return history.getRevision(index - 1);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void processEvents(IObservationContext context, Iterator<? extends IEvent> events) {
    }

    @Override
    public void detach() {
        history.detach();
        if (versionModel != null) {
            versionModel.detach();
        }
        super.detach();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Revision) {
            Revision that = (Revision) obj;
            return that.getNodeModel().equals(getNodeModel()) && (that.index == index);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getNodeModel().hashCode() ^ index ^ 3877;
    }

}
