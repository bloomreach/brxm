/*
 * Copyright 2013-2020 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MIXIN_BRANCH_INFO;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.UNPUBLISHED;
import static org.onehippo.repository.branch.BranchConstants.MASTER_BRANCH_ID;
import static org.onehippo.repository.util.JcrConstants.MIX_VERSIONABLE;
import static org.onehippo.repository.util.JcrConstants.NT_FROZEN_NODE;

/**
 * Custom workflow task for determining if current draft is modified compared to the unpublished variant.
 */
public class IsModifiedTask extends AbstractDocumentTask {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(IsModifiedTask.class);

    private static final String[] IGNORED_PROPERTIES = {
            HippoStdPubWfNodeType.HIPPOSTDPUBWF_LAST_MODIFIED_DATE
    };

    private static final String[] IGNORED_FROZEN_PROPERTIES = {
            JcrConstants.JCR_UUID,
            JcrConstants.JCR_PRIMARY_TYPE,
            JcrConstants.JCR_FROZEN_MIXIN_TYPES,
            JcrConstants.JCR_FROZEN_PRIMARY_TYPE,
            JcrConstants.JCR_FROZEN_UUID
    };

    static {
        // Sort arrays to be able to do binary search
        Arrays.sort(IGNORED_PROPERTIES);
        Arrays.sort(IGNORED_FROZEN_PROPERTIES);
    }

    @Override
    public Object doExecute() throws RepositoryException {

        DocumentHandle dm = getDocumentHandle();
        DocumentVariant draft = dm.getDocuments().get(HippoStdNodeType.DRAFT);
        DocumentVariant unpublished = dm.getDocuments().get(HippoStdNodeType.UNPUBLISHED);

        if (draft != null && unpublished != null) {
            Node draftNode = draft.getNode(getWorkflowContext().getInternalWorkflowSession());
            if (getWorkflowContext().getUserIdentity().equals(draft.getHolder())) {
                // use user session bound draftNode which might contain outstanding changes
                draftNode = draft.getNode(getWorkflowContext().getUserSession());
            }

            final Node unpublishedNode = findUnpublishedCompareTo(unpublished, draft);

            return !equals(draftNode, unpublishedNode);
        }
        return null;
    }

    protected boolean equals(Node a, Node b) throws RepositoryException {
        final boolean virtualA = JcrUtils.isVirtual(a);
        if (virtualA != JcrUtils.isVirtual(b)) {
            return false;
        } else if (virtualA) {
            return true;
        }

        final Map<String, Property> aProperties = getPropertyMap(a);
        final Map<String, Property> bProperties = getPropertyMap(b);

        if (!aProperties.keySet().equals(bProperties.keySet())) {
            return false;
        }
        for (Map.Entry<String, Property> aEntries : aProperties.entrySet()) {
            final Property aProp = aEntries.getValue();
            final Property bProp = bProperties.get(aEntries.getKey());
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

    private Node findUnpublishedCompareTo(final DocumentVariant unpublishedVariant, final DocumentVariant draft) throws RepositoryException {
        final Node unpublished = unpublishedVariant.getNode(getWorkflowContext().getInternalWorkflowSession());

        if (!unpublished.isNodeType(MIX_VERSIONABLE)) {
            return unpublished;
        }

        final VersionManager versionManager = unpublished.getSession().getWorkspace().getVersionManager();
        final VersionHistory versionHistory = versionManager.getVersionHistory(unpublished.getPath());

        Node draftNode = draft.getNode(getWorkflowContext().getInternalWorkflowSession());

        if (draft.isMaster()) {
            // get the correct unpublished node, possibly from version history
            if (unpublishedVariant.isMaster()) {
                return unpublished;
            } else {
                return getVersionedUnpublished(unpublished, versionHistory, MASTER_BRANCH_ID);

            }
        } else if (draftNode.isNodeType(HIPPO_MIXIN_BRANCH_INFO)) {
            final String branchId = draftNode.getProperty(HIPPO_PROPERTY_BRANCH_ID).getString();
            if (unpublishedVariant.isBranch(branchId)) {
                return unpublished;
            } else {
                return getVersionedUnpublished(unpublished, versionHistory, branchId);
            }
        }
        return unpublished;
    }

    private Node getVersionedUnpublished(final Node unpublished, final VersionHistory versionHistory, final String branchId) throws RepositoryException {
        final String versionLabel = branchId + "-" + UNPUBLISHED.getState();
        if (!versionHistory.hasVersionLabel(versionLabel)) {
            log.warn("Cannot find frozen node for versionLabel '{}' to compare draft with. Use unpublished " +
                    "variant instead", versionLabel);
            return unpublished;
        }
        return versionHistory.getVersionByLabel(versionLabel).getFrozenNode();
    }

    private Map<String, Property> getPropertyMap(final Node node) throws RepositoryException {
        final PropertyFilter filter = node.isNodeType(NT_FROZEN_NODE)
                ? this::includeFrozenForComparison
                : this::includeForComparison;
        final Map<String, Property> propertyMap = new HashMap<>();
        final PropertyIterator propertyIterator = node.getProperties();
        while (propertyIterator.hasNext()) {
            final Property property = propertyIterator.nextProperty();
            if (filter.test(property)) {
                propertyMap.put(property.getName(), property);
            }
        }
        return propertyMap;
    }

    private boolean includeForComparison(final Property property) throws RepositoryException {
        final String name = property.getName();
        return Arrays.binarySearch(PROTECTED_PROPERTIES, name) < 0
                && Arrays.binarySearch(IGNORED_PROPERTIES, name) < 0
                && !property.getDefinition().isProtected();
    }

    private boolean includeFrozenForComparison(final Property property) throws RepositoryException {
        final String name = property.getName();
        return Arrays.binarySearch(PROTECTED_PROPERTIES, name) < 0
                && Arrays.binarySearch(IGNORED_PROPERTIES, name) < 0
                && Arrays.binarySearch(IGNORED_FROZEN_PROPERTIES, name) < 0;
    }

    @FunctionalInterface
    private interface PropertyFilter {
        boolean test(Property property) throws RepositoryException;
    }
}
