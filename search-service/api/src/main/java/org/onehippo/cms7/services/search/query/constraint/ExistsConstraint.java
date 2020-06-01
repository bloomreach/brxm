/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
 */
package org.onehippo.cms7.services.search.query.constraint;

public class ExistsConstraint implements Constraint {

    private final String property;

    public ExistsConstraint(String property) {
        this.property = property;
    }

    public String getProperty() {
        return this.property;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExistsConstraint)) {
            return false;
        }

        final ExistsConstraint that = (ExistsConstraint) o;
        return property.equals(that.property);
    }

    @Override
    public int hashCode() {
        return property.hashCode() ^ 137;
    }
}
