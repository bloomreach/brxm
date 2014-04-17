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
import java.util.Collections;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.onehippo.repository.util.JcrConstants;

/**
 * Custom workflow task for retrieving the list of versions of a variant node.
 */
public class ListVersionsVariantTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private DocumentVariant variant;

    public DocumentVariant getVariant() {
        return variant;
    }

    public void setVariant(DocumentVariant variant) {
        this.variant = variant;
    }

    @Override
    public Object doExecute() throws WorkflowException, RepositoryException, RemoteException {
        if (getVariant() != null && getVariant().hasNode()) {
            Node subject = getVariant().getNode(getWorkflowContext().getInternalWorkflowSession());
            if (subject.isNodeType(JcrConstants.MIX_VERSIONABLE)) {
                final SortedMap<Calendar, Set<String>> listing = new TreeMap<>();
                VersionHistory versionHistory = subject.getVersionHistory();

                for (VersionIterator iter = versionHistory.getAllVersions(); iter.hasNext(); ) {
                    Version version = iter.nextVersion();
                    if (version.getName().equals(JcrConstants.JCR_ROOT_VERSION)) {
                        continue;
                    }
                    Set<String> labelsSet = new TreeSet<String>();
                    String[] labels = versionHistory.getVersionLabels();
                    Collections.addAll(labelsSet, labels);
                    labelsSet.add(version.getName());
                    listing.put(version.getCreated(), labelsSet);
                }
                return listing;
            }
        }
        return new TreeMap<>();
    }
}
