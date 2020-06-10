/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
 *
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PropertyRepresentationFactory;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemComponentPropertyRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemComponentRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.ContainerItemComponentService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ServerErrorException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.UnknownClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerItemHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.util.PageComposerUtil;
import org.hippoecm.hst.util.ParametersInfoAnnotationUtils;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.WorkflowException;
import org.onehippo.repository.documentworkflow.DocumentWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.pagecomposer.jaxrs.model.ParametersInfoProcessor.getPopulatedProperties;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageUtils.getDocumentWorkflow;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageUtils.getInternalWorkflowSession;
import static org.hippoecm.hst.pagecomposer.jaxrs.services.experiencepage.XPageUtils.validateTimestamp;
import static org.hippoecm.hst.pagecomposer.jaxrs.util.MissingParametersInfo.defaultMissingParametersInfo;
import static org.hippoecm.hst.pagecomposer.jaxrs.util.PageComposerUtil.executeWithWebsiteClassLoader;

public class XPageContainerItemComponentServiceImpl implements ContainerItemComponentService {
    private static Logger log = LoggerFactory.getLogger(XPageContainerItemComponentServiceImpl.class);

    private final PageComposerContextService pageComposerContextService;
    private final ContainerItemHelper containerItemHelper;
    private final List<PropertyRepresentationFactory> propertyPresentationFactories;

    public XPageContainerItemComponentServiceImpl(final PageComposerContextService pageComposerContextService,
                                                  final ContainerItemHelper containerItemHelper,
                                                  final List<PropertyRepresentationFactory> propertyPresentationFactories) {
        this.pageComposerContextService = pageComposerContextService;
        this.containerItemHelper = containerItemHelper;
        this.propertyPresentationFactories = propertyPresentationFactories;
    }

    @Override
    public Set<String> getVariants() throws RepositoryException {
        final Node currentContainerItem = getContainerItem(0L, pageComposerContextService.getRequestContext().getSession());
        final XPageComponentParameters componentParameters = getCurrentHstComponentParameters(currentContainerItem);
        return componentParameters.getPrefixes();
    }

    @Override
    public ContainerItemComponentRepresentation getVariant(final String variantId, final String localeString) throws ClientException, RepositoryException, ServerErrorException {
        try {
            // just use user session
            return represent(getContainerItem(0L, pageComposerContextService.getRequestContext().getSession()), getLocale(localeString), variantId);
        } catch (ClassNotFoundException e) {
            throw new ServerErrorException(e);
        }
    }

    @Override
    public Set<String> retainVariants(final Set<String> variants, final long versionStamp) throws RepositoryException {
        try {
            // use workflow session to write to preview
            DocumentWorkflow documentWorkflow = getDocumentWorkflow((HippoSession) pageComposerContextService.getRequestContext().getSession(),
                    pageComposerContextService);

            final Session internalWorkflowSession = getInternalWorkflowSession(documentWorkflow);
            final Node containerItem = getContainerItem(versionStamp, internalWorkflowSession);

            Set<String> removedVariants = doRetainVariants(containerItem, variants);

            if (!removedVariants.isEmpty()) {
                log.info("Modified variants, save unpublished");
                updateVersionStamp(containerItem);
                documentWorkflow.saveUnpublished();
            }
            log.info("Removed variants: {}", removedVariants);
            return removedVariants;
        } catch (WorkflowException e) {
            throw new UnknownClientException(e.getMessage());
        }
    }


    @Override
    public void createVariant(final String variantId, final long versionStamp) throws ClientException, RepositoryException, ServerErrorException {
        try {
            // use workflow session to write to preview
            DocumentWorkflow documentWorkflow = getDocumentWorkflow((HippoSession) pageComposerContextService.getRequestContext().getSession(),
                    pageComposerContextService);

            final Session internalWorkflowSession = getInternalWorkflowSession(documentWorkflow);
            final Node containerItem = getContainerItem(versionStamp, internalWorkflowSession);

            final XPageComponentParameters componentParameters = new XPageComponentParameters(containerItem, containerItemHelper);
            if (componentParameters.hasPrefix(variantId)) {
                throw new ClientException("Cannot create variant '" + variantId + "' because it already exists", ClientError.ITEM_EXISTS);
            }
            doCreateVariant(containerItem, componentParameters, variantId);

            // trigger the changes on unpublished node to be set
            componentParameters.setNodeChanges();
            updateVersionStamp(containerItem);

            documentWorkflow.saveUnpublished();

            log.info("Variant '{}' created successfully", variantId);
        } catch (IllegalStateException | IllegalArgumentException | WorkflowException e) {
            log.warn("Could not create variant '{}'", variantId, e);
            throw new UnknownClientException("Could not create variant '" + variantId + "'");
        } catch (RepositoryException e) {
            log.error("Unable to create new variant '{}'", variantId, e);
            throw e;
        }
    }

    private Locale getLocale(final String localeString) {
        try {
            return LocaleUtils.toLocale(localeString);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to create Locale from string '{}'. Using default locale", localeString);
            return Locale.getDefault();
        }
    }

    @Override
    public void deleteVariant(final String variantId, final long versionStamp) throws ClientException, RepositoryException {
        try {
            // use workflow session to write to preview
            DocumentWorkflow documentWorkflow = getDocumentWorkflow((HippoSession) pageComposerContextService.getRequestContext().getSession(),
                    pageComposerContextService);

            final Session internalWorkflowSession = getInternalWorkflowSession(documentWorkflow);
            Node containerItem = getContainerItem(versionStamp, internalWorkflowSession);

            final XPageComponentParameters componentParameters = getCurrentHstComponentParameters(containerItem);
            if (!componentParameters.hasPrefix(variantId)) {
                throw new ClientException(String.format("Cannot delete variantId with id='%s'", variantId) , ClientError.ITEM_NOT_FOUND);
            }

            deleteVariant(componentParameters, variantId);

            // trigger the changes on unpublished node to be set
            componentParameters.setNodeChanges();
            updateVersionStamp(containerItem);

            documentWorkflow.saveUnpublished();

            log.info("Variant '{}' deleted successfully", variantId);
        } catch (IllegalStateException | IllegalArgumentException | WorkflowException e) {
            log.warn("Could not delete variantId '{}'", variantId, e);
            throw new UnknownClientException("Could not delete the variantId");
        } catch (RepositoryException e) {
            log.error("Could not delete variantId '{}'", variantId, e);
            throw e;
        }
    }

    @Override
    public void updateVariant(final String variantId, final long versionStamp, final MultivaluedMap<String, String> params) throws ClientException, RepositoryException {
        try {
            // use workflow session to write to preview
            DocumentWorkflow documentWorkflow = getDocumentWorkflow((HippoSession) pageComposerContextService.getRequestContext().getSession(),
                    pageComposerContextService);

            final Session internalWorkflowSession = getInternalWorkflowSession(documentWorkflow);
            Node containerItem = getContainerItem(versionStamp, internalWorkflowSession);

            final XPageComponentParameters componentParameters = getCurrentHstComponentParameters(containerItem);
            setParameters(componentParameters, variantId, params);

            // trigger the changes on unpublished node to be set
            componentParameters.setNodeChanges();
            updateVersionStamp(containerItem);

            documentWorkflow.saveUnpublished();

            log.info("Parameters for '{}' saved successfully.", variantId);
        } catch (IllegalStateException | IllegalArgumentException | WorkflowException e) {
            log.warn("Could not save parameters for variant '{}'", variantId, e);
            throw new UnknownClientException(e.getMessage());
        } catch (RepositoryException e) {
            log.warn("Could not save parameters for variant '{}'", variantId, e);
            throw e;
        }
    }

    @Override
    public void moveAndUpdateVariant(final String oldVariantId, final String newVariantId, final long versionStamp, final MultivaluedMap<String, String> params) throws ClientException, RepositoryException {
        try {
            // use workflow session to write to preview
            DocumentWorkflow documentWorkflow = getDocumentWorkflow((HippoSession) pageComposerContextService.getRequestContext().getSession(),
                    pageComposerContextService);

            final Session internalWorkflowSession = getInternalWorkflowSession(documentWorkflow);

            Node containerItem = getContainerItem(versionStamp, internalWorkflowSession);

            final XPageComponentParameters componentParameters = new XPageComponentParameters(containerItem, containerItemHelper);
            componentParameters.removePrefix(oldVariantId);
            componentParameters.removePrefix(newVariantId);
            setParameters(componentParameters, newVariantId, params);

            // trigger the changes on unpublished node to be set
            componentParameters.setNodeChanges();
            updateVersionStamp(containerItem);

            documentWorkflow.saveUnpublished();

            log.info("Parameters renamed from '{}' to '{}' and saved successfully.", oldVariantId, newVariantId);
        } catch (IllegalStateException | IllegalArgumentException | WorkflowException e) {
            logParameterSettingFailed(e);
            throw new UnknownClientException(e.getMessage());
        } catch (RepositoryException e) {
            logParameterSettingFailed(e);
            log.warn("Unable to set the parameters of component", e);
            throw e;
        }
    }


    private XPageComponentParameters getCurrentHstComponentParameters(final Node containerItem) throws RepositoryException {
        return new XPageComponentParameters(containerItem, containerItemHelper);
    }


    /**
     * Returns the container item validated against the versionStamp of the parent container: if the parent container has
     * a different versionStamp than {@code versionStamp} a {@link ClientException} will be thrown. If the {@code versionStamp}
     * argument is 0, then the versionStamp check is omitted
     */
    private Node getContainerItem(final long versionStamp, final Session session) throws RepositoryException {
        // note this can be a frozen node
        final Node containerItem = session.getNodeByIdentifier(pageComposerContextService.getRequestConfigIdentifier());

        final Node container = containerItem.getParent();
        validateTimestamp(versionStamp, container);
        return containerItem;
    }

    /**
     * Deletes all parameters of a variantId.
     *
     * @param componentParameters the component parameters of the current container item
     * @param variantId           the variantId to remove
     * @throws IllegalStateException when the variantId is the 'default' variantId
     */
    private void deleteVariant(final XPageComponentParameters componentParameters, final String variantId)
            throws IllegalStateException {
        if (!componentParameters.removePrefix(variantId)) {
            throw new IllegalStateException("Variant '" + variantId + "' could not be removed");
        }
    }

    private void setParameters(final XPageComponentParameters componentParameters,
                               final String prefix,
                               final MultivaluedMap<String, String> parameters) throws IllegalStateException {
        for (String parameterName : parameters.keySet()) {
            String parameterValue = parameters.getFirst(parameterName);
            componentParameters.setValue(prefix, parameterName, parameterValue);
        }
    }

    private void logParameterSettingFailed(final Exception e) {
        log.warn("Unable to set the parameters of component", e);
    }

    /**
     * Constructs a component node wrapper
     *
     * @param componentItemNode   JcrNode for a component.
     * @param locale the locale to get localized names, can be null
     * @param prefix the parameter prefix
     * @throws RepositoryException    Thrown if the repository exception occurred during reading of the properties.
     * @throws ClassNotFoundException thrown when this class can't instantiate the component class.
     */
    private ContainerItemComponentRepresentation represent(final Node componentItemNode,
                                                           final Locale locale,
                                                           final String prefix) throws RepositoryException, ClassNotFoundException {
        final String contentPath = getContentPath();


        ParametersInfo parametersInfo = executeWithWebsiteClassLoader(node -> {
            try {
                return ParametersInfoAnnotationUtils.getParametersInfoAnnotation(node);
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        }, componentItemNode);

        if (parametersInfo == null) {
            parametersInfo = defaultMissingParametersInfo;
        }

        List<ContainerItemComponentPropertyRepresentation> properties = getPopulatedProperties(parametersInfo, locale, contentPath, prefix, componentItemNode,
                containerItemHelper, propertyPresentationFactories);

        ContainerItemComponentRepresentation representation = new ContainerItemComponentRepresentation();
        representation.setProperties(properties);
        return representation;
    }

    private String getContentPath() {
        final HstRequestContext requestContext = RequestContextProvider.get();
        if (requestContext != null) {
            return this.pageComposerContextService.getEditingMount().getContentPath();
        }
        return StringUtils.EMPTY;
    }

    /**
     * Creates a new variant. The new variant will consists of all the explicitly configured 'default' parameters and
     * values <b>MERGED</b> with all default annotated parameters (and their values) that are not explicitly configured
     * as 'default' parameter.
     *
     * @param containerItem       the node of the current container item
     * @param componentParameters the component parameters of the current container item
     * @param variantId           the id of the variant to create
     * @throws RepositoryException when something went wrong in the repository
     */
    private void doCreateVariant(final Node containerItem,
                         final XPageComponentParameters componentParameters,
                         final String variantId) throws RepositoryException, IllegalStateException {

        Map<String, String> annotatedParameters = PageComposerUtil.getAnnotatedDefaultValues(containerItem);

        for (String parameterName : annotatedParameters.keySet()) {
            String value = componentParameters.hasDefaultParameter(parameterName) ? componentParameters.getDefaultValue(parameterName) : annotatedParameters.get(parameterName);
            componentParameters.setValue(variantId, parameterName, value);
        }

    }

    /**
     * Removes all variants that are not provided.
     *
     * @param containerItem the container item node
     * @param variants      the variants to keep
     * @return a list of variants that have been removed
     * @throws RepositoryException when some repository exception happened
     */
    private Set<String> doRetainVariants(final Node containerItem,
                                         final Set<String> variants) throws RepositoryException, IllegalStateException {
        final Set<String> keepVariants = new HashSet<>();
        keepVariants.addAll(variants);

        final XPageComponentParameters componentParameters = new XPageComponentParameters(containerItem, containerItemHelper);
        final Set<String> removed = new HashSet<>();

        for (String variant : componentParameters.getPrefixes()) {
            if (!keepVariants.contains(variant) && componentParameters.removePrefix(variant)) {
                log.debug("Removed configuration for variant {} of container item {}", variant, containerItem.getIdentifier());
                removed.add(variant);
            }
        }

        if (!removed.isEmpty()) {
            componentParameters.setNodeChanges();
        }

        log.info("Removed variants '{}'", removed.toString());
        return removed;
    }

    // sets the new timestamp
    private void updateVersionStamp(final Node containerItem) throws RepositoryException {
        containerItem.getParent().setProperty(HstNodeTypes.GENERAL_PROPERTY_LAST_MODIFIED, System.currentTimeMillis());
    }

}
