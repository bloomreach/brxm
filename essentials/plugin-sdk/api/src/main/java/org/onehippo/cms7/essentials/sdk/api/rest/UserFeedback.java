/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.sdk.api.rest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * UserFeedback encapsulates one or more messages to be displayed to a user of the dashboard web application,
 * as feedback for some user-triggered action.
 *
 * UserFeedback will typically be serialized to JSON and sent to the front-end in the response of a REST call.
 * The field 'feedbackMessages' is used by the front-end to detect feedback messages in arbitrary REST call responses.
 */
public class UserFeedback implements Serializable {
    private final List<Details> feedbackMessages = new ArrayList<>();

    public List<Details> getFeedbackMessages() {
        return feedbackMessages;
    }

    public UserFeedback addSuccess(final String message) {
        addMessage(message, false);
        return this; // for chaining
    }

    public UserFeedback addError(final String message) {
        addMessage(message, true);
        return this; // for chaining
    }

    private void addMessage(final String message, final boolean error) {
        final Details details = new Details();

        details.message = message;
        details.error = error;

        feedbackMessages.add(details);
    }

    private static class Details {
        private String message;
        private boolean error;

        public String getMessage() {
            return message;
        }

        public boolean isError() {
            return error;
        }
    }
}
