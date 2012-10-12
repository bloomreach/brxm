/*
 *  Copyright 2010-2012 Hippo.
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemComponentRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstComponentParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The REST resource handler for the nodes that are of the type "hst:containeritemcomponent". This is specified using the @Path annotation.
 *
 * @version $Id$
 */

@Path("/hst:containeritemcomponent/")
public class ContainerItemComponentResource extends AbstractConfigResource {
    
    private static Logger log = LoggerFactory.getLogger(ContainerItemComponentResource.class);
    private static final String HST_COMPONENTCLASSNAME = "hst:componentclassname";

    @GET
    @Path("/parameters/{locale}/{prefix}")
    @Produces(MediaType.APPLICATION_JSON)
    public ContainerItemComponentRepresentation getParameters(@Context HttpServletRequest servletRequest,
                                          @PathParam("locale") String localeString,
                                          @PathParam("prefix") String prefix) {
        try {
            HstRequestContext requestContext = getRequestContext(servletRequest);
            Locale locale = null;
            if (localeString != null) {
                String[] localeParts = localeString.split("_");
                if (localeParts.length == 3) {
                    locale = new Locale(localeParts[0], localeParts[1], localeParts[2]);
                } else if (localeParts.length == 2) {
                    locale = new Locale(localeParts[0], localeParts[1]);
                } else if (localeParts.length == 1) {
                    locale = new Locale(localeParts[0]);
                } else {
                    log.warn("Failed to create Locale from string '{}'", localeString);
                }
            }
            String currentMountCanonicalContentPath = getCurrentMountCanonicalContentPath(servletRequest);
            return doGetParameters(getRequestConfigNode(requestContext), locale, prefix, currentMountCanonicalContentPath);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Failed to retrieve parameters.", e);
            } else {
                log.warn("Failed to retrieve parameters . {}", e.toString());
            }
        }
        return null;
    }
    
    ContainerItemComponentRepresentation doGetParameters(Node node, Locale locale, String prefix, String currentMountCanonicalContentPath) throws RepositoryException, ClassNotFoundException {
        return new ContainerItemComponentRepresentation().represents(node, locale, prefix, currentMountCanonicalContentPath);
    }

    @POST
    @Path("/parameters/{variant}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setParameters(@Context HttpServletRequest servletRequest,
                            @PathParam("variant") String variant,
                            MultivaluedMap<String, String> params) {
        try {
            final Node containerItem = getRequestConfigNode(getRequestContext(servletRequest));
            HstComponentParameters componentParameters = new HstComponentParameters(containerItem);
            doSetParameters(componentParameters, variant, params);
            return ok("Parameters for '" + variant + "' saved successfully.", null);
        } catch (IllegalStateException e) {
            return error(e.getMessage());
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        } catch (RepositoryException e) {
            log.error("Unable to set the parameters of component", e);
            throw new WebApplicationException(e);
        }
    }

    @POST
    @Path("/parameters/{oldVariant}/{newVariant}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setParametersAndRenameVariant(@Context HttpServletRequest servletRequest,
                                  @PathParam("oldVariant") String oldVariant,
                                  @PathParam("newVariant") String newVariant,
                                  MultivaluedMap<String, String> params) {
        try {
            final Node containerItem = getRequestConfigNode(getRequestContext(servletRequest));
            HstComponentParameters componentParameters = new HstComponentParameters(containerItem);
            componentParameters.removePrefix(oldVariant);
            doSetParameters(componentParameters, newVariant, params);
            return ok("Parameters renamed from '" + oldVariant + "' to '" + newVariant + "' and saved successfully.", null);
        } catch (IllegalStateException e) {
            return error(e.getMessage());
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        } catch (RepositoryException e) {
            log.error("Unable to set the parameters of component", e);
            throw new WebApplicationException(e);
        }
    }

    void doSetParameters(HstComponentParameters componentParameters, String prefix, MultivaluedMap<String, String> parameters) throws RepositoryException {
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
        componentParameters.save();
    }

   /**
    * Note that the variants returned might contain variants that are not available any more in the variants store : Just
    * all configured variant ids for the current component are returned
    * @param servletRequest the current servlet request
    * @return all the configured unique variants (prefixes) currently configured for this component
    */
    @GET 
    @Path("/variants") 
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVariants(@Context HttpServletRequest servletRequest) {
        final Node containerItem = getRequestConfigNode(getRequestContext(servletRequest));
        try {
            Set<String> variants = doGetVariants(containerItem);
            return ok("Available variants: ", variants);
        } catch (RepositoryException e) {
            log.error("Unable to get the parameters of the component", e);
            throw new WebApplicationException(e);
        }
    }

    static Set<String> doGetVariants(final Node containerItem) throws RepositoryException {
        HstComponentParameters componentParameters = new HstComponentParameters(containerItem);
        return componentParameters.getPrefixes();
    }

    /**
     * Removes all variants from this container item that are not provided.
     *
     * @param servletRequest the current servlet request
     * @param variantsToKeep a list of variants to keep for this container item
     * @return the variants that have been removed from this container item
     */
    @POST
    @Path("/variants")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response cleanupVariants(@Context HttpServletRequest servletRequest, String[] variantsToKeep) {
        Node containerItem = getRequestConfigNode(getRequestContext(servletRequest));
        try {
            Set<String> removedVariants = doCleanupVariants(containerItem, variantsToKeep);
            return ok("Removed variants:", removedVariants);
        } catch (RepositoryException e) {
            log.error("Unable to cleanup the variants of the component", e);
            throw new WebApplicationException(e);
        }

    }

    /**
     * Removes all variants that are not provided.
     *
     * @param containerItem the container item node
     * @param variantsToKeep the list of variants to keep
     * @return a list of variants that have been removed
     * @throws RepositoryException when some repository exception happened
     */
    private Set<String> doCleanupVariants(final Node containerItem, final String[] variantsToKeep) throws RepositoryException {
        final Set<String> keepVariants = new HashSet<String>();
        keepVariants.addAll(Arrays.asList(variantsToKeep));

        final HstComponentParameters componentParameters = new HstComponentParameters(containerItem);
        final Set<String> removed = new HashSet<String>();

        for (String variant : componentParameters.getPrefixes()) {
            if (!keepVariants.contains(variant) && componentParameters.removePrefix(variant)) {
                log.debug("Removed configuration for variant {} of container item {}", variant, containerItem.getIdentifier());
                removed.add(variant);
            }
        }
        if (!removed.isEmpty()) {
            componentParameters.save();
        }

        return removed;
    }

    /**
     * <p>
     * Creates new variant / prefix for <code>variantid</code> with values for the 'default' variant copied. Note that
     * <b>only</b> the values of 'default' are copied that are actually represented by a {@link Parameter} annotation
     * in the corresponding hst component {@link ParametersInfo} interface. If the 'default' does not have a parametervalue
     * configured, then, the default value if present from {@link Parameter} for that parametername is used
     * </p>
     * <p>
     * If the variant already exists, we return a 409 conflict {@link AbstractConfigResource#conflict(String)}. If created,
     * we return {@link AbstractConfigResource#created(String)}
     * </p>
     * @param servletRequest the current servlet request
     * @param variantId the variant to create
     * @return If the variant already exists, we return a 409 conflict {@link AbstractConfigResource#conflict(String)}. If created,
     * we return {@link AbstractConfigResource#created(String)}
     */
    @POST
    @Path("/variant/{variantId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createVariant(@Context HttpServletRequest servletRequest, @PathParam("variantId") String variantId) {
        Node containerItem = getRequestConfigNode(getRequestContext(servletRequest));
        try {
            HstComponentParameters componentParameters = new HstComponentParameters(containerItem);
            if (componentParameters.hasPrefix(variantId)) {
                return conflict("Cannot create variant '" + variantId + "' because it already exists");
            }
            doCreateVariant(containerItem, componentParameters, variantId);
            return created("Variant '"+ variantId + "' created successfully");
        } catch (IllegalStateException e) {
            log.warn("Could not create variant ", e);
            return error(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Could not create variant ", e);
            return error(e.getMessage());
        } catch (RepositoryException e) {
            log.error("Unable to create new variant " + e, e);
            throw new WebApplicationException(e);
        }
    }

    @POST
    @Path("/variant/rename/{oldVariantId}/{newVariantId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response renameVariant(@Context HttpServletRequest servletRequest, @PathParam("oldVariantId") String oldVariantId, @PathParam("newVariantId") String newVariantId) {
        Node containerItem = getRequestConfigNode(getRequestContext(servletRequest));
        try {
            HstComponentParameters componentParameters = new HstComponentParameters(containerItem);
            if (!componentParameters.hasPrefix(oldVariantId)) {
                return conflict("Cannot rename variant '" + oldVariantId + "' because it does not exist");
            }
            doRenameVariant(componentParameters, oldVariantId, newVariantId);
            return ok("Renamed variant '" + oldVariantId + "' to '" + newVariantId + "'");
        } catch (IllegalStateException e) {
            log.warn("Could not rename variant '" + oldVariantId + "' to '" + newVariantId + "'", e);
            return error(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Could not rename variant '" + oldVariantId + "' to '" + newVariantId + "'", e);
            return error(e.getMessage());
        } catch (RepositoryException e) {
            log.error("Could not rename variant '" + oldVariantId + "' to '" + newVariantId + "'", e);
            throw new WebApplicationException(e);
        }
    }

    @POST
    @Path("/variant/delete/{variantId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteVariant(@Context HttpServletRequest servletRequest, @PathParam("variantId") String variantId) {
        Node containerItem = getRequestConfigNode(getRequestContext(servletRequest));
        try {
            HstComponentParameters componentParameters = new HstComponentParameters(containerItem);
            if (!componentParameters.hasPrefix(variantId)) {
                return conflict("Cannot delete variant '" + variantId + "' because it does not exist");
            }
            doDeleteVariant(componentParameters, variantId);
            return ok("Variant '" + variantId + "' deleted successfully");
        } catch (IllegalStateException e) {
            log.warn("Could not delete variant '" + variantId + "'", e);
            return error(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Could not delete variant '" + variantId + "'", e);
            return error(e.getMessage());
        } catch (RepositoryException e) {
            log.error("Unable to create new variant '" + variantId + "'", e);
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
    void doCreateVariant(Node containerItem, HstComponentParameters componentParameters, String variantId) throws RepositoryException {
        Map<String, String> annotatedParameters = getAnnotatedDefaultValues(containerItem);

        for(String parameterName : annotatedParameters.keySet()) {
            String value = componentParameters.hasDefaultParameter(parameterName) ? componentParameters.getDefaultValue(parameterName) : annotatedParameters.get(parameterName);
            componentParameters.setValue(variantId, parameterName, value);
        }
        componentParameters.save();
    }

    /**
     * Renames a variant.
     *
     * @param componentParameters the component parameters of the current container item
     * @param oldVariantId the old variant
     * @param newVariantId the new variant
     * @throws IllegalArgumentException when either the old or new variant is the 'default' variant.
     * @throws RepositoryException when something went wrong in the repository
     */
    void doRenameVariant(HstComponentParameters componentParameters, String oldVariantId, String newVariantId) throws IllegalArgumentException, RepositoryException {
        componentParameters.renamePrefix(oldVariantId, newVariantId);
        componentParameters.save();
    }

    /**
     * Deletes all parameters of a variant.
     *
     * @param componentParameters the component parameters of the current container item
     * @param variantId the variant to remove
     * @throws IllegalArgumentException when the variant is the 'default' variant
     * @throws RepositoryException
     */
    void doDeleteVariant(HstComponentParameters componentParameters, String variantId) throws IllegalArgumentException, RepositoryException {
        if (!componentParameters.removePrefix(variantId)) {
            throw new IllegalStateException("Variant '" + variantId + "' could not be removed");
        }
        componentParameters.save();
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
