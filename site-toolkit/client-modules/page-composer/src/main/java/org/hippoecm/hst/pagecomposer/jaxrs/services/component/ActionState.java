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

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class ActionState {

    private final Set<Action> actions;
    private final Set<State> states;

    private static final ActionState EMPTY = new ActionState(Collections.emptySet(), Collections.emptySet());

    public ActionState(final Set<Action> actions, final Set<State> states) {
        Objects.requireNonNull(actions);
        Objects.requireNonNull(states);
        this.actions = actions;
        this.states = states;
    }

    public static ActionState empty() {
        return EMPTY;
    }

    static ActionState merge(final ActionState si1, final ActionState si2) {
        final Set<Action> actions = new HashSet<>(si1.actions);
        si2.actions.forEach(a2 -> {
            if (actions.contains(a2)) {
                actions.remove(a2);
                if (a2.isEnabled() != null) {
                    actions.add(a2);
                }
            }
        });
        final Set<State> states = new HashSet<>(si1.states);
        si2.states.forEach(s2 -> {
            if (states.contains(s2)) {
                states.remove(s2);
                if (s2.getValue() != null) {
                    states.add(s2);
                }
            }
        });
        return new ActionState(actions, states);
    }

    public Set<Action> getActions() {
        return actions;
    }

    public Set<State> getStates() {
        return states;
    }

}
