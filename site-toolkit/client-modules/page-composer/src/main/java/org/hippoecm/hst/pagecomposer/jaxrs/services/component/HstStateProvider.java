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
import java.util.stream.Stream;

import org.hippoecm.hst.pagecomposer.jaxrs.services.component.state.util.ScheduledRequest;
import org.hippoecm.hst.pagecomposer.jaxrs.services.component.state.util.WorkflowRequest;

import static java.util.stream.Collectors.toSet;

final class HstStateProvider {

    Set<State> getStates(final ActionStateProviderContext context) {
        if (!context.isExperiencePageRequest()) {
            return Collections.emptySet();
        }
        final XPageContext xPageContext = context.getXPageContext();
        return Stream.of(
                xPageStates(xPageContext),
                workflowRequestStates(xPageContext),
                scheduledRequestStates(xPageContext)
        ).flatMap(Set::stream).collect(toSet());
    }

    private Set<State> xPageStates(XPageContext xPageContext) {
        final Set<State> states = new HashSet<>();
        states.add(HstState.XPAGE_BRANCH_ID.toState(xPageContext.getBranchId()));
        states.add(HstState.XPAGE_ID.toState(xPageContext.getXPageId()));
        states.add(HstState.XPAGE_NAME.toState(xPageContext.getXPageName()));
        states.add(HstState.XPAGE_STATE.toState(xPageContext.getXPageState()));
        return states;
    }

    private Set<State> workflowRequestStates(XPageContext xPageContext) {
        final WorkflowRequest workflowRequest = xPageContext.getWorkflowRequest();
        if (workflowRequest == null) {
            return Collections.emptySet();
        }
        final Set<State> states = new HashSet<>();
        states.add(HstState.WORKFLOWREQUEST_CREATION_DATE.toState(workflowRequest.getCreationDate()));
        if (workflowRequest.getRequestDate() != null) {
            states.add(HstState.WORKFLOWREQUEST_REQUEST_DATE.toState(workflowRequest.getRequestDate()));
        }
        states.add(HstState.WORKFLOWREQUEST_TYPE.toState(workflowRequest.getType()));
        states.add(HstState.WORKFLOWREQUEST_USERNAME.toState(workflowRequest.getUsername()));
        if (workflowRequest.getReason() != null) {
            states.add(HstState.WORKFLOWREQUEST_REASON.toState(workflowRequest.getReason()));
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
