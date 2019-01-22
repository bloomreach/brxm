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

import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.feedback.IFeedbackMessageFilter;

/**
 * FeedbackMessage filter for ValidationScopes.
 */
public class ScopedFeedBackMessageFilter implements IFeedbackMessageFilter {

    private ValidationScope validationScope;

    public ScopedFeedBackMessageFilter(final String scope) {
        validationScope = ValidatorUtils.getValidationScope(scope);
    }

    @Override
    public boolean accept(final FeedbackMessage message) {
        if (message instanceof ScopedFeedBackMessage) {
            ScopedFeedBackMessage scopedMessage = (ScopedFeedBackMessage) message;
            return scopedMessage.getScope().equals(this.validationScope);
        }
        return false;
    }
}
