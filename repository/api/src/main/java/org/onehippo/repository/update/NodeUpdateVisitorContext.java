/*
 *  Copyright 2015-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.update;

/**
 * Provided as an execution context to {@link NodeUpdateVisitor} instance in order to allow an {@link NodeUpdateVisitor}
 * to be able to manually update skipped/updated/failed node count while being executed on single node iteration
 * (e.g, in <code>#doUpdate(node)</code> method in a groovy updater script).
 */
public interface NodeUpdateVisitorContext {

    /**
     * Manually report/increment the skipped count in the current execution.
     */
    void reportSkipped(String path);

    /**
     * Manually report/increment the updated count in the current execution.
     * <P>
     * <EM>WARNING: this invocation may trigger committing or reverting the batch.</EM>
     * </P>
     */
    void reportUpdated(String path);

    /**
     * Manually report/increment the failed count in the current execution.
     */
    void reportFailed(String path);

}
