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
package org.hippoecm.repository.standardworkflow;

import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.repository.events.HippoWorkflowEvent;

public final class FolderWorkflowEvent extends HippoWorkflowEvent<FolderWorkflowEvent> {

    private static final String WORKFLOW_EVENT_TYPE = "event-type";

    private static final String DELETE = "delete";
    private static final String ADD = "add";

    public enum Type {

        UNKNOWN,

        /** when a new document gets created. */
        CREATED,

        /** when a document gets removed. */
        DELETED

    }

    public FolderWorkflowEvent(final HippoEvent<?> event) {
        super(event);

        String eventMethod = methodName();
        if (ADD.equals(eventMethod)) {
            type(Type.CREATED);
        } else if (DELETE.equals(eventMethod)) {
            type(Type.DELETED);
        } else {
            type(Type.UNKNOWN);
        }
    }

    public Type type() {
        return get(WORKFLOW_EVENT_TYPE);
    }

    private FolderWorkflowEvent type(Type type) {
        return set(WORKFLOW_EVENT_TYPE, type);
    }
}
