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
package org.hippoecm.hst.pagecomposer.jaxrs.services.action;

import java.util.Objects;

public final class HstCategories {

    private HstCategories() {
    }

    public static Category channel() {
        return CHANNEL;
    }

    public static Category page() {
        return PAGE;
    }

    public static Category xpage() {
        return XPAGE;
    }

    private static final Category CHANNEL = new HstCategory("channel");
    private static final Category PAGE = new HstCategory("page");
    private static final Category XPAGE = new HstCategory("xpage");

    static final class HstCategory implements Category {

        private final String name;

        private HstCategory(final String name) {
            Objects.requireNonNull(name);
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final HstCategory that = (HstCategory) o;
            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
