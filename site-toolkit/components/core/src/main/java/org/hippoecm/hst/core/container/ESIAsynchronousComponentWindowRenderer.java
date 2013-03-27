/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.core.container;

import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.HstRequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 * ESIAsynchronousComponentWindowRenderer
 * <P>
 * Asynchronous component window rendering implementation leveraging ESI technologies.
 * </P>
 */
public class ESIAsynchronousComponentWindowRenderer implements AsynchronousComponentWindowRenderer {

    private static Logger log = LoggerFactory.getLogger(ESIAsynchronousComponentWindowRenderer.class);

    @Override
    public void processWindowBeforeRender(HstComponentWindow window, HstRequest request, HstResponse response) {
        HstRequestContext requestContext = request.getRequestContext();
        HstURL compUrl = response.createComponentRenderingURL();
        String url = HstRequestUtils.getFullyQualifiedHstURL(requestContext, compUrl, false);
        Element esiElem = response.createElement("esi:include");
        esiElem.setAttribute("src", url);
        esiElem.setAttribute("onerror", "continue");
        response.addPreamble(esiElem);
    }

}
