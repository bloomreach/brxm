/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.event;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class HippoEventTest {

    static class MyEvent extends HippoEvent<MyEvent> {

        public MyEvent(String application) {
            super(application);
        }

        public MyEvent error(String error) {
            return put("error", error);
        }
    }

    /**
     * Verify that a subclass of the HippoEvent class can be fluent as well.
     * I.e. after invoking a HippoEvent method, the return type should be the subtype,
     * allowing the user to continue in the fluent style.
     */
    @Test
    public void testSubclassIsFluentToo() {
        MyEvent error = new MyEvent("test").message("haha").error("oeps");
        assertEquals(error.get("error"), "oeps");
    }

}
