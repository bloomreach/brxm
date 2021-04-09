/*
 * Copyright 2017-2021 Hippo B.V. (http://www.onehippo.com)
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
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.Function;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.EventIterator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.core.internal.BranchSelectionService;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.cms.channelmanager.content.document.DocumentVersionServiceImpl;
import org.onehippo.cms.channelmanager.content.document.DocumentsServiceImpl;
import org.onehippo.cms.channelmanager.content.document.NodeFieldService;
import org.onehippo.cms.channelmanager.content.document.NodeFieldServiceImpl;
import org.onehippo.cms.channelmanager.content.document.util.BranchSelectionServiceImpl;
import org.onehippo.cms.channelmanager.content.document.util.BranchingService;
import org.onehippo.cms.channelmanager.content.document.util.BranchingServiceImpl;
import org.onehippo.cms.channelmanager.content.document.util.HintsInspector;
import org.onehippo.cms.channelmanager.content.document.util.HintsInspectorImpl;
import org.onehippo.cms.channelmanager.content.document.util.HintsUtils;
import org.onehippo.cms.channelmanager.content.documenttype.DocumentTypesService;
import org.onehippo.cms.channelmanager.content.serialization.CampaignSerializationMixin;
import org.onehippo.cms.channelmanager.content.workflows.WorkflowServiceImpl;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.cmscontext.CmsSessionContext;
import org.onehippo.repository.documentworkflow.version.Campaign;
import org.onehippo.repository.jaxrs.api.JsonResourceServiceModule;
import org.onehippo.repository.jaxrs.api.ManagedUserSessionInvoker;
import org.onehippo.repository.jaxrs.api.SessionRequestContextProvider;
import org.onehippo.repository.jaxrs.event.JcrEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.hippoecm.repository.util.JcrUtils.ALL_EVENTS;

/**
 * ChannelContentServiceModule registers and manages a JAX-RS endpoint of the repository module.
 * <p>
 * That endpoint represents the REST resource {@link ContentResource} and the resource's root address (configurable, but
 * defaulting to "content"), and it registers the {@link ManagedUserSessionInvoker} to take care of authentication and
 * authorization.
 * <p>
 * The module requires implementation classes for the following interfaces
 * - HintsInspector: provides methods that validate if workflow actions are allowed
 * - BranchingService: provides methods for manipulation of document branches
 * - BranchSelectionService: provides methods for manipulation the currently selected branch of a user
 * <p>
 * Implementation classes can be configured via the following module properties:
 * - hintsInspectorClass
 * - branchingServiceClass
 * - branchSelectionClass
 * <p>
 * Implementation classes must have a default constructor so that this module can create instances of them.
 * In absence of a module property a default implementation will be instantiated instead. The default
 * implementations do not support branching of documents.
 */
public class ChannelContentServiceModule extends JsonResourceServiceModule {

    private final static Logger log = LoggerFactory.getLogger(ChannelContentServiceModule.class);

    private final DocumentsServiceImpl documentsService;
    private final WorkflowServiceImpl workflowsService;
    private Function<HttpServletRequest, Map<String, Serializable>> contextPayloadService;
    private BranchSelectionService branchSelectionService;
    private DocumentVersionServiceImpl documentVersionService;

    public ChannelContentServiceModule() {
        documentsService = new DocumentsServiceImpl();
        workflowsService = new WorkflowServiceImpl();

        contextPayloadService = request -> {
            final HttpSession session = request.getSession();
            return Optional.ofNullable(CmsSessionContext.getContext(session).getContextPayload())
                    .orElse(Collections.emptyMap());
        };

        documentVersionService = new DocumentVersionServiceImpl(HintsUtils::getDocumentWorkflowHints);
        addEventListener(new HippoNamespacesEventListener() {
            @Override
            public void onEvent(final EventIterator events) {
                DocumentTypesService.get().invalidateCache();
            }
        });
    }

    @Override
    protected ObjectMapper createObjectMapper() {
        final ObjectMapper objectMapper = super.createObjectMapper();
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"));
        objectMapper.setTimeZone(TimeZone.getTimeZone("UTC"));
        objectMapper.addMixIn(Campaign.class, CampaignSerializationMixin.class);
        return objectMapper;
    }

    @Override
    protected void doInitialize(final Session session) throws RepositoryException {
        branchSelectionService = createBranchSelectionService(session);

        final BranchSelectionService prev = HippoServiceRegistry.getService(BranchSelectionService.class);
        if (prev != null) {
            log.debug("Remove earlier registered BranchSelectionService");
            HippoServiceRegistry.unregister(prev, BranchSelectionService.class);
        }

        HippoServiceRegistry.register(branchSelectionService, BranchSelectionService.class);

        // First create the branchSelectionService because doInitialize needs it to be non-null
        super.doInitialize(session);
        documentsService.setHintsInspector(createHintsInspector(session));
        documentsService.setBranchingService(createBranchingService(session));
        documentsService.setNodeFieldService(createNodeFieldService(session));
    }

    private NodeFieldService createNodeFieldService(final Session session) {
        return new NodeFieldServiceImpl(session);
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

    private BranchSelectionService createBranchSelectionService(final Session session) throws RepositoryException {
        final String propertyPath = moduleConfigPath + "/branchSelectionServiceClass";
        final String defaultValue = BranchSelectionServiceImpl.class.getName();
        final String className = JcrUtils.getStringProperty(session, propertyPath, defaultValue);
        try {
            final BranchSelectionService branchSelectionService = (BranchSelectionService) Class.forName(className).newInstance();
            return branchSelectionService;
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    protected Object getRestResource(final SessionRequestContextProvider sessionRequestContextProvider) {
        return new ContentResource(
                sessionRequestContextProvider,
                documentsService,
                workflowsService,
                contextPayloadService,
                branchSelectionService,
                documentVersionService
        );
    }

    private abstract static class HippoNamespacesEventListener extends JcrEventListener {

        static final String HIPPO_NAMESPACES = "/hippo:namespaces";

        HippoNamespacesEventListener() {
            super(ALL_EVENTS, HIPPO_NAMESPACES, true, null, null);
        }
    }
}
