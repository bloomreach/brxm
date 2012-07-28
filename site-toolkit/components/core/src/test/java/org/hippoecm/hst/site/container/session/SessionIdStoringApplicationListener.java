/**
 * Copyright 2012 Hippo.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.site.container.session;

import java.util.List;

import org.junit.Ignore;
import org.springframework.context.ApplicationListener;

@Ignore
public class SessionIdStoringApplicationListener implements ApplicationListener<HttpSessionCreatedEvent> {

    private List<String> sessionIdStore;

    public SessionIdStoringApplicationListener(List<String> sessionIdStore) {
        if (null == sessionIdStore) {
            throw new IllegalArgumentException("Set non null set.");
        }

        this.sessionIdStore = sessionIdStore;
    }

    /* (non-Javadoc)
     * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
     */
    @Override
    public void onApplicationEvent(HttpSessionCreatedEvent event) {
        sessionIdStore.add(event.getSession().getId());
    }

}
