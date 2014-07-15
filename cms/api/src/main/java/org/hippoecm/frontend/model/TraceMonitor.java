/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.model;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Item;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>javax.jcr.Item</code> usage tracing/monitoring utility.
 * <P>
 * <EM>
 *   Note: This is turned off by default. Set '-DTraceMonitor.enabled=true' system property to turn it on.
 *   Also, the default trace item size is 2000. You can increase it by setting '-DTraceMonitor.max=3000' for instance.
 *   Setting it to zero means unlimited size, which could be dangerous at runtime. So use it only in debugging mode!
 * </EM>
 * </P>
 */
@SuppressWarnings("unchecked")
public class TraceMonitor {

    static Logger log = LoggerFactory.getLogger(TraceMonitor.class);

    static final String ENABLED_PROP = TraceMonitor.class.getSimpleName() + ".enabled";
    static final String MAX_PROP = TraceMonitor.class.getSimpleName() + ".max";
    static final int DEFAULT_MAX_SIZE = 2000;
    private static final int maxSize;
    private static final Map<String, String> initializedAndNotDetached;

    static {
        boolean enabled = BooleanUtils.toBoolean(System.getProperty(ENABLED_PROP));

        int max = NumberUtils.toInt(System.getProperty(MAX_PROP), -1);
        if (max < 0) {
            maxSize = DEFAULT_MAX_SIZE;
        } else {
            maxSize = max;
        }

        if (enabled) {
            if (maxSize > 0) {
                log.info("{}: Max monitoring items set to {}", TraceMonitor.class.getSimpleName(), maxSize);
                initializedAndNotDetached = Collections.synchronizedMap(new LRUMap(maxSize));
            } else {
                log.warn("{}: No limit to monitoring items.", TraceMonitor.class.getSimpleName());
                initializedAndNotDetached = new ConcurrentHashMap<String, String>();
            }
        } else {
            initializedAndNotDetached = null;
        }
    }

    protected static void track(Item item) {
        if (initializedAndNotDetached != null && log.isDebugEnabled()) {
            initializedAndNotDetached.put(item.toString(), getCallee());
        }
    }

    protected static void release(Item item) {
        if (initializedAndNotDetached != null && log.isDebugEnabled()) {
            initializedAndNotDetached.remove(item.toString());
        }
    }

    protected static void trace(Item item) {
        if (initializedAndNotDetached != null && log.isDebugEnabled() && initializedAndNotDetached.containsKey(item.toString())) {
            String stackTrace = initializedAndNotDetached.get(item.toString());
            log.debug(stackTrace);
        }
    }

    protected static String getCallee() {
        Exception exception = new RuntimeException("Determine CallStackTrace");

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(os);
        exception.printStackTrace(pw);
        pw.flush();

        return os.toString();
    }

    protected static int getSize() {
        if (initializedAndNotDetached != null) {
            return initializedAndNotDetached.size();
        } else {
            return 0;
        }
    }
}
