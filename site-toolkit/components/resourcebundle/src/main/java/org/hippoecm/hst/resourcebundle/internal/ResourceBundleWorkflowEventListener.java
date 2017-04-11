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

package org.hippoecm.hst.resourcebundle.internal;

import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.event.HippoEventConstants;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.events.HippoWorkflowEvent;
import org.onehippo.repository.events.PersistedHippoEventListener;
import org.onehippo.repository.events.PersistedHippoEventsService;

/**
 * Trigger eviction of a resource bundle (family) from the registry (cache) based on workflow events.
 */
public class ResourceBundleWorkflowEventListener implements PersistedHippoEventListener {

    private MutableResourceBundleRegistry resourceBundleRegistry;
    private final String contextPath;

    public ResourceBundleWorkflowEventListener(MutableResourceBundleRegistry resourceBundleRegistry, final String contextPath) {
        this.resourceBundleRegistry = resourceBundleRegistry;
        this.contextPath = contextPath;
    }

    public void init() {
        HippoServiceRegistry.registerService(this, PersistedHippoEventsService.class);
    }

    public void destroy() {
        HippoServiceRegistry.unregisterService(this, PersistedHippoEventsService.class);
    }

    @Override
    public String getEventCategory() {
        return HippoEventConstants.CATEGORY_WORKFLOW;
    }

    @Override
    public String getChannelName() {
        return contextPath + "_" + getClass().getName();
    }

    @Override
    public boolean onlyNewEvents() {
        return true;
    }

    /**
     * The resourceBundleRegistry contains a cache of ResourceBundleFamily instances, each representing a certain
     * resource bundle document, keyed by document handle UUID. The workflow event contains that key in the
     * 'subjectId' field. For each workflow event pertaining to a resopirce bundle document, we tell the registry
     * to evict the entry from its cache if the event's subjectId matches with an existing entry.
     */
    @Override
    public void onHippoEvent(final HippoEvent event) {
        final HippoWorkflowEvent wfEvent = new HippoWorkflowEvent(event);
        final String documentType = wfEvent.documentType();

        if (!"resourcebundle:resourcebundle".equals(documentType) && !"hippo:deleted".equals(documentType)) {
            // When a resource bundle document is deleted, the document type in the workflow event
            // is set to "hippo:deleted". The extra check above makes sure these events are also
            // passed on to the resource bundle registry, even if they don't belong to resource bundle documents.
            return;
        }

        // only publish/depublish workflow actions affect the live version of the resource bundle,
        // all other actions affect the preview version.
        final String action = wfEvent.action();
        final boolean preview = !("publish".equals(action) || "depublish".equals(action));

        resourceBundleRegistry.unregisterBundleFamily(wfEvent.subjectId(), preview);
    }

}
