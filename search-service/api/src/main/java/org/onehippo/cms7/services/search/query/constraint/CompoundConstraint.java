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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class CompoundConstraint implements OrConstraint, AndConstraint {

    public enum Type {
        OR, AND
    }

    private Type type;
    private final List<Constraint> constraints = new ArrayList<Constraint>();

    public CompoundConstraint(final Constraint constraint, Type type) {
        this.constraints.add(constraint);
        this.type = type;
    }

    @Override
    public OrConstraint or(final Constraint constraint) {
        if (type != Type.OR) {
            throw new IllegalStateException();
        }
        constraints.add(constraint);
        return this;
    }

    @Override
    public AndConstraint and(final Constraint constraint) {
        if (type != Type.AND) {
            throw new IllegalStateException();
        }
        constraints.add(constraint);
        return this;
    }

    public List<Constraint> getConstraints() {
        return new ArrayList<Constraint>(constraints);
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CompoundConstraint)) {
            return false;
        }

        final CompoundConstraint that = (CompoundConstraint) o;
        return constraints.equals(that.constraints) && type == that.type;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + constraints.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "(" + join(constraints.iterator(), (type == Type.AND ? " and " : " or ")) + ")";
    }

    public static String join(Iterator<Constraint> iterator, String separator) {

        // handle null, zero and one elements before building a buffer
        if (iterator == null) {
            return null;
        }
        if (!iterator.hasNext()) {
            return "";
        }
        Constraint first = iterator.next();
        if (!iterator.hasNext()) {
            return first.toString();
        }

        // two or more elements
        StringBuilder buf = new StringBuilder(256); // Java default is 16, probably too small
        if (first != null) {
            buf.append(first.toString());
        }

        while (iterator.hasNext()) {
            if (separator != null) {
                buf.append(separator);
            }
            Constraint obj = iterator.next();
            if (obj != null) {
                buf.append(obj.toString());
            }
        }
        return buf.toString();
    }
}
