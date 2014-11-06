/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.repository.xml;

import java.util.List;
import java.util.Stack;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.xml.Importer;
import org.apache.jackrabbit.core.xml.NodeInfo;
import org.apache.jackrabbit.core.xml.PropInfo;
import org.hippoecm.repository.jackrabbit.InternalHippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeRecordingImporter implements Importer {

    private final static Logger log = LoggerFactory.getLogger(ChangeRecordingImporter.class);

    private final NodeImpl importTargetNode;
    private final ChangeRecorder changeRecorder;
    private final Stack<NodeImpl> parents = new Stack<>();

    public ChangeRecordingImporter(NodeImpl importTargetNode, ImportContext importContext, InternalHippoSession session) {
        this.importTargetNode = importTargetNode;
        changeRecorder = importContext.getChangeRecorder();
        parents.push(importTargetNode);
    }

    @Override
    public void start() throws RepositoryException {
    }

    @Override
    public void startNode(final NodeInfo info, final List<PropInfo> propInfos) throws RepositoryException {
        final EnhancedNodeInfo nodeInfo = (EnhancedNodeInfo) info;
        final NodeImpl parent = parents.peek();

        if (parent == null) {
            parents.push(null);
            return;
        }

        final NodeImpl existingNode = getExistingNode(parent, nodeInfo);
        if (existingNode != null) {
            if (hasSameNameSiblings(existingNode)) {
                throw new ChangeRecordingLimitationException(
                        String.format("Can't determine change to node %s: same name sibling",
                                existingNode.safeGetJCRPath()));
            } else if (!isMerge(nodeInfo)) {
                if (nodeInfo.mergeSkip()) {
                    throw new ChangeRecordingLimitationException(
                            String.format("Can't determine change to node %s: whether skipped or not",
                                    existingNode.safeGetJCRPath()));
                } else {
                    changeRecorder.nodeAdded(existingNode);
                }
                if (parent.isSame(importTargetNode)) {
                    throw new ChangeRecordingShortCircuitException();
                }
                parents.push(null);
            } else {
                changeRecorder.nodeMerged(existingNode);
                for (PropInfo propInfo : propInfos) {
                    ((EnhancedPropInfo) propInfo).record(existingNode);
                }
                parents.push(existingNode);
            }
        } else {
            // unexpected but doesn't invalidate the change record
            log.debug("Expected to find node {}/{}[{}] but didn't", parent.safeGetJCRPath(), nodeInfo.getName(), nodeInfo.getIndex());
            parents.push(null);
        }
    }

    private boolean hasSameNameSiblings(final NodeImpl existingNode) throws RepositoryException {
        return existingNode.getParent().getNodes(existingNode.getName()).getSize() > 1;
    }

    private NodeImpl getExistingNode(final NodeImpl parent, final EnhancedNodeInfo nodeInfo) throws RepositoryException {
        try {
            if (nodeInfo.getIndex() == -1) {
                return parent.getNode(nodeInfo.getName());
            } else {
                return parent.getNode(nodeInfo.getName(), nodeInfo.getIndex());
            }
        } catch (ItemNotFoundException e) {
            return null;
        }
    }

    @Override
    public void endNode(final NodeInfo nodeInfo) throws RepositoryException {
        parents.pop();
    }

    @Override
    public void end() throws RepositoryException {
    }

    private static boolean isMerge(EnhancedNodeInfo info) {
        return info.mergeCombine() || info.mergeOverlay();
    }

}
