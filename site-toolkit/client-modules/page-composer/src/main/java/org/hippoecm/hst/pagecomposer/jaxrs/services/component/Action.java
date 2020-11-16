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

import java.util.Objects;
import java.util.StringJoiner;

public final class Action {

    private final String name;
    private final String category;
    private final boolean enabled;

    public Action(final String name, final String category, final boolean enabled) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(category);
        this.name = name;
        this.category = category;
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Action.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("category='" + category + "'")
                .add("enabled=" + enabled)
                .toString();
    }

}
