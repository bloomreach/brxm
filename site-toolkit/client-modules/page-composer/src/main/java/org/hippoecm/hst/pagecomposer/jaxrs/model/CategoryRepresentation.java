/*
 * Copyright 2020-2023 Bloomreach
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

import org.hippoecm.hst.pagecomposer.jaxrs.services.component.Action;

import static java.util.stream.Collectors.toMap;

public final class CategoryRepresentation {

    private Map<String, ActionRepresentation> items;

    public static CategoryRepresentation represent(final Set<Action> actions) {
        final CategoryRepresentation representation = new CategoryRepresentation();
        representation.setItems(actions.stream().collect(toMap(Action::getName, ActionRepresentation::represent)));
        return representation;
    }

    public Map<String, ActionRepresentation> getItems() {
        return items;
    }

    public void setItems(final Map<String, ActionRepresentation> items) {
        this.items = items;
    }

}
