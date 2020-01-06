/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.security;

import org.hippoecm.hst.container.security.AccessToken;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

public class AccessTokenImpl implements AccessToken {

    private Jws<Claims> claimsJws;
    private CmsSessionContext cmsSessionContext;

    public AccessTokenImpl(final Jws<Claims> claimsJws, final CmsSessionContext cmsSessionContext) {
        this.claimsJws = claimsJws;
        this.cmsSessionContext = cmsSessionContext;
    }
    @Override
    public String getSubject() {
        return claimsJws.getBody().getSubject();
    }

    @Override
    public CmsSessionContext getCmsSessionContext() {
        return cmsSessionContext;
    }

    public String getClaim(final String claimName) {
        final Object claim = claimsJws.getBody().get(claimName);
        return claim == null ? null : claim.toString();
    }

    public String getHeader(final String headerName) {
        Object header = claimsJws.getHeader().get(headerName);
        return header == null ? null : header.toString();
    }
}
