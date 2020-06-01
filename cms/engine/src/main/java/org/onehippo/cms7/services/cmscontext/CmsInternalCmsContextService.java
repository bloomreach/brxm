/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

/**
 * CMS Application only (internal) extension of the {@link CmsContextService}
 */
public interface CmsInternalCmsContextService extends CmsContextService {
    /**
     * Create a new CmsSessionContext for the provided (CMS Application) HttpSession.
     * <p>
     * As a minimum the CmsSessionContext created must provide access to the {@link CmsSessionContext#REPOSITORY_CREDENTIALS} keyed data.
     * </p>
     *
     * @param session the HttpSession to create the CmsSessionContext for and bind into
     * @return the new CmsSessionContext
     */
    CmsSessionContext create(final HttpSession session);

    /**
     * Add specific data into a CmsSessionContext by key
     * @param ctx the CmsSessionContext
     * @param key the key for the data
     * @param data the data
     */
    void setData(CmsSessionContext ctx, String key, Object data);
}
