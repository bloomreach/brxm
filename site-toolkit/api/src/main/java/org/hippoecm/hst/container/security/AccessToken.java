/*
 *  Copyright 2020-2023 Bloomreach
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
package org.hippoecm.hst.container.security;

import org.onehippo.cms7.services.cmscontext.CmsSessionContext;

public interface AccessToken {

    /**
     * @return the subject, eg an identifier, of this {@link AccessToken}
     */
    String getSubject();

    /**
     * @return the {@link CmsSessionContext} for this {@link AccessToken}
     */
    CmsSessionContext getCmsSessionContext();

    /**
     * @return the header for {@code headerName} or {@code null} if not present
     */
    String getHeader(final String headerName);

    /**
     * @return the claim for {@code claimName} or {@code null} if not present
     */
    String getClaim(final String claimName);
}
