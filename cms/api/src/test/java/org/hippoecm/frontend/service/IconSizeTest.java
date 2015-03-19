/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class IconSizeTest {

    @Test
    public void get_by_name() {
        assertSymbolicNameMatch("s", IconSize.S);
        assertSymbolicNameMatch("m", IconSize.M);
        assertSymbolicNameMatch("l", IconSize.L);
        assertSymbolicNameMatch("xl", IconSize.XL);
    }

    private void assertSymbolicNameMatch(final String symbolicName, final IconSize size) {
        assertEquals(size, IconSize.getIconSize(symbolicName.toLowerCase()));
        assertEquals(size, IconSize.getIconSize(symbolicName.toUpperCase()));
    }

    @Test
    public void get_by_size_returns_same_or_larger_icon() {
        assertEquals(IconSize.S, IconSize.getIconSize("0"));
        assertEquals(IconSize.S, IconSize.getIconSize("1"));
        assertEquals(IconSize.S, IconSize.getIconSize("8"));

        assertEquals(IconSize.M, IconSize.getIconSize("9"));
        assertEquals(IconSize.M, IconSize.getIconSize("15"));
        assertEquals(IconSize.M, IconSize.getIconSize("16"));

        assertEquals(IconSize.L, IconSize.getIconSize("17"));
        assertEquals(IconSize.L, IconSize.getIconSize("20"));
        assertEquals(IconSize.L, IconSize.getIconSize("32"));

        assertEquals(IconSize.XL, IconSize.getIconSize("33"));
        assertEquals(IconSize.XL, IconSize.getIconSize("47"));
        assertEquals(IconSize.XL, IconSize.getIconSize("48"));

        assertEquals(IconSize.XL, IconSize.getIconSize("200"));
    }

}
