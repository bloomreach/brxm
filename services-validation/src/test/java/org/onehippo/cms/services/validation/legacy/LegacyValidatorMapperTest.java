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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

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
        
        assertTrue(mappedSet.contains("required"));
        assertEquals(mappedSet.size(), 1);
    }

    @Test
    public void testResourceRequired() {
        final Set<String> mappedSet = LegacyValidatorMapper.legacyMapper(Collections.singleton("resource-required"), null);
        
        assertTrue(mappedSet.contains("required"));
        assertEquals(mappedSet.size(), 1);
    }

    @Test
    public void testNonEmptyHtml() {
        final Set<String> mappedSet = LegacyValidatorMapper.legacyMapper(Collections.singleton("non-empty"), "Html");

        assertTrue(mappedSet.contains("non-empty-html"));
        assertEquals(mappedSet.size(), 1);
    }
    
    @Test
    public void testNonEmptyOther() {
        final Set<String> mappedSet = LegacyValidatorMapper.legacyMapper(Collections.singleton("non-empty"), "Othertype");

        assertTrue(mappedSet.contains("non-empty"));
        assertEquals(mappedSet.size(), 1);
    }
}
