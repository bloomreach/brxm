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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class TraceMonitor {

    static final Logger log = LoggerFactory.getLogger(TraceMonitor.class);

    private static final Map<String, String> initializedAndNotDetached = new HashMap<String, String>();

    protected static final void track(Item item) {
        if (log.isDebugEnabled()) {
            initializedAndNotDetached.put(item.toString(), getCallee());
        }
    }

    protected static final void release(Item item) {
        if (log.isDebugEnabled()) {
            initializedAndNotDetached.remove(item.toString());
        }
    }

    protected static final void trace(Item item) {
        if (log.isDebugEnabled() && initializedAndNotDetached.containsKey(item.toString())) {
            String stackTrace = initializedAndNotDetached.get(item.toString());
            log.debug(stackTrace);
        }
    }

    protected static final String getCallee() {
        Exception exception = new RuntimeException("Determine CallStackTrace");

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(os);
        exception.printStackTrace(pw);
        pw.flush();

        return os.toString();
    }

}
