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

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.spi.LoggingEvent;

/**
 * JndiPropertyFilter
 *
 * <P>
 * This filter retrieves the property value by the name from the JNDI resources.
 * </P>
 * Exammple configuration is:
 * <XMP>
 * <filter class="org.onehippo.cms7.logging.log4j.JndiPropertyFilter">
 *   <param name="name" value="logging/contextName" />
 *   <param name="resourceRef" value="true" />
 *   <param name="value" value="cms" />
 *   <param name="onMatchOption" value="ACCEPT" />
 * </filter>
 * </XMP>
 */
public class JndiPropertyFilter extends AbstractPropertyFilter {

    /** JNDI prefix used in a J2EE container */
    public static final String CONTAINER_PREFIX = "java:comp/env/";

    private boolean resourceRef;

    /**
     * Return whether the lookup occurs in a J2EE container.
     * @return
     */
    public boolean isResourceRef() {
        return resourceRef;
    }

    /**
     * Set whether the lookup occurs in a J2EE container, i.e. if the prefix "java:comp/env/" needs to be added if the key (JNDI name) doesn't already contain it. Default is "false".
     *
     * Note: Will only get applied if no other scheme (e.g. "java:") is given.
     * @param resourceRef
     */
    public void setResourceRef(boolean resourceRef) {
        this.resourceRef = resourceRef;
    }

    @Override
    protected String getProperty(LoggingEvent event, String name) {
        //long t1 = System.currentTimeMillis();
        try {
            InitialContext ctx = new InitialContext();

            if (ctx != null) {
                String prop = (String) ctx.lookup(convertJndiName(name));
                //System.out.println("TTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTT " + (System.currentTimeMillis() - t1) + "ms");
                return prop;
            }
        } catch (NamingException e) {
        }

        return null;
    }

    /**
     * Convert the given JNDI name to the actual JNDI name to use.
     * Default implementation applies the "java:comp/env/" prefix if
     * resourceRef is true and no other scheme like "java:" is given.
     * @param jndiName
     * @return
     */
    private String convertJndiName(String jndiName) {
        if (isResourceRef() && !jndiName.startsWith(CONTAINER_PREFIX) && jndiName.indexOf(':') == -1) {
            jndiName = CONTAINER_PREFIX + jndiName;
        }

        return jndiName;
    }

}
