/*
 *  Copyright 2013-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.channelmanager;

import java.util.Map;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.w3c.dom.Comment;

public abstract class AbstractComponentWindowResponseAppender implements ComponentWindowResponseAppender {


    /**
     * @deprecated since 13.2.0 : Use {@link #isChannelManagerPreviewRequest(HstRequest)}
     */
    @Deprecated
    protected boolean isCmsRequest(HstRequest request) {
        return request.getRequestContext().isChannelManagerPreviewRequest();
    }

    protected boolean isChannelManagerPreviewRequest(HstRequest request) {
        return request.getRequestContext().isChannelManagerPreviewRequest();
    }

    protected boolean isTopHstResponse(final HstComponentWindow rootWindow,
                                       final HstComponentWindow rootRenderingWindow,
                                       final HstComponentWindow window) {
        return rootWindow == rootRenderingWindow && window == rootWindow;
    }

    protected Comment createCommentWithAttr(Map<String, String> attributes, HstResponse response) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> attr : attributes.entrySet()) {
            if (builder.length() != 0) {
                builder.append(", ");
            }
            builder.append("\"").append(attr.getKey()).append("\":").append("\"").append(attr.getValue()).append("\"");
        }
        Comment comment = response.createComment(" { " + builder.toString() + "} ");
        return comment;
    }

    protected static boolean isContainerOrContainerItem(final HstComponentConfiguration compConfig) {
        return HstComponentConfiguration.Type.CONTAINER_ITEM_COMPONENT.equals(compConfig.getComponentType())
                || HstComponentConfiguration.Type.CONTAINER_COMPONENT.equals(compConfig.getComponentType());
    }
}
