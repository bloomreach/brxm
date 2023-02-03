/*
 * Copyright 2015-2023 Bloomreach
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

package org.hippoecm.frontend.usagestatistics;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UsageEventTest {

    @Test
    public void eventWithoutParameters() {
        UsageEvent event = new UsageEvent("test");
        assertEquals("Hippo.Events.publish('test');", event.getJavaScript());
    }

    @Test
    public void eventWithOneParameter() {
        UsageEvent event = new UsageEvent("test");
        event.setParameter("a", "valueA");
        assertEquals("Hippo.Events.publish('test',{\"a\":\"valueA\"});", event.getJavaScript());
    }

    @Test
    public void eventWithTwoParameters() {
        UsageEvent event = new UsageEvent("test");
        event.setParameter("a", "valueA");
        event.setParameter("b", "valueB");
        assertEquals("Hippo.Events.publish('test',{\"a\":\"valueA\",\"b\":\"valueB\"});", event.getJavaScript());
    }

}