/**
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
 */
@Plugin(name = "LookupFilter", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE, printObject = true)
public class LookupFilter extends AbstractFilter {

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

    protected LookupFilter(final String key, final String value, final Result onMatch, final Result onMismatch) {
        super(onMatch, onMismatch);
        this.key = key;
        this.value = value;
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
