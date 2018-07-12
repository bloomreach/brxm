/*
 *  Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.cmscontext;

import javax.servlet.http.HttpSession;

public interface CmsContextService {

    /** Returns the unique identifier for this service **/
    String getId();

    /**
     * Returns the CmsSessionContext by its unique id
     * @param ctxId The id for the CmsSessionContext to return
     * @return the CmsSessionContext
     */
    CmsSessionContext getSessionContext(final String ctxId);

    /**
     * Creates a new CmsSessionContext attached to an existing (CMS Application HttpSession based) CmsSessionContext and
     * stores it in the provided HttpSession under attribute {@link CmsSessionContext#SESSION_KEY}.
     * <p>
     * The attached CmsSessionContext will be automatically destroyed and removed from its HttpSession when the
     * underlying CMS application CmsSessionContext is destroyed.
     * </p>
     * <p>
     * When the provided HttpSession expires or invalidated <em>OR</em> the attached CmsSessionContext
     * is removed from its HttpSession otherwise, the attached CmsSessionContext is automatically destroyed and detached
     * from the underlying CmsSessionContext.
     * </p>
     * @param ctxId Id for the CmsSessionContext to attach a new CmsSessionContext to
     * @param session The HttpSession in which to bind the new attached CmsSessionContext
     * @return the attached CmsSessionContext, also bound to the provided HttpSession attribute {@link CmsSessionContext#SESSION_KEY}
     */
    CmsSessionContext attachSessionContext(final String ctxId, HttpSession session);
}
