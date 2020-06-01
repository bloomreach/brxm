/*
 *  Copyright 2010-2017 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ContainerItemComponentRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ErrorStatus;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The REST resource handler for the nodes that are of the type "hst:containeritemcomponent". This is specified using
 * the @Path annotation.
 * <p>
 * The resources handler operates on variants and their parameters.
 */
@Path("/hst:containeritemcomponent/")
public class ContainerItemComponentResource extends AbstractConfigResource {
    private static Logger log = LoggerFactory.getLogger(ContainerItemComponentResource.class);

    private ContainerItemComponentService containerItemComponentService;

    public void setContainerItemComponentService(final ContainerItemComponentService containerItemComponentService) {
        this.containerItemComponentService = containerItemComponentService;
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

        try {
            Set<String> variants = this.containerItemComponentService.getVariants();
            log.info("Available variants: {}", variants);
            return ok("Available variants: ", variants);
        } catch (ClientException e){
            log.warn("Unable to get the parameters of the component", e);
            return clientError("Unable to get the parameters of the component", e.getErrorStatus());
        } catch (RepositoryException e) {
            log.error("Unable to get the parameters of the component", e);
            return error("Unable to get the parameters of the component", ErrorStatus.unknown(e.getMessage()));
        }
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
                                   final @HeaderParam("lastModifiedTimestamp") long versionStamp) {
        try {
            final HashSet<String> retainedVariants = new HashSet<>(Arrays.asList(variants));
            final Set<String> removedVariants = this.containerItemComponentService.retainVariants(retainedVariants, versionStamp);
            return ok("Removed variants:", removedVariants);
        } catch (RepositoryException e) {
            log.error("Unable to cleanup the variants of the component", e);
            return error("Unable to cleanup the variants of the component", ErrorStatus.unknown(e.getMessage()));
        } catch (IllegalStateException e) {
            log.error("Unable to cleanup the variants of the component", e);
            throw new WebApplicationException(e);
        }
    }

    /**
     * Returns all parameters of a specific variant. The names of the parameters will be in the given locale.
     *
     * @param variant      the variant
     * @param localeString the desired locale of the parameter names
     * @return the values and translated names of the parameters of the given variant.
     */
    @GET
    @Path("/{variant}/{locale}")
    @Produces(MediaType.APPLICATION_JSON)
    public ContainerItemComponentRepresentation getVariant(final @PathParam("variant") String variant,
                               final @PathParam("locale") String localeString) {
        try {
            return this.containerItemComponentService.getVariant(variant, localeString);
        } catch (Exception e) {
            log.warn("Failed to retrieve parameters.", e);
        }
        return null;
    }

    /**
     * Saves parameters for the given new variant. If a sub-set of the parameters is provided, only that sub-set is
     * changed and the other parameters are left as-is.
     *
     * If a new variant is provided, the old variant is removed and a new variant is created with the given
     * parameters. This effectively renames the old variant to the new one. Note that in this case, all parameters
     * for the new variant have to be provided.
     *
     * @param variantId the variant to update parameters of, or (if a newVariantId is provided) remove.
     * @param newVariantId the new variant to store parameters for. Can be null, in which case only the given
     *                     variant is updated.
     * @param params     the parameters to store
     * @return whether saving the parameters went successfully or not.
     */
    @PUT
    @Path("/{variantId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response moveAndUpdateVariant(final @PathParam("variantId") String variantId,
                                         final @HeaderParam("Move-To") String  newVariantId,
                                         final @HeaderParam("lastModifiedTimestamp") long versionStamp,
                                         final MultivaluedMap<String, String> params) {
        try {
            if (StringUtils.isEmpty(newVariantId)) {
                this.containerItemComponentService.updateVariant(variantId, versionStamp, params);
                return ok("Parameters for '" + variantId + "' saved successfully.");
            } else {
                this.containerItemComponentService.moveAndUpdateVariant(variantId, newVariantId, versionStamp, params);
                return ok("Parameters renamed from '" + variantId + "' to '" + newVariantId + "' and saved successfully.");
            }
        } catch (ClientException e) {
            return clientError("Unable to set the parameters of component", e.getErrorStatus());
        } catch (RepositoryException e) {
            return error("Unable to set the parameters of component", ErrorStatus.unknown(e.getMessage()));
        }
    }

    /**
     * <p> Creates new variant with all values from the 'default' variant. Note that <b>only</b> the values of 'default'
     * variant are copied that are actually represented by a {@link Parameter} annotation in the corresponding HST
     * component {@link ParametersInfo} interface. If the 'default' does not have a parametervalue configured, then the
     * default value if present from {@link Parameter} for that parametername is used. </p> <p> If the variant already
     * exists, we return a 409 conflict {@link AbstractConfigResource#conflict(String)}. If created, we return {@link
     * AbstractConfigResource#created(String)} </p>
     *
     * @param variantId the variant to create
     * @return If the variant already exists, we return a 409 conflict {@link AbstractConfigResource#conflict(String)}.
     * If created, we return {@link AbstractConfigResource#created(String)}
     */
    @POST
    @Path("/{variantId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createVariant(final @PathParam("variantId") String variantId,
                                  final @HeaderParam("lastModifiedTimestamp") long versionStamp) {

        try {
            this.containerItemComponentService.createVariant(variantId, versionStamp);
            return created("Variant '" + variantId + "' created successfully");
        } catch (ClientException e) {
            return clientError("Could not create variant '" + variantId + "'", e.getErrorStatus());
        } catch (RepositoryException | ServerErrorException e) {
            log.error("Could not create variant '{}'", variantId, e);
            return error("Could not create variant '" + variantId + "'", ErrorStatus.unknown(e.getMessage()));
        }
    }

    @DELETE
    @Path("/{variantId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteVariant(final @PathParam("variantId") String variantId,
                                  final @HeaderParam("lastModifiedTimestamp") long versionStamp) {
        try {
            this.containerItemComponentService.deleteVariant(variantId, versionStamp);
            return ok("Variant '" + variantId + "' deleted successfully");
        } catch (ClientException e) {
            log.warn("Could not delete variant '{}'", variantId, e);
            return clientError("Could not delete variant '" + variantId + "'", e.getErrorStatus());
        } catch (RepositoryException e) {
            log.error("Could not delete variant '{}'", variantId, e);
            final ErrorStatus errorStatus = ErrorStatus.unknown(e.getMessage());
            return error("Could not delete variant '" + variantId + "'", errorStatus);
        }
    }
}
