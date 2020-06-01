/*
 * Copyright 2014-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.webfiles.watch;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.webfiles.watch.OsNameMatcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OsNameMatcherTest {

    private OsNameMatcher matcher;

    @Before
    public void setUp() {
        matcher = new OsNameMatcher();
    }

    @Test
    public void explicitNameMatches() {
        matcher.setCurrentOsName("Linux");
        matcher.include("Linux");
        assertTrue(matcher.matchesCurrentOs());
    }

    @Test
    public void explicitNameDoesNotMatch() {
        matcher.setCurrentOsName("Windows");
        matcher.include("Linux");
        assertFalse(matcher.matchesCurrentOs());
    }

    @Test
    public void explicitMatchOfCurrentOs() {
        String currentOsName = System.getProperty("os.name");
        matcher.include(currentOsName);
        assertTrue(matcher.matchesCurrentOs());
    }

    @Test
    public void wildcardNameMatches() {
        matcher.setCurrentOsName("Windows 10");
        matcher.include("Windows*");
        assertTrue(matcher.matchesCurrentOs());
    }

    @Test
    public void wildcardNameDoesNotMatch() {
        matcher.setCurrentOsName("Linux");
        matcher.include("Windows*");
        assertFalse(matcher.matchesCurrentOs());
    }

    @Test
    public void wildcardCharactersMatch() {
        matcher.setCurrentOsName("Windows 7");
        matcher.include("Windows ?");
        assertTrue(matcher.matchesCurrentOs());
    }

    @Test
    public void wildcardCharactersDoNotMatch() {
        matcher.setCurrentOsName("Windows 95");
        matcher.include("Windows ?");
        assertFalse(matcher.matchesCurrentOs());
    }

}