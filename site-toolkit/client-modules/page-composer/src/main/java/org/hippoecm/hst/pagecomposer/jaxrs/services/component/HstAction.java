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

public enum HstAction implements NamedCategory {

    CHANNEL_SETTINGS("settings", HstCategory.CHANNEL),
    CHANNEL_PUBLISH("publish", HstCategory.CHANNEL),
    CHANNEL_DISCARD_CHANGES("discard-changes", HstCategory.CHANNEL),
    CHANNEL_MANAGE_CHANGES("manage-changes", HstCategory.CHANNEL),
    CHANNEL_DELETE("delete", HstCategory.CHANNEL),
    CHANNEL_CLOSE("close", HstCategory.CHANNEL),

    PAGE_PROPERTIES("properties", HstCategory.PAGE),
    PAGE_COPY("copy", HstCategory.PAGE),
    PAGE_MOVE("move", HstCategory.PAGE),
    PAGE_DELETE("delete", HstCategory.PAGE),
    PAGE_NEW("new", HstCategory.PAGE),

    XPAGE_PUBLISH("publish", HstCategory.XPAGE),
    XPAGE_SCHEDULE_PUBLICATION("schedule-publish", HstCategory.XPAGE),
    XPAGE_CANCEL_SCHEDULED_PUBLICATION("cancel-scheduled-publish", HstCategory.XPAGE),
    XPAGE_REQUEST_PUBLICATION("request-publish", HstCategory.XPAGE),
    XPAGE_REQUEST_SCHEDULE_PUBLICATION("request-schedule-publish", HstCategory.XPAGE),

    XPAGE_UNPUBLISH("unpublish", HstCategory.XPAGE),
    XPAGE_SCHEDULE_UNPUBLICATION("schedule-unpublish", HstCategory.XPAGE),
    XPAGE_CANCEL_SCHEDULED_DEPUBLICATION("cancel-scheduled-depublish", HstCategory.XPAGE),
    XPAGE_REQUEST_UNPUBLICATION("request-unpublish", HstCategory.XPAGE),
    XPAGE_REQUEST_SCHEDULE_UNPUBLICATION("request-schedule-unpublish", HstCategory.XPAGE),

    XPAGE_REQUEST_ACCEPT("accept", HstCategory.XPAGE),
    XPAGE_REQUEST_CANCEL("cancel", HstCategory.XPAGE),
    XPAGE_REQUEST_REJECT("reject", HstCategory.XPAGE),
    XPAGE_REQUEST_REJECTED("rejected", HstCategory.XPAGE),

    XPAGE_RENAME("rename", HstCategory.XPAGE),
    XPAGE_COPY("copy", HstCategory.XPAGE),
    XPAGE_MOVE("move", HstCategory.XPAGE),
    XPAGE_DELETE("delete", HstCategory.XPAGE),
    ;

    private final String name;
    private final Category category;

    HstAction(final String name, final Category category) {
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
