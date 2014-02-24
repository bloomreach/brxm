/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.LocaleUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemComponentPropertyRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemComponentRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ParametersInfoProcessor;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstComponentParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The REST resource handler for the nodes that are of the type "hst:containeritemcomponent".
 * This is specified using the @Path annotation.
 *
 * The resources handler operates on variants and their parameters.
 */
@Path("/hst:containeritemcomponent/")
public class ContainerItemComponentResource extends AbstractConfigResource {
    
    private static Logger log = LoggerFactory.getLogger(ContainerItemComponentResource.class);
    private static final String HST_COMPONENTCLASSNAME = "hst:componentclassname";

    private ParametersInfoProcessor processor;

    public void setProcessor(final ParametersInfoProcessor processor) {
        this.processor = processor;
    }

    /**
     * Returns all variants of this container item component. Note that the returned list might contain variants that
     * are not available any more in the variants store.
     *
     * @return all the configured unique variants (i.e. parameter prefixes) currently configured for this component
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVariants() {
        final Node containerItem = getPageComposerContextService().getRequestConfigNode(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT);
        try {
            Set<String> variants = doGetVariants(containerItem);
            log.info("Available variants: {}", variants);
            return ok("Available variants: ", variants);
        } catch (RepositoryException e) {
            log.error("Unable to get the parameters of the component", e);
            throw new WebApplicationException(e);
        }
    }

    protected Set<String> doGetVariants(final Node containerItem) throws RepositoryException {
        HstComponentParameters componentParameters = new HstComponentParameters(containerItem);
        return componentParameters.getPrefixes();
    }

    /**
     * Retains all given variants. All other variants from this container item are removed.
     *
     * @param variants the variants to keep
     * @return the variants that have been removed from this container item
     */
    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response retainVariants(final String[] variants,
                                   final @HeaderParam("lastModifiedTimestamp") long lastModifiedTimestamp) {
        Node containerItem = getPageComposerContextService().getRequestConfigNode(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT);
        try {
            Set<String> removedVariants = doRetainVariants(containerItem, variants, lastModifiedTimestamp);
            log.info("Removed variants: {}", removedVariants.toString());
            return ok("Removed variants:", removedVariants);
        } catch (RepositoryException e) {
            log.error("Unable to cleanup the variants of the component", e);
            throw new WebApplicationException(e);
        } catch (IllegalStateException e) {
            log.error("Unable to cleanup the variants of the component", e);
            throw new WebApplicationException(e);
        }

    }

    /**
     * Removes all variants that are not provided.
     *
     * @param containerItem the container item node
     * @param variants the variants to keep
     * @return a list of variants that have been removed
     * @throws RepositoryException when some repository exception happened
     */
    private Set<String> doRetainVariants(final Node containerItem,
                                         final String[] variants,
                                         final long lastModifiedTimestamp) throws RepositoryException, IllegalStateException {
        final Set<String> keepVariants = new HashSet<>();
        keepVariants.addAll(Arrays.asList(variants));

        final HstComponentParameters componentParameters = new HstComponentParameters(containerItem);
        final Set<String> removed = new HashSet<>();

        for (String variant : componentParameters.getPrefixes()) {
            if (!keepVariants.contains(variant) && componentParameters.removePrefix(variant)) {
                log.debug("Removed configuration for variant {} of container item {}", variant, containerItem.getIdentifier());
                removed.add(variant);
            }
        }

        if (!removed.isEmpty()) {
            componentParameters.save(lastModifiedTimestamp);
        }
        log.info("Removed variants '{}'", removed.toString());
        return removed;
    }

    /**
     * Returns all parameters of a specific variant. The names of the parameters will be in the given locale.
     *
     * @param variant the variant
     * @param localeString the desired locale of the parameter names
     * @return the values and translated names of the parameters of the given variant.
     */
    @GET
    @Path("/{variant}/{locale}")
    @Produces(MediaType.APPLICATION_JSON)
    public ContainerItemComponentRepresentation getParameters(final @PathParam("variant") String variant,
                                                              final @PathParam("locale") String localeString) {
        try {
            Locale locale;
            try {
                locale = LocaleUtils.toLocale(localeString);
            } catch (IllegalArgumentException e) {
                log.warn("Failed to create Locale from string '{}'. Using default locale", localeString);
                locale = Locale.getDefault();
            }
            return doGetParameters(getPageComposerContextService().getRequestConfigNode(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT), locale, variant);
        } catch (Exception e) {
            log.warn("Failed to retrieve parameters.", e);
        }
        return null;
    }
    
    ContainerItemComponentRepresentation doGetParameters(final Node node,
                                                         final Locale locale,
                                                         final String prefix) throws RepositoryException, ClassNotFoundException {
        return represent(node, locale, prefix);
    }


    /**
     * Constructs a component node wrapper
     *
     * @param node JcrNode for a component.
     * @param locale the locale to get localized names, can be null
     * @param prefix  the parameter prefix
     * @throws RepositoryException    Thrown if the repository exception occurred during reading of the properties.
     * @throws ClassNotFoundException thrown when this class can't instantiate the component class.
     */
    private ContainerItemComponentRepresentation represent(final Node node,
                                                           final Locale locale,
                                                           final String prefix) throws RepositoryException, ClassNotFoundException {
        List<ContainerItemComponentPropertyRepresentation> properties= new ArrayList<>();

        HstComponentParameters componentParameters = new HstComponentParameters(node);

        //Get the properties via annotation on the component class
        String componentClassName = null;
        if (node.hasProperty(HST_COMPONENTCLASSNAME)) {
            componentClassName = node.getProperty(HST_COMPONENTCLASSNAME).getString();
        }
        if (componentClassName != null) {
            Class<?> componentClass = Thread.currentThread().getContextClassLoader().loadClass(componentClassName);
            if (componentClass.isAnnotationPresent(ParametersInfo.class)) {
                ParametersInfo parametersInfo = componentClass.getAnnotation(ParametersInfo.class);
                String contentPath = "";
                final HstRequestContext requestContext = RequestContextProvider.get();
                if (requestContext != null) {
                    contentPath = getPageComposerContextService().getEditingMount().getContentPath();
                }
                properties = processor.getProperties(parametersInfo, locale, contentPath);
            }
            if (componentParameters.hasPrefix(prefix)) {
                for (ContainerItemComponentPropertyRepresentation prop : properties) {
                    String value = componentParameters.getValue(prefix, prop.getName());
                    if (value != null && !value.isEmpty()) {
                        prop.setValue(value);
                    }
                }
            }
        }

        ContainerItemComponentRepresentation representation = new ContainerItemComponentRepresentation();
        representation.setProperties(properties);
        return representation;
    }


    /**
     * Saves parameters for the given variant.
     *
     * @param variant the variant to store parameters for
     * @param params the parameters to store
     * @return whether saving the parameters went successfully or not.
     */
    @POST
    @Path("/{variant}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setParameters(final @PathParam("variant") String variant,
                                  final @HeaderParam("lastModifiedTimestamp") long lastModifiedTimestamp,
                                  final MultivaluedMap<String, String> params) {
        try {
            final Node containerItem = getPageComposerContextService().getRequestConfigNode(HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT);
            HstComponentParameters componentParameters = new HstComponentParameters(containerItem);
            doSetParameters(componentParameters, variant, params, lastModifiedTimestamp);
            log.info("Parameters for '{}' saved successfully.", variant);
            return ok("Parameters for '" + variant + "' saved successfully.", null);
        } catch (IllegalStateException e) {
            log.warn("Could not save parameters for variant '{}'", variant, e);
            return error(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Could not save parameters for variant '{}'", variant, e);
            return error(e.getMessage());
        } catch (RepositoryException e) {
            log.warn("Could not save parameters for variant '{}'", variant, e);
            throw new WebApplicationException(e);
        }
    }

    /**
     * Saves parameters for the given new variant, and also removes the old variant. This effectively renames the
     * old variant to the new one.
     *
     * @param oldVariant the old variant to remove
     * @param newVariant the new variant to store parameters for
     * @param params the parameters to store
     * @return whether saving the parameters went successfully or not.
     */
    @POST
    @Path("/{oldVariant}/rename/{newVariant}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setParametersAndRenameVariant(final @PathParam("oldVariant") String oldVariant,
                                                  final @PathParam("newVariant") String newVariant,
                                                  final @HeaderParam("lastModifiedTimestamp") long lastModifiedTimestamp,
                                                  final MultivaluedMap<String, String> params) {
        try {
            final Node containerItem = getPageComposerContextService().getRequestConfigNode(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT);
            HstComponentParameters componentParameters = new HstComponentParameters(containerItem);
            componentParameters.removePrefix(oldVariant);
            doSetParameters(componentParameters, newVariant, params, lastModifiedTimestamp);
            log.info("Parameters renamed from '{}' to '{}' and saved successfully.", oldVariant, newVariant);
            return ok("Parameters renamed from '" + oldVariant + "' to '" + newVariant + "' and saved successfully.", null);
        } catch (IllegalStateException e) {
            logParameterSettingFailed(e);
            return error(e.getMessage());
        } catch (IllegalArgumentException e) {
            logParameterSettingFailed(e);
            return error(e.getMessage());
        } catch (RepositoryException e) {
            logParameterSettingFailed(e);
            log.warn("Unable to set the parameters of component", e);
            throw new WebApplicationException(e);
        }
    }

    private void logParameterSettingFailed(final Exception e) {
        log.warn("Unable to set the parameters of component", e);
    }

    void doSetParameters(final HstComponentParameters componentParameters,
                         final String prefix,
                         final MultivaluedMap<String, String> parameters,
                         final long lastModifiedTimestamp) throws RepositoryException, IllegalStateException {
        componentParameters.removePrefix(prefix);
        for (String parameterName : parameters.keySet()) {
            // the FORCE_CLIENT_HOST is some 'magic' parameter we do not need to store
            // this check can be removed once in all code, the FORCE_CLIENT_HOST parameter from the queryString
            // has been replaced by a request header.
            if(!"FORCE_CLIENT_HOST".equals(parameterName)) {
                String parameterValue = parameters.getFirst(parameterName);
                componentParameters.setValue(prefix, parameterName, parameterValue);
            }
        }
        componentParameters.save(lastModifiedTimestamp);
        log.info("Succesfully set componentParameters.");
    }

    /**
     * <p>
     * Creates new variant with all values from the 'default' variant. Note that <b>only</b> the values of 'default'
     * variant are copied that are actually represented by a {@link Parameter} annotation
     * in the corresponding HST component {@link ParametersInfo} interface. If the 'default' does not have a
     * parametervalue configured, then the default value if present from {@link Parameter} for that parametername is used.
     * </p>
     * <p>
     * If the variant already exists, we return a 409 conflict {@link AbstractConfigResource#conflict(String)}. If created,
     * we return {@link AbstractConfigResource#created(String)}
     * </p>
     * @param variant the variant to create
     * @return If the variant already exists, we return a 409 conflict {@link AbstractConfigResource#conflict(String)}. If created,
     * we return {@link AbstractConfigResource#created(String)}
     */
    @POST
    @Path("/{variant}/default")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createVariant(final @PathParam("variant") String variant,
                                  final @HeaderParam("lastModifiedTimestamp") long lastModifiedTimestamp) {
        Node containerItem = getPageComposerContextService().getRequestConfigNode(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT);
        try {
            HstComponentParameters componentParameters = new HstComponentParameters(containerItem);
            if (componentParameters.hasPrefix(variant)) {
                return conflict("Cannot create variant '" + variant + "' because it already exists");
            }
            doCreateVariant(containerItem, componentParameters, variant, lastModifiedTimestamp);
            log.info("Variant '{}' created successfully", variant);
            return created("Variant '" + variant + "' created successfully");
        } catch (IllegalStateException e) {
            log.warn("Could not create variant '{}'", variant, e);
            return error(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Could not create variant '{}'", variant, e);
            return error(e.getMessage());
        } catch (RepositoryException e) {
            log.error("Unable to create new variant '{}'", variant, e);
            throw new WebApplicationException(e);
        }
    }

    /**
     * Creates a new variant. The new variant will consists of all the explicitly configured 'default' parameters and
     * values <b>MERGED</b> with all default annotated parameters (and their values) that are not explicitly configured
     * as 'default' parameter.
     *
     * @param containerItem the node of the current container item
     * @param componentParameters the component parameters of the current container item
     * @param variantId the id of the variant to create
     *
     * @throws RepositoryException when something went wrong in the repository
     */
    void doCreateVariant(final Node containerItem,
                         final HstComponentParameters componentParameters,
                         final String variantId,
                         final long lastModifiedTimestamp) throws RepositoryException, IllegalStateException {
        Map<String, String> annotatedParameters = getAnnotatedDefaultValues(containerItem);

        for(String parameterName : annotatedParameters.keySet()) {
            String value = componentParameters.hasDefaultParameter(parameterName) ? componentParameters.getDefaultValue(parameterName) : annotatedParameters.get(parameterName);
            componentParameters.setValue(variantId, parameterName, value);
        }
        componentParameters.save(lastModifiedTimestamp);
    }

    @DELETE
    @Path("/{variant}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteVariant(final @PathParam("variant") String variant,
                                  final @HeaderParam("lastModifiedTimestamp") long lastModifiedTimestamp) {
        Node containerItem = getPageComposerContextService().getRequestConfigNode(HstNodeTypes.NODETYPE_HST_CONTAINERITEMCOMPONENT);
        try {
            HstComponentParameters componentParameters = new HstComponentParameters(containerItem);
            if (!componentParameters.hasPrefix(variant)) {
                return conflict("Cannot delete variant '" + variant + "' because it does not exist");
            }
            doDeleteVariant(componentParameters, variant, lastModifiedTimestamp);

            log.info("Variant '{}' deleted successfully", variant);
            return ok("Variant '" + variant + "' deleted successfully");
        } catch (IllegalStateException e) {
            log.warn("Could not delete variant '{}'", variant,  e);
            return error(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Could not delete variant '{}'", variant, e);
            return error(e.getMessage());
        } catch (RepositoryException e) {
            log.error("Could not delete variant '{}'", variant, e);
            throw new WebApplicationException(e);
        }
    }

    /**
     * Deletes all parameters of a variant.
     *
     * @param componentParameters the component parameters of the current container item
     * @param variantId the variant to remove
     * @throws IllegalArgumentException when the variant is the 'default' variant
     * @throws RepositoryException
     */
    void doDeleteVariant(final HstComponentParameters componentParameters,
                         final String variantId, long lastModifiedTimestamp)
            throws IllegalArgumentException, RepositoryException, IllegalStateException {
        if (!componentParameters.removePrefix(variantId)) {
            throw new IllegalStateException("Variant '" + variantId + "' could not be removed");
        }
        componentParameters.save(lastModifiedTimestamp);
    }

    /**
     * Returns the {@link Map} of annotated parameter name as key and annotated default value as value. Parameters with
     * empty default value are also represented in the returned map.
     * @param node the current container item node
     * @return the Map of all {@link Parameter} names and their default value
     */
    static Map<String, String> getAnnotatedDefaultValues(Node node) {
        try {
            String componentClassName = null;
            if (node.hasProperty(HST_COMPONENTCLASSNAME)) {
                componentClassName = node.getProperty(HST_COMPONENTCLASSNAME).getString();
            }

            if (componentClassName != null) {
                Class<?> componentClass = Thread.currentThread().getContextClassLoader().loadClass(componentClassName);
                if (componentClass.isAnnotationPresent(ParametersInfo.class)) {
                    ParametersInfo parametersInfo = componentClass.getAnnotation(ParametersInfo.class);
                    Class<?> classType = parametersInfo.type();
                    if (classType == null) {
                        return Collections.emptyMap();
                    }
                    Map<String, String> result = new HashMap<String, String>();
                    for (Method method : classType.getMethods()) {
                        if (method.isAnnotationPresent(Parameter.class)) {
                            Parameter annotation = method.getAnnotation(Parameter.class);
                            result.put(annotation.name(), annotation.defaultValue());
                        }
                    }
                    return result;
                }
            }
        } catch (RepositoryException e) {
            log.error("Failed to load annotated default values", e);
        } catch (ClassNotFoundException e) {
            log.error("Failed to load annotated default values", e);
        }
        return Collections.emptyMap();
    }
}
