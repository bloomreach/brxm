/*
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
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.documentworkflow.DocumentVariant;

/**
 * Custom workflow task for versioning a variant node.
 */
public class RetrieveVersionTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private DocumentVariant variant;
    private Calendar historic;

    public DocumentVariant getVariant() {
        return variant;
    }

    public void setVariant(DocumentVariant variant) {
        this.variant = variant;
    }

    public Calendar getHistoric() {
        return historic;
    }

    public void setHistoric(final Calendar historic) {
        this.historic = historic;
    }

    @Override
    public Object doExecute() throws WorkflowException, RepositoryException, RemoteException {

        if (getVariant() == null || getVariant().getNode() == null || getHistoric() == null) {
            throw new WorkflowException("No variant or date provided");
        }
        Node variant = getVariant().getNode();

        final Version node = lookupVersion(variant, getHistoric());
        if (node != null) {
            return new Document(node);
        }
        return null;
    }
}
