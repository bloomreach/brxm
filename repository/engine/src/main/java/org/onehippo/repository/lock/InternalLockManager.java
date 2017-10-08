/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.lock;

import org.onehippo.cms7.services.lock.LockManager;

public interface InternalLockManager extends LockManager {

    void destroy();

    /**
     * does similar logic as #destroy without closing background threads or marking the InternalLockManager as destroyed:
     * This is useful / needed for integration tests which need to be able to validate certain behavior without destroying
     * the InternalLockManager for real (since next test needs it again since repository is kept)
     */
    void clear();

    void addJob(final Runnable runnable, final long initialDelaySeconds, final long periodSeconds);

}
