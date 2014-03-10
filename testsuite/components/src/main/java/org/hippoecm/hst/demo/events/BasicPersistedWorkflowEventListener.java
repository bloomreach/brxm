/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.demo.events;

import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.event.HippoEventConstants;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.events.PersistedHippoEventListener;
import org.onehippo.repository.events.PersistedHippoEventsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.json.JSONObject;

public class BasicPersistedWorkflowEventListener implements PersistedHippoEventListener {

    private static Logger log = LoggerFactory.getLogger(BasicPersistedWorkflowEventListener.class);

    public void register() {
        HippoServiceRegistry.registerService(this, PersistedHippoEventsService.class);
    }

    public void unregister() {
        HippoServiceRegistry.unregisterService(this, PersistedHippoEventsService.class);
    }

    @Override
    public  String getEventCategory() {
        return HippoEventConstants.CATEGORY_WORKFLOW;
    }

    @Override
    public String getChannelName() {
        return "basic";
    }

    @Override
    public boolean onlyNewEvents() {
        return false;
    }

    @Override
    public void onHippoEvent(final HippoEvent event) {
        if (log.isInfoEnabled()) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.putAll(event.getValues());
            log.info(jsonObject.toString());
        }
    }
}
