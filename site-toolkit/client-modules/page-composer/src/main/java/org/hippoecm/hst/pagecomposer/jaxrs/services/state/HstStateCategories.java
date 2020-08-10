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
package org.hippoecm.hst.pagecomposer.jaxrs.services.state;

import java.util.Objects;

import org.hippoecm.hst.pagecomposer.jaxrs.services.action.Category;

public final class HstStateCategories {

    private HstStateCategories() {
    }

    public static Category xpage() {
        return XPAGE;
    }

    public static Category workflowrequest() {
        return WORKFLOWREQUEST;
    }

    public static Category scheduledrequest() {
        return SCHEDULEDREQUEST;
    }

    private static final Category XPAGE = new HstStateCategory("xpage");
    private static final Category WORKFLOWREQUEST = new HstStateCategory("workflowRequest");
    private static final Category SCHEDULEDREQUEST = new HstStateCategory("scheduledRequest");

    static final class HstStateCategory implements Category {

        private final String name;

        private HstStateCategory(final String name) {
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
            final HstStateCategory that = (HstStateCategory) o;
            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }
}
