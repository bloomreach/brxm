/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.reviewedactions;

import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.repository.events.HippoWorkflowEvent;

/**
 * The ReviewedActionsWorkflowEvent is an <em>example</em> of defining your own HippoEvent wrapper.
 * More detailed documentation about its usage can be found online
 * <a href="https://cms.onehippo.org/?path=/content/documents/7_8/library/concepts/hippo-services/event-bus">here</a>
 *
 * @deprecated since CMS 7.9, The reviewedactions workflow has been deprecated and replaced by
 * {@link org.onehippo.repository.documentworkflow.DocumentWorkflow}, so this example no longer should be used 'as is'
 */
@Deprecated
public final class ReviewedActionsWorkflowEvent extends HippoWorkflowEvent<ReviewedActionsWorkflowEvent> {

    private static final String WORKFLOW_EVENT_TYPE = "event-type";

    private static final String DEPUBLISH = "depublish";
    private static final String PUBLISH = "publish";
    private static final String COMMIT_EDITABLE_INSTANCE = "commitEditableInstance";

    public enum Type {

        UNKNOWN,

        /** when a document gets published. */
        PUBLISHED,

        /** when a document gets depublished. */
        UNPUBLISHED,

        /** when a document gets updated. */
        CHANGED
    }

    public ReviewedActionsWorkflowEvent(final HippoEvent<?> event) {
        super(event);

        final String eventMethod = methodName();
        if (PUBLISH.equals(eventMethod)) {
            type(Type.PUBLISHED);
        } else if (DEPUBLISH.equals(eventMethod)) {
            type(Type.UNPUBLISHED);
        } else if (COMMIT_EDITABLE_INSTANCE.equals(eventMethod)) {
            type(Type.CHANGED);
        } else {
            type(Type.UNKNOWN);
        }
    }

    public Type type() {
        return get(WORKFLOW_EVENT_TYPE);
    }

    private ReviewedActionsWorkflowEvent type(Type type) {
        return set(WORKFLOW_EVENT_TYPE, type);
    }
}
