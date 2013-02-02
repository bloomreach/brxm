/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.demo.util;

import java.util.Calendar;
import java.util.Date;

import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.hst.content.beans.query.filter.Filter;

public class DateRangeQueryConstraints {

    final private String property;
    final private Calendar fromDate;
    final private Calendar toDate;
    final Filter.Resolution resolution;

    public DateRangeQueryConstraints(final String property, final Calendar fromDate, final Calendar toDate, final Filter.Resolution resolution) {
        this.property = property;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.resolution = resolution;
    }

    public void addConstraintToFilter(final Filter filter) throws FilterException {
       if (resolution == null) {
           if (fromDate == null) {
               filter.addLessOrEqualThan(property, toDate);
           } else if (toDate == null) {
               filter.addGreaterOrEqualThan(property, fromDate);
           } else {
               filter.addBetween(property, fromDate, toDate);
           }
       } else {
           if (fromDate == null) {
               filter.addLessOrEqualThan(property, toDate, resolution);
           } else if (toDate == null) {
               filter.addGreaterOrEqualThan(property, fromDate, resolution);
           } else {
               filter.addBetween(property, fromDate, toDate, resolution);
           }
       }
    }
}
