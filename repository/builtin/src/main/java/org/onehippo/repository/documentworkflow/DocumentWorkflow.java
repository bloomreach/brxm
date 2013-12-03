/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.repository.documentworkflow;

import org.hippoecm.repository.reviewedactions.FullRequestWorkflow;
import org.hippoecm.repository.reviewedactions.FullReviewedActionsWorkflow;
import org.hippoecm.repository.reviewedactions.UnlockWorkflow;
import org.hippoecm.repository.standardworkflow.VersionWorkflow;

/**
 * Aggregate DocumentWorkflow, combining all Document handle based workflow operations into one generic interface
 */
public interface DocumentWorkflow extends FullRequestWorkflow, FullReviewedActionsWorkflow, UnlockWorkflow, VersionWorkflow {

    /**
     * The Features enumeration can be used to 'filter' which subset of the DocumentWorkflow functionality should be used and exposed
     */
    enum Features {

        all, request, document, unlock, version;

        public boolean request() {
            return this == all || this == request;
        }

        public boolean document() {
            return this == all || this == document;
        }

        public boolean unlock() {
            return this == all || this == unlock;
        }

        public boolean version() {
            return this == all || this == version;
        }
    };
}
