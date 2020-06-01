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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FeedbackMessageComparatorTest {
    
    @Test
    public void testOrder() {
        final List<ScopedFeedBackMessage> messages = new ArrayList<>();

        messages.add(new ScopedFeedBackMessage(null, "m1", 0)); // by default FeedbackPriority.NORMAL

        final ScopedFeedBackMessage m2 = new ScopedFeedBackMessage(null, "m2", 0);
        m2.setFeedbackPriority(FeedbackPriority.HIGH);
        messages.add(m2);

        messages.sort(new FeedbackMessageComparator());
        
        assertEquals(messages.get(0).getMessage(),"m2");
    }
}
