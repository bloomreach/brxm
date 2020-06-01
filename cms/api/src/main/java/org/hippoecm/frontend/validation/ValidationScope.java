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

/**
 * @deprecated Use {@link FeedbackScope} instead
 */
@Deprecated
public enum ValidationScope {
    COMPOUND,
    DOCUMENT,
    FIELD;

    public static ValidationScope from(final FeedbackScope feedbackScope) {
        switch (feedbackScope) {
            case COMPOUND:
                return ValidationScope.COMPOUND;
            case FIELD:
                return ValidationScope.FIELD;
            case DOCUMENT:
            default:
                return ValidationScope.DOCUMENT;
        }
    }

    public FeedbackScope toFeedbackScope() {
        switch (this) {
            case COMPOUND:
                return FeedbackScope.COMPOUND;
            case FIELD:
                return FeedbackScope.FIELD;
            case DOCUMENT:
            default:
                return FeedbackScope.DOCUMENT;
        }
    }
}
