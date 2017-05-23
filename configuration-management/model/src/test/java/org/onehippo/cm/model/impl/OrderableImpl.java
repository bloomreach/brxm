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

package org.onehippo.cm.impl.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.onehippo.cm.api.model.Orderable;

/**
 * Dummy implementation of {@link Orderable} for testing the sorting of orderables.
 */
public class OrderableImpl implements Orderable {
    private final String name;
    private final Set<String> after;

    public OrderableImpl(final String name) {
        this.name = name;
        after = Collections.emptySet();
    }

    public OrderableImpl(final String name, final String afterCsv) {
        this.name = name;
        after = new LinkedHashSet<>(Arrays.asList(afterCsv.split("\\s*,\\s*")));
    }

    public String getName() {
        return name;
    }

    public Set<String> getAfter() {
        return after;
    }
}
