/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.demo.container;

import java.security.Principal;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.container.valves.AbstractOrderableValve;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentBean;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.ContainerConstants;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.HeadContributable;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.services.hst.Channel;
import org.w3c.dom.Element;

/**
 * Simple demo Visitor tracking valve by providing a custom <code>HeadContributable</code>.
 */
public class ExampleTrackingContributionValve extends AbstractOrderableValve {

    public ExampleTrackingContributionValve() {
    }

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        final HstRequestContext requestContext = context.getRequestContext();
        // Register a custom HeadContributable by the contributor name, "exampleTracking".
        requestContext.setHeadContributable("exampleTracking", new ExampleTrackingHeadContributable());

        context.invokeNext();
    }

    /*
     * Example demo visitor tracking data generation as string.
     * In reality, you might want to use a JSON library instead to put data items and convert into a string.
     * 
     * Example:
     * 
     * var _tracking_data = {
     *   "username": "john",
     *   "ts": 123456789,
     *   "document": "<document_handle_uuid>",
     *   "channel": "<channel_id>"
     * };
     */
    private String generateTrackingDataObjectAsString() {
        final HstRequestContext requestContext = RequestContextProvider.get();

        StringBuilder sb = new StringBuilder(256);
        sb.append("\n");
        sb.append("var _tracking_data = {\n");

        final Principal userPrincipal = requestContext.getServletRequest().getUserPrincipal();
        final String userName = (userPrincipal != null) ? userPrincipal.getName() : "";
        sb.append("  \"username\": \"" + userName + "\",\n");

        sb.append("  \"ts\": " + System.currentTimeMillis() + ",\n");

        final HippoBean document = requestContext.getContentBean();
        if (document != null && document instanceof HippoDocumentBean) {
            sb.append("  \"document\": \"" + ((HippoDocumentBean) document).getCanonicalHandleUUID() + "\",\n");
        } else {
            sb.append("  \"document\": \"\",\n");
        }

        String channelId = "";
        if (requestContext.getResolvedMount() != null) {
            final Channel channel = requestContext.getResolvedMount().getMount().getChannel();
            if (channel != null) {
                channelId = channel.getId();
            }
        }
        sb.append("  \"channel\": \"" + channelId + "\",\n");

        sb.append("};\n");
        return sb.toString();
    }

    private class ExampleTrackingHeadContributable implements HeadContributable {

        @Override
        public void contributeHeadElements(HstResponse response) {
            final HstRequestContext requestContext = RequestContextProvider.get();

            final Element trackingDataElem = response.createElement("script");
            trackingDataElem.setTextContent(generateTrackingDataObjectAsString());
            trackingDataElem.setAttribute(ContainerConstants.HEAD_ELEMENT_CONTRIBUTION_CATEGORY_HINT_ATTRIBUTE, "scripts");
            response.addHeadElement(trackingDataElem, "exampleTracking.tracking.data");

            // Contribute the example tracker library javascript tag.
            final HstLink trackerJsLink = requestContext.getHstLinkCreator().create("/javascript/example-tracker.js",
                    requestContext.getResolvedMount().getMount());
            final Element trackingLibElem = response.createElement("script");
            trackingLibElem.setAttribute("src", trackerJsLink.toUrlForm(requestContext, false));
            trackingLibElem.setAttribute(ContainerConstants.HEAD_ELEMENT_CONTRIBUTION_CATEGORY_HINT_ATTRIBUTE, "scripts");
            response.addHeadElement(trackingLibElem, "exampleTracking.tracking.library");
        }

    }
}
