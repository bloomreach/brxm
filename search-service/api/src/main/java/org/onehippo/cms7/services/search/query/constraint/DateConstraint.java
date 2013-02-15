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

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public final class DateConstraint implements UpperBoundedDateConstraint, LowerBoundedDateConstraint {

    /**
     * The supported resolutions/granularities for <b>fast</b> date range queries and fast equals on dates
     * Note that EXACT resolution will be precise, but expensive
     */
     public enum Resolution {
        YEAR, MONTH, DAY, HOUR, EXACT;

        /**
         * @param resolution the name of the resolution, for example, year, Year,YEAR. if resolution is <code>null</code>,
         *            {@link Resolution#DAY} is returned.
         * @return Resolution for <code>name</code>. <code>name</code> is compared case-insensitive. If non matches,
         *         {@link Resolution#DAY} is returned
         */
        public static Resolution fromString(String resolution) {
            if (resolution == null) {
                return DAY;
            }
            resolution = resolution.toLowerCase();
            if (resolution.equals("year")) {
                return YEAR;
            }
            if (resolution.equals("month")) {
                return MONTH;
            }
            if (resolution.equals("day")) {
                return DAY;
            }
            if (resolution.equals("hour")) {
                return HOUR;
            }
            if (resolution.equals("exact")) {
                return EXACT;
            }
            return DAY;
        }
    }

    public enum Type {
        EQUAL, FROM, TO, BETWEEN
    }

    private final String property;
    private Calendar value;
    private Calendar upper;
    private Type type;
    private Resolution resolution;

    /**
     * Creates a DateConstraint with default {@link Resolution#DAY} if <code>type</code> is NOT
     * equal to {@link Type#EQUAL}. If <code>type</code> is {@link Type#EQUAL}, then {@link Resolution#EXACT}
     * is used as that default to an exact equals for a date.
     */
    public DateConstraint(final String property, final Calendar value, final Type type) {
        this(property, value, type, (type == Type.EQUAL) ? Resolution.EXACT : Resolution.DAY);
    }

    public DateConstraint(final String property, final Calendar value, final Type type, final Resolution resolution) {
        this.property = property;
        this.value = value;
        this.type = type;
        this.resolution = resolution;
    }

    @Override
    public Constraint andTo(final Date date) {
        if (type != Type.FROM) {
            throw new IllegalStateException();
        }
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        upper = cal;
        type = Type.BETWEEN;
        return this;
    }

    @Override
    public Constraint andFrom(final Date date) {
        if (type != Type.TO) {
            throw new IllegalStateException();
        }
        upper = value;

        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        value = cal;
        type = Type.BETWEEN;
        return this;
    }

    public String getProperty() {
        return property;
    }

    public Type getType() {
        return type;
    }

    public Calendar getUpper() {
        return upper;
    }

    public Calendar getValue() {
        return value;
    }

    public Resolution getResolution() {
        if (resolution == null) {
            throw new IllegalStateException("Resolution is not allowed to be null");
        }
        return resolution;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        DateFormat format = DateFormat.getDateInstance();
        sb.append("(date ");
        sb.append(property);
        switch (type) {
            case TO:
                sb.append(" <= ");
                sb.append(format.format(value.getTime()));
                break;
            case FROM:
                sb.append(" >= ");
                sb.append(format.format(value.getTime()));
                break;
            case BETWEEN:
                sb.append(" in [");
                sb.append(format.format(value.getTime()));
                sb.append(", ");
                sb.append(format.format(upper.getTime()));
                sb.append("]");
                break;
            case EQUAL:
                sb.append(" = ");
                sb.append(format.format(value.getTime()));
                break;
        }
        sb.append(", resolution = ").append(resolution.toString());
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof DateConstraint) {
            DateConstraint other = (DateConstraint) obj;
            if (other.type == type && other.value == value && property.equals(other.property)) {
                return type != Type.BETWEEN || other.upper == upper;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return type.hashCode() << 3 ^ value.hashCode() << 2 ^ property.hashCode() ^ (type == Type.BETWEEN ? upper.hashCode() << 4 : 0);
    }
}
