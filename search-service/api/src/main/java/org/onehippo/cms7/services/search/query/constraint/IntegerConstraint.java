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

public final class IntegerConstraint implements LowerBoundedIntegerConstraint, UpperBoundedIntegerConstraint {

    public enum Type {
        EQUAL, TO, FROM, BETWEEN
    }

    private final String property;
    private int value;
    private int upper;
    private Type type;

    public IntegerConstraint(final String property, final int value, final Type type) {
        this.property = property;
        this.value = value;
        this.type = type;
    }

    public IntegerConstraint(final String property, final int before, final int after) {
        this.property = property;
        this.value = before;
        this.upper = after;
        this.type = Type.BETWEEN;
    }

    @Override
    public Constraint andTo(final int upper) {
        if (this.type != Type.FROM) {
            throw new IllegalStateException();
        }

        this.upper = upper;
        this.type = Type.BETWEEN;
        return this;
    }

    @Override
    public Constraint andFrom(final int lower) {
        if (this.type != Type.TO) {
            throw new IllegalStateException();
        }
        this.upper = value;

        this.value = lower;
        this.type = Type.BETWEEN;
        return this;
    }

    public String getProperty() {
        return property;
    }

    public Type getType() {
        return type;
    }

    public int getUpper() {
        return upper;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(int ");
        sb.append(property);
        switch (type) {
            case TO:
                sb.append(" <= ");
                sb.append(value);
                break;
            case FROM:
                sb.append(" >= ");
                sb.append(value);
                break;
            case BETWEEN:
                sb.append(" in [");
                sb.append(value);
                sb.append(", ");
                sb.append(upper);
                sb.append("]");
                break;
            case EQUAL:
                sb.append(" = ");
                sb.append(value);
                break;
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof IntegerConstraint) {
            IntegerConstraint other = (IntegerConstraint) obj;
            if (other.type == type && other.value == value && property.equals(other.property)) {
                return type != Type.BETWEEN || other.upper == upper;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return type.hashCode() << 3 ^ value << 2 ^ property.hashCode() ^ (type == Type.BETWEEN ? upper << 4 : 0);
    }
}
