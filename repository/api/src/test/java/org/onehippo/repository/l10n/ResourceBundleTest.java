/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.repository.l10n;

import java.util.Collections;
import java.util.Locale;

import org.junit.Test;

import static org.junit.Assert.*;

public class ResourceBundleTest {

    @Test
    public void interfaceIsBackwardsCompatible() {
        final ResourceBundle resourceBundle = new ResourceBundle() {

            @Override
            public Locale getLocale() {
                return null;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public String getString(final String key) {
                return null;
            }
        };

        assertNull(resourceBundle.getString("key", Collections.emptyMap()));
        assertNull(resourceBundle.getString("key", "parameterName", "parameterValue"));
        assertNull(resourceBundle.toJavaResourceBundle());
    }

}