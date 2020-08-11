/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.util;

import java.util.Collections;
import java.util.Date;
import java.util.Calendar;
import java.util.Map;
import java.util.HashMap;
import java.text.ParseException;
import java.util.regex.Pattern;

/**
 * A Simple Utility class for parsing "math" like strings relating to Calendar Dates.
 *
 * <p>
 *   Largely based upon and adapted from the Apache Solr DateMathParser at:
 *   http://svn.apache.org/repos/asf/lucene/dev/trunk/solr/core/src/java/org/apache/solr/util/DateMathParser.java
 * </p>
 *
 * <p>
 * The basic syntax support addition, subtraction and rounding at various
 * levels of granularity (or "units").  Commands can be chained together
 * and are parsed from left to right.  '+' and '-' denote addition and
 * subtraction, while '/' denotes "round".  Round requires only a unit, while
 * addition/subtraction require an integer value and a unit.
 * Command strings must not include white space, but the "No-Op" command
 * (empty string) is allowed....
 * </p>
 *
 * <pre>
 *   /H
 *      ... Round to the start of the current hour
 *   /D
 *      ... Round to the start of the current day
 *   +2Y
 *      ... Exactly two years in the future from now
 *   -1D
 *      ... Exactly 1 day prior to now
 *   /D+6M+3D
 *      ... 6 months and 3 days in the future from the start of
 *          the current day
 *   +6M+3D/D
 *      ... 6 months and 3 days in the future from now, rounded
 *          down to nearest day
 * </pre>
 *
 * <p>
 * The complete list of aliases that exist for the different units of
 * time can be found by inspecting the keySet of {@link #CALENDAR_UNITS})
 * </p>
 *
 * <p>
 * All commands are relative to a "now" which is the time at the moment
 * when the DateMathParser.parseMath function is called.  As such,
 * <code>p.parseMath("+0MIL").equals(p.parseMath("+0MIL"))!=true</code>.
 * </p>
 */
public class DateMathParser  {

    /**
     * A mapping from String labels identifying time units,
     * to the corresponding Calendar constant used to set/add/roll that unit
     * of measurement.
     *
     * @see Calendar
     */
    public static final Map<String,Integer> CALENDAR_UNITS =
            Collections.unmodifiableMap(new HashMap<String, Integer>() {{
                put("Y",   Calendar.YEAR);
                put("M",   Calendar.MONTH);
                put("D",   Calendar.DATE);
                put("H",   Calendar.HOUR_OF_DAY);
                put("MIN", Calendar.MINUTE);
                put("SEC", Calendar.SECOND);
                put("MIL", Calendar.MILLISECOND);
            }});

    private static final Pattern SPLITTER = Pattern.compile("\\b|(?<=\\d)(?=\\D)");

    /**
     * Modifies the specified Calendar by "adding" the specified value of units
     *
     * @exception IllegalArgumentException if unit isn't recognized.
     * @see #CALENDAR_UNITS
     */
    public static void add(Calendar c, int val, String unit) {
        Integer uu = CALENDAR_UNITS.get(unit);
        if (null == uu) {
            throw new IllegalArgumentException("Adding Unit not recognized: " + unit);
        }
        c.add(uu.intValue(), val);
    }

    /**
     * Modifies the specified Calendar by "rounding" down to the specified unit
     *
     * @exception IllegalArgumentException if unit isn't recognized.
     * @see #CALENDAR_UNITS
     */
    public static void round(Calendar c, String unit) {
        Integer uu = CALENDAR_UNITS.get(unit);
        if (null == uu) {
            throw new IllegalArgumentException("Rounding Unit not recognized: "
                    + unit);
        }
        switch (uu) {
            case Calendar.YEAR:
                c.clear(Calendar.MONTH);
            /* fall through */
            case Calendar.MONTH:
                c.clear(Calendar.DAY_OF_MONTH);
                c.clear(Calendar.DAY_OF_WEEK);
                c.clear(Calendar.DAY_OF_WEEK_IN_MONTH);
                c.clear(Calendar.DAY_OF_YEAR);
                c.clear(Calendar.WEEK_OF_MONTH);
                c.clear(Calendar.WEEK_OF_YEAR);
            /* fall through */
            case Calendar.DATE:
                c.clear(Calendar.HOUR_OF_DAY);
                c.clear(Calendar.HOUR);
                c.clear(Calendar.AM_PM);
            /* fall through */
            case Calendar.HOUR_OF_DAY:
                c.clear(Calendar.MINUTE);
            /* fall through */
            case Calendar.MINUTE:
                c.clear(Calendar.SECOND);
            /* fall through */
            case Calendar.SECOND:
                c.clear(Calendar.MILLISECOND);
                break;
            default:
                throw new IllegalStateException("No logic for rounding unit " + unit);
        }
    }

    /**
     * Parses a string of commands relative "now" and returns the resulting Date.
     *
     * @exception ParseException positions in ParseExceptions are token positions, not character positions.
     */
    public static Calendar parseMath(String math) throws ParseException {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        return parseMath(cal, math);
    }

    /**
     * Parses a string of commands relative to a specific calendar and returns the resulting Date.
     *
     * @exception ParseException positions in ParseExceptions are token positions, not character positions.
     */
    protected static Calendar parseMath(Calendar cal, String math) throws ParseException {

        /* check for No-Op */
        if ((math == null) || (0 == math.length())) {
            return cal;
        }

        String[] ops = SPLITTER.split(math);
        int pos = 0;
        while (pos < ops.length) {

            if (1 != ops[pos].length()) {
                throw new ParseException("Multi character command found: \"" + ops[pos] + "\"", pos);
            }
            char command = ops[pos++].charAt(0);

            switch (command) {
                case '/':
                    if (ops.length < pos + 1) {
                        throw new ParseException("Need a unit after command: \"" + command + "\"", pos);
                    }
                    try {
                        round(cal, ops[pos++]);
                    } catch (IllegalArgumentException e) {
                        throw new ParseException("Unit not recognized: \"" + ops[pos - 1] + "\"", pos - 1);
                    }
                    break;
                case '+': /* fall through */
                case '-':
                    if (ops.length < pos + 2) {
                        throw new ParseException("Need a value and unit for command: \"" + command + "\"", pos);
                    }
                    int val = 0;
                    try {
                        val = Integer.valueOf(ops[pos++]);
                    } catch (NumberFormatException e) {
                        throw new ParseException("Not a Number: \"" + ops[pos - 1] + "\"", pos - 1);
                    }
                    if ('-' == command) {
                        val = 0 - val;
                    }
                    try {
                        String unit = ops[pos++];
                        add(cal, val, unit);
                    } catch (IllegalArgumentException e) {
                        throw new ParseException("Unit not recognized: \"" + ops[pos - 1] + "\"", pos - 1);
                    }
                    break;
                default:
                    throw new ParseException("Unrecognized command: \"" + command + "\"", pos - 1);
            }
        }
        return cal;
    }
}