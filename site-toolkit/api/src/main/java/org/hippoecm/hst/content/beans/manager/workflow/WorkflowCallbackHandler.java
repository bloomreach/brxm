/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.manager.workflow;

import org.hippoecm.repository.api.Workflow;

/**
 * @param <T> The Workflow specific type which is expected to be provided during the callback
 *
 * @deprecated since 2.28.00 (CMS 7.9), use {@link QualifiedWorkflowCallbackHandler}
 * or extend from {@link BaseWorkflowCallbackHandler}
 */
@Deprecated
public interface WorkflowCallbackHandler<T extends Workflow> {

    void processWorkflow(T workflow) throws Exception;
}
