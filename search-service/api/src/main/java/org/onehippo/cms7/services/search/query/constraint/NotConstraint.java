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

public class NotConstraint implements Constraint {

    private final Constraint constraint;

    public NotConstraint(final Constraint constraint) {
        this.constraint = constraint;
    }

    public Constraint getConstraint() {
        return constraint;
    }

    @Override
    public String toString() {
        return "(not " + constraint + ")";
    }

    @Override
    public int hashCode() {
        return constraint.hashCode() ^ 0x1;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof NotConstraint) {
            return ((NotConstraint) obj).getConstraint().equals(constraint);
        }
        return false;
    }
}
