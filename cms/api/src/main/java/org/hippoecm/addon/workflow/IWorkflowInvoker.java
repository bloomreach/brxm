/*
 *  Copyright 2012-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.addon.workflow;

import org.apache.wicket.util.io.IClusterable;

public interface IWorkflowInvoker extends IClusterable {

    void invokeWorkflow() throws Exception;

    /**
     * After a workflow invocation has finished successfully it can be resolved. This is introduced to allow workflow
     * calls to be invoked from javascript via a promise API.
     *
     * For backwards compatibility this method has a default implementation.
     *
     * @param result The result of a workflow call. Can be null.
     * @since 14.3
     */
    default void resolve(final String result) {}

    /**
     * If a workflow invocation has failed it can be rejected. This is introduced to allow workflow calls to be
     * invoked from javascript via a promise API.
     *
     * For backwards compatibility this method has a default implementation.
     *
     * @param reason The reason the workflow invocation failed. Can be null.
     * @since 14.3
     */
    default void reject(final String reason) {}
}
