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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.documentworkflow.DocumentVariant;

/**
 * Custom workflow task for restoring a version.
 */
public class RestoreVersionTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private Document version;

    public Document getVersion() {
        return version;
    }

    public void setVersion(Document version) {
        this.version = version;
    }

    @Override
    public Object doExecute() throws WorkflowException, RepositoryException, RemoteException {
        Node versionNode = getVersion().getNode();
        if (versionNode == null || !(versionNode instanceof Version)) {
            throw new WorkflowException("No version provided");
        }

        Version version = (Version) versionNode;
        String id = version.getContainingHistory().getVersionableIdentifier();
        Node variant = version.getSession().getNodeByIdentifier(id);
        variant.restore(version, true);
        return new DocumentVariant(variant);
    }

}
