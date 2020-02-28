/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.frontend.editor;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.branch.BranchHandle;
import org.onehippo.repository.documentworkflow.BranchHandleImpl;
import org.onehippo.repository.util.JcrConstants;

/**
 * Maps a {@link javax.jcr.Node} to a {@link Document}
 */
public class DocumentBuilder {

    private DocumentImpl document;
    private Node node;
    private Node handleNode;
    private String branchId;

    public static DocumentBuilder create() {
        return new DocumentBuilder();
    }

    public DocumentImpl build() throws EditorException {
        document = new DocumentImpl();
        try {
            if (node.isNodeType(JcrConstants.NT_VERSION)) {
                final Node frozenNode = node.getNode(JcrConstants.JCR_FROZEN_NODE);
                document.setRevision(frozenNode.getPath());
                final String uuid = frozenNode.getProperty(JcrConstants.JCR_FROZEN_UUID).getString();
                final Node variant = node.getSession().getNodeByIdentifier(uuid);
                handleNode = variant.getParent();
            }
            BranchHandle branchHandle = new BranchHandleImpl(branchId, handleNode);
            if (handleNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                for (final NodeIterator iter = handleNode.getNodes(); iter.hasNext(); ) {
                    final Node child = iter.nextNode();
                    if (child.getName().equals(handleNode.getName())) {
                        if (child.hasProperty(HippoStdNodeType.HIPPOSTD_STATE)) {
                            final String state = child.getProperty(HippoStdNodeType.HIPPOSTD_STATE).getString();
                            switch (state) {
                                case HippoStdNodeType.UNPUBLISHED:
                                    final Node unpublished = branchHandle.getUnpublished();
                                    if (unpublished == null) {
                                        document.setUnpublished(child.getPath());
                                    } else {
                                        document.setUnpublished(unpublished.getPath());
                                    }
                                    break;
                                case HippoStdNodeType.PUBLISHED:
                                    // if there is no published for the branch we fallback to master published and if there is
                                    // no master published we just use child
                                    Node published = branchHandle.getPublished();
                                    if (published == null) {
                                        published = branchHandle.getPublishedMaster();
                                    }
                                    if (published == null) {
                                        document.setPublished(child.getPath());
                                    } else {
                                        document.setPublished(published.getPath());
                                    }
                                    break;
                                case HippoStdNodeType.DRAFT:
                                    Node draft = branchHandle.getDraft();
                                    if (draft == null) {
                                        document.setDraft(child.getPath());
                                    } else {
                                        document.setDraft(draft.getPath());
                                    }
                                    final String user = UserSession.get().getJcrSession().getUserID();
                                    if (child.hasProperty(HippoStdNodeType.HIPPOSTD_HOLDER)
                                            && child.getProperty(HippoStdNodeType.HIPPOSTD_HOLDER).getString().equals(user)) {
                                        document.setHolder(true);
                                    }
                                    if (child.hasProperty(HippoStdNodeType.HIPPOSTD_TRANSFERABLE)) {
                                        document.setTransferable(true);
                                        document.setTransferable(child.getProperty(HippoStdNodeType.HIPPOSTD_TRANSFERABLE).getBoolean());
                                    }
                                    break;
                            }
                        }
                    }
                }
            }
        } catch (RepositoryException | WorkflowException e) {
            throw new EditorException("Something went wrong  when reading the document", e);
        }
        return document;

    }

    public DocumentBuilder branchId(final String branchId) {
        this.branchId = branchId;
        return this;
    }

    public DocumentBuilder node(final Node node) {
        this.node = node;
        this.handleNode = node;
        return this;
    }
}
