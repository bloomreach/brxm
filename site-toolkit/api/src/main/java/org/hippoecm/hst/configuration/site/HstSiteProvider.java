/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.site;

import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.services.hst.Channel;

@FunctionalInterface
public interface HstSiteProvider {

    /**
     * This key is used to store active project id on the http session of the webmaster in the channel mngr. If it is
     * not null then and thre is a channel with {@link Channel#getBranchId()} equal to projectId then that branch will
     * be used for rendering.
     */
    String ATTRIBUTE_ACTIVE_PROJECT_ID = "com.onehippo.cms7.services.wpm.project.active_project_id";

    HstSite getHstSite(CompositeHstSite compositeHstSite, HstRequestContext requestContext);

}
