/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content.document.util;

import java.io.Serializable;
import java.util.Map;

/**
 * Provides methods for server-side manipulation of the selected branch
 */
public interface BranchSelectionService {

    /**
     * Returns the id of the currently selected branch from the context payload, where the context payload is coming
     * from the {@link org.onehippo.cms7.services.cmscontext.CmsSessionContext} of the logged in user.
     *
     * @param contextPayload context payload of user's cms session
     * @return selected branch id
     */
    String getSelectedBranchId(Map<String, Serializable> contextPayload);
}
