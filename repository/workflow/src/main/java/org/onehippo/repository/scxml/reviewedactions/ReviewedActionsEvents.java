/*
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
package org.onehippo.repository.scxml.reviewedactions;

/**
 * SCXMLEvents
 */
public class ReviewedActionsEvents {

    public static final String WFE_DOC_DELETE = "workflow.event.document.delete";
    public static final String WFE_DOC_COPY = "workflow.event.document.copy";
    public static final String WFE_DOC_MOVE = "workflow.event.document.move";
    public static final String WFE_DOC_RENAME = "workflow.event.document.rename";

    public static final String WFE_DOC_EDIT_OBTAIN = "workflow.event.document.edit.obtain";
    public static final String WFE_DOC_EDIT_COMMIT = "workflow.event.document.edit.commit";
    public static final String WFE_DOC_EDIT_DISPOSE = "workflow.event.document.edit.dispose";

    public static final String WFE_DOC_PUBLISH = "workflow.event.document.publish";
    public static final String WFE_DOC_DEPUBLISH = "workflow.event.document.depublish";

    public static final String WFE_DOC_SCHED_PUBLISH = "workflow.event.document.schedule.publish";
    public static final String WFE_DOC_SCHED_DEPUBLISH = "workflow.event.document.schedule.depublish";

    private ReviewedActionsEvents() {
    }

}
