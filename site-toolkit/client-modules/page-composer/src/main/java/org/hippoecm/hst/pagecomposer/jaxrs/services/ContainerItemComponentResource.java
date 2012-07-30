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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
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

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemComponentRepresentation;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The REST resource handler for the nodes that are of the type "hst:containeritemcomponent". This is specified using the @Path annotation.
 *
 * @version $Id$
 */

@Path("/hst:containeritemcomponent/")
public class ContainerItemComponentResource extends AbstractConfigResource {
    
    public static final String DEFAULT_PARAMETER_PREFIX = "default";
    
    private static Logger log = LoggerFactory.getLogger(ContainerItemComponentResource.class);
    private static final String HST_PARAMETERVALUES = "hst:parametervalues";
    private static final String HST_PARAMETERNAMES = "hst:parameternames";
    private static final String HST_PARAMETERNAMEPREFIXES = "hst:parameternameprefixes";
    private static final String HST_COMPONENTCLASSNAME = "hst:componentclassname";

    @GET
    @Path("/parameters/{locale}/{prefix}")
    @Produces(MediaType.APPLICATION_JSON)
    public ContainerItemComponentRepresentation getParameters(@Context HttpServletRequest servletRequest,
                                          @PathParam("locale") String localeString,
                                          @PathParam("prefix") String prefix) {

        try {
            
            if (prefix == null || prefix.equals(DEFAULT_PARAMETER_PREFIX)) {
                prefix = "";
            }
            
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
    @Path("/parameters/{prefix}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setParameters(@Context HttpServletRequest servletRequest,
                            @PathParam("prefix") String prefix,
                            MultivaluedMap<String, String> params) {
        try {
            return doSetParameters(getRequestConfigNode(getRequestContext(servletRequest)), prefix, params);
        } catch (IllegalStateException e) {
            return error(e.getMessage());
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        } catch (RepositoryException e) {
            log.error("Unable to set the parameters of the component", e);
            throw new WebApplicationException(e);
        }
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
        try {
            Set<String> variants = getVariants(getRequestConfigNode(getRequestContext(servletRequest)));
            return ok("Available variants: ", variants);
        } catch (RepositoryException e) {
            log.error("Unable to get the parameters of the component", e);
            throw new WebApplicationException(e);
        }
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
        try {
            Node containerItem = getRequestConfigNode(getRequestContext(servletRequest));
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
        Set<String> keepVariants = new HashSet<String>();
        keepVariants.addAll(Arrays.asList(variantsToKeep));
        keepVariants.add(DEFAULT_PARAMETER_PREFIX);

        Set<String> allVariants = getVariants(containerItem);

        Set<String> removed = new HashSet<String>();

        for (String variant : allVariants) {
            if (StringUtils.isNotEmpty(variant) && !keepVariants.contains(variant)) {
                log.debug("Removing configuration for variant {} of container item {}", variant, containerItem.getIdentifier());
                doSetParameters(containerItem, variant, null);
                removed.add(variant);
            }
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
     * @param variantid the variant to create
     * @return If the variant already exists, we return a 409 conflict {@link AbstractConfigResource#conflict(String)}. If created,
     * we return {@link AbstractConfigResource#created(String)}
     */
    @POST
    @Path("/variant/{variantid}") 
    @Produces(MediaType.APPLICATION_JSON)
    public Response createVariant(@Context HttpServletRequest servletRequest, @PathParam("variantid") String variantid) {
        Node containerItem = getRequestConfigNode(getRequestContext(servletRequest));
        try {
            Set<String> variants = getVariants(containerItem);
            if (variants.contains(variantid)) {
                return conflict("Cannot create variant '"+variantid+"' as the variant exists already");
            }
            
            return doCreateVariant(containerItem, variantid);
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
    @Path("/variant/delete/{variantid}") 
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteVariant(@Context HttpServletRequest servletRequest, @PathParam("variantid") String variantid) {
        Node containerItem = getRequestConfigNode(getRequestContext(servletRequest));
        try {
            Set<String> variants = getVariants(containerItem);
            if (!variants.contains(variantid)) {
                return conflict("Cannot delete variant '"+variantid+"' because does not exist");
            }
            return doDeleteVariant(containerItem, variantid);
        } catch (IllegalStateException e) {
            log.warn("Could not delete variant ", e);
            return error(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Could not delete variant ", e);
            return error(e.getMessage());
        } catch (RepositoryException e) {
            log.error("Unable to create new variant " + e, e);
            throw new WebApplicationException(e);
        }
    }

    /**
     * Sets the parameters for some prefix. If the prefix is <code>null</code>, then 'default' prefix is assumed.
     * 
     * If <code>params</code> is <code>null</code> it means that the <code>prefix</code> (only if NOT null) will have <b>all</b> its params 
     * removed
     * <p>
     * If the <code>prefix</code> is <code>null</code> or equals to {@link #DEFAULT_PARAMETER_PREFIX}, then only the
     * parameters that are part of <code>params</code> will be set. Already existing default parameters will be untouched. If the
     * <code>prefix</code> is not empty, then all existing parameters for the <code>prefix</code> are removed and reset to
     * <code>params</code>
     * </p>
     * @param node the current container item node
     * @param prefix the variant/prefix to set the parameters for
     * @param params the params to set, or, if <code>null</code>, indicates that the prefix needs to be removed
     * @return the ext result of the action
     * @throws IllegalStateException when the parameters cannot be stored
     * @throws IllegalArgumentException when the input is invalid
     * @throws RepositoryException when some repository exception happened
     */
    Response doSetParameters(Node node, String prefix, MultivaluedMap<String, String> params)
            throws IllegalStateException, IllegalArgumentException, RepositoryException {
        if (prefix == null || prefix.isEmpty()) {
            if(params == null) {
                throw new IllegalArgumentException("Not both prefix and params can be null");
            }
            prefix = DEFAULT_PARAMETER_PREFIX;
        }
        // first fill all the already stored parameters of the jcr Node in hstParameters
        // the KEY = the prefix, and the value a Map of paramatername/parametervalue entries
        Map<String, Map<String, String>> hstParameters;
        try {
            hstParameters = getHstParameters(node, false);
        } catch (IllegalStateException e) {
            return error(e.getMessage());
        }
        // now get the map of parameters for the current 'prefix' as those are the ones we will change
        Map<String, String> prefixedParameters = hstParameters.get(prefix);
        if (prefixedParameters == null) {
            prefixedParameters = new HashMap<String, String>();
            hstParameters.put(prefix, prefixedParameters);
        }
        
        if(params == null) {
            // remove the prefix if it is not DEFAULT
            if (DEFAULT_PARAMETER_PREFIX.equals(prefix)) {
                throw new IllegalArgumentException("Not allowed to remove the 'default' prefix");
            }
            hstParameters.remove(prefix);
        } else {
            if (!DEFAULT_PARAMETER_PREFIX.equals(prefix)) {
                // because not default prefix, we first remove the existing parameters
                prefixedParameters.clear();
            }
            // add / replace parameters for which there are values
            for (String param : params.keySet()) {
                // the FORCE_CLIENT_HOST is some 'magic' parameter we do not need to store
                // this check can be removed once in all code, the FORCE_CLIENT_HOST parameter from the queryString
                // has been replaced by a request header.
                if(!"FORCE_CLIENT_HOST".equals(param)) {
                   prefixedParameters.put(param, params.getFirst(param));
                }
            }
        }
        List<String> prefixes = new ArrayList<String>();
        List<String> names = new ArrayList<String>();
        List<String> values = new ArrayList<String>();
        boolean addPrefixes = false;
        for (Entry<String, Map<String, String>> e : hstParameters.entrySet()) {
            String paramPrefix = e.getKey().equals(DEFAULT_PARAMETER_PREFIX) ? "" : e.getKey();
            for (Entry<String, String> f : e.getValue().entrySet()) {
                String name = f.getKey();
                String value = f.getValue();
                if (!paramPrefix.isEmpty()) {
                    // only add the HST_PARAMETERNAMEPREFIXES property 
                    // if there are actually prefixed name/value pairs
                    addPrefixes = true;
                }
                prefixes.add(paramPrefix);
                names.add(name);
                values.add(value);
            } 
        }
        if (addPrefixes) {
            node.setProperty(HST_PARAMETERNAMEPREFIXES, prefixes.toArray(new String[prefixes.size()]));
        } else if (node.hasProperty(HST_PARAMETERNAMEPREFIXES)) {
            node.getProperty(HST_PARAMETERNAMEPREFIXES).remove();
        }
        if (names.size() == 0) {
            if (node.hasProperty(HST_PARAMETERNAMES)) {
                node.getProperty(HST_PARAMETERNAMES).remove();
            }
            assert values.size() == 0;
            if (node.hasProperty(HST_PARAMETERVALUES)) {
                node.getProperty(HST_PARAMETERVALUES).remove();
            }
        } else {
            node.setProperty(HST_PARAMETERNAMES, names.toArray(new String[names.size()]));
            node.setProperty(HST_PARAMETERVALUES, values.toArray(new String[values.size()]));
        }

        node.getSession().save();

        return ok("Properties saved successfully.", null);
    }

    static Map<String, Map<String, String>> getHstParameters(Node node, boolean defaultParametersOnly) throws RepositoryException, IllegalStateException {
        Map<String, Map<String, String>> hstParameters = new HashMap<String, Map<String, String>>();
        if (node.hasProperty(HST_PARAMETERNAMES) && node.hasProperty(HST_PARAMETERVALUES)) {
            if (node.hasProperty(HST_PARAMETERNAMEPREFIXES)) {
                Value[] paramPrefixes = node.getProperty(HST_PARAMETERNAMEPREFIXES).getValues();
                Value[] paramNames = node.getProperty(HST_PARAMETERNAMES).getValues();
                Value[] paramValues = node.getProperty(HST_PARAMETERVALUES).getValues();
                if (!(paramPrefixes.length == paramNames.length && paramPrefixes.length == paramValues.length)) {
                    log.warn("Parameter names, values and prefixes are are not all of equal length for '{}'", node.getPath());
                    throw new IllegalStateException(HST_PARAMETERNAMEPREFIXES + ", " + HST_PARAMETERNAMES + " and " 
                            + HST_PARAMETERVALUES + " properties do not have the same number of values");
                    
                }
                for (int i = 0; i < paramNames.length; i++) {
                    String paramPrefix = paramPrefixes[i].getString().isEmpty() ? DEFAULT_PARAMETER_PREFIX : paramPrefixes[i].getString();
                    if (defaultParametersOnly && !DEFAULT_PARAMETER_PREFIX.equals(paramPrefix)) {
                        // we are looking only for default parameters: Skip others
                        continue;
                    }
                    Map<String, String> prefixedParameters = hstParameters.get(paramPrefix);
                    if (prefixedParameters == null) {
                        prefixedParameters = new HashMap<String, String>();
                        hstParameters.put(paramPrefix, prefixedParameters);
                    }
                    prefixedParameters.put(paramNames[i].getString(), paramValues[i].getString());
                }
            } else {
                Map<String, String> parameters = new HashMap<String, String>();
                Value[] paramNames = node.getProperty(HST_PARAMETERNAMES).getValues();
                Value[] paramValues = node.getProperty(HST_PARAMETERVALUES).getValues();
                if (paramNames.length != paramValues.length) {
                    log.warn("Parameter names and values are not of equal length for '{}'", node.getPath());
                    throw new IllegalStateException(HST_PARAMETERNAMES + " and " + HST_PARAMETERVALUES 
                            + " properties do not have the same number of values");
                }
                for (int i = 0; i < paramNames.length; i++) {
                    parameters.put(paramNames[i].getString(), paramValues[i].getString());
                }
                hstParameters.put(DEFAULT_PARAMETER_PREFIX, parameters);
            }
        }
        return hstParameters;
    }
    
    /**
     * tries to create a new variant for id <code>variantid</code>. The new variant will consists of all the explicitly 
     * configured 'default' parameters and values <b>MERGED</b> with all default annotated parameters (and their values) that are not explicitly configured as
     * 'default' parameter. 
     * @param containerItem the node of the current container item
     * @param variantid the id of the variant to create
     * @return {@link AbstractConfigResource#created(String)} when the variant with id <code>variantid</code> could be created
     * @throws IllegalStateException when the parameters cannot be stored
     * @throws IllegalArgumentException when the input is invalid
     * @throws RepositoryException when some repository exception happened
     */
    Response doCreateVariant(Node containerItem, String variantid) throws IllegalStateException, IllegalArgumentException, RepositoryException {
        Map<String, String> annotatedParameters = getAnnotatedDefaultValues(containerItem);
        Map<String, String> configuredDefaultValues = getConfiguredDefaultValues(containerItem);
        
        MultivaluedMap<String, String> paramsForNewVariant = new MetadataMap<String, String>();
        for(String key : annotatedParameters.keySet()) {
            if (configuredDefaultValues.containsKey(key)) {
                paramsForNewVariant.add(key, configuredDefaultValues.get(key));
            } else {
                paramsForNewVariant.add(key, annotatedParameters.get(key));
            }
        }
        doSetParameters(containerItem, variantid, paramsForNewVariant);
        return created("Variant '"+variantid+"' created sucessfully");
    }
    
    Response doDeleteVariant(Node containerItem, String variantid) throws IllegalStateException, IllegalArgumentException, RepositoryException {
        doSetParameters(containerItem, variantid, null);
        return ok("Variant '"+variantid+"' deleted succesfully");
    }
    
    
    static Set<String> getVariants(Node containerItem) throws RepositoryException {
        Map<String, Map<String, String>> hstParameters = getHstParameters(containerItem, false);
        return hstParameters.keySet();
    }
    
    
    /**
     * @param node the current container item node
     * @return the Map<String,String> of all configured parameternames & values, without taking annotated  defaults 
     * or names into account. If no default configured values are present, an Empty map is returned
     * @throws IllegalStateException when the parameters cannot be stored
     * @throws RepositoryException when some repository exception happened
     */
    static Map<String, String> getConfiguredDefaultValues(Node node) throws IllegalStateException, RepositoryException {
        Map<String, Map<String, String>> hstParameters = getHstParameters(node, true);
        Map<String, String> defaultConfiguredParameters = hstParameters.get(DEFAULT_PARAMETER_PREFIX);
        if (defaultConfiguredParameters == null) {
            return Collections.emptyMap();
        }
        return defaultConfiguredParameters;
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
