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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.pagecomposer.jaxrs.services.component.state.util.ScheduledRequest;
import org.hippoecm.hst.platform.api.experiencepages.XPageLayout;

import static java.util.stream.Collectors.toMap;

final class HstStateProvider {

    Map<NamedCategory, Object> getStates(final ActionStateProviderContext context) {
        final Map<NamedCategory, Object> states = new HashMap<>();
        final ChannelContext channelContext = context.getChannelContext();
        states.put(HstState.CHANNEL_XPAGE_LAYOUTS, channelContext.getXPageLayouts().stream()
                .collect(toMap(XPageLayout::getKey, XPageLayout::getLabel)));
        states.put(HstState.CHANNEL_XPAGE_TEMPLATE_QUERIES, channelContext.getXPageTemplateQueries());

        if (context.isExperiencePageRequest()) {
            final XPageContext xPageContext = context.getXPageContext();
            states.putAll(xPageStates(xPageContext));
            states.putAll(scheduledRequestStates(xPageContext));
            states.put(HstState.WORKFLOW_REQUESTS, xPageContext.getWorkflowRequests());
        }

        return states;
    }

    private Map<NamedCategory, Object> xPageStates(XPageContext xPageContext) {
        final Map<NamedCategory, Object> states = new HashMap<>();
        states.put(HstState.XPAGE_BRANCH_ID, xPageContext.getBranchId());
        states.put(HstState.XPAGE_ID, xPageContext.getXPageId());
        states.put(HstState.XPAGE_NAME, xPageContext.getXPageName());
        states.put(HstState.XPAGE_STATE, xPageContext.getXPageState());

        if (!StringUtils.isEmpty(xPageContext.getLockedBy())) {
            states.put(HstState.XPAGE_LOCKED_BY, xPageContext.getLockedBy());
        }

        return states;
    }

    private Map<NamedCategory, Object> scheduledRequestStates(XPageContext xPageContext) {
        final ScheduledRequest scheduledRequest = xPageContext.getScheduledRequest();
        if (scheduledRequest == null) {
            return Collections.emptyMap();
        }
        final Map<NamedCategory, Object> states = new HashMap<>();
        states.put(HstState.SCHEDULEDREQUEST_SCHEDULED_DATE, scheduledRequest.getScheduledDate());
        states.put(HstState.SCHEDULEDREQUEST_TYPE, scheduledRequest.getType());
        return states;
    }

}
