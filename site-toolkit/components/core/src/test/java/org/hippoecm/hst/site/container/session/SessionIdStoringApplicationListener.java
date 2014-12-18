/**
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.site.container.session;

import java.util.List;

import com.google.common.eventbus.Subscribe;

import org.hippoecm.hst.container.event.HttpSessionCreatedEvent;
import org.hippoecm.hst.container.event.HttpSessionDestroyedEvent;
import org.hippoecm.hst.core.container.ComponentManager;
import org.hippoecm.hst.core.container.ComponentManagerAware;

public class SessionIdStoringApplicationListener implements ComponentManagerAware {

    private List<String> sessionIdStore;
    private ComponentManager componentManager;

    public SessionIdStoringApplicationListener(List<String> sessionIdStore) {
        if (null == sessionIdStore) {
            throw new IllegalArgumentException("Set non null set.");
        }

        this.sessionIdStore = sessionIdStore;
    }

    @Override
    public void setComponentManager(ComponentManager componentManager) {
        this.componentManager = componentManager;
    }

    public void init() {
        componentManager.registerEventSubscriber(this);
    }

    public void destroy() {
        componentManager.unregisterEventSubscriber(this);
    }

    @Subscribe
    public void onHttpSessionCreatedEvent(HttpSessionCreatedEvent event) {
        sessionIdStore.add(event.getSession().getId());
    }

    @Subscribe
    public void onHttpSessionDestroyedEvent(HttpSessionDestroyedEvent event) {
        sessionIdStore.remove(event.getSession().getId());
    }

}
