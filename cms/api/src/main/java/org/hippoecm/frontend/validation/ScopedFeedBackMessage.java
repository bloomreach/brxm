/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.validation;

import java.io.Serializable;

import org.apache.wicket.Component;
import org.apache.wicket.feedback.FeedbackMessage;

public class ScopedFeedBackMessage extends FeedbackMessage {

    private FeedbackScope scope;

    public ScopedFeedBackMessage(final Component reporter, final Serializable message, final int level) {
        super(reporter, message, level);
        this.scope = FeedbackScope.DOCUMENT;
    }

    public ScopedFeedBackMessage(final Component reporter, final Serializable message, final int level,
                                 final FeedbackScope scope) {
        super(reporter, message, level);
        this.scope = scope;
    }

    public FeedbackScope getScope() {
        return scope;
    }

    @Override
    public String toString() {
        return "ScopedFeedBackMessage{" +
                "message = " + getMessage() +
                ", scope=" + scope +
                ", reporter = " + ((getReporter() == null) ? "null" : getReporter().getId()) +
                ", level = " + getLevelAsString() +
                '}';
    }
}
