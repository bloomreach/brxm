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
package org.onehippo.cms.services.validation.legacy;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

public class LegacyValidatorMapperTest {
    
    @Test
    public void testEmptyInputs() {
        assertNull(LegacyValidatorMapper.legacyMapper((Set<String>) null, null));
        assertNull(LegacyValidatorMapper.legacyMapper((Set<String>) null, ""));

        assertEquals(LegacyValidatorMapper.legacyMapper(Collections.emptySet(), null).size(), 0);

        assertNull(LegacyValidatorMapper.legacyMapper((List<String>) null, null));
        assertNull(LegacyValidatorMapper.legacyMapper((List<String>) null, ""));

        assertEquals(LegacyValidatorMapper.legacyMapper(Collections.emptyList(), null).size(), 0);
    }

    @Test
    public void testRequiredNonEmpty() {
        Set<String> set = new LinkedHashSet<>();
        set.add("required");
        set.add("non-empty");
        final Set<String> mappedSet = LegacyValidatorMapper.legacyMapper(set, null);

        assertEquals(Collections.singleton("required"), mappedSet);
    }

    @Test
    public void testResourceRequired() {
        final Set<String> mappedSet = LegacyValidatorMapper.legacyMapper(Collections.singleton("resource-required"), null);

        assertEquals(Collections.singleton("required"), mappedSet);
    }

    @Test
    public void testNonEmptyHtml() {
        final Set<String> mappedSet = LegacyValidatorMapper.legacyMapper(Collections.singleton("non-empty"), "Html");

        assertEquals(Collections.singleton("non-empty-html"), mappedSet);
    }
    
    @Test
    public void testNonEmptyOther() {
        final Set<String> mappedSet = LegacyValidatorMapper.legacyMapper(Collections.singleton("non-empty"), "Othertype");

        assertEquals(Collections.singleton("non-empty"), mappedSet);
    }

    @Test
    public void testHtml() {
        final Set<String> mappedSet = LegacyValidatorMapper.legacyMapper(Collections.singleton("html"), "Sometype");

        assertEquals(Collections.singleton("non-empty-html"), mappedSet);
    }

    @Test
    public void orderDoesNotChange() {
        final List<String> original = Arrays.asList("html", "custom1", "custom2");
        final List<String> expected = Arrays.asList("non-empty-html", "custom1", "custom2");

        final List<String> actual = LegacyValidatorMapper.legacyMapper(original, "String");

        assertEquals(expected, actual);
    }
}
