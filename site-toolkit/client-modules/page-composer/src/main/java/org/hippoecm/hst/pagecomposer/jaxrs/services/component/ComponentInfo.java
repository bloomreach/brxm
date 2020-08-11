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

public final class ComponentInfo {

    private final Set<Action> actions;
    private final Set<State> states;

    private static final ComponentInfo EMPTY = new ComponentInfo(Collections.emptySet(), Collections.emptySet());

    public ComponentInfo(final Set<Action> actions, final Set<State> states) {
        Objects.requireNonNull(actions);
        Objects.requireNonNull(states);
        this.actions = actions;
        this.states = states;
    }

    public static ComponentInfo empty() {
        return EMPTY;
    }

    static ComponentInfo merge(final ComponentInfo si1, final ComponentInfo si2) {
        final Set<Action> actions = new HashSet<>(si1.actions);
        actions.addAll(si2.getActions());
        final Set<State> states = new HashSet<>(si1.states);
        states.addAll(si2.getStates());
        return new ComponentInfo(actions, states);
    }

    public Set<Action> getActions() {
        return actions;
    }

    public Set<State> getStates() {
        return states;
    }

}
