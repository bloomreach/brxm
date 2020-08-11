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
package org.hippoecm.hst.pagecomposer.jaxrs.services.component;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;

public class ComponentInfoContext {

    private final PageComposerContextService contextService;
    private final CmsSessionContext cmsSessionContext;
    private final String siteMapItemUuid;
    private final String hostGroup;

    public ComponentInfoContext(
            final PageComposerContextService contextService,
            final CmsSessionContext cmsSessionContext,
            final String siteMapItemUuid,
            final String hostGroup
    ) {
        this.contextService = contextService;
        this.cmsSessionContext = cmsSessionContext;
        this.siteMapItemUuid = siteMapItemUuid;
        this.hostGroup = hostGroup;
    }

    public PageComposerContextService getContextService() {
        return contextService;
    }

    public Map<String, Serializable> getContextPayload() {
        return cmsSessionContext.getContextPayload();
    }

    public String getSiteMapItemUuid() {
        return siteMapItemUuid;
    }

    public String getHostGroup() {
        return hostGroup;
    }

    public Locale getLocale() {
        return cmsSessionContext.getLocale();
    }

    public String getUserId() throws RepositoryException {
        return contextService.getRequestContext().getSession().getUserID();
    }

    public String getMountId() {
        return getContextPayload().get(ContainerConstants.CMS_REQUEST_RENDERING_MOUNT_ID).toString();
    }
}
