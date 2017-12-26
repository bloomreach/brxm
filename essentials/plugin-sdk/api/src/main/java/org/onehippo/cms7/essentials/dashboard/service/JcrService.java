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

import java.util.Map;

import javax.jcr.Node;
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

    /**
     * Interpolate and import an XML (classpath) resource into the repository.
     *
     * Does *not* save changes.
     *
     * @param targetNode      JCR node to import the resource to
     * @param resourcePath    absolute resource path
     * @param placeholderData data to fill placeholders in the input XML
     * @return true if the resource was found and imported successfully, false otherwise.
     */
    boolean importResource(Node targetNode, String resourcePath, Map<String, Object> placeholderData);
}
