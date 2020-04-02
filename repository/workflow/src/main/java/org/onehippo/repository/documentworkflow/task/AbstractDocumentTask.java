/**
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

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowContext;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.HippoStdPubWfNodeType;
import org.hippoecm.repository.util.CopyHandler;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeInfo;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.OverwritingCopyHandler;
import org.hippoecm.repository.util.PropInfo;
import org.hippoecm.repository.util.PropertyIterable;
import org.onehippo.repository.api.WorkflowTask;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.util.JcrConstants;

import static org.hippoecm.repository.HippoStdNodeType.MIXIN_SKIPDRAFT;
import static org.hippoecm.repository.api.HippoNodeType.NT_DOCUMENT;
import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;

/**
 * Abstract base workflow task class having sharable workflow/document related operations and
 * delegating operations to the underlying AbstractAction to retrieve execution context attributes.
 */
public abstract class AbstractDocumentTask implements WorkflowTask, Serializable {

    private static final long serialVersionUID = 1L;

    protected static final String[] PROTECTED_MIXINS = new String[]{
            JcrConstants.MIX_VERSIONABLE,
            JcrConstants.MIX_REFERENCEABLE,
            HippoNodeType.NT_HARDDOCUMENT,
            HippoStdNodeType.NT_PUBLISHABLE,
            HippoStdNodeType.NT_PUBLISHABLESUMMARY,
            HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT
    };

    protected static final String[] PROTECTED_PROPERTIES = new String[]{
            HippoNodeType.HIPPO_AVAILABILITY,
            HippoNodeType.HIPPO_RELATED,
            HippoNodeType.HIPPO_PATHS,
            HippoStdNodeType.HIPPOSTD_STATE,
            HippoStdNodeType.HIPPOSTD_HOLDER,
            HippoStdNodeType.HIPPOSTD_STATESUMMARY,
            HippoStdPubWfNodeType.HIPPOSTDPUBWF_PUBLICATION_DATE
    };

    static {
        Arrays.sort(PROTECTED_PROPERTIES);
        Arrays.sort(PROTECTED_MIXINS);
    }

    private DocumentHandle documentHandle;
    private WorkflowContext workflowContext;

    /**
     * Execute this workflow task
     * @throws WorkflowException
     */
    @Override
    public final Object execute() throws WorkflowException {
        try {
            return doExecute();
        } catch (RepositoryException | RemoteException e) {
            throw new WorkflowException(e.getMessage(), e);
        }
    }

    protected abstract Object doExecute() throws WorkflowException, RepositoryException, RemoteException;

    /**
     * @return the document handle object from the current SCXML execution context.
     */
    public DocumentHandle getDocumentHandle() {
        return documentHandle;
    }

    public void setDocumentHandle(final DocumentHandle documentHandle) {
        this.documentHandle = documentHandle;
    }

    public WorkflowContext getWorkflowContext() {
        return workflowContext;
    }

    public void setWorkflowContext(final WorkflowContext workflowContext) {
        this.workflowContext = workflowContext;
    }

    /**
     * Copies {@link Node} {@code srcNode} to {@code destNode}.
     * Special properties and mixins are filtered out; those are actively maintained by the workflow.
     *
     * @param srcNode the node to copy
     * @param destNode the node that the contents of srcNode will be copied to
     * @return destNode
     * @throws RepositoryException
     * @deprecated since 14.3.0 : do not use this method any more. Instead use {@link #copyTo(Node, Node, boolean)} and
     * specify whether source or dest is draft variant. This methods invokes {@link #copyTo(Node, Node, boolean)} with
     * 'srcOrDestIsDraft = false'
     */
    @Deprecated
    protected Node copyTo(final Node srcNode, final Node destNode) throws RepositoryException {
        return copyTo(srcNode, destNode, false);
    }

    /**
     * <p>
     *     Copies {@link Node} {@code srcNode} to {@code destNode}.
     *     Special properties and mixins are filtered out; those are actively maintained by the workflow.
     * </p>
     * <p>
     *     If {@code srcOrDestIsDraft} is {@code true}, the copy is between a draft and other variant. In that case,
     *     nodes of type {@code hippo:skipDraft} are skipped from being copied (and also not removed from the
     *     {@code destNode} if present
     * </p>
     * @param srcNode the node to copy
     * @param destNode the node that the contents of srcNode will be copied to
     * @return destNode
     * @throws RepositoryException
     */
    protected Node copyTo(final Node srcNode, final Node destNode, final boolean srcOrDestIsDraft) throws RepositoryException {
        final CopyHandler chain = new OverwritingCopyHandler(destNode) {

            @Override
            public void startNode(final NodeInfo nodeInfo) throws RepositoryException {
                NodeType[] oldMixins = nodeInfo.getMixinTypes();
                Set<NodeType> mixins = new HashSet<>();
                for (NodeType mixinType : oldMixins) {
                    if (Arrays.binarySearch(PROTECTED_MIXINS, mixinType.getName()) >= 0) {
                        continue;
                    }
                    mixins.add(mixinType);
                }
                NodeType[] newMixins = mixins.toArray(new NodeType[mixins.size()]);
                final NodeInfo newInfo = new NodeInfo(nodeInfo.getName(), nodeInfo.getIndex(), nodeInfo.getNodeType(), newMixins);
                super.startNode(newInfo);
            }

            @Override
            protected void removeProperties(final Node node) throws RepositoryException {
                for (Property property : new PropertyIterable(node.getProperties())) {
                    if (property.getDefinition().isProtected()) {
                        continue;
                    }
                    String name = property.getName();
                    if (Arrays.binarySearch(PROTECTED_PROPERTIES, name) >= 0) {
                        continue;
                    }
                    property.remove();
                }
            }

            @Override
            protected void removeChildNodes(final Node node) throws RepositoryException {
                for (Node child : new NodeIterable(node.getNodes())) {
                    if (child.getDefinition().isProtected()) {
                        continue;
                    }
                    if (srcOrDestIsDraft && child.isNodeType(MIXIN_SKIPDRAFT) &&
                            getCurrent() == destNode &&
                            destNode.isNodeType(NT_DOCUMENT) && destNode.getParent().isNodeType(NT_HANDLE)) {
                        // do not replace direct children of type 'skipdraft' since when copy from draft to unpublished,
                        // these nodes should not be replaced
                        continue;
                    }

                    child.remove();
                }
            }

            @Override
            protected void replaceMixins(final Node node, final NodeInfo nodeInfo) throws RepositoryException {
                Set<String> mixinSet = new TreeSet<>();
                Collections.addAll(mixinSet, nodeInfo.getMixinNames());
                for (NodeType nodeType : node.getMixinNodeTypes()) {
                    final String mixinName = nodeType.getName();
                    if (!mixinSet.contains(mixinName)) {
                        if (Arrays.binarySearch(PROTECTED_MIXINS, mixinName) < 0) {
                            node.removeMixin(mixinName);
                        }
                    } else {
                        mixinSet.remove(mixinName);
                    }
                }
                for (String mixinName : mixinSet) {
                    node.addMixin(mixinName);
                }
            }

            @Override
            public void setProperty(final PropInfo propInfo) throws RepositoryException {
                String name = propInfo.getName();
                if (Arrays.binarySearch(PROTECTED_PROPERTIES, name) >= 0) {
                    return;
                }
                super.setProperty(propInfo);
            }

            @Override
            public boolean skipNode(final Node child) throws RepositoryException {
                if (srcOrDestIsDraft && child.isNodeType(MIXIN_SKIPDRAFT) &&
                        getCurrent() == destNode && destNode.isNodeType(NT_DOCUMENT)
                        && destNode.getParent().isNodeType(NT_HANDLE)) {
                    return true;
                }
                return false;
            }
        };
        JcrUtils.copyTo(srcNode, chain);

        return destNode;
    }

    protected Version lookupVersion(Node variant, Calendar historic) throws RepositoryException {
        VersionHistory versionHistory = variant.getVersionHistory();
        for (VersionIterator iter = versionHistory.getAllVersions(); iter.hasNext(); ) {
            Version version = iter.nextVersion();
            if (version.getCreated().equals(historic)) {
                return version;
            }
        }
        return null;
    }
}
