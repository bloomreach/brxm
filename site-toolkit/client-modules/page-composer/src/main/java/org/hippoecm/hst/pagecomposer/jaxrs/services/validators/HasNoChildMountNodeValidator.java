/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

/**
 * Validate that the given hst:mount node does not have any children hst:mount nodes
 */
public class HasNoChildMountNodeValidator implements Validator {

    private final String mountId;

    public HasNoChildMountNodeValidator(final String mountId) {
        this.mountId = mountId;
    }

    @Override
    public void validate(final HstRequestContext requestContext) throws RuntimeException {
        final Mount mount = requestContext.getVirtualHost().getVirtualHosts().getMountByIdentifier(mountId);
        if (!mount.getChildMounts().isEmpty()) {
            throw new ClientException("Child mount exists", ClientError.CHILD_MOUNT_EXISTS);
        }
    }
}
