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
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services.validators;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;

public class BelongToSameModelValidator implements Validator {

    private final Mount mount1;
    private final Mount mount2;

    public BelongToSameModelValidator(final Mount mount1, final Mount mount2) {
        this.mount1 = mount1;
        this.mount2 = mount2;
    }

    @Override
    public void validate(final HstRequestContext requestContext) throws RuntimeException {
        // do not compare on mount1.getVirtualHost().getVirtualHosts() instance equality since might be different
        // decorated instances

        if (!mount1.getContextPath().equals(mount2.getContextPath())) {
            throw new ClientException("Mounts do not belong to the same webapp", ClientError.FORBIDDEN);
        }
    }
}
