/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.api.PropertyRepresentationFactory;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemComponentPropertyRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemComponentRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ItemNotFoundException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ServerErrorException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.UnknownClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.ContainerItemHelper;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstComponentParameters;
import org.hippoecm.hst.pagecomposer.jaxrs.util.PageComposerUtil;
import org.hippoecm.hst.util.ParametersInfoAnnotationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.hst.pagecomposer.jaxrs.model.ParametersInfoProcessor.getPopulatedProperties;
import static org.hippoecm.hst.pagecomposer.jaxrs.util.MissingParametersInfo.defaultMissingParametersInfo;
import static org.hippoecm.hst.pagecomposer.jaxrs.util.PageComposerUtil.executeWithWebsiteClassLoader;

public class ContainerItemComponentServiceImpl implements ContainerItemComponentService {
    private static Logger log = LoggerFactory.getLogger(ContainerItemComponentServiceImpl.class);

    private final PageComposerContextService pageComposerContextService;
    private final ContainerItemHelper containerItemHelper;
    private final List<PropertyRepresentationFactory> propertyPresentationFactories;

    public ContainerItemComponentServiceImpl(final PageComposerContextService pageComposerContextService,
                                             final ContainerItemHelper containerItemHelper,
                                             final List<PropertyRepresentationFactory> propertyPresentationFactories) {
        this.pageComposerContextService = pageComposerContextService;
        this.containerItemHelper = containerItemHelper;
        this.propertyPresentationFactories = propertyPresentationFactories;
    }

    @Override
    public Set<String> getVariants() throws RepositoryException {
        final HstComponentParameters componentParameters = getCurrentHstComponentParameters();
        return componentParameters.getPrefixes();
    }

    @Override
    public ContainerItemComponentRepresentation getVariant(final String variantId, final String localeString) throws ClientException, RepositoryException, ServerErrorException {
        try {
            return represent(getCurrentContainerItem(), getLocale(localeString), variantId);
        } catch (ClassNotFoundException e) {
            throw new ServerErrorException(e);
        }
    }

    @Override
    public Set<String> retainVariants(final Set<String> variants, final long versionStamp) throws RepositoryException {
        Node containerItem = getCurrentContainerItem();
        Set<String> removedVariants = doRetainVariants(containerItem, variants, versionStamp);
        log.info("Removed variants: {}", removedVariants);
        return removedVariants;
    }

    @Override
    public void createVariant(final String variantId, final long versionStamp) throws ClientException, RepositoryException, ServerErrorException {
        try {
            Node containerItem = getCurrentContainerItem();
            HstComponentParameters componentParameters = new HstComponentParameters(containerItem, containerItemHelper);
            if (componentParameters.hasPrefix(variantId)) {
                throw new ClientException("Cannot create variant '" + variantId + "' because it already exists", ClientError.ITEM_EXISTS);
            }
            doCreateVariant(containerItem, componentParameters, variantId);

            componentParameters.save(versionStamp);
            log.info("Variant '{}' created successfully", variantId);
        } catch (IllegalStateException | IllegalArgumentException e) {
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
            final HstComponentParameters componentParameters = getCurrentHstComponentParameters();
            if (!componentParameters.hasPrefix(variantId)) {
                throw new ItemNotFoundException("Cannot delete variantId with id='" + variantId + "'");
            }
            deleteVariant(componentParameters, variantId);

            componentParameters.save(versionStamp);
            log.info("Variant '{}' deleted successfully", variantId);
        } catch (IllegalStateException | IllegalArgumentException e) {
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
            final HstComponentParameters componentParameters = getCurrentHstComponentParameters();
            setParameters(componentParameters, variantId, params);

            componentParameters.save(versionStamp);
            log.info("Parameters for '{}' saved successfully.", variantId);
        } catch (IllegalStateException | IllegalArgumentException e) {
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
            final Node containerItem = getCurrentContainerItem();
            final HstComponentParameters componentParameters = new HstComponentParameters(containerItem, containerItemHelper);
            componentParameters.removePrefix(oldVariantId);
            componentParameters.removePrefix(newVariantId);
            setParameters(componentParameters, newVariantId, params);

            componentParameters.save(versionStamp);
            log.info("Parameters renamed from '{}' to '{}' and saved successfully.", oldVariantId, newVariantId);
        } catch (IllegalStateException | IllegalArgumentException e) {
            logParameterSettingFailed(e);
            throw new UnknownClientException(e.getMessage());
        } catch (RepositoryException e) {
            logParameterSettingFailed(e);
            log.warn("Unable to set the parameters of component", e);
            throw e;
        }
    }

    private HstComponentParameters getCurrentHstComponentParameters() throws RepositoryException {
        final Node containerItem = getCurrentContainerItem();
        return new HstComponentParameters(containerItem, containerItemHelper);
    }

    private Node getCurrentContainerItem() throws RepositoryException {
        return pageComposerContextService.getRequestConfigNode(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT);
    }

    /**
     * Deletes all parameters of a variantId.
     *
     * @param componentParameters the component parameters of the current container item
     * @param variantId           the variantId to remove
     * @throws IllegalStateException when the variantId is the 'default' variantId
     */
    private void deleteVariant(final HstComponentParameters componentParameters, final String variantId)
            throws IllegalStateException {
        if (!componentParameters.removePrefix(variantId)) {
            throw new IllegalStateException("Variant '" + variantId + "' could not be removed");
        }
    }

    private void setParameters(final HstComponentParameters componentParameters,
                               final String prefix,
                               final MultivaluedMap<String, String> parameters) throws IllegalStateException {
        for (String parameterName : parameters.keySet()) {
            // the Force-Client-Host is some 'magic' parameter we do not need to store
            // this check can be removed once in all code, the Force-Client-Host parameter from the queryString
            // has been replaced by a request header.
            if (!"Force-Client-Host".equals(parameterName)) {
                String parameterValue = parameters.getFirst(parameterName);
                componentParameters.setValue(prefix, parameterName, parameterValue);
            }
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
                         final HstComponentParameters componentParameters,
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
                                         final Set<String> variants,
                                         final long versionStamp) throws RepositoryException, IllegalStateException {
        final Set<String> keepVariants = new HashSet<>();
        keepVariants.addAll(variants);

        final HstComponentParameters componentParameters = new HstComponentParameters(containerItem, containerItemHelper);
        final Set<String> removed = new HashSet<>();

        for (String variant : componentParameters.getPrefixes()) {
            if (!keepVariants.contains(variant) && componentParameters.removePrefix(variant)) {
                log.debug("Removed configuration for variant {} of container item {}", variant, containerItem.getIdentifier());
                removed.add(variant);
            }
        }

        if (!removed.isEmpty()) {
            componentParameters.save(versionStamp);
        }
        log.info("Removed variants '{}'", removed.toString());
        return removed;
    }
}
