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

package org.onehippo.repository.scxml;

import org.hippoecm.repository.api.WorkflowException;

/**
 * SCXMLWorkflowData is an interface which can be implemented to provide a dedicated data object for a specific SCXML
 * workflow state machine.
 * <p>
 * A concrete SCXMLWorkflowData instance will be injected in the SCXML root context under the predefined and reserved
 * {@link #SCXML_CONTEXT_KEY} ("workflowData") key by the {@link SCXMLWorkflowExecutor}, which will also manage its
 * internal state by invoking the {@link #initialize()} and {@link #reset()} method when the SCXML state machine is
 * started and reset.
 */
public interface SCXMLWorkflowData {

    String SCXML_CONTEXT_KEY = "workflowData";

    void initialize() throws WorkflowException;
    void reset();
}
