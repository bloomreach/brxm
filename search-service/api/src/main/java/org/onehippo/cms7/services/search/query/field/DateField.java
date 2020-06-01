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
package org.onehippo.cms7.services.search.query.field;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.onehippo.cms7.services.search.query.constraint.Constraint;
import org.onehippo.cms7.services.search.query.constraint.DateConstraint;
import org.onehippo.cms7.services.search.query.constraint.LowerBoundedDateConstraint;
import org.onehippo.cms7.services.search.query.constraint.UpperBoundedDateConstraint;

public final class DateField extends Field {

    public DateField(final String property) {
        super(property);
    }

    public Constraint isEqualTo(final Date value) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(value);
        return new DateConstraint(getProperty(), cal, DateConstraint.Type.EQUAL);
    }

    /**
     * Creates an equal based on a {@link DateConstraint.Resolution} : For example, if the resolution
     * {@link DateConstraint.Resolution#DAY} is used, then this results in a constraint for all results
     * having the <strong>same</strong> Date rounded to Day as <code>value</code> has.
     */
    public Constraint isEqualTo(final Date value, final DateConstraint.Resolution resolution) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(value);
        return new DateConstraint(getProperty(), cal, DateConstraint.Type.EQUAL, resolution);
    }

    /**
     * Creates a range query with default {@link DateConstraint.Resolution} set to {@link DateConstraint.Resolution#DAY}
     */
    public LowerBoundedDateConstraint from(final Date date) {
        return from(date, DateConstraint.Resolution.DAY);
    }

    public LowerBoundedDateConstraint from(final Date date, final DateConstraint.Resolution resolution) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        return new DateConstraint(getProperty(), cal, DateConstraint.Type.FROM, resolution);
    }

    /**
     * Creates a range query with default {@link DateConstraint.Resolution} set to {@link DateConstraint.Resolution#DAY}
     */
    public UpperBoundedDateConstraint to(final Date date) {
        return to(date, DateConstraint.Resolution.DAY);
    }

    public UpperBoundedDateConstraint to(final Date date, final DateConstraint.Resolution resolution) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        return new DateConstraint(getProperty(), cal, DateConstraint.Type.TO, resolution);
    }
}
