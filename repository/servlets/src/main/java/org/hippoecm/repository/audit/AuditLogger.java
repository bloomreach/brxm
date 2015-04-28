/*
 *  Copyright 2012-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.audit;

import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.json.JSONObject;

public class AuditLogger {

    private static final String AUDIT_LOGGER = "org.onehippo.audit";
    private static final Logger log = LoggerFactory.getLogger(AUDIT_LOGGER);

    public AuditLogger() {
    }

    public static Logger getLogger() {
        return log;
    }

    @Subscribe
    public void logHippoEvent(HippoEvent event) {
        if (log.isInfoEnabled()) {
            final JSONObject jsonObject = new JSONObject();
            for (Object key : event.getValues().keySet()) {
                final Object value = event.get((String) key);
                jsonObject.put(key, getValue(value));
            }
            AuditLogger.getLogger().info(jsonObject.toString());
        }
    }

    private Object getValue(final Object value) {
        if (value instanceof Throwable) {
            return value.toString();
        }
        return value;
    }
}
