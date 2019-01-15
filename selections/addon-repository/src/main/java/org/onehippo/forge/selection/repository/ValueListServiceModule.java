/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.selection.repository;

import javax.jcr.observation.EventIterator;

import org.onehippo.forge.selection.frontend.Namespace;
import org.onehippo.forge.selection.repository.valuelist.ValueListService;
import org.onehippo.repository.jaxrs.api.JsonResourceServiceModule;
import org.onehippo.repository.jaxrs.api.SessionRequestContextProvider;
import org.onehippo.repository.jaxrs.event.JcrEventListener;

import static org.hippoecm.repository.util.JcrUtils.ALL_EVENTS;

public class ValueListServiceModule extends JsonResourceServiceModule {

    public ValueListServiceModule() {
        addEventListener(new ValueListsEventListener() {
            @Override
            public void onEvent(final EventIterator events) {
                ValueListService.get().invalidateCache();
            }
        });
    }

    @Override
    protected Object getRestResource(final SessionRequestContextProvider sessionRequestContextProvider) {
        return new ValueListResource(sessionRequestContextProvider);
    }

    private abstract static class ValueListsEventListener extends JcrEventListener {
        ValueListsEventListener() {
            super(ALL_EVENTS, "/content/documents", true, null,
                    new String[]{Namespace.Type.VALUE_LIST, Namespace.Type.VALUE_LIST_ITEM});
        }
    }
}
