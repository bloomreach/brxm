/*
 * Copyright 2020 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.platform.provider;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.hippoecm.hst.core.parameters.ValueListProvider;

public class TestProvider implements ValueListProvider {

    List<String> values = Arrays.asList("test1", "test2", "test3");
    @Override
    public List<String> getValues() {
        return values;
    }

    @Override
    public String getDisplayValue(final String value) {
        return getDisplayValue(value, null);
    }

    @Override
    public String getDisplayValue(final String value, final Locale locale) {
        return value;
    }
}
