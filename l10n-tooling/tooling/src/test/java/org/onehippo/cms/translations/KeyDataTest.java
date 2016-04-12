/*
 *  Copyright 2016-2016 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms.translations;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.onehippo.cms.translations.KeyData.KeyStatus.ADDED;
import static org.onehippo.cms.translations.KeyData.KeyStatus.CLEAN;
import static org.onehippo.cms.translations.KeyData.LocaleStatus.RESOLVED;
import static org.onehippo.cms.translations.KeyData.LocaleStatus.UNRESOLVED;

public class KeyDataTest {

    @Test
    public void test_locales_are_nullified_and_status_is_cleared_when_last_locale_is_resolved() {
        KeyData keyData = new KeyData(ADDED);
        keyData.setLocaleStatus("de", UNRESOLVED);
        keyData.setLocaleStatus("fr", UNRESOLVED);

        keyData.setLocaleStatus("de", RESOLVED);
        assertNotNull(keyData.getLocales());
        assertEquals(ADDED, keyData.getStatus());

        keyData.setLocaleStatus("fr", RESOLVED);
        assertNull(keyData.getLocales());
        assertEquals(CLEAN, keyData.getStatus());
    }
}
