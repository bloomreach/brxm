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

import org.apache.log4j.spi.LoggingEvent;


/**
 * MdcOrJndiPropertyFilter
 *
 * <P>
 * This filter tries to retrieve the property value by the name from the log4j MDC.
 * If not found from MDC, then it tries to retrieve the property value by the name from the JNDI resources.
 * </P>
 * Exammple configuration is:
 * <XMP>
 * <filter class="org.onehippo.cms7.logging.log4j.MdcPropertyFilter">
 *   <param name="name" value="logging/contextName" />
 *   <param name="resourceRef" value="true" />
 *   <param name="value" value="cms" />
 *   <param name="onMatchOption" value="ACCEPT" />
 * </filter>
 * </XMP>
 */
public class MdcOrJndiPropertyFilter extends JndiPropertyFilter {

    @Override
    protected String getProperty(LoggingEvent event, String key) {
        Object prop = event.getMDC(key);

        if (prop != null) {
            return prop.toString();
        }

        return super.getProperty(event, key);
    }

}
