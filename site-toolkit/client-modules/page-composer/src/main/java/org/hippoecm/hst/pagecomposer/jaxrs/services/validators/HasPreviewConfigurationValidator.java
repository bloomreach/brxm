/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;

public class HasPreviewConfigurationValidator implements Validator {

    private final PageComposerContextService pageComposerContextService;
    private final String mountId;

    public HasPreviewConfigurationValidator(final PageComposerContextService pageComposerContextService) {
        this.pageComposerContextService = pageComposerContextService;
        this.mountId = pageComposerContextService.getEditingMount().getIdentifier();
    }
    public HasPreviewConfigurationValidator(final PageComposerContextService pageComposerContextService,
                                            final String mountId) {
        this.pageComposerContextService = pageComposerContextService;
        this.mountId = mountId;
    }

    @Override
    public void validate(final HstRequestContext requestContext) throws RuntimeException {
        Mount mount = pageComposerContextService.getEditingPreviewVirtualHosts().getMountByIdentifier(mountId);
        if (!mount.getHstSite().hasPreviewConfiguration()) {
            final String message = String.format("There is no preview configuration for '%s'", mount.getHstSite().getConfigurationPath());
            throw new ClientException("There is no preview configuration", ClientError.NO_PREVIEW_CONFIGURATION, Collections.singletonMap("errorReason", message));
        }
    }
}
