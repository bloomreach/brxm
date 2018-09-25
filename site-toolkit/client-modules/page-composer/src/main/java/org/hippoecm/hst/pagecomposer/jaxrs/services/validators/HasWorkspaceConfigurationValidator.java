/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Collections;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;

import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_WORKSPACE;

public class HasWorkspaceConfigurationValidator implements Validator {

    private PageComposerContextService pageComposerContextService;
    private final String mountId;

    public HasWorkspaceConfigurationValidator(final PageComposerContextService pageComposerContextService, final String mountId) {
        this.pageComposerContextService = pageComposerContextService;
        this.mountId = mountId;
    }

    @Override
    public void validate(final HstRequestContext requestContext) throws RuntimeException {
        Mount mount = pageComposerContextService.getEditingPreviewVirtualHosts().getMountByIdentifier(mountId);
        try {
            if (!requestContext.getSession().nodeExists(mount.getHstSite().getConfigurationPath() + "/" + NODENAME_HST_WORKSPACE)) {
                final String message = String.format("There is no workspace configuration for '%s'", mount.getHstSite().getConfigurationPath());
                throw new ClientException("There is no workspace configuration", ClientError.NO_PREVIEW_CONFIGURATION, Collections.singletonMap("errorReason", message));
            }
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException(e);
        }
    }
}
