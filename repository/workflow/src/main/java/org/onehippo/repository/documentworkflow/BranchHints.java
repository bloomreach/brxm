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

import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.standardworkflow.DocumentVariant;
import org.onehippo.repository.branch.BranchHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modifies the hints that are based on the variants under the handle. The hints follow the same logic
 * as in the scxml, but then variants from version history are taken into account.
 *
 * Mind that this only modifies the hints, the action itself might fail, because the conditions only
 * take into account the variants under the handle. First do a checkout if necessary.
 */
public class BranchHints {

    private final static Logger log = LoggerFactory.getLogger(BranchHints.class);
    private final Map<String, Serializable> hints;

    public BranchHints(final Builder builder) {
        this.hints = builder.branchHandleHints;
    }

    public Map<String, Serializable> getHints() {
        return hints;
    }

    public static class Builder {

        private BranchHandle branchHandle;
        private DocumentHandle documentHandle;
        private Map<String, Serializable> branchHandleHints;
        private Map<String, Serializable> documentHandleHints;

        public Builder branchHandle(final BranchHandle branchHandle) {
            this.branchHandle = branchHandle;
            return this;
        }


        public Builder documentHandle(final DocumentHandle documentHandle) {
            this.documentHandle = documentHandle;
            return this;
        }

        public Builder hints(final Map<String, Serializable> hints) {
            this.documentHandleHints = hints;
            return this;
        }

        public BranchHints build() throws RepositoryException {
            this.branchHandleHints = new TreeMap<>();
            this.branchHandleHints.putAll(this.documentHandleHints);
            if (!(branchHandle.isMaster() && documentHandle.getDocuments().get(HippoStdNodeType.UNPUBLISHED).isMaster())) {
                modifyBooleanHint("publish", getPublish());
                modifyBooleanHint("depublish", getDepublish());
                modifyBooleanHint("removeBranch", getRemoveBranch());
                modifyBooleanHint("checkoutBranch", getCheckoutBranch());
            }
            return new BranchHints(this);
        }

        private boolean getCheckoutBranch() {
            return branchHandle.isMaster();
        }

        private boolean getRemoveBranch() throws RepositoryException {
            return !(editingCurrentBranch()) && unpublishedExists();
        }

        private boolean unpublishedExists() {
            return branchHandle.getUnpublished() != null;
        }

        private boolean hasDraftAssociatedWithCurrentBranch() throws RepositoryException {
            final Node draft = branchHandle.getDraft();
            if (draft != null) {
                return new DocumentVariant(draft).isBranch(branchHandle.getBranchId());
            }
            return false;
        }

        private boolean editingCurrentBranch() throws RepositoryException {
            return holder() != null && hasDraftAssociatedWithCurrentBranch();
        }

        private String holder() throws RepositoryException {
            final Node draft = branchHandle.getDraft();
            if (draft != null) {
                return new DocumentVariant(draft).getHolder();
            }
            return null;
        }


        private void modifyBooleanHint(String key, boolean modifier) {

            this.branchHandleHints.put(key, modifier);
        }

        private boolean getPublish() throws RepositoryException {
            return branchHandle.isMaster();
        }

        private boolean getDepublish() throws RepositoryException {
            return getPublish();
        }
    }
}
