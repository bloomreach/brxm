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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateRangeQueryConstraints {

    private static final Logger log = LoggerFactory.getLogger(DateRangeQueryConstraints.class);

    final private String property;
    final private Calendar fromDate;
    final private Calendar toDate;
    final Filter.Resolution resolution;

    public DateRangeQueryConstraints(final String property, final Calendar fromDate, final Calendar toDate, final String resolutionString) {
        this.property = property;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.resolution = fromString(resolutionString);
    }

    private Filter.Resolution fromString(final String resolutionString) {
        if (resolutionString == null) {
            return null;
        }
        if (resolutionString.equals("year")) {
            return Filter.Resolution.YEAR;
        }
        if (resolutionString.equals("month")) {
            return Filter.Resolution.MONTH;
        }
        if (resolutionString.equals("day")) {
            return Filter.Resolution.DAY;
        }
        if (resolutionString.equals("hour")) {
            return Filter.Resolution.HOUR;
        }
        log.warn("Unknown resolution '{}'", resolutionString);
        return null;
    }

    public void addConstraintToFilter(final Filter filter) throws FilterException {
       if (resolution == null) {
           if (fromDate == null && toDate == null) {
               return;
           }
           if (fromDate == null) {
               filter.addLessOrEqualThan(property, toDate);
           } else if (toDate == null) {
               filter.addGreaterOrEqualThan(property, fromDate);
           } else {
               filter.addBetween(property, fromDate, toDate);
           }
       } else {
           if (fromDate == null && toDate == null) {
               return;
           }
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
