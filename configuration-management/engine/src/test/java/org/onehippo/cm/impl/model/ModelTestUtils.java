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

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.onehippo.cm.api.model.Orderable;
import org.onehippo.cm.api.model.Source;

public class ModelTestUtils {

    public static <T extends Orderable> T findByName(final String name, final Collection<T> entries) {
        return findInCollection(name, entries, Orderable::getName);
    }

    public static <T extends Source> T findByPath(final String name, final Collection<T> entries) {
        return findInCollection(name, entries, Source::getPath);
    }

    public static <T extends Source> T findByPath(final String name, final Set<T> entries) {
        return findInSet(name, entries, Source::getPath);
    }

    private static <T> T findInCollection(final String id, final Collection<T> entries, Function<T, String> identifier) {
        return entries.stream().collect(Collectors.toMap(identifier, t -> t)).get(id);
    }

    private static <T> T findInSet(final String id, final Set<T> entries, Function<T, String> identifier) {
        return entries.stream().collect(Collectors.toMap(identifier, t -> t)).get(id);
    }
}
