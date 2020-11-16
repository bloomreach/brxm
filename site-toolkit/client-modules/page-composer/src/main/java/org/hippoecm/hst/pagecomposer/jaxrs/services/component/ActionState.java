/*
 * Copyright 2020 Bloomreach
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
 *
 */
package org.hippoecm.hst.pagecomposer.jaxrs.services.component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.emptyMap;

public final class ActionState {

    private final Map<NamedCategory, Boolean> actions;
    private final Map<NamedCategory, Object> states;

    private static final ActionState EMPTY = new ActionState(emptyMap(), emptyMap());

    public ActionState(final Map<NamedCategory, Boolean> actions, final Map<NamedCategory, Object> states) {
        Objects.requireNonNull(actions);
        Objects.requireNonNull(states);
        this.actions = actions;
        this.states = states;
    }

    public static ActionState empty() {
        return EMPTY;
    }

    static ActionState merge(final ActionState as1, final ActionState as2) {
        final Map<NamedCategory, Boolean> actions = mergeMaps(as1.actions, as2.actions);
        final Map<NamedCategory, Object> states = mergeMaps(as1.states, as2.states);
        return new ActionState(actions, states);
    }

    public Map<NamedCategory, Boolean> getActions() {
        return actions;
    }

    public Map<NamedCategory, Object> getStates() {
        return states;
    }

    private static  <T> Map<NamedCategory, T> mergeMaps(final Map<NamedCategory, T> m1, final Map<NamedCategory, T> m2) {
        final Map<NamedCategory, T> merged = new HashMap<>(m1);
        m2.forEach( (s, v) -> {
            if (merged.containsKey(s)) {
                merged.remove(s);
                if (v != null) {
                    merged.put(s, v);
                }
            }
        });
        return merged;
    }
}
