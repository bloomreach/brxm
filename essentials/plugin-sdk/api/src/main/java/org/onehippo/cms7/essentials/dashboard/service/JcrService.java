/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.dashboard.service;

import javax.jcr.Session;

/**
 * JcrService provides an abstraction layer for easing a number of high- and low-level JCR operations.
 *
 * It can be @Inject-ed into an Essentials plugin's REST resource or custom {@code Instruction}.
 */
public interface JcrService {
    /**
     * Login a fresh session with admin privileges.
     *
     * @return JCR session or null
     */
    Session createSession();

    /**
     * Call Session#refresh, send potential RepositoryException to service logger.
     *
     * @param session JCR session to refresh
     * @param keepChanges See Session#refresh
     */
    void refreshSession(Session session, boolean keepChanges);

    /**
     * Logout the session.
     *
     * @param session JCR session
     */
    void destroySession(Session session);
}
