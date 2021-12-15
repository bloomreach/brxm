/**
 * Copyright 2017-2021 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Objects;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.apache.logging.log4j.message.Message;

/**
 * Generic log4j2 Filter using log4j lookup variable interpolation to decide if the log event
 * will be logged by the Appender or not.
 * <p>
 * Example usage:
 * </p>
 * <pre><code>
 *     &lt;LookupFilter key="jndi:logging/contextName" value="cms" onMatch="ACCEPT"/&gt;
 * </code></pre>
 * <p>
 * The <code>key</code> attribute can interpolate all log4j2
 * <a href="https://logging.apache.org/log4j/2.0/manual/configuration.html#PropertySubstitution">Lookup variables</a>
 * which current value will be matched against the <code>value</code> attribute.
 * </p>
 * <p>
 * Note: the log4j2 LookupFilter replaces the now deprecated log4j1 {@link JndiPropertyFilter}.
 * </p>
 * <p>
 *  <br/>
 *  <em>Note: As of log4j2 2.16+, jndi based lookups are now by default disabled!</em>
 *  The most common usage thereof, for brXM, is specifically the lookup of the logging/contextName
 *  to filter log events for specific contexts, as seen in the above example.
 *  This specific jndi lookup is <b>not</b> a security vulnerability however, so to cater for log4j2 2.16+
 *  with disabled jndi lookup, a custom (fixed) workaround has been implemented which <b>only</b> intercepts a filter
 *  key="jndi:logging/contextName" and then do the jndi lookup directly, not via log4j2.
 * </p>
 */
@Plugin(name = "LookupFilter", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE, printObject = true)
public class LookupFilter extends AbstractFilter {

    private static String JNDI_LOGGING_CONTEXT_NAME_FILTER_KEY = "jndi:logging/contextName";
    private static String JNDI_LOGGING_CONTEXT_NAME_KEY = "java:comp/env/logging/contextName";

    private static String jndiLookupLoggingContextName() {
        try {
            InitialContext ctx = new InitialContext();
            return Objects.toString(ctx.lookup(JNDI_LOGGING_CONTEXT_NAME_KEY), null);
        } catch (NamingException e) {
            return null;
        }
    }

    /**
     * Extended Interpolator to intercept its lookup methods to guard against invocation from within the
     * Java System FinalizerThread which does <em>not</em> have a contextClassLoader set.
     * In that scenario a jndi key lookup will fail (on Tomcat) with a
     * <pre>ClassNotFoundException: org.apache.naming.java.javaURLContextFactory</pre>
     * To fix this the Thread currentContextClassLoader is temporarily set to the classloader of this class.
     */
    private Interpolator interpolator = new Interpolator(null, null) {
        @Override
        public String lookup(final LogEvent event, final String var) {
            final boolean noContextClassLoader = Thread.currentThread().getContextClassLoader() == null;
            try {
                if (noContextClassLoader) {
                    Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                }
                if (LookupFilter.this.loggingContentNameLookupWorkaround) {
                    return jndiLookupLoggingContextName();
                }
                return super.lookup(event, var);
            } finally {
                if (noContextClassLoader) {
                    Thread.currentThread().setContextClassLoader(null);
                }
            }
        }
        @Override
        public String lookup(final String key) {
            final boolean noContextClassLoader = Thread.currentThread().getContextClassLoader() == null;
            try {
                if (noContextClassLoader) {
                    Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                }
                if (LookupFilter.this.loggingContentNameLookupWorkaround) {
                    return jndiLookupLoggingContextName();
                }
                return super.lookup(key);
            } finally {
                if (noContextClassLoader) {
                    Thread.currentThread().setContextClassLoader(null);
                }
            }
        }
    };

    private final String key;
    private final String value;
    private final boolean loggingContentNameLookupWorkaround;

    protected LookupFilter(final String key, final String value, final Result onMatch, final Result onMismatch) {
        super(onMatch, onMismatch);
        this.key = key;
        this.value = value;
        this.loggingContentNameLookupWorkaround = JNDI_LOGGING_CONTEXT_NAME_FILTER_KEY.equals(key);
    }

    protected Result filter() {
        return value.equals(interpolator.lookup(key)) ? getOnMatch() : getOnMismatch();
    }

    @Override
    public Result filter(final LogEvent event) {
        return value.equals(interpolator.lookup(event, key)) ? getOnMatch() : getOnMismatch();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Message msg,
                         final Throwable t) {
        return filter();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Object msg,
                         final Throwable t) {
        return filter();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
                         final Object... params) {
        return filter();
    }

    @PluginFactory
    public static LookupFilter createFilter(
            @PluginAttribute("key") final String key,
            @PluginAttribute("value") final String value,
            @PluginAttribute("onMatch") final Result match,
            @PluginAttribute("onMismatch") final Result mismatch) {
        if (key == null) {
            LOGGER.error("LookupFilter name attribute is required");
            return null;
        }
        if (value == null) {
            LOGGER.error("LookupFilter value attribute is required");
            return null;
        }
        return new LookupFilter(key, value, match, mismatch);
    }
}
