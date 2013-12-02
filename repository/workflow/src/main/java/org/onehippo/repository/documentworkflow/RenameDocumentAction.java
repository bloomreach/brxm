/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.documentworkflow;

import java.rmi.RemoteException;
import java.util.Collection;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.scxml2.ErrorReporter;
import org.apache.commons.scxml2.EventDispatcher;
import org.apache.commons.scxml2.SCInstance;
import org.apache.commons.scxml2.SCXMLExpressionException;
import org.apache.commons.scxml2.TriggerEvent;
import org.apache.commons.scxml2.model.ModelException;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom action for renaming document.
 */
public class RenameDocumentAction extends AbstractDocumentAction {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(RenameDocumentAction.class);

    private String newNameExpr;

    public String getNewNameExpr() {
        return newNameExpr;
    }

    public void setNewNameExpr(String newNameExpr) {
        this.newNameExpr = newNameExpr;
    }

    @Override
    protected void doExecute(EventDispatcher evtDispatcher, ErrorReporter errRep, SCInstance scInstance, Log appLog,
            Collection<TriggerEvent> derivedEvents) throws ModelException, SCXMLExpressionException,
            RepositoryException {

        String newName = null;

        if (getNewNameExpr() != null) {
            newName = eval(scInstance, getNewNameExpr());
        }

        if (StringUtils.isBlank(newName)) {
            throw new ModelException("New document name is blank.");
        }

        DocumentHandle dm = getDataModel(scInstance);

        PublishableDocument document = null;

        if (dm.getU() != null) {
            document = dm.getU();
        }

        if (document == null) {
            if (dm.getP() != null) {
                document = dm.getP();
            }
        }

        if (document == null) {
            if (dm.getD() != null) {
                document = dm.getD();
            }
        }

        if (document == null) {
            throw new ModelException("No source document found.");
        }

        try {
            // doDepublish();
            DefaultWorkflow defaultWorkflow = (DefaultWorkflow) getWorkflowContext(scInstance).getWorkflow("core", document);
            defaultWorkflow.rename(newName);
        } catch (WorkflowException e) {
            throw new ModelException(e);
        } catch (RemoteException e) {
            throw new ModelException(e);
        }
    }

}
