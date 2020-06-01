/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.onehippo.cms7.essentials.components.info.EssentialsDocumentComponentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hippo HST component for simple (document detail) request handling.
 * If document path is not defined, EssentialsDocumentComponent will try to fetch sitemap mapped bean.
 * @version "$Id$"
 * @see EssentialsContentComponent
 */
@ParametersInfo(type = EssentialsDocumentComponentInfo.class)
public class EssentialsDocumentComponent extends CommonComponent {

    private static Logger log = LoggerFactory.getLogger(EssentialsDocumentComponent.class);

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        super.doBeforeRender(request, response);
        final EssentialsDocumentComponentInfo paramInfo = getComponentParametersInfo(request);
        final String documentPath = paramInfo.getDocument();
        log.debug("Calling EssentialsDocumentComponent for document path:  [{}]", documentPath);
        setContentBeanForPath(documentPath, request, response);
        request.setAttribute(REQUEST_ATTR_PARAM_INFO, paramInfo);
    }


}
