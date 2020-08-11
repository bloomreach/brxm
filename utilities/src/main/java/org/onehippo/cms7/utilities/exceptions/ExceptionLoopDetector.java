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
package org.onehippo.cms7.utilities.exceptions;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Loop detector, detects if there is already same exception occuring for x times within x seconds
 */
public class ExceptionLoopDetector {

    public ExceptionLoopDetector(long timeToLive, int threshold) {
        this.timeToLive = timeToLive;
        this.threshold = threshold;
    }

    /**
     * Time to live
     */
    private final long timeToLive;

    /**
     * Treshold
     */
    private final long threshold;
    private final Map<String, List<Long>> exceptionMap = new HashMap<>();

    /**
     * Detect if exception with the same message already exists in cache
     */
    public boolean loopDetected(Exception ex) {
        purge();
        final String key = createKey(ex);
        if (exceptionMap.containsKey(key)) {
            final List<Long> hits = exceptionMap.get(key);
            hits.add(System.currentTimeMillis());
            return hits.size() >= threshold;
        } else {
            final List<Long> entries = new ArrayList<>();
            entries.add(System.currentTimeMillis());
            exceptionMap.put(key, entries);
            return false;
        }
    }

    private String createKey(Exception ex) {
        String message = ex.getMessage();
        return ex.getStackTrace() != null && ex.getStackTrace().length > 0
                ? message + ":" + ex.getStackTrace()[0].getMethodName() + ":" + ex.getStackTrace()[0].getLineNumber() : message;
    }

    /**
     * Remove expired values
     */
    public void purge() {
        final long currentTime = System.currentTimeMillis();
        for (Iterator<Map.Entry<String, List<Long>>> it = exceptionMap.entrySet().iterator(); it.hasNext();) {
            final List<Long> events = it.next().getValue();
            events.removeIf(eventTime -> {
                long l = eventTime + timeToLive;
                return currentTime > l;
            });
            if (events.size() == 0) {
                it.remove();
            }
        }
    }
}
