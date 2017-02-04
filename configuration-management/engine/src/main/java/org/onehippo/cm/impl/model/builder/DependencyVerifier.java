/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.impl.model.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.onehippo.cm.api.model.Orderable;
import org.onehippo.cm.impl.model.builder.exceptions.CircularDependencyException;
import org.onehippo.cm.impl.model.builder.exceptions.MissingDependencyException;


class DependencyVerifier {

    void verify(final Collection<? extends Orderable> orderableList) {
        Map<String, Orderable> objectMap = new HashMap<>();
        for (Orderable orderable : orderableList) {
            objectMap.put(orderable.getName(), orderable);
        }
        for (Orderable orderable : orderableList) {
            if (orderable.getAfter().isEmpty()) {
                continue;
            }
            Set<Orderable> checked = new HashSet<>();
            List<Orderable> trail = new ArrayList<>();
            trail.add(orderable);
            recurse(objectMap, orderable, orderable, checked, trail);
        }
    }

    private void recurse(final Map<String, ? extends Orderable> configurationMap,
                         final Orderable investigate,
                         final Orderable current,
                         final Set<Orderable> checked,
                         final List<Orderable> trail) {
        if (checked.contains(current)) {
            return;
        }
        checked.add(current);
        for (String dependsOn : current.getAfter()) {
            Orderable dependsOnOrderable = configurationMap.get(dependsOn);
            trail.add(dependsOnOrderable);
            if (dependsOnOrderable == null) {
                throw new MissingDependencyException(String.format("%s '%s' has missing dependency '%s'",
                        current.getClass().getSimpleName(), current.getName(), dependsOn));
            }
            if (dependsOnOrderable == investigate) {
                final String circle = trail.stream().map(Orderable::getName).collect(Collectors.joining(" -> "));
                throw new CircularDependencyException(String.format("%s '%s' has circular dependency: [%s].",
                        dependsOnOrderable.getClass().getSimpleName(), investigate.getName(), circle));
            }
            if (dependsOnOrderable.getAfter().isEmpty()) {
                continue;
            }
            recurse(configurationMap, investigate, dependsOnOrderable, checked, trail);
        }
    }
}
