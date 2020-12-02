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

public enum HstState implements NamedCategory {

    CHANNEL_XPAGE_LAYOUTS("xPageLayouts", HstCategory.CHANNEL),
    CHANNEL_XPAGE_TEMPLATE_QUERIES("xPageTemplateQueries", HstCategory.CHANNEL),

    XPAGE_BRANCH_ID("branchId", HstCategory.XPAGE),
    XPAGE_ID("id", HstCategory.XPAGE),
    XPAGE_LOCKED_BY("lockedBy", HstCategory.XPAGE),
    XPAGE_NAME("name", HstCategory.XPAGE),
    XPAGE_STATE("state", HstCategory.XPAGE),

    WORKFLOW_REQUESTS("requests", HstCategory.WORKFLOW),

    SCHEDULEDREQUEST_SCHEDULED_DATE("scheduledDate", HstCategory.SCHEDULED_REQUEST),
    SCHEDULEDREQUEST_TYPE("type", HstCategory.SCHEDULED_REQUEST),
    ;

    private final String name;
    private final Category category;

    HstState(final String name, final Category category) {
        this.name = name;
        this.category = category;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Category getCategory() {
        return category;
    }
}
