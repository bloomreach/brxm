/*
 *  Copyright 2012-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.testutils.log4j;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class StringMatchFilterTest {

    private static LogEvent buildEvent(String logger, String message) {
        return new Log4jLogEvent.Builder().setLoggerName(logger).setMessage(new SimpleMessage(message)).build();
    }

    @Test
    public void testStringMatchFilter() {
        final StringMatchFilter stringMatchFilter = new StringMatchFilter();
        assertEquals(Filter.Result.DENY, stringMatchFilter.filter(buildEvent("test","foobar")));
        assertEquals(Filter.Result.NEUTRAL, stringMatchFilter.filter(buildEvent("test","quzquux")));
        assertEquals(Filter.Result.DENY, stringMatchFilter.filter(buildEvent("test","bazbar")));
    }

}
