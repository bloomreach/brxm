/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.EventIterator;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms.channelmanager.content.document.DocumentsServiceImpl;
import org.onehippo.cms.channelmanager.content.document.util.BranchingService;
import org.onehippo.cms.channelmanager.content.document.util.BranchingServiceImpl;
import org.onehippo.cms.channelmanager.content.document.util.HintsInspector;
import org.onehippo.cms.channelmanager.content.document.util.HintsInspectorImpl;
import org.onehippo.cms.channelmanager.content.documenttype.DocumentTypesService;
import org.onehippo.cms.channelmanager.content.workflows.WorkflowServiceImpl;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.onehippo.repository.jaxrs.api.JsonResourceServiceModule;
import org.onehippo.repository.jaxrs.api.ManagedUserSessionInvoker;
import org.onehippo.repository.jaxrs.api.SessionRequestContextProvider;
import org.onehippo.repository.jaxrs.event.JcrEventListener;

import static org.hippoecm.repository.util.JcrUtils.ALL_EVENTS;

/**
 * ChannelContentServiceModule registers and manages a JAX-RS endpoint of the repository module.
 * <p>
 * That endpoint represents the REST resource {@link ContentResource} and the resource's root address (configurable, but
 * defaulting to "content"), and it registers the {@link ManagedUserSessionInvoker} to take care of authentication and
 * authorization.
 */
public class ChannelContentServiceModule extends JsonResourceServiceModule {

    private final DocumentsServiceImpl documentsService;
    private final WorkflowServiceImpl workflowsService;
    private Function<HttpServletRequest, Map<String, Serializable>> contextPayloadService;

    public ChannelContentServiceModule() {
        this.documentsService = new DocumentsServiceImpl();
        this.workflowsService = new WorkflowServiceImpl();
        this.contextPayloadService = request -> Optional.ofNullable(CmsSessionContext.getContext(request.getSession()).getContextPayload()).orElse(Collections.emptyMap());
        addEventListener(new HippoNamespacesEventListener() {
            @Override
            public void onEvent(final EventIterator events) {
                DocumentTypesService.get().invalidateCache();
            }
        });
    }

    @Override
    protected void doInitialize(final Session session) throws RepositoryException {
        super.doInitialize(session);
        this.documentsService.setHintsInspector(createHintsInspector(session));
        this.documentsService.setBranchingService(createBranchingService(session));
    }

    private HintsInspector createHintsInspector(final Session session) throws RepositoryException {
        final String propertyPath = moduleConfigPath + "/hintsInspectorClass";
        final String defaultValue = HintsInspectorImpl.class.getName();
        final String className = JcrUtils.getStringProperty(session, propertyPath, defaultValue);
        try {
            return (HintsInspector) Class.forName(className).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RepositoryException(e);
        }
    }

    private BranchingService createBranchingService(final Session session) throws RepositoryException {
        final String propertyPath = moduleConfigPath + "/branchingServiceClass";
        final String defaultValue = BranchingServiceImpl.class.getName();
        final String className = JcrUtils.getStringProperty(session, propertyPath, defaultValue);
        try {
            return (BranchingService) Class.forName(className).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    protected Object getRestResource(final SessionRequestContextProvider sessionRequestContextProvider) {
        return new ContentResource(sessionRequestContextProvider, documentsService, workflowsService, contextPayloadService);
    }

    private abstract static class HippoNamespacesEventListener extends JcrEventListener {

        static final String HIPPO_NAMESPACES = "/hippo:namespaces";

        HippoNamespacesEventListener() {
            super(ALL_EVENTS, HIPPO_NAMESPACES, true, null, null);
        }
    }
}
