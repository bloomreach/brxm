/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms.channelmanager.content;

import javax.jcr.observation.EventIterator;

import org.onehippo.cms.channelmanager.content.documenttype.DocumentTypesService;
import org.onehippo.repository.jaxrs.api.JsonResourceServiceModule;
import org.onehippo.repository.jaxrs.api.ManagedUserSessionInvoker;
import org.onehippo.repository.jaxrs.event.JcrEventListener;

import static org.hippoecm.repository.util.JcrUtils.ALL_EVENTS;

/**
 * ChannelContentServiceModule registers and manages a JAX-RS endpoint of the repository module.
 *
 * That endpoint represents the REST resource {@link ContentResource} and the resource's
 * root address (configurable, but defaulting to "content"), and it registers the
 * {@link ManagedUserSessionInvoker} to take care of authentication and authorization.
 */
public class ChannelContentServiceModule extends JsonResourceServiceModule {

    public ChannelContentServiceModule() {
        addEventListener(new HippoNamespacesEventListener() {
            @Override
            public void onEvent(final EventIterator events) {
                DocumentTypesService.get().invalidateCache();
            }
        });
    }

    @Override
    protected Object getRestResource(final ManagedUserSessionInvoker managedUserSessionInvoker) {
        return new ContentResource(managedUserSessionInvoker);
    }

    private abstract static class HippoNamespacesEventListener extends JcrEventListener {

        static final String HIPPO_NAMESPACES = "/hippo:namespaces";

        HippoNamespacesEventListener() {
            super(ALL_EVENTS, HIPPO_NAMESPACES, true, null, null);
        }
    }
}
