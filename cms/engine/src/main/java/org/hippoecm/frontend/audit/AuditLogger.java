/*
 *  Copyright 2012 Hippo.
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
package org.hippoecm.frontend.audit;

import java.util.HashMap;
import java.util.Map;

import com.google.common.eventbus.Subscribe;

import org.onehippo.event.audit.HippoAuditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.json.JSONObject;

public class AuditLogger {

    private static final String AUDIT_LOGGER = "org.onehippo.audit";
    private static final Logger log = LoggerFactory.getLogger(AUDIT_LOGGER);

    private static final String APPLICATION = "application";
    private static final String RESULT = "result";
    private static final String USER = "user";
    private static final String ACTION = "action";
    private static final String CATEGORY = "category";
    private static final String MESSAGE = "message";

    public static Logger getLogger() {
        return log;
    }

    @Subscribe
    public void logHippoAuditEvent(HippoAuditEvent event) {
        Map<String, Object> values = new HashMap<String, Object>();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put(USER, event.user());
        jsonObject.put(APPLICATION, event.application());
        jsonObject.put(ACTION, event.action());
        jsonObject.put(CATEGORY, event.category());
        jsonObject.put(MESSAGE, event.message());
        jsonObject.put(RESULT, event.result());
        jsonObject.putAll(values);

        AuditLogger.getLogger().info(jsonObject.toString());
    }
}
