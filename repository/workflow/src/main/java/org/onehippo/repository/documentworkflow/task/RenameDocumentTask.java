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
package org.onehippo.repository.documentworkflow.task;

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DefaultWorkflow;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom workflow task for renaming document.
 */
public class RenameDocumentTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(RenameDocumentTask.class);

    private String newName;

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    @Override
    public Object doExecute() throws WorkflowException, RepositoryException, RemoteException {
        if (StringUtils.isBlank(newName)) {
            throw new WorkflowException("New document name is blank.");
        }

        DocumentHandle dm = getDocumentHandle();

        DocumentVariant document = null;

        if (dm.getUnpublished() != null) {
            document = dm.getUnpublished();
        }

        if (document == null) {
            if (dm.getPublished() != null) {
                document = dm.getPublished();
            }
        }

        if (document == null) {
            if (dm.getDraft() != null) {
                document = dm.getDraft();
            }
        }

        if (document == null) {
            throw new WorkflowException("No source document found.");
        }

        // doDepublish();
        DefaultWorkflow defaultWorkflow = (DefaultWorkflow) dm.getWorkflowContext().getWorkflow("core", document);
        defaultWorkflow.rename(newName);

        return null;
    }

}
