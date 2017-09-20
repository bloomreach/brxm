/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.parameters;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.hippoecm.hst.core.component.HstParameterValueConversionException;
import org.hippoecm.hst.core.component.HstParameterValueConverter;

/**
 * Default implementation of {@link HstParameterValueConverter} by using <code>ConvertUtils</code> of commons-beanutils.
 */
public class DefaultHstParameterValueConverter implements HstParameterValueConverter {

    /**
     * ISO8601 formatter for date-time without time zone.
     * The format used is <tt>yyyy-MM-dd'T'HH:mm:ss</tt>.
     */
    public static final String ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    /**
     * ISO8601 formatter for date without time zone.
     * The format used is <tt>yyyy-MM-dd</tt>.
     */
    public static final String ISO_DATE_FORMAT = "yyyy-MM-dd";

    /**
     * ISO8601 formatter for time without time zone.
     * The format used is <tt>'T'HH:mm:ss</tt>.
     */
    public static final String ISO_TIME_FORMAT = "'T'HH:mm:ss";

    /**
     * All supported date string formats.
     */
    private static final String [] ISO8601_DATETIME_PATTERNS = {
        ISO_DATETIME_FORMAT,
        ISO_DATE_FORMAT,
        ISO_TIME_FORMAT,
    };

    @Override
    public Object convert(String parameterValue, Class<?> returnType) throws HstParameterValueConversionException {
        try {
            if (returnType == Date.class) {
                if (StringUtils.isBlank(parameterValue)) {
                    return null;
                }
                return DateUtils.parseDate(parameterValue, ISO8601_DATETIME_PATTERNS);
            } else if (returnType == Calendar.class) {
                if (StringUtils.isBlank(parameterValue)) {
                    return null;
                }
                final Date date = DateUtils.parseDate(parameterValue, ISO8601_DATETIME_PATTERNS);
                final Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                return cal;
            }

            return ConvertUtils.convert(parameterValue, returnType);
        } catch (ParseException e) {
            throw new HstParameterValueConversionException(e);
        }
    }

}
