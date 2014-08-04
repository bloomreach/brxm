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
package org.onehippo.repository.testutils.log4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Category;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class StringMatchFilterTest {

    @Test
    public void testStringMatchFilter() {
        final StringMatchFilter stringMatchFilter = new StringMatchFilter();
        assertEquals(Filter.DENY, stringMatchFilter.decide(new LoggingEvent(null, new Category("test") {}, null, "foobar", null)));
        assertEquals(Filter.NEUTRAL, stringMatchFilter.decide(new LoggingEvent(null, new Category("test") {}, null, "quzquux", null)));
        assertEquals(Filter.DENY, stringMatchFilter.decide(new LoggingEvent(null, new Category("test") {}, null, "bazbar", null)));
    }

}
