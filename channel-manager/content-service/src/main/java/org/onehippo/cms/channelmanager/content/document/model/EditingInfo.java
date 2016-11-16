/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms.channelmanager.content.document.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * This bean carries information about the editing state of a document.
 * It can be serialized into JSON to expose it through a REST API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EditingInfo {

    public enum State {
        AVAILABLE,
        UNAVAILABLE,
        UNAVAILABLE_HELD_BY_OTHER_USER,
        UNAVAILABLE_REQUEST_PENDING,
        UNAVAILABLE_CUSTOM_VALIDATION_PRESENT
    }

    private State state = State.UNAVAILABLE;
    private UserInfo holder;

    public State getState() {
        return state;
    }

    public void setState(final State state) {
        this.state = state;
    }

    public UserInfo getHolder() {
        return holder;
    }

    public void setHolder(final UserInfo holder) {
        this.holder = holder;
    }
}
