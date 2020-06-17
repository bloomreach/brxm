/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

import static java.util.stream.Collectors.toMap;

public final class ActionsRepresentation {

    private Map<String, CategoryRepresentation> actions;

    public static ActionsRepresentation represent(Map<String, Set<Action>> actionsByCategory) {
        final ActionsRepresentation representation = new ActionsRepresentation();
        representation.setActions(actionsByCategory.entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        e -> CategoryRepresentation.represent(e.getValue()))));
        return representation;
    }

    public Map<String, CategoryRepresentation> getActions() {
        return actions;
    }

    public void setActions(final Map<String, CategoryRepresentation> actions) {
        this.actions = actions;
    }
}
