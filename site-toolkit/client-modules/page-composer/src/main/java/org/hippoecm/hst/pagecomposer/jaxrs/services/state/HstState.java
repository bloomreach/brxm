/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services.state;

import java.util.stream.Stream;

import org.hippoecm.hst.pagecomposer.jaxrs.services.action.Category;

public enum HstState {

    XPAGE_BRANCH_ID("branchId", HstStateCategories.xpage()),
    XPAGE_ID("id", HstStateCategories.xpage()),
    XPAGE_NAME("name", HstStateCategories.xpage()),
    XPAGE_STATE("state", HstStateCategories.xpage()),

    WORKFLOWREQUEST_CREATION_DATE("creationDate", HstStateCategories.workflowrequest()),
    WORKFLOWREQUEST_REQUEST_DATE("requestDate", HstStateCategories.workflowrequest()),
    WORKFLOWREQUEST_REASON("reason", HstStateCategories.workflowrequest()),
    WORKFLOWREQUEST_TYPE("type", HstStateCategories.workflowrequest()),
    WORKFLOWREQUEST_USERNAME("username", HstStateCategories.workflowrequest()),

    SCHEDULEDREQUEST_SCHEDULED_DATE("scheduledDate", HstStateCategories.scheduledrequest()),
    SCHEDULEDREQUEST_TYPE("type", HstStateCategories.scheduledrequest()),
    ;

    private final String name;
    private final Category category;

    HstState(final String name, final Category category) {
        this.name = name;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public State toState(Object value) {
        return new State(name, category.getName(), value);
    }

    public static Stream<HstState> states(Category category) {
        return Stream.of(values()).filter(hstAction -> category.equals(hstAction.category));
    }
}
