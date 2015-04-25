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
import org.hippoecm.repository.util.OverwritingCopyHandler;
import org.hippoecm.repository.util.PropInfo;
import org.hippoecm.repository.util.PropertyIterable;
import org.onehippo.repository.api.WorkflowTask;
import org.onehippo.repository.documentworkflow.DocumentHandle;
import org.onehippo.repository.util.JcrConstants;

/**
 * Abstract base workflow task class having sharable workflow/document related operations and
 * delegating operations to the underlying AbstractAction to retrieve execution context attributes.
 */
public abstract class AbstractDocumentTask implements WorkflowTask, Serializable {

    private static final long serialVersionUID = 1L;

    protected static final String[] PROTECTED_MIXINS = new String[] {
        JcrConstants.MIX_VERSIONABLE,
        JcrConstants.MIX_REFERENCEABLE,
        HippoNodeType.NT_HARDDOCUMENT,
        HippoStdNodeType.NT_PUBLISHABLE,
        HippoStdNodeType.NT_PUBLISHABLESUMMARY,
        HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT
    };

    protected static final String[] PROTECTED_PROPERTIES = new String[] {
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

    protected Node cloneDocumentNode(Node srcNode) throws RepositoryException {
        final Node parent = srcNode.getParent();
        JcrUtils.ensureIsCheckedOut(parent);

        Node destNode = parent.addNode(srcNode.getName(), srcNode.getPrimaryNodeType().getName());

        if (!destNode.isNodeType(HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT)) {
            destNode.addMixin(HippoStdPubWfNodeType.HIPPOSTDPUBWF_DOCUMENT);
        }

        if (srcNode.isNodeType(HippoStdNodeType.NT_PUBLISHABLESUMMARY) && !destNode.isNodeType(HippoStdNodeType.NT_PUBLISHABLESUMMARY)) {
            destNode.addMixin(HippoStdNodeType.NT_PUBLISHABLESUMMARY);
        }

        return copyTo(srcNode, destNode);
    }

    /**
     * Copies {@link Node} {@code srcNode} to {@code destNode}.
     * Special properties and mixins are filtered out; those are actively maintained by the workflow.
     *
     * @param srcNode the node to copy
     * @param destNode the node that the contents of srcNode will be copied to
     * @return destNode
     * @throws RepositoryException
     */
    protected Node copyTo(final Node srcNode, Node destNode) throws RepositoryException {
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
