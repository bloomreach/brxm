/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.logging.log4j;

import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

/**
 * AbstractPropertyFilter (Log4j1)
 *
 * <P>
 * Abstract log4j filter base class which can be extended for specific property based logging event filtering.
 * This abstract class provides three base properties, name, value and onMatchOption.
 * </P>
 * <P>
 * This abstract filter base class compares the runtime property value with the specified property value
 * by invoking the abstract {@link #getProperty(LoggingEvent, String)} method. Therefore, derived classes can implement
 * the {@link #getProperty(LoggingEvent, String)} method to read runtime properties from any environments such as Log4j MDC,
 * JNDI resources, etc.
 * </P>
 * <P>
 * The 'name' property is for specifying a property name, and the 'value' property is for specifying a property value
 * to be compared with the real property value at runtime.
 * Finally, the 'onMatchOption' property is for specifying the behavior when the property is matched.
 * The 'onMatchOption' property can be set to either 'ACCEPT' ({@link org.apache.log4j.spi.Filter#ACCEPT}) or 'DENY' ({@link org.apache.log4j.spi.Filter#DENY}).
 * </P>
 * <P>
 * If the 'onMatchOption' is set to 'ACCEPT', then the logging even will be accepted when the runtime property value matches
 * with the specified property value. In this case, the logging event will be rejected if the runtime property value doesn't match.
 * </P>
 * <P>
 * If the 'onMatchOption' is set to 'DENY', then the logging event will be rejected when the runtime property value matches
 * with the specified property value. In this case, the logging event will be accepted when the runtime property value doesn't match.
 * </P>
 *
 * @see org.apache.log4j.spi.Filter
 *
 * @deprecated since the switch to Log4j2 in CMS v12. Instead use {@link LookupFilter} which is log4j2 based.
 */
@Deprecated
public abstract class AbstractPropertyFilter extends Filter {

    /**
     * Property name to read from the runtime environment
     */
    private String name;

    /**
     * Property value to be compared with the runtime property value
     */
    private String value;

    /**
     * Filtering option to decide on the specific logging event.
     * This can be either <code>ACCEPT</code> or <code>DENY</code>.
     */
    private int onMatch = NEUTRAL;

    @Override
    public int decide(LoggingEvent event) {
        if (name == null || value == null) {
            return NEUTRAL;
        }

        if (value.equals(getProperty(event, name))) {
            //System.out.println("MMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMMM MATCH: " + name + ", " + value);
            return onMatch;
        } else {
            if (ACCEPT == onMatch) {
                //System.out.println("DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD DENY: " + name + ", " + value);
                return DENY;
            } else if (DENY == onMatch) {
                //System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA ACCEPT: " + name + ", " + value);
                return ACCEPT;
            }
        }

        //System.out.println("NNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNNN NEUTRAL: " + name + ", " + value);
        return NEUTRAL;
    }

    /**
     * The property name on which to filter.
     *
     * @return property name on which to filter
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the property name on which to filter.
     *
     * @param name the property name on which to filter
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the value to match.
     *
     * @return the value to match.
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value to match.
     *
     * @param value the value to match.
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Returns the deciding option whether or not the log message should be logged if a match is found.
     * @see org.apache.log4j.spi.Filter#ACCEPT
     * @see org.apache.log4j.spi.Filter#DENY
     */
    public int getOnMatch() {
        return onMatch;
    }

    /**
     * Sets the deciding option whether or not the log message should be logged.
     * Allowed option is either {@link org.apache.log4j.spi.Filter#ACCEPT} or {@link org.apache.log4j.spi.Filter#DENY}.
     * @param onMatch
     * @see org.apache.log4j.spi.Filter#ACCEPT
     * @see org.apache.log4j.spi.Filter#DENY
     */
    public void setOnMatch(int onMatch) {
        this.onMatch = onMatch;
    }

    /**
     * Returns string representation of the option flag whether or not the log message should be logged if a match is found.
     * @see org.apache.log4j.spi.Filter#ACCEPT
     * @see org.apache.log4j.spi.Filter#DENY
     */
    public String getOnMatchOption() {
        if (ACCEPT == onMatch) {
            return "ACCEPT";
        } else  if (DENY == onMatch) {
            return "DENY";
        } else {
            return "NEUTRAL";
        }
    }

    /**
     * Sets string representation of the option flag whether or not the log message should be logged.
     * Allowed option is either 'ACCEPT' or 'DENY'.
     * @param onMatchOption
     * @see org.apache.log4j.spi.Filter#ACCEPT
     * @see org.apache.log4j.spi.Filter#DENY
     */
    public void setOnMatchOption(String onMatchOption) {
        if (!"ACCEPT".equals(onMatchOption) && !"DENY".equals(onMatchOption) && !"NEUTRAL".equals(onMatchOption)) {
            throw new IllegalArgumentException("Invalid onMatchOption: '" + onMatchOption + "'. onMatchOption must be one of 'ACCEPT', 'DENY' and 'NEUTRAL'.");
        }

        if ("ACCEPT".equals(onMatchOption)) {
            onMatch = ACCEPT;
        } else  if ("DENY".equals(onMatchOption)) {
            onMatch = DENY;
        } else  if ("NEUTRAL".equals(onMatchOption)) {
            onMatch = NEUTRAL;
        }
    }

    /**
     * Finds the property value by the name from the underlying environment.
     * @param event
     * @param name
     */
    abstract protected String getProperty(LoggingEvent event, String name);
}
