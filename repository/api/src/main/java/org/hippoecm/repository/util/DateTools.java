/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
/*    
 *    @See initial copy of {@link org.apache.lucene.document.DateTools}
 */
package org.hippoecm.repository.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

public class DateTools {

    private static final SimpleDateFormat YEAR_FORMAT = new SimpleDateFormat("yyyy");
    private static final SimpleDateFormat MONTH_FORMAT = new SimpleDateFormat("yyyyMM");
    private static final SimpleDateFormat WEEK_FORMAT = new SimpleDateFormat("yyyyww");
    private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("yyyyMMdd");
    private static final SimpleDateFormat HOUR_FORMAT = new SimpleDateFormat("yyyyMMddHH");
    private static final SimpleDateFormat MINUTE_FORMAT = new SimpleDateFormat("yyyyMMddHHmm");
    private static final SimpleDateFormat SECOND_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
    private static final SimpleDateFormat MILLISECOND_FORMAT = new SimpleDateFormat("yyyyMMddHHmmssSSS");
   
    // cannot create, the class has static methods only
    private DateTools() {}

    /**
     * Converts a Date to a string suitable for indexing.
     *
     * @param date       the date to be converted
     * @param resolution the desired resolution, see {@link #round(long, DateTools.Resolution)}
     * @return a string in format <code>yyyyMMddHHmmssSSS</code> or shorter, depeding on <code>resolution</code>; using
     *         UTC as timezone
     */
    public static String dateToString(Date date, Resolution resolution) {
        return timeToString(date.getTime(), resolution);
    }

    /**
     * Converts a millisecond time to a string suitable for indexing.
     *
     * @param time the date expressed as milliseconds since January 1, 1970, 00:00:00 GMT
     * @param resolution the desired resolution, see {@link #round(long, DateTools.Resolution)}
     * @return a string in format <code>yyyyMMddHHmmssSSS</code> or shorter,depending on <code>resolution</code>; using UTC as timezone
     */
    public static String timeToString(long time, Resolution resolution) {
        Calendar cal = Calendar.getInstance();

        cal.setTime(new Date(round(time, resolution)));

        
        String result;
        if (resolution == Resolution.YEAR) {
            synchronized (YEAR_FORMAT) {
                result = YEAR_FORMAT.format(cal.getTime());
            }
        } else if (resolution == Resolution.MONTH) {
            synchronized (MONTH_FORMAT) {
                result = MONTH_FORMAT.format(cal.getTime());
            }
        } else if (resolution == Resolution.WEEK) {
            synchronized (WEEK_FORMAT) {
                result = WEEK_FORMAT.format(cal.getTime());
            }
        } else if (resolution == Resolution.DAY) {
            synchronized (DAY_FORMAT) {
                result = DAY_FORMAT.format(cal.getTime());
            }
        } else if (resolution == Resolution.HOUR) {
            synchronized (HOUR_FORMAT) {
                result = HOUR_FORMAT.format(cal.getTime());
            }
        } else if (resolution == Resolution.MINUTE) {
            synchronized (MINUTE_FORMAT) {
                result = MINUTE_FORMAT.format(cal.getTime());
            }
        } else if (resolution == Resolution.SECOND) {
            synchronized (SECOND_FORMAT) {
                result = SECOND_FORMAT.format(cal.getTime());
            }
        } else if (resolution == Resolution.MILLISECOND) {
            synchronized (MILLISECOND_FORMAT) {
                result = MILLISECOND_FORMAT.format(cal.getTime());
            }
        } else {
            throw new IllegalArgumentException("unknown resolution " + resolution);
        }
        return result;
    }


    /**
     * Limit a date's resolution. For example, the date <code>1095767411000</code>
     * (which represents 2004-09-21 13:50:11) will be changed to
     * <code>1093989600000</code> (2004-09-01 00:00:00) when using
     * <code>Resolution.MONTH</code>.
     *
     * @param resolution The desired resolution of the date to be returned
     * @return the date with all values more precise than <code>resolution</code>
     *    set to 0 or 1, expressed as milliseconds since January 1, 1970, 00:00:00 GMT
     */
    public static long round(long time, Resolution resolution) {
        return roundDate(time, resolution).getTimeInMillis();
    }

    /**
     * Limit a date's resolution. For example, the date <code>1095767411000</code>
     * (which represents 2004-09-21 13:50:11) will be changed to 
     * <code>1093989600000</code> (2004-09-01 00:00:00) when using
     * <code>Resolution.MONTH</code>.
     *
     * @return the date with all values more precise than <code>resolution</code>
     *    set to 0 or 1, expressed as milliseconds since January 1, 1970, 00:00:00 GMT
     */
    public static Calendar roundDate(long time, Resolution resolution) {
        Calendar cal = Calendar.getInstance();

        cal.setTime(new Date(time));
        
        if (resolution == Resolution.YEAR) {
            cal.set(Calendar.MONTH, 0);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        } else if (resolution == Resolution.MONTH) {
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        } else if (resolution == Resolution.WEEK) {
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        } else if (resolution == Resolution.DAY) {
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        } else if (resolution == Resolution.HOUR) {
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        } else if (resolution == Resolution.MINUTE) {
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        } else if (resolution == Resolution.SECOND) {
            cal.set(Calendar.MILLISECOND, 0);
        } else if (resolution == Resolution.MILLISECOND) {
            // don't cut off anything
        } else {
            throw new IllegalArgumentException("unknown resolution " + resolution);
        }
        return cal;
    }

    public static Date stringToDate(String dateString, Resolution resolution) throws ParseException {
            Date date;
            if (dateString.length() == 4) {
                synchronized (YEAR_FORMAT) {
                    date = YEAR_FORMAT.parse(dateString);
                }
            } else if (dateString.length() == 6) {
                if(resolution == Resolution.WEEK) {
                    synchronized (WEEK_FORMAT) {
                        date = WEEK_FORMAT.parse(dateString);
                    }
                } else {
                    synchronized (MONTH_FORMAT) {
                        date = MONTH_FORMAT.parse(dateString);
                    }
                }
            } else if (dateString.length() == 8) {
                synchronized (DAY_FORMAT) {
                    date = DAY_FORMAT.parse(dateString);
                }
            } else if (dateString.length() == 10) {
                synchronized (HOUR_FORMAT) {
                    date = HOUR_FORMAT.parse(dateString);
                }
            } else if (dateString.length() == 12) {
                synchronized (MINUTE_FORMAT) {
                    date = MINUTE_FORMAT.parse(dateString);
                }
            } else if (dateString.length() == 14) {
                synchronized (SECOND_FORMAT) {
                    date = SECOND_FORMAT.parse(dateString);
                }
            } else if (dateString.length() == 17) {
                synchronized (MILLISECOND_FORMAT) {
                    date = MILLISECOND_FORMAT.parse(dateString);
                }
            } else {
                throw new ParseException("Input is not valid date string: " + dateString, 0);
            }
            return date;
        }

    public static String createXPathConstraint(final Session session,
                                               final Calendar calendar) {
        try {
            return "xs:dateTime('"+session.getValueFactory().createValue(calendar).getString()+ "')";
        } catch (RepositoryException e) {
            throw new IllegalArgumentException("RepositoryException while creating a calendar jcr Value " +
                    "for '"+calendar.toString()+"'", e);
        }
    }

    public static String createXPathConstraint(final Session session,
                                               final Calendar calendar,
                                               final Resolution roundDateBy) {
        final Calendar roundedCalendar = roundDate(calendar.getTimeInMillis(), roundDateBy);
        return createXPathConstraint(session, roundedCalendar);
    }

    public static String getPropertyForResolution(final String property, final Resolution resolution) {
        return property + "____" + resolution.resolution;

    }

    private static final List<Resolution> SUPPORTED_RESOLUTIONS = unmodifiableList(asList(
            DateTools.Resolution.YEAR,
            DateTools.Resolution.MONTH,
            DateTools.Resolution.DAY,
            DateTools.Resolution.HOUR));

    /**
     * @return An array copy of the supported resolutions
     * @deprecated Use {@link #getSupportedResolutions()} instead
     */
    @Deprecated
    public static Resolution[] getSupportedDateResolutions() {
        return SUPPORTED_RESOLUTIONS.toArray(new Resolution[SUPPORTED_RESOLUTIONS.size()]);
    }

    public static Iterable<Resolution> getSupportedResolutions() {
        return SUPPORTED_RESOLUTIONS;
    }

    /** Specifies the time granularity. */
    public static class Resolution {
        
        public static final Resolution YEAR = new Resolution("year", Calendar.YEAR);
        public static final Resolution MONTH = new Resolution("month", Calendar.MONTH);
        public static final Resolution WEEK = new Resolution("week", Calendar.WEEK_OF_YEAR);
        public static final Resolution DAY = new Resolution("day", Calendar.DAY_OF_MONTH);
        public static final Resolution HOUR = new Resolution("hour", Calendar.HOUR_OF_DAY);
        public static final Resolution MINUTE = new Resolution("minute", Calendar.MINUTE);
        public static final Resolution SECOND = new Resolution("second", Calendar.SECOND);
        public static final Resolution MILLISECOND = new Resolution("millisecond", Calendar.MILLISECOND);

        public static final Map<String, Resolution> RESOLUTIONSMAP = new HashMap<String, Resolution>();
        static {
                RESOLUTIONSMAP.put("year", YEAR);
                RESOLUTIONSMAP.put("month", MONTH);
                RESOLUTIONSMAP.put("week", WEEK);
                RESOLUTIONSMAP.put("day", DAY);
                RESOLUTIONSMAP.put("hour", HOUR);
                RESOLUTIONSMAP.put("minute", MINUTE);
                RESOLUTIONSMAP.put("second", SECOND);
                RESOLUTIONSMAP.put("millisecond", MILLISECOND);
        }
        
        private String resolution;
        private int calendarField;

        private Resolution(String resolution, int calendarField) {
            this.resolution = resolution;
            this.calendarField = calendarField;
        }
        
        public String toString() {
            return resolution;
        }
        
        public int getCalendarField(){
                return this.calendarField;
        }

    }
}
