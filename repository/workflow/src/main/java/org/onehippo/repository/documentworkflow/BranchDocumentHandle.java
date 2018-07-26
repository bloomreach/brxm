/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.onehippo.repository.documentworkflow;

import java.util.Optional;


import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.standardworkflow.DocumentVariant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.HippoStdNodeType.DRAFT;
import static org.hippoecm.repository.HippoStdNodeType.PUBLISHED;
import static org.hippoecm.repository.HippoStdNodeType.UNPUBLISHED;

public class BranchDocumentHandle extends DocumentHandle {

    private Optional<Node> published;
    private Optional<Node> unpublished;
    private Optional<Node> draft;
    private final static Logger log = LoggerFactory.getLogger(BranchDocumentHandle.class);

    public BranchDocumentHandle(final Node handle) throws WorkflowException {
        super(handle);
    }

    public void setPublished(Node published){
        this.published = Optional.ofNullable(published);
    }

    public void setUnpublished(Node unpublished){
        this.unpublished = Optional.ofNullable(unpublished);
    }

    public void setDraft(Node draft){
        this.draft = Optional.ofNullable(draft);
    }

    /**
     * Provide hook for extension
     * <p>
     * This implementation calls {@link #createDocumentVariant(Node)}
     *
     * @throws RepositoryException
     */
    @Override
    protected void initializeDocumentVariants() throws RepositoryException {
        super.initializeDocumentVariants();
        replaceVariant(published, PUBLISHED);
        replaceVariant(unpublished, UNPUBLISHED);
        replaceVariant(draft, DRAFT);
    }

    private void replaceVariant(final Optional<Node> node, final String state) {
        getDocuments().put(state, node.map(n -> getVariant(n)).orElse(getDocuments().get(state)));
    }

    private DocumentVariant getVariant(Node node){
        try {
            return new DocumentVariant(node);
        } catch (RepositoryException e) {
            log.warn(e.getMessage(), e);
        }
        return null;
    }
}
