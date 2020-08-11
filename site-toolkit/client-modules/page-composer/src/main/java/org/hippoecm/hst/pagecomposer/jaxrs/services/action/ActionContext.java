/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services.action;

import java.io.Serializable;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;

public class ActionContext {

    private final PageComposerContextService contextService;
    private final Map<String, Serializable> contextPayload;
    private final String siteMapItemUuid;
    private final String hostGroup;

    public ActionContext(
            final PageComposerContextService contextService,
            final String siteMapItemUuid,
            final Map<String, Serializable> contextPayload,
            final String hostGroup
    ) {
        this.contextService = contextService;
        this.siteMapItemUuid = siteMapItemUuid;
        this.contextPayload = contextPayload;
        this.hostGroup = hostGroup;
    }

    public PageComposerContextService getContextService() {
        return contextService;
    }

    public String getSiteMapItemUuid() {
        return siteMapItemUuid;
    }

    public String getMountId() {
        return contextPayload.get(ContainerConstants.CMS_REQUEST_RENDERING_MOUNT_ID).toString();
    }

    public Map<String, Serializable> getContextPayload() {
        return contextPayload;
    }

    public String getHostGroup() {
        return hostGroup;
    }

    public String getUserId() throws RepositoryException {
        return contextService.getRequestContext().getSession().getUserID();
    }
}
