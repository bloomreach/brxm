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
package org.onehippo.cms.channelmanager.content.document.util;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

import javax.jcr.Session;

import org.onehippo.cms.channelmanager.content.error.ErrorInfo;

public interface HintsInspector {

    /**
     * Check if a document draft can be created, given its workflow hints.
     *
     * @param hints workflow hints
     * @return true if a draft can be created for the document.
     */
    boolean canCreateDraft(Map<String, Serializable> hints);

    /**
     * Check if a document can be updated, given its workflow hints.
     *
     * @param hints workflow hints
     * @return true if document can be updated, false otherwise
     */
    boolean canUpdateDraft(Map<String, Serializable> hints);

    /**
     * Check if a document can be updated, given its workflow hints.
     *
     * @param hints workflow hints
     * @return true if document can be updated, false otherwise
     */
    boolean canDeleteDraft(Map<String, Serializable> hints);

    /**
     * Determine the reason why editing failed for the present workflow hints.
     *
     * @param hints   workflow hints
     * @param session current user's JCR session
     * @return Specific reason or nothing (unknown), wrapped in an Optional
     */
    Optional<ErrorInfo> determineEditingFailure(Map<String, Serializable> hints, Session session);

}
