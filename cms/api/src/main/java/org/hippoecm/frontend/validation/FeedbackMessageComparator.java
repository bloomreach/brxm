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

import java.util.Comparator;

import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.util.io.IClusterable;

public class FeedbackMessageComparator implements Comparator<FeedbackMessage>, IClusterable {

    @Override
    public int compare(final FeedbackMessage f1, final FeedbackMessage f2) {
        if (f1 instanceof ScopedFeedBackMessage && f2 instanceof ScopedFeedBackMessage) {
            ScopedFeedBackMessage m1 = (ScopedFeedBackMessage) f1;
            ScopedFeedBackMessage m2 = (ScopedFeedBackMessage) f2;
            return m1.getFeedbackPriority().ordinal() - m2.getFeedbackPriority().ordinal();
        }
        return 0;
    }
}
