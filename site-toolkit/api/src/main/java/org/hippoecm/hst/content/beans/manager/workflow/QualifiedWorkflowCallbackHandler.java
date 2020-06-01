/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.content.beans.manager.workflow;

import org.hippoecm.repository.api.Workflow;

/**
 * {@link WorkflowPersistenceManager} callback handler interface which can be used to perform Workflow based
 * post-processing (like publishing) during a WorkflowPersistenceManager update call.
 * @param <T> The Workflow specific type which should be provided during the callback
 */
public interface QualifiedWorkflowCallbackHandler<T extends Workflow> {

    /**
     * @return The expected type of Workflow in the callback call
     */
    Class<? extends T> getWorkflowType();

    /**
     * The callback method called by the WorkflowPersistenceManager during an update call, after the update already
     * has been performed <em>and</em> saved.
     * @param workflow The Workflow instance retrieved for the update
     * @throws Exception To report back any exception to the WorkflowPersistenceManager invoking process.
     */
    void processWorkflow(T workflow) throws Exception;
}
