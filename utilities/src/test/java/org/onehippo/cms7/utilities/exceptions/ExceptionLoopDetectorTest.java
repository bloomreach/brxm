/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.utilities.exceptions;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExceptionLoopDetectorTest {

    @Test
    public void testLoopDetection() throws InterruptedException {

        final ExceptionLoopDetector loopDetector = new ExceptionLoopDetector(2000L, 3);

        final Exception exception = new Exception("My msg");
        loopDetector.loopDetected(exception);
        boolean loopDetected = loopDetector.loopDetected(exception);
        assertFalse(loopDetected);
        loopDetected = loopDetector.loopDetected(exception);
        assertTrue(loopDetected);

        Thread.sleep(2500L);

        loopDetected = loopDetector.loopDetected(exception);
        assertFalse(loopDetected);
    }


}