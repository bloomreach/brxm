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
package org.hippoecm.hst.pagecomposer.jaxrs.services.action;

import java.util.stream.Stream;

public enum HstAction {

    CHANNEL_SETTINGS("settings", HstCategories.channel()),
    CHANNEL_PUBLISH("publish", HstCategories.channel()),
    CHANNEL_DISCARD_CHANGES("discard-changes", HstCategories.channel()),
    CHANNEL_MANAGE_CHANGES("manage-changes", HstCategories.channel()),
    CHANNEL_DELETE("delete", HstCategories.channel()),
    CHANNEL_CLOSE("close", HstCategories.channel()),

    PAGE_PROPERTIES("properties", HstCategories.page()),
    PAGE_COPY("copy", HstCategories.page()),
    PAGE_MOVE("move", HstCategories.page()),
    PAGE_DELETE("delete", HstCategories.page()),
    PAGE_NEW("new", HstCategories.page()),

    XPAGE_PUBLISH("publish", HstCategories.xpage()),
    XPAGE_SCHEDULE_PUBLICATION("schedule-publish", HstCategories.xpage()),
    XPAGE_REQUEST_PUBLICATION("request-publish", HstCategories.xpage()),
    XPAGE_REQUEST_SCHEDULE_PUBLICATION("request-schedule-publish", HstCategories.xpage()),

    XPAGE_UNPUBLISH("unpublish", HstCategories.xpage()),
    XPAGE_SCHEDULE_UNPUBLICATION("schedule-unpublish", HstCategories.xpage()),
    XPAGE_REQUEST_UNPUBLICATION("request-unpublish", HstCategories.xpage()),
    XPAGE_REQUEST_SCHEDULE_UNPUBLICATION("request-schedule-unpublish", HstCategories.xpage()),

    XPAGE_COPY("copy", HstCategories.xpage()),
    XPAGE_MOVE("move", HstCategories.xpage()),
    XPAGE_DELETE("delete", HstCategories.xpage()),
    ;

    private final String name;
    private final Category category;

    HstAction(final String name, final Category category) {
        this.name = name;
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public Action toAction(boolean enabled) {
        return new Action(name, category.getName(), enabled);
    }

    public static Stream<HstAction> actions(Category category) {
        return Stream.of(values()).filter(hstAction -> category.equals(hstAction.category));
    }
}
