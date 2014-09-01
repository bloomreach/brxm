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

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.onehippo.cms7.essentials.components.info.EssentialsImageComponentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */

@ParametersInfo(type = EssentialsImageComponentInfo.class)
public class EssentialsImageComponent extends CommonComponent {
    private static final Logger log = LoggerFactory.getLogger(EssentialsImageComponent.class);

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        super.doBeforeRender(request, response);
        final EssentialsImageComponentInfo paramInfo = getComponentParametersInfo(request);
        final String documentPath = paramInfo.getDocument();
        log.debug("Calling EssentialsImageComponent for document path:  [{}]", documentPath);
        if (!Strings.isNullOrEmpty(documentPath)) {
            try {
                final Object image = RequestContextProvider.get().getObjectBeanManager().getObject(documentPath);
                request.setAttribute(REQUEST_ATTR_DOCUMENT, image);
            } catch (ObjectBeanManagerException e) {
                if (log.isDebugEnabled()) {
                    log.error("Error getting image", e);
                }
            }
        }
        request.setAttribute(REQUEST_ATTR_PARAM_INFO, paramInfo);
    }
}
