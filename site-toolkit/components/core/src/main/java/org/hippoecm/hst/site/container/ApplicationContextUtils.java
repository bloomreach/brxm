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
package org.hippoecm.hst.site.container;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * ApplicationContextUtils
 * @version $Id$
 */
public class ApplicationContextUtils {

    private static Logger log = LoggerFactory.getLogger(ApplicationContextUtils.class);

    private ApplicationContextUtils() {

    }

    /**
     * Returns location patterns which were checked by <CODE>ApplicationContext.getResources(locationPattern);</CODE>
     * without any IOException.
     * @param applicationContext
     * @param locationPatterns
     * @return
     */
    public static String[] getCheckedLocationPatterns(ApplicationContext applicationContext,
            String[] locationPatterns) {
        ArrayList<String> existingLocationPatterns = new ArrayList<String>();

        if (locationPatterns != null) {
            for (String locationPattern : locationPatterns) {
                try {
                    applicationContext.getResources(locationPattern);
                    existingLocationPatterns.add(locationPattern);
                } catch (IOException e) {
                    log.debug("Ignoring resources on {}. It does not exist.", locationPattern);
                }
            }
        }

        return existingLocationPatterns.toArray(new String[existingLocationPatterns.size()]);
    }

    /**
     * Returns location patterns which were checked by <CODE>ApplicationContext.getResources(locationPattern);</CODE>
     * without any IOException.
     * @param applicationContext
     * @param locationPatterns
     * @return
     */
    public static String[] getCheckedLocationPatterns(ApplicationContext applicationContext,
            List<String> locationPatterns) {
        return getCheckedLocationPatterns(applicationContext, new ArrayList<String>(locationPatterns).toArray(new String[locationPatterns.size()]));
    }
}
