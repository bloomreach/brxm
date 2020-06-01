/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
 */

package org.hippoecm.frontend.widgets;

import org.apache.wicket.ajax.AjaxRequestTarget;

/**
 * The payload class used by wicket events to notify feedback messages changes from wicket fields to parent components.
 * If parent components have feedback panels, they can consume these events to redraw their feedback panels.
 */
public class UpdateFeedbackInfo {
    private final AjaxRequestTarget target;
    private final boolean clearAll;

    public UpdateFeedbackInfo(final AjaxRequestTarget target) {
        this(target, false);
    }

    public UpdateFeedbackInfo(final AjaxRequestTarget target, final boolean clearAllFeedbacks) {
        this.target = target;
        this.clearAll = clearAllFeedbacks;
    }

    public AjaxRequestTarget getTarget(){
        return target;
    }

    /**
     * @return the value to determine if the receiver should delete feedback messages of all children components
     */
    public boolean isClearAll() {
        return clearAll;
    }
}
