/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.admin.comparators;

import java.text.CollationKey;
import java.text.Collator;
import java.util.Comparator;

public class PropertyComparator implements Comparator<String> {
    private final Collator collator;

    public PropertyComparator(Collator collator) {
        this.collator = collator;
    }

    @Override
    public int compare(final String property1, final String property2) {
        final CollationKey key1 = collator.getCollationKey(property1);
        final CollationKey key2 = collator.getCollationKey(property2);
        return key1.compareTo(key2);
    }
}
