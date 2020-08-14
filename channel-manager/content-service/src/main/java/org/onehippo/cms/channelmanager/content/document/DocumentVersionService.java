/*
 * Copyright 2020 Bloomreach
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
 *
 */
package org.onehippo.cms.channelmanager.content.document;

import org.onehippo.cms.channelmanager.content.UserContext;
import org.onehippo.cms.channelmanager.content.document.model.DocumentVersionInfo;

public interface DocumentVersionService {
    /**
     * Returns a {@link DocumentVersionInfo} instance for the document with the given id.
     *
     * @param handleId  UUID of the document handle for which to get version infocan be a UUID from version history (a frozen node)
     * @param branchId    Id of the requested document branch
     * @param userContext Properties of the user that executes the request
     * @return list of version info
     */
    DocumentVersionInfo getVersionInfo(String handleId, final String branchId, UserContext userContext);
}
