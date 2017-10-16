/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hippoecm.repository.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class WorkflowTransition {

    private final String requestIdentifier;
    private Map<String, Object> eventPayload = new HashMap<>();
    private final Map<String, Serializable> initializationPayload;
    private final String action;
    private Map<String, Boolean> actionsMap;

    public Map<String, Boolean> getActionsMap() {
        return actionsMap;
    }

    public Map<String, Serializable> getInitializationPayload() {
        return initializationPayload;
    }

    public Map<String, Object> getEventPayload() {
        return eventPayload;
    }

    public String getAction() {
        return action;
    }

    public String getRequestIdentifier(){
        return requestIdentifier;
    }

    public static class Builder {
        private final Map<String, Object> eventPayload = new HashMap<>();
        private Map<String, Serializable> initializationPayload;
        private String action;
        private Map<String, Boolean> actionsMap;
        private String requestIdentifier;

        public Builder actionsMap(final Map<String, Boolean> actionsMap) {
            this.actionsMap = actionsMap;
            return this;
        }

        public Builder eventPayload(final Map<String, Object> eventPayload) {
            this.eventPayload.putAll(eventPayload);
            return this;
        }

        public Builder eventPayload(final String key, final Object value){
            this.eventPayload.put(key,value);
            return this;
        }

        public Builder eventPayload(final String key1, final Object value1, final String key2, final Object value2){
            this.eventPayload.put(key1,value1);
            this.eventPayload.put(key2,value2);
            return this;
        }

        /**
         * Sets the initialization payload. Note that the argument does not get cloned meaning that if you change it
         * after invoking this menthod, the {@code initializationPayload} object in this {@link Builder} changes
         * @param initializationPayload the initial payload to set
         * @return this {@link Builder}
         */
        public Builder initializationPayload(final Map<String, Serializable> initializationPayload) {
            this.initializationPayload = initializationPayload;
            return this;
        }


        public Builder action(final String action){
            this.action = action;
            return this;
        }

        public Builder action(final ActionAware action){
            this.action = action.getAction();
            return this;
        }

        public WorkflowTransition build() {
            return new WorkflowTransition(this);
        }

        public Builder requestIdentifier(final String requestIdentifier) {
            this.requestIdentifier = requestIdentifier;
            return this;
        }
    }

    private WorkflowTransition(final Builder b) {
        this.action = b.action;
        this.actionsMap = b.actionsMap;
        this.initializationPayload = b.initializationPayload;
        this.eventPayload = b.eventPayload;
        this.requestIdentifier = b.requestIdentifier;
    }
}
