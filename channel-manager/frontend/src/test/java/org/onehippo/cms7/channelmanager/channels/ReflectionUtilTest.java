/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.channels;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests {@link ReflectionUtil}.
 */
public class ReflectionUtilTest {

    @Test
    public void stringValue() throws URISyntaxException {
        assertEquals("www.example.com", ReflectionUtil.getStringValue(new URI("http://www.example.com"), "host"));
        assertEquals("true", ReflectionUtil.getStringValue(new URI("http://www.example.com"), "absolute"));
    }

    @Test
    public void nonExistingGetters() throws URISyntaxException {
        assertNull(ReflectionUtil.getStringValue(new URI("http://www.example.com"), "nosuchgetter"));
    }

    @Test
    public void gettersDoNotHaveArguments() {
        assertNull(ReflectionUtil.getStringValue(Calendar.getInstance(), "greatestMinimum"));
        assertNull(ReflectionUtil.getStringValue(Calendar.getInstance(), "set"));
    }

    @Test
    public void nullValueReturnsEmptyString() throws URISyntaxException {
        assertEquals("", ReflectionUtil.getStringValue(new URI("/foo/bar"), "host"));
    }

}
