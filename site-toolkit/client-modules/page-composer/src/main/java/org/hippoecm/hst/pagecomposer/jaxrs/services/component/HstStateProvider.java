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
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.pagecomposer.jaxrs.services.component.state.util.ScheduledRequest;
import org.hippoecm.hst.platform.api.experiencepages.XPageLayout;

import static java.util.stream.Collectors.toMap;

final class HstStateProvider {

    Set<State> getStates(final ActionStateProviderContext context) {
        final Set<State> states = new HashSet<>();
        final ChannelContext channelContext = context.getChannelContext();
        states.add(HstState.CHANNEL_XPAGE_LAYOUTS.toState(channelContext.getXPageLayouts().stream()
                .collect(toMap(XPageLayout::getKey, XPageLayout::getLabel))));
        states.add(HstState.CHANNEL_XPAGE_TEMPLATE_QUERIES.toState(channelContext.getXPageTemplateQueries()));

        if (context.isExperiencePageRequest()) {
            final XPageContext xPageContext = context.getXPageContext();
            states.addAll(xPageStates(xPageContext));
            states.addAll(scheduledRequestStates(xPageContext));
            states.add(HstState.WORKFLOW_REQUESTS.toState(xPageContext.getWorkflowRequests()));
        }

        return states;
    }

    private Set<State> xPageStates(XPageContext xPageContext) {
        final Set<State> states = new HashSet<>();
        states.add(HstState.XPAGE_BRANCH_ID.toState(xPageContext.getBranchId()));
        states.add(HstState.XPAGE_ID.toState(xPageContext.getXPageId()));
        states.add(HstState.XPAGE_NAME.toState(xPageContext.getXPageName()));
        states.add(HstState.XPAGE_STATE.toState(xPageContext.getXPageState()));

        if (!StringUtils.isEmpty(xPageContext.getLockedBy())) {
            states.add(HstState.XPAGE_LOCKED_BY.toState(xPageContext.getLockedBy()));
        }

        return states;
    }

    private Set<State> scheduledRequestStates(XPageContext xPageContext) {
        final ScheduledRequest scheduledRequest = xPageContext.getScheduledRequest();
        if (scheduledRequest == null) {
            return Collections.emptySet();
        }
        final Set<State> states = new HashSet<>();
        states.add(HstState.SCHEDULEDREQUEST_SCHEDULED_DATE.toState(scheduledRequest.getScheduledDate()));
        states.add(HstState.SCHEDULEDREQUEST_TYPE.toState(scheduledRequest.getType()));
        return states;
    }

}
