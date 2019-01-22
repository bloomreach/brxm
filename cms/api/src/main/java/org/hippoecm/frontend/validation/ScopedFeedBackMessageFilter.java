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

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.feedback.FeedbackMessage;

/**
 * A ContainerFeedbackMessageFilter filter that is scope aware.
 */
public class ScopedFeedBackMessageFilter extends ContainerFeedbackMessageFilter {

    private ValidationScope validationScope;

    /**
     * Constructor with default scope of {@code ValidationScope.DOCUMENT}
     *
     * @param container The container that message reporters must be a child of
     */
    public ScopedFeedBackMessageFilter(final MarkupContainer container) {
        super(container);
        validationScope = ValidationScope.DOCUMENT;
    }

    /**
     * Constructor
     *
     * @param container The container that message reporters must be a child of
     * @param scope     The scope to filter feedback messages by.
     */
    public ScopedFeedBackMessageFilter(final MarkupContainer container, final String scope) {
        this(container);
        validationScope = ValidatorUtils.getValidationScope(scope);
    }

    @Override
    public boolean accept(final FeedbackMessage message) {
        if (super.accept(message)) {
            if (message instanceof ScopedFeedBackMessage) {
                ScopedFeedBackMessage scopedMessage = (ScopedFeedBackMessage) message;
                return scopedMessage.getScope().equals(this.validationScope);
            }
        }
        return false;
    }
}
