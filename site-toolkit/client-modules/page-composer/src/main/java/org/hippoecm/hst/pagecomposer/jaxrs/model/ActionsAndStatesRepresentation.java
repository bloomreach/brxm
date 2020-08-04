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
package org.hippoecm.hst.pagecomposer.jaxrs.model;

import java.util.Map;
import java.util.Set;

import org.hippoecm.hst.pagecomposer.jaxrs.services.action.Action;
import org.hippoecm.hst.pagecomposer.jaxrs.services.state.XPageState;

import static java.util.stream.Collectors.toMap;

public final class ActionsAndStatesRepresentation {

    private Map<String, CategoryRepresentation> actions;
    private StatesRepresentation states;

    public static ActionsAndStatesRepresentation represent(final Map<String, Set<Action>> actionsByCategory,
                                                           final XPageState xPageState) {
        final ActionsAndStatesRepresentation representation = new ActionsAndStatesRepresentation();
        representation.setActions(actionsByCategory.entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        e -> CategoryRepresentation.represent(e.getValue()))));
        representation.setStates(StatesRepresentation.represent(xPageState));
        return representation;
    }

    public Map<String, CategoryRepresentation> getActions() {
        return actions;
    }

    public void setActions(final Map<String, CategoryRepresentation> actions) {
        this.actions = actions;
    }

    public StatesRepresentation getStates() {
        return states;
    }

    public void setStates(final StatesRepresentation states) {
        this.states = states;
    }
}
