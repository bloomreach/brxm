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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.PropertyIterable;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.PublishableDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom workflow task for determining if current draft is modified compared to the unpublished variant
 * and save this state in the handle hints.
 */
public class IsModifiedWorkflowTask extends AbstractDocumentWorkflowTask {

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(IsModifiedWorkflowTask.class);

    @Override
    public void doExecute() throws WorkflowException, RepositoryException, RemoteException {

        DocumentHandle dm = getDataModel();

        if (dm.getDraft() != null && dm.getUnpublished() != null && PublishableDocument.DRAFT.equals(dm.getSubjectState())) {
            // TODO: BasicReviewedActionsWorkflowImpl#hints() method retrieves a 'fresh' draftNode based on the dm.d.identifier. Why would that be needed?
            dm.getHints().put("modified", !equals(dm.getDraft().getNode(), dm.getUnpublished().getNode()));
        }
    }

    protected boolean equals(Node a, Node b) throws RepositoryException {
        final boolean virtualA = JcrUtils.isVirtual(a);
        if (virtualA != JcrUtils.isVirtual(b)) {
            return false;
        } else if (virtualA) {
            return true;
        }

        final PropertyIterator aProperties = a.getProperties();
        final PropertyIterator bProperties = b.getProperties();

        Map<String, Property> properties = new HashMap<>();
        for (Property property : new PropertyIterable(aProperties)) {
            final String name = property.getName();
            if (Arrays.binarySearch(PROTECTED_PROPERTIES, name) >= 0) {
                continue;
            }
            if (property.getDefinition().isProtected()) {
                continue;
            }
            if (!b.hasProperty(name)) {
                return false;
            }

            properties.put(name, property);
        }
        for (Property bProp : new PropertyIterable(bProperties)) {
            final String name = bProp.getName();
            if (Arrays.binarySearch(PROTECTED_PROPERTIES, name) >= 0) {
                continue;
            }
            if (bProp.getDefinition().isProtected()) {
                continue;
            }
            if (!properties.containsKey(name)) {
                return false;
            }

            Property aProp = properties.get(name);
            if (!equals(bProp, aProp)) {
                return false;
            }
        }

        NodeIterator aIter = a.getNodes();
        NodeIterator bIter = b.getNodes();
        if (aIter.getSize() != bIter.getSize()) {
            return false;
        }
        while (aIter.hasNext()) {
            Node aChild = aIter.nextNode();
            Node bChild = bIter.nextNode();
            if (!equals(aChild, bChild)) {
                return false;
            }
        }
        return true;
    }

    private boolean equals(final Property bProp, final Property aProp) throws RepositoryException {
        if (aProp.isMultiple() != bProp.isMultiple() || aProp.getType() != bProp.getType()) {
            return false;
        }

        if (aProp.isMultiple()) {
            Value[] aValues = aProp.getValues();
            Value[] bValues = bProp.getValues();
            if (aValues.length != bValues.length) {
                return false;
            }
            for (int i = 0; i < aValues.length; i++) {
                if (!equals(aValues[i], bValues[i])) {
                    return false;
                }
            }
        } else {
            Value aValue = aProp.getValue();
            Value bValue = bProp.getValue();
            if (!equals(aValue, bValue)) {
                return false;
            }
        }
        return true;
    }

    private boolean equals(final Value aValue, final Value bValue) throws RepositoryException {
        return aValue.getString().equals(bValue.getString());
    }
}
