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
package org.onehippo.event.impl;

import com.google.common.eventbus.EventBus;

import org.onehippo.event.HippoEventBus;

public class HippoEventBusBinder {

    private static final HippoEventBus EVENT_BUS = new HippoEventBus() {

        final EventBus eventBus = new EventBus();

        @Override
        protected void doRegister(final Object listener) {
            eventBus.register(listener);
        }

        @Override
        protected void doUnregister(final Object listener) {
            eventBus.unregister(listener);
        }

        @Override
        protected void doPost(final Object event) {
            eventBus.post(event);
        }
    };

    public static HippoEventBus getInstance() {
        return EVENT_BUS;
    }

}
