/*
 * Copyright 2015-2019 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.components;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.onehippo.cms7.essentials.components.info.EssentialsGoogleMapsComponentInfo;

@ParametersInfo(type = EssentialsGoogleMapsComponentInfo.class)
public class EssentialsGoogleMapsComponent extends CommonComponent {

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);

        setComponentId(request, response);

        final EssentialsGoogleMapsComponentInfo paramInfo = getComponentParametersInfo(request);
        request.setAttribute(REQUEST_ATTR_PARAM_INFO, paramInfo);
        // 'cmsrequest' attribute deprecated since 13.2, use 'hstRequest.requestContext.channelManagerPreviewRequest' in
        // templates instead
        request.setAttribute("cmsrequest", RequestContextProvider.get().isChannelManagerPreviewRequest());
    }
}
