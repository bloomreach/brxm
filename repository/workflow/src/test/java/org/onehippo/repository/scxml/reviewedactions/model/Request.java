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
package org.onehippo.repository.scxml.reviewedactions.model;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Request
 */
public class Request {

    public static final String TYPE_PUBLISH = "publish";
    public static final String TYPE_SCHEDULE_PUBLISH = "schedule-publish";
    public static final String TYPE_DEPUBLISH = "depublish";
    public static final String TYPE_SCHEDULE_DEPUBLISH = "schedule-depublish";

    private String type = "";

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
